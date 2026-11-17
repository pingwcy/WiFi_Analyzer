package top.pingwcy.wifianalyzer.testutil

import top.pingwcy.wifianalyzer.model.BssLoad
import top.pingwcy.wifianalyzer.model.ChannelWidth
import top.pingwcy.wifianalyzer.model.WifiData

/**
 * 单元测试专用的 WifiData 构造工厂，提供合理默认值，只覆盖需要关注的字段。
 */
object WifiDataFactory {

    val WIDTH_20: ChannelWidth = ChannelWidth.WIDTH_20MHZ
    val WIDTH_40: ChannelWidth = ChannelWidth.WIDTH_40MHZ
    val WIDTH_80: ChannelWidth = ChannelWidth.WIDTH_80MHZ
    val WIDTH_160: ChannelWidth = ChannelWidth.WIDTH_160MHZ
    val WIDTH_80_PLUS_80: ChannelWidth = ChannelWidth.WIDTH_80MHZ_PLUS

    fun create(
        ssid: String = "TestSSID",
        bssid: String = "00:11:22:33:44:55",
        frequency: Int = 5180,          // 5GHz 信道 36 的中心频率
        rssi: Int = -60,
        channelWidth: ChannelWidth = ChannelWidth.WIDTH_20MHZ,
        centerFreq0: Int = 0,
        centerFreq1: Int = 0,
        primaryChannel: Int = 36,
        band: String = "5 GHz",
        capabilities: String = "[WPA2-PSK-CCMP][ESS]",
        isHidden: Boolean = false,
        countryCode: String = "CN",
        bssLoad: BssLoad? = null,
    ): WifiData = WifiData(
        ssid = ssid,
        bssid = bssid,
        frequency = frequency,
        rssi = rssi,
        channelWidth = channelWidth,
        centerFreq0 = centerFreq0,
        centerFreq1 = centerFreq1,
        primaryChannel = primaryChannel,
        band = band,
        wifiStandard = "802.11ac (WiFi 5)",
        capabilities = capabilities,
        isHidden = isHidden,
        countryCode = countryCode,
        bssLoad = bssLoad,
        beaconInfo = null,
    )
}
