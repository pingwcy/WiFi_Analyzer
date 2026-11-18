package top.pingwcy.wifianalyzer.ui

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import top.pingwcy.wifianalyzer.R
import top.pingwcy.wifianalyzer.data.CrashLogger
import top.pingwcy.wifianalyzer.ui.component.CrashReportDialog
import top.pingwcy.wifianalyzer.ui.component.SharePrivacyConfirmDialog
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
@Suppress("DEPRECATION")
@Composable
fun WifiScannerApp(
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val viewModel: WifiViewModel = viewModel(factory = WifiViewModel.Factory)
    val uiState by viewModel.uiState.collectAsState()

    // 检测上次是否崩溃，若有则弹窗提示
    var showCrashDialog by remember { mutableStateOf(CrashLogger.hasCrashReport(context)) }
    var showSharePrivacyConfirm by remember { mutableStateOf(false) }

    // 感知前后台：进入后台暂停持续扫描，回到前台按需恢复。
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> viewModel.onAppForeground()
                Lifecycle.Event.ON_STOP -> viewModel.onAppBackground()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

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
                allowedChannelsByBand = uiState.allowedChannelsByBand,
                bandAvailabilityByBand = uiState.bandAvailabilityByBand,
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
                allowedChannelsByBand = uiState.allowedChannelsByBand,
                bandAvailabilityByBand = uiState.bandAvailabilityByBand,
                onBack = { navController.popBackStack() }
            )
        }
    }

    if (showCrashDialog) {
        CrashReportDialog(
            onShare = { showSharePrivacyConfirm = true },
            onDismiss = {
                CrashLogger.clear(context)
                showCrashDialog = false
            }
        )
    }

    if (showSharePrivacyConfirm) {
        SharePrivacyConfirmDialog(
            onConfirm = {
                shareCrashReport(context)
                CrashLogger.clear(context)
                showCrashDialog = false
                showSharePrivacyConfirm = false
            },
            onDismiss = { showSharePrivacyConfirm = false }
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
