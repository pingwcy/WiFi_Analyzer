package top.pingwcy.wifianalyzer.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.TipsAndUpdates
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.pingwcy.wifianalyzer.R
import top.pingwcy.wifianalyzer.domain.ChannelAnalyzer
import top.pingwcy.wifianalyzer.domain.ChannelRecommendation
import top.pingwcy.wifianalyzer.domain.ChannelRecommender
import top.pingwcy.wifianalyzer.domain.ChannelUsage
import top.pingwcy.wifianalyzer.model.WifiData
import top.pingwcy.wifianalyzer.ui.component.ChannelBarRow
import top.pingwcy.wifianalyzer.ui.component.ChannelInfoDialog
import top.pingwcy.wifianalyzer.ui.component.ChannelScoreDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelAnalysisScreen(
    wifiList: List<WifiData>,
    onBack: () -> Unit
) {
    var band by remember { mutableStateOf<String?>(null) }
    var useOccupied by remember { mutableStateOf(true) }
    var showEmpty by remember { mutableStateOf(false) }
    var ignoreWeak by remember { mutableStateOf(false) }
    var weakThreshold by remember { mutableStateOf(-82) }
    var selectedChannel by remember { mutableStateOf<ChannelUsage?>(null) }
    var showMenu by remember { mutableStateOf(false) }
    var showChannelInfoDialog by remember { mutableStateOf(false) }
    var showRecommendation by remember { mutableStateOf(true) }

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
    val maxLoad = usage.maxOfOrNull { it.load }?.takeIf { it > 0 } ?: 1.0

    // 信道推荐（仅当选中具体频段时显示）
    val recommendation = remember(wifiList, band) {
        band?.let { ChannelRecommender.recommend(wifiList, it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.channel_analysis_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.btn_back))
                    }
                },
                actions = {
                    IconButton(onClick = { showMenu = !showMenu }) {
                        Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.menu_more))
                    }
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
                        if (!showRecommendation && band != null) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_show_recommendation)) },
                                onClick = {
                                    showMenu = false
                                    showRecommendation = true
                                }
                            )
                        }
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
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
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

            // 推荐信道
            if (showRecommendation) {
                recommendation?.let {
                    RecommendationCard(it, onClose = { showRecommendation = false })
                    Spacer(Modifier.height(8.dp))
                }
            }

            // 统计口径开关（整行可点击）
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { useOccupied = !useOccupied }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Switch(checked = useOccupied, onCheckedChange = { useOccupied = it })
                Spacer(Modifier.width(8.dp))
                Text(
                    if (useOccupied) stringResource(R.string.toggle_use_occupied_on)
                    else stringResource(R.string.toggle_use_occupied_off),
                    fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 显示空白信道开关（整行可点击）
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showEmpty = !showEmpty }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Switch(checked = showEmpty, onCheckedChange = { showEmpty = it })
                Spacer(Modifier.width(8.dp))
                Text(
                    if (showEmpty) stringResource(R.string.toggle_show_empty_on)
                    else stringResource(R.string.toggle_show_empty_off),
                    fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 忽略弱信号开关 + 阈值滑块（整行可点击）
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { ignoreWeak = !ignoreWeak }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Switch(checked = ignoreWeak, onCheckedChange = { ignoreWeak = it })
                Spacer(Modifier.width(8.dp))
                Text(
                    if (ignoreWeak) stringResource(R.string.toggle_ignore_weak_on, weakThreshold)
                    else stringResource(R.string.toggle_ignore_weak_off),
                    fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (ignoreWeak) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.label_threshold), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                Text(stringResource(R.string.no_data_hint), color = MaterialTheme.colorScheme.onSurfaceVariant)
                return@Column
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(usage) { item ->
                    ChannelBarRow(
                        item = item,
                        maxLoad = maxLoad,
                        onClick = { selectedChannel = item }
                    )
                }
            }
        }
    }

    selectedChannel?.let { item ->
        ChannelScoreDialog(item = item, onDismiss = { selectedChannel = null })
    }

    if (showChannelInfoDialog) {
        ChannelInfoDialog(onDismiss = { showChannelInfoDialog = false })
    }
}

@Composable
private fun RecommendationCard(recommendation: ChannelRecommendation, onClose: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.TipsAndUpdates,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.recommendation_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onClose) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.recommendation_close)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.recommendation_best, recommendation.recommended),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.recommendation_hint),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                recommendation.candidates.forEach { c ->
                    val isRecommended = c.channel == recommendation.recommended
                    CandidateChip(c.channel, c.load, isRecommended)
                }
            }
        }
    }
}

@Composable
private fun CandidateChip(channel: Int, load: Double, recommended: Boolean) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (recommended) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            stringResource(R.string.channel_label, channel),
            fontSize = 13.sp,
            fontWeight = if (recommended) FontWeight.Bold else FontWeight.Normal,
            color = if (recommended) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (load > 0.0) {
            Text(
                "%.1f".format(load),
                fontSize = 10.sp,
                color = if (recommended) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
