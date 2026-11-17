package top.pingwcy.wifianalyzer.ui.component

import top.pingwcy.wifianalyzer.domain.ChannelScore
import top.pingwcy.wifianalyzer.domain.ChannelUsage
import top.pingwcy.wifianalyzer.domain.Grade
import top.pingwcy.wifianalyzer.model.ChannelWidth
import top.pingwcy.wifianalyzer.model.WifiData

/** Preview 专用示例数据。 */
internal val sampleWifi = WifiData(
    ssid = "MyHomeWiFi",
    bssid = "AA:BB:CC:DD:EE:FF",
    frequency = 5180,
    rssi = -55,
    channelWidth = ChannelWidth.WIDTH_80MHZ,
    centerFreq0 = 5210,
    centerFreq1 = 0,
    primaryChannel = 36,
    band = "5 GHz",
    wifiStandard = "802.11ac (WiFi 5)",
    capabilities = "[WPA2-PSK-CCMP][ESS]",
    isHidden = false,
    countryCode = "CN"
)

internal val sampleChannelUsage = ChannelUsage(
    channel = 6,
    count = 2,
    load = 1.5,
    networks = listOf("MyHomeWiFi", "Office"),
    score = ChannelScore(
        channel = 6,
        grade = Grade.GOOD,
        apCount = 2,
        load = 1.5,
        avgRssi = -60.0,
        avgUtilizationPct = 30.0,
        apCountPenalty = 8,
        rssiPenalty = 15,
        bssLoadPenalty = 9,
        totalPenalty = 32,
        score = 68,
        apScores = emptyList()
    )
)
