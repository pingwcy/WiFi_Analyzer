package top.pingwcy.wifianalyzer // 确保包名和你自己的一致

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
// ============ 数据模型 ============

data class WifiData(
    val ssid: String,
    val bssid: String,
    val frequency: Int,          // 主信道频率 MHz
    val rssi: Int,               // 信号强度 dBm
    val channelWidth: Int,       // 信道宽度枚举值（WifiManager.CHANNEL_WIDTH_*），非 MHz
    val centerFreq0: Int,        // 中心频率 0（MHz），0 表示无
    val centerFreq1: Int,        // 中心频率 1（MHz），0 表示无
    val primaryChannel: Int,     // 主信道号
    val band: String,            // 2.4GHz / 5GHz / 6GHz
    val wifiStandard: String,    // WiFi 4/5/6/7 等
    val capabilities: String,    // 加密/能力描述
    val isHidden: Boolean,
    val countryCode: String  // AP 广播的 802.11d 国家码
)

// ============ 主 Activity ============

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                WifiScannerApp()
            }
        }
    }
}

// ============ 应用根 Composable：状态提升，管理"列表页 / 详情页"切换 ============

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
// ============ 列表页（状态由外部传入）============

// 筛选选项
enum class BandFilter(val label: String, val match: (WifiData) -> Boolean) {
    ALL("全部", { true }),
    BAND_24("2.4G", { it.band == "2.4 GHz" }),
    BAND_5("5G",   { it.band == "5 GHz" }),
    BAND_6("6G",   { it.band == "6 GHz" })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WifiScannerScreen(
    wifiList: List<WifiData>,
    isScanning: Boolean,
    onListChange: (List<WifiData>) -> Unit,
    onScanningChange: (Boolean) -> Unit,
    onItemClick: (WifiData) -> Unit,
    onOpenChannelAnalysis: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    //    筛选状态（放这里即可，切换详情页回来时由 remember 保留在组合中；
    //   若想跨页面保留，可提升到 WifiScannerApp，见下方说明）
    var bandFilter by remember { mutableStateOf(BandFilter.ALL) }

    val permissionsToRequest = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.NEARBY_WIFI_DEVICES
        else
            Manifest.permission.ACCESS_FINE_LOCATION
    )

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            startScan(
                context = context,
                scope = scope,
                onStart = { onScanningChange(true) },
                onComplete = { list ->
                    onListChange(list)
                    onScanningChange(false)
                }
            )
        } else {
            Toast.makeText(context, "你拒绝了权限，无法扫描", Toast.LENGTH_SHORT).show()
            onScanningChange(false)
        }
    }

    // 按频段过滤（每次重组重新计算，列表很小，性能无压力）
    val filteredList = remember(wifiList, bandFilter) {
        wifiList.filter(bandFilter.match)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("WiFi 扫描器") },
                actions = {
                    TextButton(onClick = onOpenChannelAnalysis) {
                        Text("信道分析")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )        }
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
                        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
                    }
                    if (allGranted) {
                        startScan(
                            context = context,
                            scope = scope,
                            onStart = { onScanningChange(true) },
                            onComplete = { list ->
                                onListChange(list)
                                onScanningChange(false)
                            }
                        )
                    } else {
                        permissionLauncher.launch(permissionsToRequest)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isScanning
            ) {
                Text(if (isScanning) "扫描中..." else "开始扫描 WiFi")
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ★ 新增：筛选按钮行
            FilterChipRow(
                selected = bandFilter,
                onSelect = { bandFilter = it }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ★ 新增：数量提示（可选，体验更好）
            Text(
                text = "共 ${filteredList.size} 个网络" +
                        if (bandFilter != BandFilter.ALL) "（已筛选：${bandFilter.label}）" else "",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // ★ 改成 filteredList
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filteredList) { wifi ->
                    WifiCard(wifi = wifi, onClick = { onItemClick(wifi) })
                }
            }
        }
    }
}

// 筛选按钮组件
@Composable
fun FilterChipRow(
    selected: BandFilter,
    onSelect: (BandFilter) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        BandFilter.entries.forEach { filter ->
            FilterChip(
                selected = selected == filter,
                onClick = { onSelect(filter) },
                label = { Text(filter.label) }
            )
        }
    }
}

// ============ 列表卡片 ============

@Composable
fun WifiCard(wifi: WifiData, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = wifi.ssid,
                fontSize = 18.sp,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "频率: ${wifi.frequency} MHz", color = Color.Gray)
                Text(
                    text = "信号: ${wifi.rssi} dBm",
                    color = when {
                        wifi.rssi >= -50 -> Color(0xFF4CAF50)
                        wifi.rssi >= -70 -> Color(0xFFFF9800)
                        else -> Color(0xFFF44336)
                    }
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "频宽: ${widthText(wifi.channelWidth)}", color = Color.Gray)
                Text(text = "信道: ${wifi.primaryChannel}", color = Color.Gray)
                Text(text = "国家: ${wifi.countryCode}", color = Color.Gray)
            }
        }
    }
}

// ============ 详情页 ============

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WifiDetailScreen(wifi: WifiData, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(wifi.ssid) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
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
                .verticalScroll(rememberScrollState())
        ) {
            DetailSection(title = "基本信息") {
                DetailRow("SSID", wifi.ssid)
                DetailRow("BSSID", wifi.bssid)
                DetailRow("是否隐藏", if (wifi.isHidden) "是" else "否")
                DetailRow("频段", wifi.band)
                DetailRow("WiFi 标准", wifi.wifiStandard)
                DetailRow("AP Country Code", wifi.countryCode)
            }

            Spacer(modifier = Modifier.height(16.dp))

            DetailSection(title = "信号与信道") {
                DetailRow(
                    "信号强度",
                    "${wifi.rssi} dBm",
                    valueColor = when {
                        wifi.rssi >= -50 -> Color(0xFF4CAF50)
                        wifi.rssi >= -70 -> Color(0xFFFF9800)
                        else -> Color(0xFFF44336)
                    }
                )
                DetailRow("主信道频率", "${wifi.frequency} MHz")
                DetailRow("主信道号", "${wifi.primaryChannel}")
                DetailRow("信道宽度", widthText(wifi.channelWidth))
                DetailRow("中心频率 0", if (wifi.centerFreq0 > 0) "${wifi.centerFreq0} MHz" else "无")
                DetailRow("中心频率 1", if (wifi.centerFreq1 > 0) "${wifi.centerFreq1} MHz" else "无")
                val occChannels = occupiedChannels(wifi)
                DetailRow(
                    "占用信道",
                    "${occChannels.joinToString(" ")}（${occChannels.size} 个）"
                )            }

            Spacer(modifier = Modifier.height(16.dp))

            DetailSection(title = "能力 / 加密") {
                Text(
                    text = wifi.capabilities.ifEmpty { "未知" },
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun DetailSection(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
fun DetailRow(label: String, value: String, valueColor: Color = Color.Unspecified) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 14.sp, color = Color.Gray)
        Text(
            text = value,
            fontSize = 14.sp,
            color = if (valueColor == Color.Unspecified) MaterialTheme.colorScheme.onSurface else valueColor
        )
    }
}

// ============ 扫描逻辑 ============

@SuppressLint("MissingPermission")
fun startScan(
    context: Context,
    scope: kotlinx.coroutines.CoroutineScope,
    onStart: () -> Unit,
    onComplete: (List<WifiData>) -> Unit
) {
    scope.launch {
        onStart()
        try {
            val results = withContext(Dispatchers.IO) {
                val wifiManager =
                    context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                wifiManager.startScan()
                delay(1200)

                wifiManager.scanResults.map { it.toWifiData() }
                    .sortedByDescending { it.rssi }
            }
            onComplete(results)

            if (results.isEmpty()) {
                Toast.makeText(
                    context,
                    "未扫描到 WiFi，请检查 GPS 定位开关是否打开",
                    Toast.LENGTH_LONG
                ).show()
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "底层安全拦截：${e.message}", Toast.LENGTH_LONG).show()
                onComplete(emptyList())
            }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "发生错误：${e.message}", Toast.LENGTH_LONG).show()
                onComplete(emptyList())
            }
        }
    }
}
/**
 * 解析 AP 广播的 802.11d Country Information Element
 *
 * IE ID = 7
 * 前两个字节 = Country Code
 */
@android.annotation.SuppressLint("NewApi")
fun android.net.wifi.ScanResult.getCountryCode(): String {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
        return "不支持"
    }

    return try {
        val countryIe = this.informationElements
            ?.firstOrNull { it.id == 7 }
            ?: return "未广播"

        val buffer = countryIe.bytes.duplicate()

        if (buffer.remaining() < 3) {
            return "数据不完整"
        }

        val country = ByteArray(2)
        buffer.get(country)

        String(country, Charsets.US_ASCII)
            .takeIf { it.matches(Regex("[A-Z]{2}")) }
            ?: "无效数据"

    } catch (e: Exception) {
        "读取失败"
    }
}
// ============ ScanResult -> WifiData 转换 ============

@SuppressLint("MissingPermission")
private fun android.net.wifi.ScanResult.toWifiData(): WifiData {
    val freq = this.frequency

    // channelWidth / centerFreq0 / centerFreq1 需要 API 23+；低版本保持 0
    val chWidth = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) this.channelWidth else 0
    val cf0 = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) this.centerFreq0 else 0
    val cf1 = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) this.centerFreq1 else 0

    return WifiData(
        ssid = this.SSID.ifEmpty { "隐藏网络" },
        bssid = this.BSSID ?: "未知",
        frequency = freq,
        rssi = this.level,
        channelWidth = chWidth,   // 注意：这是 WifiManager.CHANNEL_WIDTH_* 枚举值
        centerFreq0 = cf0,
        centerFreq1 = cf1,
        primaryChannel = freqToChannel(freq),
        band = freqToBand(freq),
        wifiStandard = wifiStandardToString(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) this.wifiStandard else -1
        ),
        capabilities = this.capabilities ?: "",
        isHidden = this.SSID.isEmpty(),
        countryCode = this.getCountryCode()
    )
}

// 频率 -> 信道号
fun freqToChannel(freq: Int): Int {
    return when {
        freq == 2484 -> 14
        freq in 2412..2472 -> (freq - 2412) / 5 + 1
        freq in 4915..4980 -> (freq - 4915) / 5
        freq in 5000..5895 -> (freq - 5000) / 5
        freq in 5925..7125 -> (freq - 5950) / 5  // 6GHz 粗略估算
        else -> 0
    }
}

// 频率 -> 频段
fun freqToBand(freq: Int): String = when {
    freq in 2400..2500 -> "2.4 GHz"
    freq in 4900..5900 -> "5 GHz"
    freq in 5925..7125 -> "6 GHz"
    else -> "未知"
}

// WiFi 标准枚举 -> 字符串
fun wifiStandardToString(standard: Int): String = when (standard) {
    -1 -> "未知"
    1 -> "802.11a"
    2 -> "802.11b"
    3 -> "802.11g"
    4 -> "802.11n (WiFi 4)"
    5 -> "802.11ac (WiFi 5)"
    6 -> "802.11ax (WiFi 6)"
    7 -> "802.11ad"
    8 -> "802.11be (WiFi 7)"
    else -> "未知 ($standard)"
}

// 信道宽度枚举值 -> 显示文本（用系统常量，避免错位）
// 信道宽度枚举值 -> 显示文本
fun widthText(width: Int): String = when (width) {
    android.net.wifi.ScanResult.CHANNEL_WIDTH_20MHZ -> "20 MHz"
    android.net.wifi.ScanResult.CHANNEL_WIDTH_40MHZ -> "40 MHz"
    android.net.wifi.ScanResult.CHANNEL_WIDTH_80MHZ -> "80 MHz"
    android.net.wifi.ScanResult.CHANNEL_WIDTH_160MHZ -> "160 MHz"
    android.net.wifi.ScanResult.CHANNEL_WIDTH_80MHZ_PLUS_MHZ -> "80+80 MHz"
    else -> "$width MHz"
}

// 信道宽度枚举值 -> 真实 MHz 数值
fun widthMhz(width: Int): Int = when (width) {
    android.net.wifi.ScanResult.CHANNEL_WIDTH_20MHZ -> 20
    android.net.wifi.ScanResult.CHANNEL_WIDTH_40MHZ -> 40
    android.net.wifi.ScanResult.CHANNEL_WIDTH_80MHZ -> 80
    android.net.wifi.ScanResult.CHANNEL_WIDTH_160MHZ -> 160
    android.net.wifi.ScanResult.CHANNEL_WIDTH_80MHZ_PLUS_MHZ -> 80
    else -> 0
}

// 信道宽度枚举值 -> 占用信道数
fun occupiedChannelCount(width: Int): Int = when (width) {
    android.net.wifi.ScanResult.CHANNEL_WIDTH_20MHZ -> 1
    android.net.wifi.ScanResult.CHANNEL_WIDTH_40MHZ -> 2
    android.net.wifi.ScanResult.CHANNEL_WIDTH_80MHZ -> 4
    android.net.wifi.ScanResult.CHANNEL_WIDTH_160MHZ -> 8
    android.net.wifi.ScanResult.CHANNEL_WIDTH_80MHZ_PLUS_MHZ -> 8
    else -> 1
}

// ============ 占用信道列表推算 ============

// 由 20MHz 子信道中心频率推算信道号（5G/6G 用 5000/5950 基准即可）
private fun subChannelFromFreq(freq: Int): Int = when {
    freq in 5000..5895 -> (freq - 5000) / 5      // 5GHz
    freq in 5925..7125 -> (freq - 5950) / 5      // 6GHz
    else -> 0
}

// 推算占用信道列表
fun occupiedChannels(wifi: WifiData): List<Int> {
    val width = widthMhz(wifi.channelWidth)
    val p = wifi.primaryChannel

    // 20MHz 或拿不到中心频率：只报主信道
    if (width <= 20 || wifi.centerFreq0 <= 0) return listOf(p)

    // 2.4GHz 40MHz：主信道 1-7 向下扩，8-13 向上扩
    if (wifi.band == "2.4 GHz") {
        return when {
            p in 1..7  -> listOf(p, p + 4)
            p in 8..13 -> listOf(p - 4, p)
            else -> listOf(p)
        }
    }

    // 80+80：两块分别算
    if (wifi.channelWidth == android.net.wifi.ScanResult.CHANNEL_WIDTH_80MHZ_PLUS_MHZ) {
        val b1 = blockFromCenter(wifi.centerFreq0, 80)
        val b2 = blockFromCenter(wifi.centerFreq1, 80)
        return b1 + b2
    }

    // 5G/6G：按 centerFreq0 反推块内所有 20M 子信道
    return blockFromCenter(wifi.centerFreq0, width)
}

// 给定中心频率 + 带宽，返回块内所有 20M 子信道号
private fun blockFromCenter(centerFreq: Int, widthMhz: Int): List<Int> {
    if (centerFreq <= 0) return emptyList()
    val subCount = widthMhz / 20                 // 40→2, 80→4, 160→8
    val startFreq = centerFreq - (subCount / 2) * 20 + 10  // +10 修正到 20M 子信道中心
    return (0 until subCount).mapNotNull { i ->
        subChannelFromFreq(startFreq + i * 20).takeIf { it > 0 }
    }
}

data class ChannelUsage(
    val channel: Int,
    val count: Int,          // 该信道被占用次数
    val networks: List<String>  // 占用该信道的 SSID，用于详情
)

// 按频段统计。useOccupied = true 时按 occupiedChannels 展开，否则只算主信道
// 按频段统计。showEmpty = true 时把该频段所有标准信道都列出来（没有 AP 的 count=0）
fun analyzeChannels(
    list: List<WifiData>,
    band: String?,               // null=全部，或 "2.4 GHz" / "5 GHz" / "6 GHz"
    useOccupied: Boolean = true,
    showEmpty: Boolean = false,
    ignoreWeak: Boolean = false,     // ★ 新增：是否忽略弱信号
    weakThreshold: Int = -82         // ★ 新增：阈值 dBm（rssi < 阈值 视为弱信号）
): List<ChannelUsage> {
    // ★ 先按频段过滤，再按信号强度过滤
    val filtered = (if (band == null) list else list.filter { it.band == band })
        .filter { !ignoreWeak || it.rssi >= weakThreshold }

    val map = mutableMapOf<Int, MutableList<String>>()

    filtered.forEach { wifi ->
        val channels = if (useOccupied) occupiedChannels(wifi) else listOf(wifi.primaryChannel)
        channels.filter { it > 0 }.distinct().forEach { ch ->
            map.getOrPut(ch) { mutableListOf() }.add(wifi.ssid)
        }
    }

    if (showEmpty) {
        val bandsToFill = if (band == null)
            listOf("2.4 GHz", "5 GHz", "6 GHz")
        else listOf(band)
        bandsToFill.forEach { b ->
            allChannelsOfBand(b).forEach { ch ->
                map.getOrPut(ch) { mutableListOf() }
            }
        }
    }

    return map.entries
        .sortedBy { it.key }
        .map { ChannelUsage(it.key, it.value.size, it.value) }
}

// 各频段的标准信道列表
fun allChannelsOfBand(band: String): List<Int> = when (band) {
    "2.4 GHz" -> (1..14).toList()
    // 5 GHz 常用信道（UNII-1/2/2e/3），可依据需要增删
    "5 GHz" -> listOf(
        36, 40, 44, 48,
        52, 56, 60, 64,
        100, 104, 108, 112, 116, 120, 124, 128, 132, 136, 140, 144,
        149, 153, 157, 161, 165
    )
    // 6 GHz 全部 20MHz 信道（1,5,9,...,233）
    "6 GHz" -> (1..233 step 4).toList()
    else -> emptyList()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelAnalysisScreen(
    wifiList: List<WifiData>,
    onBack: () -> Unit
) {
    var band by remember { mutableStateOf<String?>(null) }
    var useOccupied by remember { mutableStateOf(true) }
    var showEmpty by remember { mutableStateOf(false) }

    // ★ 新增：弱信号过滤状态
    var ignoreWeak by remember { mutableStateOf(false) }
    var weakThreshold by remember { mutableStateOf(-82) }   // 默认 -82 dBm

    val usage = remember(wifiList, band, useOccupied, showEmpty, ignoreWeak, weakThreshold) {
        analyzeChannels(
            list = wifiList,
            band = band,
            useOccupied = useOccupied,
            showEmpty = showEmpty,
            ignoreWeak = ignoreWeak,
            weakThreshold = weakThreshold
        )
    }
    val maxCount = usage.maxOfOrNull { it.count } ?: 1

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("信道使用分析") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // 频段筛选
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf<Pair<String?, String>>(
                    null to "全部", "2.4 GHz" to "2.4G",
                    "5 GHz" to "5G", "6 GHz" to "6G"
                ).forEach { (value, label) ->
                    FilterChip(
                        selected = band == value,
                        onClick = { band = value },
                        label = { Text(label) }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // 统计口径开关
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = useOccupied, onCheckedChange = { useOccupied = it })
                Spacer(Modifier.width(8.dp))
                Text(
                    if (useOccupied) "覆盖占用（宽信道展开）" else "仅主信道",
                    fontSize = 13.sp, color = Color.Gray
                )
            }
            // 显示空白信道开关
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = showEmpty, onCheckedChange = { showEmpty = it })
                Spacer(Modifier.width(8.dp))
                Text(
                    if (showEmpty) "显示空白信道（0 占用）" else "仅显示有 AP 的信道",
                    fontSize = 13.sp, color = Color.Gray
                )
            }

            // ★ 新增：忽略弱信号开关 + 阈值滑块
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = ignoreWeak, onCheckedChange = { ignoreWeak = it })
                Spacer(Modifier.width(8.dp))
                Text(
                    if (ignoreWeak) "忽略弱信号（< $weakThreshold dBm）" else "统计全部信号",
                    fontSize = 13.sp, color = Color.Gray
                )
            }
            if (ignoreWeak) {
                // 阈值滑块：-100 ~ -50 dBm
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("阈值", fontSize = 13.sp, color = Color.Gray)
                    Slider(
                        value = weakThreshold.toFloat(),
                        onValueChange = { weakThreshold = it.toInt() },
                        valueRange = -100f..-50f,
                        steps = 49,   // 1 dBm 一档
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    )
                    Text("$weakThreshold dBm", fontSize = 13.sp)
                }
            }

            Spacer(Modifier.height(12.dp))

            if (usage.isEmpty()) {
                Text("暂无数据，请先返回列表扫描", color = Color.Gray)
                return@Column
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(usage) { item ->
                    ChannelBarRow(
                        item = item,
                        maxCount = maxCount,
                        onClick = { }
                    )
                }
            }
        }
    }
}
// 一行：信道号 + 横向条形 + 数量 + 颜色（0 绿 / 1 黄 / 2+ 红）
@Composable
fun ChannelBarRow(item: ChannelUsage, maxCount: Int, onClick: () -> Unit) {
    val ratio = item.count.toFloat() / maxCount.coerceAtLeast(1)
    val color = when {
        item.count == 0 -> Color(0xFF4CAF50)
        item.count == 1 -> Color(0xFFFFC107)
        item.count == 2 -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "CH ${item.channel}",
            fontSize = 13.sp,
            modifier = Modifier.width(56.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(18.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(ratio.coerceAtLeast(0.02f))
                    .background(color, RoundedCornerShape(4.dp))
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = "${item.count}",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(28.dp)
        )
    }
}