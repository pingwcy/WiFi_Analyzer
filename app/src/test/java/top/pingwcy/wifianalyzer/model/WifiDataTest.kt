package top.pingwcy.wifianalyzer.model

import org.junit.Assert.assertEquals
import org.junit.Test
import top.pingwcy.wifianalyzer.testutil.WifiDataFactory

class WifiDataTest {

    @Test
    fun `groupKey_可见网络_按SSID和加密分组`() {
        val wifi = WifiDataFactory.create(ssid = "Home", capabilities = "[WPA2][ESS]")
        assertEquals("Home|[WPA2][ESS]", groupKey(wifi))
    }

    @Test
    fun `groupKey_同名不同加密_不合并`() {
        val wpa2 = WifiDataFactory.create(ssid = "Home", capabilities = "[WPA2][ESS]")
        val open = WifiDataFactory.create(ssid = "Home", capabilities = "[ESS]")
        assertEquals("Home|[WPA2][ESS]", groupKey(wpa2))
        assertEquals("Home|[ESS]", groupKey(open))
    }

    @Test
    fun `groupKey_隐藏网络_按BSSID单独成组`() {
        val wifi = WifiDataFactory.create(
            ssid = "",
            bssid = "AA:BB:CC:DD:EE:FF",
            isHidden = true,
        )
        assertEquals("hidden:AA:BB:CC:DD:EE:FF", groupKey(wifi))
    }

    @Test
    fun `groupKey_空SSID但非隐藏_仍按BSSID成组`() {
        val wifi = WifiDataFactory.create(ssid = "   ", bssid = "11:22:33:44:55:66")
        assertEquals("hidden:11:22:33:44:55:66", groupKey(wifi))
    }
}
