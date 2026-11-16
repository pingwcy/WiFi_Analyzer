package top.pingwcy.wifianalyzer.domain

import top.pingwcy.wifianalyzer.model.WifiData
import android.net.wifi.ScanResult

object ChannelCalculator {

    fun freqToChannel(freq: Int): Int = when {
        freq == 2484 -> 14
        freq in 2412..2472 -> (freq - 2412) / 5 + 1
        freq in 4915..4980 -> (freq - 4915) / 5
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
        val p = wifi.primaryChannel
        if (width <= 20 || wifi.centerFreq0 <= 0) return listOf(p)
        if (wifi.band == "2.4 GHz") {
            return when {
                p in 1..7  -> listOf(p, p + 4)
                p in 8..13 -> listOf(p - 4, p)
                else -> listOf(p)
            }
        }
        if (wifi.channelWidth == ScanResult.CHANNEL_WIDTH_80MHZ_PLUS_MHZ) {
            return blockFromCenter(wifi.centerFreq0, 80) + blockFromCenter(wifi.centerFreq1, 80)
        }
        return blockFromCenter(wifi.centerFreq0, width)
    }

    private fun blockFromCenter(centerFreq: Int, widthMhz: Int): List<Int> {
        if (centerFreq <= 0) return emptyList()
        val subCount = widthMhz / 20
        val startFreq = centerFreq - (subCount / 2) * 20 + 10
        return (0 until subCount).mapNotNull { i ->
            subChannelFromFreq(startFreq + i * 20).takeIf { it > 0 }
        }
    }

    private fun subChannelFromFreq(freq: Int): Int = when {
        freq in 5000..5895 -> (freq - 5000) / 5
        freq in 5925..7125 -> (freq - 5950) / 5
        else -> 0
    }
}