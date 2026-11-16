package top.pingwcy.wifianalyzer.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.pingwcy.wifianalyzer.ui.component.ChannelBarRow
import top.pingwcy.wifianalyzer.domain.ChannelAnalyzer
import top.pingwcy.wifianalyzer.model.WifiData

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
