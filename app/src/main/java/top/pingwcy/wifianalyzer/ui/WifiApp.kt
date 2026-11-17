package top.pingwcy.wifianalyzer.ui

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import top.pingwcy.wifianalyzer.R
import top.pingwcy.wifianalyzer.data.CrashLogger
import top.pingwcy.wifianalyzer.model.WifiData
import top.pingwcy.wifianalyzer.ui.component.CrashReportDialog
import top.pingwcy.wifianalyzer.ui.screen.ChannelAnalysisScreen
import top.pingwcy.wifianalyzer.ui.screen.SettingsScreen
import top.pingwcy.wifianalyzer.ui.screen.SpectrumScreen
import top.pingwcy.wifianalyzer.ui.screen.WifiDetailScreen
import top.pingwcy.wifianalyzer.ui.screen.WifiScannerScreen
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavHostController

object Routes {
    const val LIST = "list"
    const val DETAIL = "detail/{wifiId}"   // 详情页带参数
    const val CHANNEL = "channel"
    const val SETTINGS = "settings"
    const val SPECTRUM = "spectrum"

    fun detail(wifiId: String) = "detail/$wifiId"
}
@Composable
fun WifiScannerApp(
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val viewModel: WifiViewModel = viewModel(factory = WifiViewModel.Factory)
    val uiState by viewModel.uiState.collectAsState()

    // 检测上次是否崩溃，若有则弹窗提示
    var showCrashDialog by remember { mutableStateOf(CrashLogger.hasCrashReport(context)) }

    NavHost(
        navController = navController,
        startDestination = Routes.LIST
    ) {
        // ---------- 列表页 ----------
        composable(Routes.LIST) {
            WifiScannerScreen(
                onItemClick = { wifi ->
                    // 用唯一标识跳转（BSSID 或你模型里的 id）
                    navController.navigate(Routes.detail(wifi.bssid))
                },
                onOpenChannelAnalysis = {
                    navController.navigate(Routes.CHANNEL)
                },
                onOpenSettings = {
                    navController.navigate(Routes.SETTINGS)
                },
                onOpenSpectrum = {
                    navController.navigate(Routes.SPECTRUM)
                },
                viewModel = viewModel
            )
        }

        // ---------- 详情页（带参数） ----------
        composable(
            route = Routes.DETAIL,
            arguments = listOf(navArgument("wifiId") { type = NavType.StringType })
        ) { backStackEntry ->
            val wifiId = backStackEntry.arguments?.getString("wifiId").orEmpty()

            // 从共享 ViewModel 的最新列表里按 id 查
            val wifi = uiState.wifiList.firstOrNull { it.bssid == wifiId }

            if (wifi != null) {
                WifiDetailScreen(
                    wifi = wifi,
                    rssiHistory = uiState.rssiHistory[wifi.bssid] ?: emptyList(),
                    onBack = { navController.popBackStack() }
                )
            } else {
                // 列表刷新后找不到该 WiFi，安全兜底，避免 NPE
                LaunchedEffect(Unit) { navController.popBackStack() }
            }
        }

        // ---------- 信道分析页 ----------
        composable(Routes.CHANNEL) {
            ChannelAnalysisScreen(
                wifiList = uiState.wifiList,
                onBack = { navController.popBackStack() }
            )
        }

        // ---------- 设置页 ----------
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                viewModel = viewModel
            )
        }

        // ---------- 频谱页 ----------
        composable(Routes.SPECTRUM) {
            SpectrumScreen(
                wifiList = uiState.wifiList,
                onBack = { navController.popBackStack() }
            )
        }
    }

    if (showCrashDialog) {
        CrashReportDialog(
            onShare = {
                shareCrashReport(context)
                CrashLogger.clear(context)
                showCrashDialog = false
            },
            onDismiss = {
                CrashLogger.clear(context)
                showCrashDialog = false
            }
        )
    }
}

private fun shareCrashReport(context: Context) {
    val content = CrashLogger.readCrashReports(context)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.crash_report_share_subject))
        putExtra(Intent.EXTRA_TEXT, content)
    }
    context.startActivity(
        Intent.createChooser(intent, context.getString(R.string.crash_report_share_title))
    )
}