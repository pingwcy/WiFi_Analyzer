package top.pingwcy.wifianalyzer.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import top.pingwcy.wifianalyzer.data.WifiScanner
import top.pingwcy.wifianalyzer.model.WifiData
import top.pingwcy.wifianalyzer.model.BandFilter
import top.pingwcy.wifianalyzer.model.groupKey
/** 排序/视图模式 */
enum class SortMode {
    GROUPED,   // 按 SSID 聚合，组间按组内最强 rssi 降序
    FLAT       // 全部展开，纯 rssi 降序
}

data class WifiUiState(
    val wifiList: List<WifiData> = emptyList(),
    val isScanning: Boolean = false,
    val errorMessage: String? = null,
    val bandFilter: BandFilter = BandFilter.ALL,
    val expandedKeys: Set<String> = emptySet(),  // 已展开的分组 key
    val sortMode: SortMode = SortMode.GROUPED     // 默认聚合视图
)

class WifiViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(WifiUiState())
    val uiState: StateFlow<WifiUiState> = _uiState.asStateFlow()

    fun setBandFilter(filter: BandFilter) {
        _uiState.value = _uiState.value.copy(bandFilter = filter)
    }

    fun setSortMode(mode: SortMode) {
        _uiState.value = _uiState.value.copy(sortMode = mode)
    }

    fun toggleGroup(key: String) {
        val cur = _uiState.value.expandedKeys
        _uiState.value = _uiState.value.copy(
            expandedKeys = if (key in cur) cur - key else cur + key
        )
    }

    fun scan(context: Context) {
        if (_uiState.value.isScanning) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isScanning = true, errorMessage = null)
            try {
                val results = WifiScanner(context.applicationContext).scan()
                _uiState.value = _uiState.value.copy(
                    wifiList = results,
                    isScanning = false,
                    // 可选：扫描后清理已不存在的展开 key，避免 Set 无限增长
                    expandedKeys = _uiState.value.expandedKeys.filter { key ->
                        results.any { groupKey(it) == key }
                    }.toSet()
                )
                if (results.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "未扫描到 WiFi，请检查 GPS 定位开关"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isScanning = false,
                    errorMessage = "扫描失败：${e.message}"
                )
            }
        }
    }
}
