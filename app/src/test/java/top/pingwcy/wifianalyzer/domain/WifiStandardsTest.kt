package top.pingwcy.wifianalyzer.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import top.pingwcy.wifianalyzer.model.ChannelWidth

class WifiStandardsTest {

    @Test
    fun `标准枚举转文本`() {
        assertEquals("未知", WifiStandards.toString(-1))
        assertEquals("802.11a", WifiStandards.toString(1))
        assertEquals("802.11n (WiFi 4)", WifiStandards.toString(4))
        assertEquals("802.11ax (WiFi 6)", WifiStandards.toString(6))
        assertEquals("802.11be (WiFi 7)", WifiStandards.toString(8))
        assertEquals("未知 (99)", WifiStandards.toString(99))
    }

    @Test
    fun `信道宽度转文本`() {
        assertEquals("20 MHz", WifiStandards.widthText(ChannelWidth.WIDTH_20MHZ))
        assertEquals("40 MHz", WifiStandards.widthText(ChannelWidth.WIDTH_40MHZ))
        assertEquals("80 MHz", WifiStandards.widthText(ChannelWidth.WIDTH_80MHZ))
        assertEquals("160 MHz", WifiStandards.widthText(ChannelWidth.WIDTH_160MHZ))
        assertEquals("80+80 MHz", WifiStandards.widthText(ChannelWidth.WIDTH_80MHZ_PLUS))
    }

    @Test
    fun `信道宽度转MHz`() {
        assertEquals(20, WifiStandards.widthMhz(ChannelWidth.WIDTH_20MHZ))
        assertEquals(40, WifiStandards.widthMhz(ChannelWidth.WIDTH_40MHZ))
        assertEquals(80, WifiStandards.widthMhz(ChannelWidth.WIDTH_80MHZ))
        assertEquals(160, WifiStandards.widthMhz(ChannelWidth.WIDTH_160MHZ))
        assertEquals(80, WifiStandards.widthMhz(ChannelWidth.WIDTH_80MHZ_PLUS)) // 80+80 单段按 80 计
    }

    @Test
    fun `占用信道数量`() {
        assertEquals(1, WifiStandards.occupiedChannelCount(ChannelWidth.WIDTH_20MHZ))
        assertEquals(2, WifiStandards.occupiedChannelCount(ChannelWidth.WIDTH_40MHZ))
        assertEquals(4, WifiStandards.occupiedChannelCount(ChannelWidth.WIDTH_80MHZ))
        assertEquals(8, WifiStandards.occupiedChannelCount(ChannelWidth.WIDTH_160MHZ))
        assertEquals(8, WifiStandards.occupiedChannelCount(ChannelWidth.WIDTH_80MHZ_PLUS))
    }
}
