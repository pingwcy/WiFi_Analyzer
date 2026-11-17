package top.pingwcy.wifianalyzer.ui.screen
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiAvailableChannel
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import top.pingwcy.wifianalyzer.model.BandFilter
import top.pingwcy.wifianalyzer.model.WifiData
import top.pingwcy.wifianalyzer.ui.WifiViewModel
import top.pingwcy.wifianalyzer.ui.component.FilterChipRow
import top.pingwcy.wifianalyzer.ui.component.WifiCard
import top.pingwcy.wifianalyzer.model.groupKey
import top.pingwcy.wifianalyzer.ui.SortMode
import top.pingwcy.wifianalyzer.ui.component.WifiGroupHeader
import androidx.compose.ui.res.stringResource
import top.pingwcy.wifianalyzer.R

data class WifiGroup(
    val key: String,
    val ssid: String,          // 隐藏网络显示 "(隐藏网络)"
    val networks: List<WifiData>, // 已按 rssi 降序
    val isExpanded: Boolean
) {
    val strongest: WifiData get() = networks.first()
    val maxRssi: Int get() = strongest.rssi
    val count: Int get() = networks.size
}

fun buildWifiGroups(
    list: List<WifiData>,
    expandedKeys: Set<String>
): List<WifiGroup> =
    list.groupBy { groupKey(it) }
        .map { (key, group) ->
            val sorted = group.sortedByDescending { it.rssi }
            WifiGroup(
                key = key,
                ssid = sorted.first().ssid.takeIf { it.isNotBlank() } ?: "(隐藏网络)",
                networks = sorted,
                isExpanded = key in expandedKeys
            )
        }
        // 组间按组内最强信号降序；信号相同再按 SSID 字母序，保证稳定
        .sortedWith(
            compareByDescending<WifiGroup> { it.maxRssi }
                .thenBy { it.ssid }
        )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WifiScannerScreen(
    onItemClick: (WifiData) -> Unit,
    onOpenChannelAnalysis: () -> Unit,
    viewModel: WifiViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val bandFilter = uiState.bandFilter
    // 把菜单状态放到最上面
    var showMenu by remember { mutableStateOf(false) }
    var showChannelInfoDialog by remember { mutableStateOf(false) }

    val permissionsToRequest = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.NEARBY_WIFI_DEVICES
        else
            Manifest.permission.ACCESS_FINE_LOCATION
    )

    val errorMessage = uiState.errorResId?.let { resId ->
        uiState.errorArg?.let { stringResource(resId, it) } ?: stringResource(resId)
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            viewModel.scan()
        } else {
            Toast.makeText(context, R.string.permission_denied, Toast.LENGTH_SHORT).show()
        }
    }

    val filteredList = remember(uiState.wifiList, bandFilter) {
        uiState.wifiList.filter(bandFilter.match)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    // 三个点菜单
                    IconButton(onClick = { showMenu = !showMenu }) {
                        Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.menu_more))
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_channel_analysis)) },
                            onClick = {
                                showMenu = false
                                onOpenChannelAnalysis()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_local_channels)) },
                            onClick = {
                                showMenu = false
                                showChannelInfoDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                // ✅ 根据当前的排序模式，提示用户“切换后”会变成什么
                                Text(
                                    if (uiState.sortMode == SortMode.GROUPED) {
                                        stringResource(R.string.menu_switch_to_flat)
                                    } else {
                                        stringResource(R.string.menu_switch_to_grouped)
                                    }
                                )
                            },
                            onClick = {
                                showMenu = false
                                // 切换逻辑没问题，保持原样
                                viewModel.setSortMode(
                                    if (uiState.sortMode == SortMode.GROUPED) SortMode.FLAT else SortMode.GROUPED
                                )
                            }
                        )
                    }

                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Button(
                onClick = {
                    val allGranted = permissionsToRequest.all {
                        ContextCompat.checkSelfPermission(
                            context,
                            it
                        ) == PackageManager.PERMISSION_GRANTED
                    }
                    if (allGranted) {
                        viewModel.scan()
                    } else {
                        permissionLauncher.launch(permissionsToRequest)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isScanning
            ) {
                Text(
                    if (uiState.isScanning) stringResource(R.string.btn_scanning)
                    else stringResource(R.string.btn_start_scan)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            FilterChipRow(
                selected = bandFilter,
                onSelect = { viewModel.setBandFilter(it) }
            )

            Spacer(modifier = Modifier.height(12.dp))


// 聚合视图的组列表（FLAT 模式不构建，省一点）
// —— 提到 Column 外层或这里都行，建议和 filteredList 放一起 ——
            val groupedList = remember(filteredList) {
                buildWifiGroups(filteredList, emptySet())   // isExpanded 不再存组里
            }
            val flatSorted = remember(filteredList, uiState.sortMode) {
                if (uiState.sortMode == SortMode.FLAT)
                    filteredList.sortedByDescending { it.rssi }
                else emptyList()
            }
// 先把筛选名取出来（Composable 上下文）
            val filterLabel = if (bandFilter != BandFilter.ALL)
                stringResource(bandFilter.labelRes)   // 或 bandFilter.labelText()
            else null

// 主文案
            val baseText = when (uiState.sortMode) {
                SortMode.GROUPED -> stringResource(
                    R.string.count_grouped, groupedList.size, filteredList.size
                )
                SortMode.FLAT -> stringResource(
                    R.string.count_flat, filteredList.size
                )
            }

// 条件后缀
            val countText = if (filterLabel != null)
                baseText + stringResource(R.string.count_filtered_suffix, filterLabel)
            else
                baseText

            Text(
                text = countText, fontSize = 12.sp, color = Color.Gray,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when (uiState.sortMode) {
                    SortMode.GROUPED -> {
                        groupedList.forEach { group ->
                            if (group.count == 1) {
                                item(key = "ap:${group.strongest.bssid}") {
                                    WifiCard(
                                        group.strongest,
                                        onClick = { onItemClick(group.strongest) })
                                }
                            } else {
                                item(key = "group:${group.key}") {
                                    WifiGroupHeader(
                                        ssid = group.ssid,
                                        count = group.count,
                                        strongest = group.strongest,
                                        expanded = group.key in uiState.expandedKeys,
                                        onToggle = { viewModel.toggleGroup(group.key) },
                                        onOpenDetail = onItemClick
                                    )
                                }
                                if (group.key in uiState.expandedKeys) {
                                    items(group.networks, key = { "ap:${it.bssid}" }) { wifi ->
                                        WifiCard(wifi = wifi, onClick = { onItemClick(wifi) })
                                    }
                                }
                            }
                        }
                    }

                    SortMode.FLAT -> {
                        items(flatSorted, key = { "ap:${it.bssid}" }) { wifi ->
                            WifiCard(wifi = wifi, onClick = { onItemClick(wifi) })
                        }
                    }
                }
            }
        }
        // 弹窗：显示本机信道信息

        if (showChannelInfoDialog) {

            val wm = context.applicationContext
                .getSystemService(Context.WIFI_SERVICE) as WifiManager

            // 获取当前监管域允许使用的信道
            val supportedFrequencies: List<Int> =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    val result = mutableListOf<Int>()

                    // 使用硬编码的频段值：1=2.4G, 6=5G(含DFS), 8=6G
                    for (band in listOf(1, 6, 8)) {
                        try {
                            result += wm.getAllowedChannels(band, WifiAvailableChannel.OP_MODE_STA)
                                .map { it.frequencyMhz }
                        } catch (e: SecurityException) {
                            Log.e("ChannelInfo", "缺少 NEARBY_WIFI_DEVICES 权限", e)
                        } catch (e: Exception) {
                            Log.e("ChannelInfo", "band=$band 获取失败", e)
                        }
                    }
                    // 这里直接返回结果
                    result.distinct().sorted()
                } else {
                    // 补上 else 分支返回空列表
                    emptyList()
                }

            // 将频率转换为信道号
            fun frequencyToChannel(freq: Int): Int {
                return when (freq) {
                    in 2412..2472 -> (freq - 2407) / 5
                    2484 -> 14
                    in 5000..5895 -> (freq - 5000) / 5
                    in 5955..7115 -> (freq - 5950) / 5
                    else -> -1
                }
            }

            AlertDialog(
                onDismissRequest = {
                    showChannelInfoDialog = false
                },
                title = {
                    Text(stringResource(R.string.dialog_channel_title)) // ✅ 本机 Wi-Fi 能力
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(stringResource(R.string.dialog_current_domain)) // ✅ 当前 Wi-Fi 监管域

                        Text(
                            stringResource(R.string.dialog_country_code_unknown), // ✅ 国家码：无法通过...
                            fontSize = 12.sp,
                            color = Color.Gray
                        )

                        Spacer(Modifier.height(12.dp))

                        Text(stringResource(R.string.dialog_allowed_channels)) // ✅ 监管域允许使用的信道

                        Spacer(Modifier.height(6.dp))

                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                            Text(
                                stringResource(R.string.dialog_android_too_old), // ✅ 当前 Android 版本低于...
                                color = Color.Red,
                                fontSize = 12.sp
                            )
                        } else if (supportedFrequencies.isEmpty()) {
                            Text(
                                stringResource(R.string.dialog_no_channels), // ✅ 未获取到允许使用的信道列表...
                                color = Color.Red,
                                fontSize = 12.sp
                            )
                        } else {
                            // ✅ 带参数：共 %1$d 个允许的信道
                            Text(
                                stringResource(R.string.dialog_channel_count, supportedFrequencies.size),
                                color = Color(0xFF4CAF50)
                            )

                            Spacer(Modifier.height(8.dp))

                            supportedFrequencies.forEach { freq ->
                                val channel = frequencyToChannel(freq)

                                // ✅ 这里的频段名称也提取了资源
                                val bandName = when (freq) {
                                    in 2400..2500 -> stringResource(R.string.band_2g)
                                    in 4900..5900 -> stringResource(R.string.band_5g)
                                    in 5925..7125 -> stringResource(R.string.band_6g)
                                    else -> stringResource(R.string.band_other)
                                }

                                // ✅ 带参数：%1$s  |  Channel %2$d  |  %3$d MHz
                                Text(
                                    stringResource(R.string.dialog_channel_line, bandName, channel, freq),
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        Text(
                            stringResource(R.string.dialog_note), // ✅ 注：以上数据表示当前监管域允许...
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showChannelInfoDialog = false
                        }
                    ) {
                        Text(stringResource(R.string.btn_ok)) // ✅ 知道了
                    }
                }
            )
        }
    }
}