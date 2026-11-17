package top.pingwcy.wifianalyzer.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.pingwcy.wifianalyzer.R
import top.pingwcy.wifianalyzer.domain.ChannelUsage
import top.pingwcy.wifianalyzer.domain.Grade
@Composable
fun ChannelScoreDialog(
    item: ChannelUsage,
    onDismiss: () -> Unit
) {
    val s = item.score
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.score_dialog_title, item.channel)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // ---- 总分 ----
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text(stringResource(R.string.score_total_label), fontSize = 14.sp)
                    Text(
                        stringResource(R.string.score_total_value, s.score),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = scoreColor(s.score)
                    )

                    // 把刚才漏掉的间距加回来
                    Spacer(Modifier.width(8.dp))

                    val gradeText = when (s.grade) {
                        Grade.EXCELLENT -> stringResource(R.string.grade_excellent)
                        Grade.GOOD -> stringResource(R.string.grade_good)
                        Grade.FAIR -> stringResource(R.string.grade_fair)
                        Grade.CROWDED -> stringResource(R.string.grade_crowded)
                    }
                    Text(gradeText, fontSize = 13.sp, color = Color.Gray)
                }
                Spacer(Modifier.height(4.dp))

                // 拼接平均信号和利用率
                val avgSignalStr = stringResource(R.string.score_avg_signal, "%.1f".format(s.avgRssi))
                val utilStr = s.avgUtilizationPct?.let {
                    stringResource(R.string.score_avg_utilization, "%.1f".format(it))
                } ?: stringResource(R.string.score_no_bss_load)

                Text(
                    avgSignalStr + utilStr,
                    fontSize = 12.sp,
                    color = Color.Gray
                )

                Spacer(Modifier.height(12.dp))
                HorizontalDivider() // 注意：Material3 建议用 HorizontalDivider 而不是 Divider
                Spacer(Modifier.height(12.dp))

                // ---- 扣分表 ----
                Text(stringResource(R.string.score_details_title), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))

                ScoreLine(stringResource(R.string.score_ap_count, s.apCount), -s.apCountPenalty)
                ScoreLine(stringResource(R.string.score_signal_strength), -s.rssiPenalty)

                val bssLoadLabel = if (s.avgUtilizationPct == null) {
                    stringResource(R.string.score_bss_load_no_data)
                } else {
                    stringResource(R.string.score_bss_load)
                }
                ScoreLine(bssLoadLabel, -s.bssLoadPenalty)

                Spacer(Modifier.height(4.dp))
                ScoreLine(stringResource(R.string.score_total_penalty), -s.totalPenalty, bold = true)

                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))

                // ---- 每个 AP ----
                Text(stringResource(R.string.score_ap_details_title, s.apScores.size), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                s.apScores.sortedByDescending { it.rssi }.forEach { ap ->
                    // 隐藏网络显示处理
                    Text(
                        ap.ssid.ifBlank { stringResource(R.string.score_hidden_ssid) },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )

                    // 拼接 AP 详情（这里用 buildString 配合 stringResource）
                    val apDetailText = buildString {
                        append(stringResource(R.string.score_ap_rssi_penalty, ap.rssi, ap.rssiPenalty))
                        if (ap.channelUtilizationPct != null) {
                            append(stringResource(R.string.score_ap_utilization, "%.1f".format(ap.channelUtilizationPct), ap.bssLoadPenalty))
                            if (ap.stationCount != null) {
                                append(stringResource(R.string.score_ap_sta, ap.stationCount))
                            }
                        } else {
                            append(stringResource(R.string.score_ap_no_bss_load))
                        }
                    }

                    Text(
                        apDetailText,
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                    Spacer(Modifier.height(6.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_close)) }
        }
    )
}

@Composable
private fun ScoreLine(label: String, delta: Int, bold: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 13.sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
        Text(
            if (delta == 0) "0" else "$delta",
            fontSize = 13.sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            color = if (delta == 0) Color.Gray else Color(0xFFF44336)
        )
    }
}

private fun scoreColor(score: Int): Color = when {
    score >= 80 -> Color(0xFF4CAF50)
    score >= 60 -> Color(0xFF8BC34A)
    score >= 40 -> Color(0xFFFF9800)
    else -> Color(0xFFF44336)
}