package top.pingwcy.wifianalyzer.ui

import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import top.pingwcy.wifianalyzer.model.WifiData
import top.pingwcy.wifianalyzer.ui.screen.ChannelAnalysisScreen
import top.pingwcy.wifianalyzer.ui.screen.WifiDetailScreen
import top.pingwcy.wifianalyzer.ui.screen.WifiScannerScreen

enum class Screen { LIST, DETAIL, CHANNEL }

@Composable
fun WifiScannerApp() {
    var selectedWifi by remember { mutableStateOf<WifiData?>(null) }
    var currentScreen by remember { mutableStateOf(Screen.LIST) }

    // 统一获取 ViewModel，让数据在页面间共享
    val viewModel: WifiViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()

    when (currentScreen) {
        Screen.LIST -> WifiScannerScreen(
            onItemClick = { selectedWifi = it; currentScreen = Screen.DETAIL },
            onOpenChannelAnalysis = { currentScreen = Screen.CHANNEL },
            viewModel = viewModel // 把 ViewModel 传给列表页
        )
        Screen.DETAIL -> WifiDetailScreen(
            wifi = selectedWifi!!,
            onBack = { selectedWifi = null; currentScreen = Screen.LIST }
        )
        Screen.CHANNEL -> ChannelAnalysisScreen(
            wifiList = uiState.wifiList, // 从 ViewModel 拿到最新的列表给信道分析
            onBack = { currentScreen = Screen.LIST }
        )
    }
}