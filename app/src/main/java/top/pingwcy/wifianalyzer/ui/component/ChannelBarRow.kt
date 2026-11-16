package top.pingwcy.wifianalyzer.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.pingwcy.wifianalyzer.model.ChannelUsage

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