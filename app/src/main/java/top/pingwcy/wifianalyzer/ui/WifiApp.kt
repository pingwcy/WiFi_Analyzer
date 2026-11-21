package top.pingwcy.wifianalyzer.ui

import android.content.Context
import android.content.Intent
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WifiFind
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
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
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

object Routes {
    const val LIST = "list"
    const val DETAIL = "detail/{wifiId}"   // 详情页带参数
    const val CHANNEL = "channel"
    const val SETTINGS = "settings"
    const val SPECTRUM = "spectrum"

    fun detail(wifiId: String) = "detail/$wifiId"
}

/** 底部导航的顶层目的地。 */
private data class TopLevelDestination(
    val route: String,
    @param:StringRes val labelRes: Int,
    val icon: ImageVector,
)

private val topLevelDestinations = listOf(
    TopLevelDestination(Routes.LIST, R.string.nav_list, Icons.Filled.WifiFind),
    TopLevelDestination(Routes.CHANNEL, R.string.nav_channels, Icons.Filled.BarChart),
    TopLevelDestination(Routes.SPECTRUM, R.string.nav_spectrum, Icons.AutoMirrored.Filled.ShowChart),
    TopLevelDestination(Routes.SETTINGS, R.string.nav_settings, Icons.Filled.Settings),
)

private val topLevelRoutes = topLevelDestinations.map { it.route }.toSet()

/** 底部 tab 切换：保留各 tab 状态，避免重复重建。 */
private fun NavHostController.navigateToTopLevel(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // 详情页标题（动态 SSID）
    val detailWifi = navBackStackEntry?.arguments
        ?.getString("wifiId")
        ?.let { id -> uiState.wifiList.firstOrNull { it.bssid == id } }
    val detailTitle = detailWifi?.let {
        it.ssid.ifBlank { stringResource(R.string.hidden_network) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val title = when (currentRoute) {
                        Routes.CHANNEL -> stringResource(R.string.channel_analysis_title)
                        Routes.SPECTRUM -> stringResource(R.string.spectrum_title)
                        Routes.SETTINGS -> stringResource(R.string.settings_title)
                        Routes.DETAIL -> detailTitle ?: ""
                        else -> stringResource(R.string.app_name)
                    }
                    Text(title)
                },
                navigationIcon = {
                    if (currentRoute == Routes.DETAIL) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.btn_back)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        bottomBar = {
            if (currentRoute in topLevelRoutes) {
                NavigationBar {
                    topLevelDestinations.forEach { destination ->
                        NavigationBarItem(
                            selected = currentRoute == destination.route,
                            onClick = { navController.navigateToTopLevel(destination.route) },
                            icon = {
                                Icon(
                                    imageVector = destination.icon,
                                    contentDescription = stringResource(destination.labelRes)
                                )
                            },
                            label = { Text(stringResource(destination.labelRes)) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.LIST,
            modifier = Modifier.padding(innerPadding)
        ) {
            // ---------- 列表页 ----------
            composable(Routes.LIST) {
                WifiScannerScreen(
                    onItemClick = { wifi ->
                        navController.navigate(Routes.detail(wifi.bssid))
                    },
                    viewModel = viewModel
                )
            }

            // ---------- 详情页（带参数，全屏，无底部导航） ----------
            composable(
                route = Routes.DETAIL,
                arguments = listOf(navArgument("wifiId") { type = NavType.StringType })
            ) { backStackEntry ->
                val wifiId = backStackEntry.arguments?.getString("wifiId").orEmpty()
                val wifi = uiState.wifiList.firstOrNull { it.bssid == wifiId }

                if (wifi != null) {
                    WifiDetailScreen(
                        wifi = wifi,
                        rssiHistory = uiState.rssiHistory[wifi.bssid] ?: emptyList(),
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
                )
            }

            // ---------- 设置页 ----------
            composable(Routes.SETTINGS) {
                SettingsScreen(viewModel = viewModel)
            }

            // ---------- 频谱页 ----------
            composable(Routes.SPECTRUM) {
                SpectrumScreen(
                    wifiList = uiState.wifiList,
                    allowedChannelsByBand = uiState.allowedChannelsByBand,
                    bandAvailabilityByBand = uiState.bandAvailabilityByBand,
                )
            }
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
