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

data class WifiUiState(
    val wifiList: List<WifiData> = emptyList(),
    val isScanning: Boolean = false,
    val errorMessage: String? = null
)

class WifiViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(WifiUiState())
    val uiState: StateFlow<WifiUiState> = _uiState.asStateFlow()

    fun scan(context: Context) {
        if (_uiState.value.isScanning) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isScanning = true, errorMessage = null)
            try {
                // 在这里传入 context
                val results = WifiScanner(context.applicationContext).scan()
                _uiState.value = _uiState.value.copy(wifiList = results, isScanning = false)
                if (results.isEmpty()) {
                    _uiState.value = _uiState.value.copy(errorMessage = "未扫描到 WiFi，请检查 GPS 定位开关")
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