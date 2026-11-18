package top.pingwcy.wifianalyzer.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import top.pingwcy.wifianalyzer.R
import top.pingwcy.wifianalyzer.data.AllowedChannelsProvider
import top.pingwcy.wifianalyzer.data.BandAvailability
import top.pingwcy.wifianalyzer.data.SettingsRepositoryImpl
import top.pingwcy.wifianalyzer.data.WifiBandSupport
import top.pingwcy.wifianalyzer.data.WifiScanner
import top.pingwcy.wifianalyzer.domain.AppLanguage
import top.pingwcy.wifianalyzer.domain.SettingsRepository
import top.pingwcy.wifianalyzer.domain.SortMode
import top.pingwcy.wifianalyzer.domain.WifiRepository
import top.pingwcy.wifianalyzer.model.BandFilter
import top.pingwcy.wifianalyzer.model.RssiSample
import top.pingwcy.wifianalyzer.model.WifiData
import top.pingwcy.wifianalyzer.model.groupKey

data class WifiUiState(
    val wifiList: List<WifiData> = emptyList(),
    val isScanning: Boolean = false,
    val errorResId: Int? = null,    // 错误消息资源 ID（null 表示无错误）
    val errorArg: String? = null,   // 错误消息格式化参数（可选）
    val bandFilter: BandFilter = BandFilter.ALL,
    val expandedKeys: Set<String> = emptySet(),  // 已展开的分组 key
    val sortMode: SortMode = SortMode.GROUPED,   // 默认聚合视图
    val autoScanEnabled: Boolean = true,         // 默认开启自动扫描
    val appLanguage: AppLanguage = AppLanguage.SYSTEM,
    val continuousScanEnabled: Boolean = false,  // 默认关闭持续扫描
    val rssiHistory: Map<String, List<RssiSample>> = emptyMap(), // bssid -> 信号历史
    val allowedChannelsByBand: Map<String, Set<Int>> = emptyMap(), // 频段 -> 监管域允许的信道集合
    val bandAvailabilityByBand: Map<String, BandAvailability> = emptyMap(), // 频段 -> 可用性
)

class WifiViewModel(
    private val appContext: Context,
    private val repository: WifiRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WifiUiState())
    val uiState: StateFlow<WifiUiState> = _uiState.asStateFlow()

    // 自动扫描只应在应用生命周期内触发一次，避免从详情/信道页返回时重复扫描。
    private var settingsLoaded = false
    private var autoScanHandled = false
    private var permissionGranted = false
    private var continuousScanJob: Job? = null

    init {
        viewModelScope.launch {
            settingsRepository.autoScanEnabled.collect { enabled ->
                _uiState.value = _uiState.value.copy(autoScanEnabled = enabled)
                settingsLoaded = true
                maybeTriggerAutoScan()
            }
        }
        viewModelScope.launch {
            settingsRepository.appLanguage.collect { language ->
                _uiState.value = _uiState.value.copy(appLanguage = language)
            }
        }
        viewModelScope.launch {
            settingsRepository.continuousScanEnabled.collect { enabled ->
                _uiState.value = _uiState.value.copy(continuousScanEnabled = enabled)
                if (enabled) startContinuousScan() else stopContinuousScan()
            }
        }
        viewModelScope.launch {
            settingsRepository.sortMode.collect { mode ->
                _uiState.value = _uiState.value.copy(sortMode = mode)
            }
        }
        loadCapabilities()
    }

    /** 在后台线程加载设备 Wi-Fi 能力（监管域允许信道 + 频段可用性），避免阻塞主线程。 */
    private fun loadCapabilities() {
        viewModelScope.launch(Dispatchers.Default) {
            val allowedChannelsByBand = AllowedChannelsProvider.allowedChannelsByBand(appContext)
                .mapValues { (_, list) -> list.map { it.channel }.toSet() }
            val bandAvailabilityByBand = listOf("2.4 GHz", "5 GHz", "6 GHz").associateWith {
                WifiBandSupport.availability(appContext, it)
            }
            _uiState.update {
                it.copy(
                    allowedChannelsByBand = allowedChannelsByBand,
                    bandAvailabilityByBand = bandAvailabilityByBand,
                )
            }
        }
    }

    fun setBandFilter(filter: BandFilter) {
        _uiState.value = _uiState.value.copy(bandFilter = filter)
    }

    fun setSortMode(mode: SortMode) {
        _uiState.value = _uiState.value.copy(sortMode = mode)
        viewModelScope.launch { settingsRepository.setSortMode(mode) }
    }

    fun toggleGroup(key: String) {
        val cur = _uiState.value.expandedKeys
        _uiState.value = _uiState.value.copy(
            expandedKeys = if (key in cur) cur - key else cur + key
        )
    }

    fun setAutoScanEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(autoScanEnabled = enabled)
        viewModelScope.launch { settingsRepository.setAutoScanEnabled(enabled) }
    }

    fun setAppLanguage(language: AppLanguage) {
        _uiState.value = _uiState.value.copy(appLanguage = language)
        viewModelScope.launch { settingsRepository.setAppLanguage(language) }
    }

    fun setContinuousScanEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(continuousScanEnabled = enabled)
        viewModelScope.launch { settingsRepository.setContinuousScanEnabled(enabled) }
        if (enabled) startContinuousScan() else stopContinuousScan()
    }

    /** App 回到前台：按需恢复持续扫描。 */
    fun onAppForeground() {
        if (_uiState.value.continuousScanEnabled) startContinuousScan()
    }

    /** App 进入后台：暂停持续扫描以省电。 */
    fun onAppBackground() {
        stopContinuousScan()
    }

    /**
     * 由列表页在首次进入时调用一次，传入当前权限是否已授予。
     *
     * 只有「设置已加载 + 权限已授予 + 自动扫描开启」三者同时满足时才会真正触发扫描；
     * 且无论结果如何，该决策只执行一次，后续页面返回不会重复触发。
     */
    fun requestInitialAutoScan(permissionsGranted: Boolean) {
        permissionGranted = permissionsGranted
        maybeTriggerAutoScan()
    }

    private fun maybeTriggerAutoScan() {
        if (autoScanHandled || !settingsLoaded) return
        if (!permissionGranted) {
            autoScanHandled = true
            return
        }
        autoScanHandled = true
        if (_uiState.value.autoScanEnabled) {
            scan()
        }
    }

    fun scan() {
        if (_uiState.value.isScanning) return
        viewModelScope.launch { performScan() }
    }

    /** 核心扫描逻辑（suspend），手动扫描与持续扫描共用。 */
    private suspend fun performScan() {
        if (_uiState.value.isScanning) return
        _uiState.value = _uiState.value.copy(isScanning = true, errorResId = null, errorArg = null)
        try {
            val results = repository.scan()
            val now = System.currentTimeMillis()
            val newHistory = _uiState.value.rssiHistory.toMutableMap()
            results.forEach { wifi ->
                val samples = (newHistory[wifi.bssid] ?: emptyList()) + RssiSample(now, wifi.rssi)
                newHistory[wifi.bssid] = samples.takeLast(MAX_HISTORY_POINTS)
            }
            _uiState.value = _uiState.value.copy(
                wifiList = results,
                rssiHistory = newHistory,
                isScanning = false,
                // 扫描后清理已不存在的展开 key，避免 Set 无限增长
                expandedKeys = _uiState.value.expandedKeys.filter { key ->
                    results.any { groupKey(it) == key }
                }.toSet()
            )
            if (results.isEmpty()) {
                _uiState.value = _uiState.value.copy(errorResId = R.string.scan_empty_result)
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isScanning = false,
                errorResId = R.string.scan_failed,
                errorArg = e.message
            )
        }
    }

    private fun startContinuousScan() {
        if (continuousScanJob?.isActive == true) return
        continuousScanJob = viewModelScope.launch {
            while (isActive) {
                performScan()
                delay(CONTINUOUS_SCAN_INTERVAL_MS)
            }
        }
    }

    private fun stopContinuousScan() {
        continuousScanJob?.cancel()
        continuousScanJob = null
    }

    companion object {
        private const val CONTINUOUS_SCAN_INTERVAL_MS = 5000L
        private const val MAX_HISTORY_POINTS = 50

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application =
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]!!
                WifiViewModel(
                    appContext = application,
                    repository = WifiScanner(application),
                    settingsRepository = SettingsRepositoryImpl(application)
                )
            }
        }
    }
}
