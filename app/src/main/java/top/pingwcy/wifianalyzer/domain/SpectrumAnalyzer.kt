package top.pingwcy.wifianalyzer.domain

import top.pingwcy.wifianalyzer.model.ChannelWidth
import top.pingwcy.wifianalyzer.model.WifiData
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos

/** 频率域上的一点：频率（MHz）与该点的归一化信号高度（0..1）。 */
data class SpectrumPoint(
    val frequencyMhz: Int,
    val height: Float,
)

/** AP 在频率轴上的一段占用区间（80+80 会拆成两段）。 */
data class FreqSegment(
    val centerMhz: Int,
    val widthMhz: Int,
) {
    val lowMhz: Int get() = centerMhz - widthMhz / 2
    val highMhz: Int get() = centerMhz + widthMhz / 2
}

/** 单个 AP 的频段占用信息，用于频谱图下方的图例列表。 */
data class ApBand(
    val bssid: String,
    val ssid: String,
    val rssi: Int,
    val primaryChannel: Int,
    val channelWidth: ChannelWidth,
    val segments: List<FreqSegment>,
)

/**
 * 将 WiFi 列表转换为真实频率域的「频谱」。
 *
 * 与常见的「离散信道条形图」不同，这里：
 *  - 横轴是连续频率 MHz；
 *  - 以 AP 的真实 [WifiData.centerFreq0]（缺失时用主信道频率）为能量中心；
 *  - 以 [WifiData.channelWidth] 决定带宽，并用「升余弦频谱掩码」描述能量分布：
 *    带宽内平坦（接近满功率），边缘滚降，带外为零 —— 与 OFDM 的真实频谱更接近；
 *  - 80+80 视为两段独立的 80MHz。
 *
 * 相邻 AP 在频率上重叠时，能量自然叠加，从而真实反映频谱占用。
 */
object SpectrumAnalyzer {

    /** RSSI 映射范围：-30 dBm 视为最强（1.0），-100 dBm 视为底噪（0.0）。 */
    private const val MAX_RSSI = -30
    private const val MIN_RSSI = -100

    /** 频谱掩码滚降系数（roll-off）：带宽内 (1-β)·half 为平坦区，向外滚降到 (1+β)·half 为零。 */
    private const val ROLL_OFF = 0.35f

    /** 每个频段的采样区间（MHz），闭区间。 */
    private fun bandRange(band: String): IntRange? = when (band) {
        "2.4 GHz" -> 2400..2500
        "5 GHz" -> 5150..5895
        "6 GHz" -> 5925..7125
        else -> null
    }

    /**
     * @param band 频段字符串，与 [WifiData.band] 保持一致（"2.4 GHz" / "5 GHz" / "6 GHz"）。
     * @return 按 1 MHz 等间隔采样得到的频谱点列表。
     */
    fun analyze(list: List<WifiData>, band: String): List<SpectrumPoint> {
        val range = bandRange(band) ?: return emptyList()
        val filtered = list.filter { it.band == band }

        return (range.first..range.last).map { freq ->
            var height = 0f
            for (wifi in filtered) {
                height = maxOf(height, wifiContributionAt(wifi, freq))
            }
            SpectrumPoint(frequencyMhz = freq, height = height)
        }
    }

    /** 单个 AP 在给定频率点的贡献高度（0..1）。 */
    private fun wifiContributionAt(wifi: WifiData, freq: Int): Float {
        val signalHeight = signalHeight(wifi.rssi)
        if (signalHeight <= 0f) return 0f

        var best = 0f
        for ((center, width) in segmentsOf(wifi)) {
            best = maxOf(best, signalHeight * spectralMask(center, width, freq))
        }
        return best
    }

    /** 返回该 AP 的「(中心频率, 带宽 MHz)」段；80+80 拆成两段。 */
    private fun segmentsOf(wifi: WifiData): List<Pair<Int, Int>> {
        return when (wifi.channelWidth) {
            ChannelWidth.WIDTH_80MHZ_PLUS -> listOf(
                wifi.centerFreq0 to 80,
                wifi.centerFreq1 to 80,
            ).filter { it.first > 0 }

            else -> {
                val width = WifiStandards.widthMhz(wifi.channelWidth).let { if (it > 0) it else 20 }
                val center = if (wifi.centerFreq0 > 0) wifi.centerFreq0 else wifi.frequency
                listOf(center to width)
            }
        }
    }

    /** RSSI -> 0..1 高度。 */
    private fun signalHeight(rssi: Int): Float {
        val clamped = rssi.coerceIn(MIN_RSSI, MAX_RSSI)
        return (clamped - MIN_RSSI).toFloat() / (MAX_RSSI - MIN_RSSI)
    }

    /**
     * 升余弦频谱掩码：带宽内平坦（1.0），边缘平滑滚降，带外为 0。
     */
    private fun spectralMask(center: Int, width: Int, freq: Int): Float {
        if (center <= 0 || width <= 0) return 0f
        val half = width / 2f
        val d = abs(freq - center).toFloat()
        val flatEdge = (1f - ROLL_OFF) * half
        val fullEdge = (1f + ROLL_OFF) * half

        return when {
            d <= flatEdge -> 1f
            d >= fullEdge -> 0f
            else -> {
                val phase = (PI * (d - flatEdge) / (2.0 * ROLL_OFF * half)).toFloat()
                0.5f * (1f + cos(phase))
            }
        }
    }

    /**
     * 逐个 AP 计算其独立频谱（bssid -> 频谱点列表）。
     *
     * 用于在频谱图中高亮某个 AP 的真实能量分布。
     */
    fun analyzePerAp(list: List<WifiData>, band: String): Map<String, List<SpectrumPoint>> {
        val range = bandRange(band) ?: return emptyMap()
        val filtered = list.filter { it.band == band }
        return filtered.associate { wifi ->
            wifi.bssid to (range.first..range.last).map { freq ->
                SpectrumPoint(freq, wifiContributionAt(wifi, freq))
            }
        }
    }

    /**
     * 返回指定频段内每个 AP 的占用区间，按信号强度降序排列。
     */
    fun apBands(list: List<WifiData>, band: String): List<ApBand> {
        return list.filter { it.band == band }
            .map { wifi ->
                ApBand(
                    bssid = wifi.bssid,
                    ssid = wifi.ssid,
                    rssi = wifi.rssi,
                    primaryChannel = wifi.primaryChannel,
                    channelWidth = wifi.channelWidth,
                    segments = segmentsOf(wifi).map { (center, width) -> FreqSegment(center, width) },
                )
            }
            .sortedByDescending { it.rssi }
    }
}
