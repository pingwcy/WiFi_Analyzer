package top.pingwcy.wifianalyzer.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import top.pingwcy.wifianalyzer.model.RssiSample

/**
 * 信号历史折线图（Canvas 自绘）。
 *
 * 横轴为采样序号，纵轴为 RSSI（-30 ~ -100 dBm）。样本按时间顺序传入。
 */
@Composable
fun RssiTrendChart(
    samples: List<RssiSample>,
    modifier: Modifier = Modifier,
) {
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
    ) {
        if (samples.size < 2) return@Canvas

        val minRssi = -100f
        val maxRssi = -30f
        val range = maxRssi - minRssi

        fun yFor(rssi: Int): Float {
            val clamped = rssi.toFloat().coerceIn(minRssi, maxRssi)
            return (maxRssi - clamped) / range * size.height
        }

        fun xFor(index: Int): Float =
            index.toFloat() / (samples.size - 1) * size.width

        // 参考网格线（-50 / -70 / -90 dBm）
        for (rssi in listOf(-50, -70, -90)) {
            val y = yFor(rssi)
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f,
            )
        }

        // 折线
        val path = Path()
        samples.forEachIndexed { index, sample ->
            val x = xFor(index)
            val y = yFor(sample.rssi)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color = lineColor, style = Stroke(width = 2.dp.toPx()))

        // 数据点
        samples.forEachIndexed { index, sample ->
            drawCircle(
                color = lineColor,
                radius = 3.dp.toPx(),
                center = Offset(xFor(index), yFor(sample.rssi))
            )
        }
    }
}
