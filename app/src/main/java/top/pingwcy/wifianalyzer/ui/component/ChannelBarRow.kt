package top.pingwcy.wifianalyzer.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.pingwcy.wifianalyzer.domain.ChannelScore
import top.pingwcy.wifianalyzer.model.ChannelUsage

@Composable
fun ChannelBarRow(
    item: ChannelUsage,
    maxCount: Int,
    onClick: () -> Unit
) {
    val s = item.score
    // 颜色按综合分：越高越绿
    val color = when {
        s.score >= 80 -> Color(0xFF4CAF50)
        s.score >= 60 -> Color(0xFF8BC34A)
        s.score >= 40 -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "CH ${item.channel}",
            fontSize = 13.sp,
            modifier = Modifier.width(56.dp)
        )
        // 进度条用综合分
        Box(
            modifier = Modifier
                .weight(1f)
                .height(18.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth((s.score / 100f).coerceAtLeast(0.02f))
                    .background(color, RoundedCornerShape(4.dp))
            )
        }
        Spacer(Modifier.width(8.dp))
        // 右侧显示分数
        Text(
            text = "${s.score}",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.width(34.dp)
        )
        Text(
            text = s.grade,
            fontSize = 11.sp,
            color = Color.Gray,
            modifier = Modifier.width(34.dp)
        )
    }
}