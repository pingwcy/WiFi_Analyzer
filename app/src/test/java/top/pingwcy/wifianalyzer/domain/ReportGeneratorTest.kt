package top.pingwcy.wifianalyzer.domain

import org.junit.Assert.assertTrue
import org.junit.Test
import top.pingwcy.wifianalyzer.model.BssLoad
import top.pingwcy.wifianalyzer.model.ChannelWidth
import top.pingwcy.wifianalyzer.testutil.WifiDataFactory

class ReportGeneratorTest {

    @Test
    fun `空列表_仅含标题和总数`() {
        val report = ReportGenerator.generate(emptyList())
        assertTrue(report.contains("\"totalAps\": 0"))
        assertTrue(report.contains("\"format\": \"wifi-analyzer-report\""))
    }

    @Test
    fun `含AP_包含频段推荐和AP详情`() {
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

        assertTrue(report.contains("\"totalAps\": 1"))
        assertTrue(report.contains("\"band\": \"2.4 GHz\""))
        assertTrue(report.contains("\"bestChannel\""))
        assertTrue(report.contains("\"ssid\": \"Home\""))
        assertTrue(report.contains("\"rssiDbm\": -55"))
        assertTrue(report.contains("\"security\": \"SECURED\""))
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
        assertTrue(report.contains("\"security\": \"OPEN\""))
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
        val strongIndex = report.indexOf("\"ssid\": \"Strong\"")
        val weakIndex = report.indexOf("\"ssid\": \"Weak\"")
        assertTrue(strongIndex in 0 until weakIndex)
    }

    @Test
    fun `含BSS Load_导出详情字段`() {
        val ap = WifiDataFactory.create(
            ssid = "Loaded",
            band = "2.4 GHz",
            frequency = 2412,
            primaryChannel = 1,
            channelWidth = ChannelWidth.WIDTH_20MHZ,
            centerFreq0 = 2412,
            rssi = -60,
            bssLoad = BssLoad(
                stationCount = 5,
                channelUtilization = 100,
                availableAdmissionCapacity = 20000,
            ),
        )
        val report = ReportGenerator.generate(listOf(ap))

        assertTrue(report.contains("\"bssLoad\""))
        assertTrue(report.contains("\"stationCount\": 5"))
        assertTrue(report.contains("\"channelUtilization\": 100"))
        assertTrue(report.contains("\"availableAdmissionCapacity\": 20000"))
    }

    @Test
    fun `SSID含特殊字符_正确转义`() {
        val ap = WifiDataFactory.create(
            ssid = "A\"B\\C",
            band = "2.4 GHz",
            frequency = 2412,
            primaryChannel = 1,
            channelWidth = ChannelWidth.WIDTH_20MHZ,
            centerFreq0 = 2412,
            rssi = -50,
        )
        val report = ReportGenerator.generate(listOf(ap))
        assertTrue(report.contains("\"ssid\": \"A\\\"B\\\\C\""))
    }
}
