package top.pingwcy.wifianalyzer.domain

import top.pingwcy.wifianalyzer.model.ChannelWidth

object WifiStandards {

    fun toString(standard: Int): String = when (standard) {
        -1 -> "未知"
        1 -> "802.11a"
        2 -> "802.11b"
        3 -> "802.11g"
        4 -> "802.11n (WiFi 4)"
        5 -> "802.11ac (WiFi 5)"
        6 -> "802.11ax (WiFi 6)"
        7 -> "802.11ad"
        8 -> "802.11be (WiFi 7)"
        else -> "未知 ($standard)"
    }

    fun widthText(width: ChannelWidth): String = when (width) {
        ChannelWidth.WIDTH_20MHZ -> "20 MHz"
        ChannelWidth.WIDTH_40MHZ -> "40 MHz"
        ChannelWidth.WIDTH_80MHZ -> "80 MHz"
        ChannelWidth.WIDTH_160MHZ -> "160 MHz"
        ChannelWidth.WIDTH_80MHZ_PLUS -> "80+80 MHz"
    }

    fun widthMhz(width: ChannelWidth): Int = width.mhz

    fun occupiedChannelCount(width: ChannelWidth): Int = when (width) {
        ChannelWidth.WIDTH_20MHZ -> 1
        ChannelWidth.WIDTH_40MHZ -> 2
        ChannelWidth.WIDTH_80MHZ -> 4
        ChannelWidth.WIDTH_160MHZ -> 8
        ChannelWidth.WIDTH_80MHZ_PLUS -> 8
    }
}
