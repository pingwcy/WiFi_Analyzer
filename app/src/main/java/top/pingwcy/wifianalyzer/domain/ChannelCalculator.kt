package top.pingwcy.wifianalyzer.domain

import top.pingwcy.wifianalyzer.model.ChannelWidth
import top.pingwcy.wifianalyzer.model.WifiData
import kotlin.math.abs

object ChannelCalculator {
    private val CH5_CHANNELS: List<Int> = buildList {
        for (ch in 36..48 step 4) add(ch)
        for (ch in 52..64 step 4) add(ch)
        for (ch in 100..144 step 4) add(ch)
        for (ch in 149..177 step 4) add(ch)
    }
    // 160MHz 标准段：每段 8 个 20MHz 子信道
    private val CH160_SEGMENTS: List<List<Int>> = listOf(
        listOf(36, 40, 44, 48, 52, 56, 60, 64),      // 中心 5250
        listOf(100, 104, 108, 112, 116, 120, 124, 128), // 中心 5570
        listOf(149, 153, 157, 161, 165, 169, 173, 177)  // 中心 5810（少见）
    )
    private fun ch160SegmentOf(primaryCh: Int): List<Int>? =
        CH160_SEGMENTS.firstOrNull { primaryCh in it }

    fun freqToChannel(freq: Int): Int = when {
        freq == 2484 -> 14
        freq in 2412..2472 -> (freq - 2412) / 5 + 1
        freq in 5000..5895 -> (freq - 5000) / 5
        freq in 5925..7125 -> (freq - 5950) / 5
        else -> 0
    }

    fun freqToBand(freq: Int): String = when {
        freq in 2400..2500 -> "2.4 GHz"
        freq in 4900..5900 -> "5 GHz"
        freq in 5925..7125 -> "6 GHz"
        else -> "未知"
    }

    fun occupiedChannels(wifi: WifiData): List<Int> {
        // 2.4GHz 的信道以 5MHz 间隔排列且频谱互相重叠，不能像 5/6GHz 那样按频段范围枚举，
        // 否则 20MHz 会错误地展开成 [1,2,3] 等多个信道。
        // 这里按「信道绑定」口径：20MHz 占 1 个，40MHz 占 2 个。
        if (freqToBand(wifi.frequency) == "2.4 GHz") {
            return occupiedChannels2Ghz(wifi)
        }

        val width = WifiStandards.widthMhz(wifi.channelWidth)

        // 80+80：两段不连续
        if (wifi.channelWidth == ChannelWidth.WIDTH_80MHZ_PLUS) {
            return (channelsCoveredBy(wifi.centerFreq0, 80) +
                    channelsCoveredBy(wifi.centerFreq1, 80)).distinct().sorted()
        }

        // 160MHz：按标准段直接枚举，不依赖 centerFreq0
        if (width == 160) {
            val primaryCh = freqToChannel(wifi.frequency)
            return ch160SegmentOf(primaryCh) ?: run {
                // 兜底：主信道不在标准段内，退回通用算法
                val center = if (wifi.centerFreq0 > 0) wifi.centerFreq0 else wifi.frequency
                channelsCoveredBy(center, width).distinct().sorted()
            }
        }

        // 优先用 centerFreq0（40/80 都有）
        if (wifi.centerFreq0 > 0) {
            return channelsCoveredBy(wifi.centerFreq0, width).distinct().sorted()
        }

        // 回退：没有 centerFreq0 时，用主信道频率 + 宽度估算
        return channelsCoveredBy(wifi.frequency, width).distinct().sorted()
    }

    /**
     * 计算某个 AP 对每个信道造成的「频谱干扰贡献」（等效负载）。
     *
     * 与 [occupiedChannels]（绑定口径）不同，这里额外考虑了 2.4GHz 的相邻信道重叠：
     *  - 2.4GHz：信道间隔 5MHz，一个 20MHz 信道会向相邻信道泄漏能量。
     *    对每个 20MHz 子信道，按重叠系数展开：Δ0=1.0、Δ1=0.75、Δ2=0.5、Δ3=0.25。
     *  - 5/6GHz：信道间隔 20MHz，基本不重叠，直接等于绑定信道的 1.0。
     */
    fun interferenceWeights(wifi: WifiData): Map<Int, Double> {
        return when (freqToBand(wifi.frequency)) {
            "2.4 GHz" -> {
                val result = mutableMapOf<Int, Double>()
                occupiedChannels2Ghz(wifi).forEach { ch ->
                    overlapSpread2Ghz(ch).forEach { (c, w) ->
                        result[c] = minOf(1.0, (result[c] ?: 0.0) + w)
                    }
                }
                result
            }
            else -> occupiedChannels(wifi).associateWith { 1.0 }
        }
    }

    /** 2.4GHz 单个 20MHz 信道的频谱足迹：自身 1.0，相邻信道按间隔衰减。 */
    private fun overlapSpread2Ghz(channel: Int): Map<Int, Double> {
        val coefficients = mapOf(0 to 1.0, 1 to 0.75, 2 to 0.5, 3 to 0.25)
        return buildMap {
            for (delta in -3..3) {
                val c = channel + delta
                val w = coefficients[abs(delta)] ?: 0.0
                if (c in 1..14 && w > 0.0) put(c, w)
            }
        }
    }

    /**
     * 2.4GHz 的占用信道计算。
     *
     * - 20MHz：只占主信道自身。
     * - 40MHz：占主信道 + 相邻扩展信道（信道绑定）。
     *
     * 40MHz 扩展信道方向用 [WifiData.centerFreq0] 与主信道频率比较得出：
     *  - centerFreq0 低于主信道频率 => 扩展信道在下方（primary - 4）
     *  - 否则（含缺失 centerFreq0）=> 默认在上方（primary + 4）
     */
    private fun occupiedChannels2Ghz(wifi: WifiData): List<Int> {
        val primary = wifi.primaryChannel.takeIf { it in 1..14 } ?: return emptyList()

        return when (WifiStandards.widthMhz(wifi.channelWidth)) {
            40 -> {
                val second = if (wifi.centerFreq0 > 0 && wifi.centerFreq0 < wifi.frequency) {
                    primary - 4
                } else {
                    primary + 4
                }
                listOf(primary, second)
                    .filter { it in 1..14 }
                    .distinct()
                    .sorted()
            }
            // 20MHz 及其它异常宽度：只占主信道
            else -> listOf(primary)
        }
    }

    private fun channelsCoveredBy(centerFreq: Int, widthMhz: Int): List<Int> {
        if (centerFreq <= 0 || widthMhz <= 0) return emptyList()
        val half = widthMhz / 2
        val lo = centerFreq - half
        val hi = centerFreq + half

        return when {
            // 5GHz：20MHz 步进，枚举 20MHz 子信道中心
            centerFreq in 4900..5900 -> CH5_CHANNELS
                .filter { (5000 + it * 5) in lo..hi }
            // 6GHz：20MHz 步进
            centerFreq in 5925..7125 -> buildList {
                var f = 5955          // 6GHz 信道 1 = 5955
                while (f <= 7125) {
                    if (f in lo..hi) {
                        val ch = subChannelFromFreq(f)
                        if (ch > 0) add(ch)
                    }
                    f += 20
                }
            }
            else -> emptyList()
        }
    }

    private fun subChannelFromFreq(freq: Int): Int = when {
        // 2.4GHz
        freq == 2484 -> 14
        freq in 2412..2472 && (freq - 2412) % 5 == 0 ->
            (freq - 2412) / 5 + 1

        // 5GHz
        freq in 5000..5895 && (freq - 5000) % 5 == 0 ->
            (freq - 5000) / 5

        // 6GHz
        freq in 5955..7115 && (freq - 5950) % 20 == 5 ->
            (freq - 5950) / 5

        else -> 0
    }
}
