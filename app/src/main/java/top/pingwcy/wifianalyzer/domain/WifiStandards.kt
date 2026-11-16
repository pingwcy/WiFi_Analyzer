package top.pingwcy.wifianalyzer.domain

import android.net.wifi.ScanResult

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

    fun widthText(width: Int): String = when (width) {
        ScanResult.CHANNEL_WIDTH_20MHZ -> "20 MHz"
        ScanResult.CHANNEL_WIDTH_40MHZ -> "40 MHz"
        ScanResult.CHANNEL_WIDTH_80MHZ -> "80 MHz"
        ScanResult.CHANNEL_WIDTH_160MHZ -> "160 MHz"
        ScanResult.CHANNEL_WIDTH_80MHZ_PLUS_MHZ -> "80+80 MHz"
        else -> "$width MHz"
    }

    fun widthMhz(width: Int): Int = when (width) {
        ScanResult.CHANNEL_WIDTH_20MHZ -> 20
        ScanResult.CHANNEL_WIDTH_40MHZ -> 40
        ScanResult.CHANNEL_WIDTH_80MHZ -> 80
        ScanResult.CHANNEL_WIDTH_160MHZ -> 160
        ScanResult.CHANNEL_WIDTH_80MHZ_PLUS_MHZ -> 80
        else -> 0
    }

    fun occupiedChannelCount(width: Int): Int = when (width) {
        android.net.wifi.ScanResult.CHANNEL_WIDTH_20MHZ -> 1
        android.net.wifi.ScanResult.CHANNEL_WIDTH_40MHZ -> 2
        android.net.wifi.ScanResult.CHANNEL_WIDTH_80MHZ -> 4
        android.net.wifi.ScanResult.CHANNEL_WIDTH_160MHZ -> 8
        android.net.wifi.ScanResult.CHANNEL_WIDTH_80MHZ_PLUS_MHZ -> 8
        else -> 1
    }

}