package top.pingwcy.wifianalyzer.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.pingwcy.wifianalyzer.domain.ChannelUsage
import androidx.compose.ui.res.stringResource
import top.pingwcy.wifianalyzer.R
import top.pingwcy.wifianalyzer.domain.Grade
import top.pingwcy.wifianalyzer.ui.theme.WiFiAnalyzerTheme
import top.pingwcy.wifianalyzer.ui.theme.scoreColor

@Composable
fun ChannelBarRow(
    item: ChannelUsage,
    maxLoad: Double,
    onClick: () -> Unit
) {
    val s = item.score
    val color = scoreColor(s.score)
    // 条形图高度按等效负载（真实频谱占用）；空信道不画条
    val fraction = if (item.load > 0.0) {
        ((item.load / maxLoad).toFloat()).coerceIn(0f, 1f).coerceAtLeast(0.02f)
    } else {
        0f
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.channel_label, item.channel),
            fontSize = 13.sp,
            modifier = Modifier.width(56.dp)
        )
        // 进度条：高度 = 等效负载占比，颜色 = 综合评分
        Box(
            modifier = Modifier
                .weight(1f)
                .height(18.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction)
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
        val gradeText = when (s.grade) {
            Grade.EXCELLENT -> stringResource(R.string.grade_excellent)
            Grade.GOOD -> stringResource(R.string.grade_good)
            Grade.FAIR -> stringResource(R.string.grade_fair)
            Grade.CROWDED -> stringResource(R.string.grade_crowded)
        }

        Text(
            text = gradeText,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ChannelBarRowPreview() {
    WiFiAnalyzerTheme {
        ChannelBarRow(
            item = sampleChannelUsage,
            maxLoad = 2.0,
            onClick = {}
        )
    }
}
