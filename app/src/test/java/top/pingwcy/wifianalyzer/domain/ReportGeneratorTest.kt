package top.pingwcy.wifianalyzer.domain

import org.junit.Assert.assertTrue
import org.junit.Test
import top.pingwcy.wifianalyzer.model.ChannelWidth
import top.pingwcy.wifianalyzer.testutil.WifiDataFactory

class ReportGeneratorTest {

    @Test
    fun `空列表_仅含标题和总数`() {
        val report = ReportGenerator.generate(emptyList())
        assertTrue(report.contains("Total: 0 AP(s)"))
    }

    @Test
    fun `含AP_包含频段推荐和AP列表`() {
        val ap = WifiDataFactory.create(
            ssid = "Home",
            band = "2.4 GHz",
            frequency = 2412,
            primaryChannel = 1,
            channelWidth = ChannelWidth.WIDTH_20MHZ,
            centerFreq0 = 2412,
            rssi = -55,
            capabilities = "[WPA2-PSK-CCMP][ESS]",
        )
        val report = ReportGenerator.generate(listOf(ap))

        assertTrue(report.contains("Total: 1 AP(s)"))
        assertTrue(report.contains("[2.4 GHz]"))
        assertTrue(report.contains("Best: CH"))
        assertTrue(report.contains("Home | CH 1 | 2.4 GHz | -55 dBm | SECURED"))
    }

    @Test
    fun `开放网络_标记为OPEN`() {
        val ap = WifiDataFactory.create(
            ssid = "FreeWiFi",
            band = "2.4 GHz",
            frequency = 2412,
            primaryChannel = 1,
            channelWidth = ChannelWidth.WIDTH_20MHZ,
            centerFreq0 = 2412,
            rssi = -70,
            capabilities = "[ESS]",
        )
        val report = ReportGenerator.generate(listOf(ap))
        assertTrue(report.contains("FreeWiFi | CH 1 | 2.4 GHz | -70 dBm | OPEN"))
    }

    @Test
    fun `AP按信号强度降序排列`() {
        val weak = WifiDataFactory.create(
            ssid = "Weak", band = "2.4 GHz", frequency = 2412, primaryChannel = 1,
            channelWidth = ChannelWidth.WIDTH_20MHZ, centerFreq0 = 2412, rssi = -90,
        )
        val strong = WifiDataFactory.create(
            ssid = "Strong", band = "2.4 GHz", frequency = 2437, primaryChannel = 6,
            channelWidth = ChannelWidth.WIDTH_20MHZ, centerFreq0 = 2437, rssi = -40,
        )
        val report = ReportGenerator.generate(listOf(weak, strong))
        val strongIndex = report.indexOf("Strong")
        val weakIndex = report.indexOf("Weak")
        assertTrue(strongIndex < weakIndex)
    }
}
