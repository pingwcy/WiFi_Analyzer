package top.pingwcy.wifianalyzer.ui.screen

import android.content.Context
import android.net.wifi.WifiAvailableChannel
import android.net.wifi.WifiManager
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.pingwcy.wifianalyzer.domain.ChannelAnalyzer
import top.pingwcy.wifianalyzer.model.WifiData
import top.pingwcy.wifianalyzer.ui.component.ChannelBarRow
import android.util.Log
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import top.pingwcy.wifianalyzer.model.ChannelUsage
import  top.pingwcy.wifianalyzer.ui.component.ChannelScoreDialog
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelAnalysisScreen(
    wifiList: List<WifiData>,
    onBack: () -> Unit
) {
    // 各种状态
    var band by remember { mutableStateOf<String?>(null) }
    var useOccupied by remember { mutableStateOf(true) }
    var showEmpty by remember { mutableStateOf(false) }
    var ignoreWeak by remember { mutableStateOf(false) }
    var weakThreshold by remember { mutableStateOf(-82) }
    var selectedChannel by remember { mutableStateOf<ChannelUsage?>(null) }
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    var showChannelInfoDialog by remember { mutableStateOf(false) }

    // 数据分析
    val usage = remember(wifiList, band, useOccupied, showEmpty, ignoreWeak, weakThreshold) {
        ChannelAnalyzer.analyze(
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
                actions = {
                    // 三个点按钮
                    IconButton(onClick = { showMenu = !showMenu }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "更多")
                    }
                    // 下拉菜单（千万注意大括号的位置）
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("本机可用信道") },
                            onClick = {
                                showMenu = false
                                showChannelInfoDialog = true
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }

    ) { padding ->
        // ... 下面的 Column 和里面的所有内容，保持你原来的代码不变 ...
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

            // 忽略弱信号开关 + 阈值滑块
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = ignoreWeak, onCheckedChange = { ignoreWeak = it })
                Spacer(Modifier.width(8.dp))
                Text(
                    if (ignoreWeak) "忽略弱信号（< $weakThreshold dBm）" else "统计全部信号",
                    fontSize = 13.sp, color = Color.Gray
                )
            }
            if (ignoreWeak) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("阈值", fontSize = 13.sp, color = Color.Gray)
                    Slider(
                        value = weakThreshold.toFloat(),
                        onValueChange = { weakThreshold = it.toInt() },
                        valueRange = -100f..-50f,
                        steps = 49,
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
                        onClick = {selectedChannel = item }
                    )
                }

            }

        }

    }
    selectedChannel?.let { item ->
        ChannelScoreDialog(item = item, onDismiss = { selectedChannel = null })
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

