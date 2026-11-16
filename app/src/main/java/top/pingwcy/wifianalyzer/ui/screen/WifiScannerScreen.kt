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

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            viewModel.scan(context)
        } else {
            Toast.makeText(context, "你拒绝了权限，无法扫描", Toast.LENGTH_SHORT).show()
        }
    }

    val filteredList = remember(uiState.wifiList, bandFilter) {
        uiState.wifiList.filter(bandFilter.match)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("WiFi 分析器") },
                actions = {
                    // 三个点菜单
                    IconButton(onClick = { showMenu = !showMenu }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "更多")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("信道分析") },
                            onClick = {
                                showMenu = false
                                onOpenChannelAnalysis()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("本机可用信道") },
                            onClick = {
                                showMenu = false
                                showChannelInfoDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (uiState.sortMode == SortMode.GROUPED) "切换为：全部列表"
                                    else "切换为：按 SSID 聚合"
                                )
                            },
                            onClick = {
                                showMenu = false
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
                        viewModel.scan(context)
                    } else {
                        permissionLauncher.launch(permissionsToRequest)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isScanning
            ) {
                Text(if (uiState.isScanning) "扫描中..." else "开始扫描 WiFi")
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
            val countText = when (uiState.sortMode) {
                SortMode.GROUPED ->
                    "共 ${groupedList.size} 个 SSID · ${filteredList.size} 个热点" +
                            if (bandFilter != BandFilter.ALL) "（已筛选：${bandFilter.label}）" else ""

                SortMode.FLAT ->
                    "共 ${filteredList.size} 个网络" +
                            if (bandFilter != BandFilter.ALL) "（已筛选：${bandFilter.label}）" else ""
            }

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
                    Text("本机 Wi-Fi 能力")
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp) // 2. 限制最高高度，防止弹窗占满全屏
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text("当前 Wi-Fi 监管域")

                        Text(
                            "国家码：无法通过普通公开 API 直接读取",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )

                        Spacer(Modifier.height(12.dp))

                        Text("监管域允许使用的信道")

                        Spacer(Modifier.height(6.dp))

                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {

                            Text(
                                "当前 Android 版本低于 Android 14，" +
                                        "无法通过公开 API 获取监管域允许的信道。",
                                color = Color.Red,
                                fontSize = 12.sp
                            )

                        } else if (supportedFrequencies.isEmpty()) {

                            Text(
                                "未获取到允许使用的信道列表。" +
                                        "请检查附近设备权限或设备系统是否支持该 API。",
                                color = Color.Red,
                                fontSize = 12.sp
                            )

                        } else {

                            Text(
                                "共 ${supportedFrequencies.size} 个允许的信道",
                                color = Color(0xFF4CAF50)
                            )

                            Spacer(Modifier.height(8.dp))

                            supportedFrequencies.forEach { freq ->

                                val channel = frequencyToChannel(freq)

                                val bandName = when (freq) {
                                    in 2400..2500 -> "2.4 GHz"
                                    in 4900..5900 -> "5 GHz"
                                    in 5925..7125 -> "6 GHz"
                                    else -> "其他频段"
                                }

                                Text(
                                    "$bandName  |  Channel $channel  |  $freq MHz",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        Text(
                            "注：以上数据表示当前监管域允许 STA 模式使用的信道，" +
                                    "不代表设备硬件支持的全部信道。",
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
                        Text("知道了")
                    }
                }
            )
        }
    }
}