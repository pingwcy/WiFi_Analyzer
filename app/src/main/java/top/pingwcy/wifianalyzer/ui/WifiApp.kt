package top.pingwcy.wifianalyzer.ui

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import top.pingwcy.wifianalyzer.model.WifiData
import top.pingwcy.wifianalyzer.ui.screen.WifiScannerScreen
import top.pingwcy.wifianalyzer.ui.screen.WifiDetailScreen
import top.pingwcy.wifianalyzer.ui.screen.ChannelAnalysisScreen
enum class Screen { LIST, DETAIL, CHANNEL }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WifiScannerApp() {
    var selectedWifi by remember { mutableStateOf<WifiData?>(null) }
    var currentScreen by remember { mutableStateOf(Screen.LIST) }

    var wifiList by remember { mutableStateOf<List<WifiData>>(emptyList()) }
    var isScanning by remember { mutableStateOf(false) }

    when (currentScreen) {
        Screen.LIST -> WifiScannerScreen(
            wifiList = wifiList,
            isScanning = isScanning,
            onListChange = { wifiList = it },
            onScanningChange = { isScanning = it },
            onItemClick = { selectedWifi = it; currentScreen = Screen.DETAIL },
            onOpenChannelAnalysis = { currentScreen = Screen.CHANNEL }
        )
        Screen.DETAIL -> WifiDetailScreen(
            wifi = selectedWifi!!,
            onBack = { selectedWifi = null; currentScreen = Screen.LIST }
        )
        Screen.CHANNEL -> ChannelAnalysisScreen(
            wifiList = wifiList,
            onBack = { currentScreen = Screen.LIST }
        )
    }
}
