package top.pingwcy.wifianalyzer.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.pingwcy.wifianalyzer.domain.SpectrumPoint

/** 频谱图上的信道标注。 */
data class ChannelMark(
    val frequencyMhz: Int,
    val label: String,
)

/**
 * 真实频率域频谱图（Canvas 自绘）。
 *
 * 横轴为连续频率 MHz，纵轴为归一化信号高度（0..1）。重叠区域能量自然叠加。
 * 支持「高亮单个 AP」：传入 [highlightedSpectrum] 时，整体频谱淡化，只突出该 AP 的频谱掩码。
 */
@OptIn(ExperimentalTextApi::class)
@Composable
fun SpectrumChart(
    points: List<SpectrumPoint>,
    channelMarks: List<ChannelMark>,
    highlightedSpectrum: List<SpectrumPoint>? = null,
    highlightColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    val fillColor = MaterialTheme.colorScheme.primary
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
    ) {
        if (points.isEmpty()) return@Canvas

        val minFreq = points.first().frequencyMhz
        val maxFreq = points.last().frequencyMhz
        val freqRange = (maxFreq - minFreq).coerceAtLeast(1)

        fun xFor(freq: Int): Float =
            (freq - minFreq).toFloat() / freqRange.toFloat() * size.width

        fun yFor(height: Float): Float =
            size.height - height * size.height

        fun buildPaths(data: List<SpectrumPoint>): Pair<Path, Path> {
            val line = Path()
            val fill = Path()
            data.forEachIndexed { index, p ->
                val x = xFor(p.frequencyMhz)
                val y = yFor(p.height)
                if (index == 0) {
                    line.moveTo(x, y)
                    fill.moveTo(x, y)
                } else {
                    line.lineTo(x, y)
                    fill.lineTo(x, y)
                }
            }
            fill.lineTo(size.width, size.height)
            fill.lineTo(0f, size.height)
            fill.close()
            return line to fill
        }

        // 水平网格线（0% / 50% / 100%）
        for (ratio in listOf(0f, 0.5f, 1f)) {
            val y = size.height - ratio * size.height
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f,
            )
        }

        if (highlightedSpectrum != null) {
            // 整体频谱淡化
            val (overallLine, overallFill) = buildPaths(points)
            drawPath(overallFill, color = fillColor.copy(alpha = 0.06f))
            drawPath(overallLine, color = lineColor.copy(alpha = 0.2f), style = Stroke(width = 1.dp.toPx()))

            // 高亮 AP 频谱
            val (hlLine, hlFill) = buildPaths(highlightedSpectrum)
            drawPath(hlFill, color = highlightColor.copy(alpha = 0.35f))
            drawPath(hlLine, color = highlightColor, style = Stroke(width = 3.dp.toPx()))
        } else {
            val (line, fill) = buildPaths(points)
            drawPath(fill, color = fillColor.copy(alpha = 0.25f))
            drawPath(line, color = lineColor, style = Stroke(width = 2.dp.toPx()))
        }

        // 信道标注竖线 + 文字
        val labelStyle = TextStyle(fontSize = 10.sp, color = labelColor)
        channelMarks.forEach { mark ->
            val x = xFor(mark.frequencyMhz)
            drawLine(
                color = gridColor,
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = 1f,
            )
            val layout = textMeasurer.measure(mark.label, style = labelStyle)
            drawText(
                textLayoutResult = layout,
                topLeft = Offset(
                    x = (x - layout.size.width / 2f).coerceIn(0f, size.width - layout.size.width),
                    y = size.height - layout.size.height,
                )
            )
        }
    }
}
