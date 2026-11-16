package top.pingwcy.wifianalyzer.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.pingwcy.wifianalyzer.model.ChannelUsage

@Composable
fun ChannelScoreDialog(
    item: ChannelUsage,
    onDismiss: () -> Unit
) {
    val s = item.score
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("CH ${item.channel} 评分详情") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // ---- 总分 ----
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text("综合得分：", fontSize = 14.sp)
                    Text(
                        "${s.score} / 100",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = scoreColor(s.score)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(s.grade, fontSize = 13.sp, color = Color.Gray)
                }

                Spacer(Modifier.height(4.dp))
                Text(
                    "平均信号 ${"%.1f".format(s.avgRssi)} dBm" +
                            (s.avgUtilizationPct?.let {
                                " · 平均利用率 ${"%.1f".format(it)}%"
                            } ?: " · 无 BSS Load 数据"),
                    fontSize = 12.sp,
                    color = Color.Gray
                )

                Spacer(Modifier.height(12.dp))
                Divider()
                Spacer(Modifier.height(12.dp))

                // ---- 扣分表 ----
                Text("扣分明细", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))

                ScoreLine("AP 数量（${s.apCount} 个）", -s.apCountPenalty)
                ScoreLine("信号强度（平均）", -s.rssiPenalty)
                ScoreLine(
                    "BSS Load" + (if (s.avgUtilizationPct == null) "（无数据）" else ""),
                    -s.bssLoadPenalty
                )
                Spacer(Modifier.height(4.dp))
                ScoreLine("合计扣分", -s.totalPenalty, bold = true)

                Spacer(Modifier.height(12.dp))
                Divider()
                Spacer(Modifier.height(12.dp))

                // ---- 每个 AP ----
                Text("各 AP 明细（${s.apScores.size}）", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                s.apScores.sortedByDescending { it.rssi }.forEach { ap ->
                    Text(
                        ap.ssid.ifBlank { "(隐藏)" },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        buildString {
                            append("RSSI ${ap.rssi} dBm（扣 ${ap.rssiPenalty}）")
                            if (ap.channelUtilizationPct != null) {
                                append(" · 利用率 ${"%.1f".format(ap.channelUtilizationPct)}%")
                                append("（扣 ${ap.bssLoadPenalty}）")
                                if (ap.stationCount != null) append(" · STA ${ap.stationCount}")
                            } else {
                                append(" · 无 BSS Load")
                            }
                        },
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                    Spacer(Modifier.height(6.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
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