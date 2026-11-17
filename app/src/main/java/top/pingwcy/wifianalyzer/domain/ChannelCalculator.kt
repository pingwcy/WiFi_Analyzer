package top.pingwcy.wifianalyzer.domain

import top.pingwcy.wifianalyzer.model.WifiData
import android.net.wifi.ScanResult

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
        // freq in 4915..4980 -> (freq - 4915) / 5
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
        val width = WifiStandards.widthMhz(wifi.channelWidth)

        // 80+80：两段不连续
        if (wifi.channelWidth == ScanResult.CHANNEL_WIDTH_80MHZ_PLUS_MHZ) {
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
        // 2.4GHz 20MHz：中心即主信道频率，宽度 20 → 覆盖 5 个信道（±10MHz 内）
        // 5/6GHz 20MHz：只覆盖自己
        return channelsCoveredBy(wifi.frequency, width).distinct().sorted()
    }
    private fun channelsCoveredBy(centerFreq: Int, widthMhz: Int): List<Int> {
        if (centerFreq <= 0 || widthMhz <= 0) return emptyList()
        val half = widthMhz / 2
        val lo = centerFreq - half
        val hi = centerFreq + half

        return when {
            // 2.4GHz：5MHz 步进枚举（信道间隔就是 5MHz，重叠按 5MHz 算）
            centerFreq in 2400..2500 -> buildList {
                var f = 2412
                while (f <= 2472) {
                    if (f in lo..hi) add((f - 2412) / 5 + 1)
                    f += 5
                }
                if (2484 in lo..hi) add(14)
            }
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