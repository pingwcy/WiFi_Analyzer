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
import androidx.compose.ui.res.stringResource
import top.pingwcy.wifianalyzer.R

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
    val bandOptions = listOf(
        null to R.string.band_all,
        "2.4 GHz" to R.string.band_2g_short,
        "5 GHz" to R.string.band_5g_short,
        "6 GHz" to R.string.band_6g_short
    )
    bandOptions.forEach { (value, labelRes) ->
        FilterChip(
            selected = band == value,
            onClick = { band = value },
            label = { Text(stringResource(labelRes)) }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.channel_analysis_title)) },
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
                            text = { Text(stringResource(R.string.menu_local_channels)) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // 频段筛选
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // 左边是业务逻辑用的字符串（不翻译），右边是 UI 显示的资源 ID（翻译）
                val bandOptions = listOf(
                    null to R.string.band_all,
                    "2.4 GHz" to R.string.band_2g_short,
                    "5 GHz" to R.string.band_5g_short,
                    "6 GHz" to R.string.band_6g_short
                )

                bandOptions.forEach { (value, labelRes) ->
                    FilterChip(
                        selected = band == value,
                        onClick = { band = value },
                        label = { Text(stringResource(labelRes)) }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))

            // 统计口径开关
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = useOccupied, onCheckedChange = { useOccupied = it })
                Spacer(Modifier.width(8.dp))
                Text(
                    if (useOccupied) stringResource(R.string.toggle_use_occupied_on)
                    else stringResource(R.string.toggle_use_occupied_off),
                    fontSize = 13.sp, color = Color.Gray
                )
            }
            // 显示空白信道开关
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = showEmpty, onCheckedChange = { showEmpty = it })
                Spacer(Modifier.width(8.dp))
                Text(
                    if (showEmpty) stringResource(R.string.toggle_show_empty_on)
                    else stringResource(R.string.toggle_show_empty_off),
                    fontSize = 13.sp, color = Color.Gray
                )
            }

            // 忽略弱信号开关 + 阈值滑块
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = ignoreWeak, onCheckedChange = { ignoreWeak = it })
                Spacer(Modifier.width(8.dp))
                Text(
                    if (ignoreWeak) stringResource(R.string.toggle_ignore_weak_on, weakThreshold)
                    else stringResource(R.string.toggle_ignore_weak_off),
                    fontSize = 13.sp, color = Color.Gray
                )
            }
            if (ignoreWeak) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.label_threshold), fontSize = 13.sp, color = Color.Gray)
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
                Text(stringResource(R.string.no_data_hint), color = Color.Gray)
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
                        Text(
                            stringResource(R.string.dialog_channel_count, supportedFrequencies.size),
                            color = Color(0xFF4CAF50)
                        )

                        Spacer(Modifier.height(8.dp))

                        supportedFrequencies.forEach { freq ->
                            val channel = frequencyToChannel(freq)

                            val bandName = when (freq) {
                                in 2400..2500 -> stringResource(R.string.band_2g)
                                in 4900..5900 -> stringResource(R.string.band_5g)
                                in 5925..7125 -> stringResource(R.string.band_6g)
                                else -> stringResource(R.string.band_other)
                            }

                            Text(
                                stringResource(R.string.dialog_channel_line, bandName, channel, freq),
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Text(
                        stringResource(R.string.dialog_note),
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
                    Text(stringResource(R.string.btn_ok))
                }
            }
        )
    }
}

