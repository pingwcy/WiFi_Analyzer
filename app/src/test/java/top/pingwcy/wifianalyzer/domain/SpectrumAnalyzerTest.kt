package top.pingwcy.wifianalyzer.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import top.pingwcy.wifianalyzer.model.ChannelWidth
import top.pingwcy.wifianalyzer.testutil.WifiDataFactory

class SpectrumAnalyzerTest {

    @Test
    fun `空列表_返回全零高度`() {
        val points = SpectrumAnalyzer.analyze(emptyList(), "2.4 GHz")
        assertTrue(points.isNotEmpty())
        assertTrue(points.all { it.height == 0f })
    }

    @Test
    fun `频段外_返回空`() {
        val points = SpectrumAnalyzer.analyze(emptyList(), "未知频段")
        assertEquals(emptyList<SpectrumPoint>(), points)
    }

    @Test
    fun `单AP_20MHz_中心频率处高度最高`() {
        val ap = WifiDataFactory.create(
            band = "2.4 GHz",
            frequency = 2412,
            primaryChannel = 1,
            channelWidth = ChannelWidth.WIDTH_20MHZ,
            centerFreq0 = 2412,
            rssi = -30,
        )
        val points = SpectrumAnalyzer.analyze(listOf(ap), "2.4 GHz")
        val byFreq = points.associateBy { it.frequencyMhz }

        // 升余弦掩码：带宽内为平坦区，中心处达到满功率
        assertEquals(1.0f, byFreq[2412]!!.height, 0.001f)
    }

    @Test
    fun `20MHz_带宽外高度为零`() {
        val ap = WifiDataFactory.create(
            band = "2.4 GHz",
            frequency = 2412,
            primaryChannel = 1,
            channelWidth = ChannelWidth.WIDTH_20MHZ,
            centerFreq0 = 2412,
            rssi = -30,
        )
        val points = SpectrumAnalyzer.analyze(listOf(ap), "2.4 GHz")
        val byFreq = points.associateBy { it.frequencyMhz }

        // 距中心 23 MHz 以上，超出滚降边界，能量为零
        assertEquals(0f, byFreq[2435]!!.height, 0.001f)
        assertEquals(0f, byFreq[2440]!!.height, 0.001f)
    }

    @Test
    fun `80MHz_带宽内平坦_覆盖四个20MHz子信道`() {
        val ap = WifiDataFactory.create(
            band = "5 GHz",
            frequency = 5180,
            primaryChannel = 36,
            channelWidth = ChannelWidth.WIDTH_80MHZ,
            centerFreq0 = 5210,
            rssi = -30,
        )
        val points = SpectrumAnalyzer.analyze(listOf(ap), "5 GHz")
        val byFreq = points.associateBy { it.frequencyMhz }

        // 中心与四个子信道中心都接近满功率（OFDM 带宽内平坦）
        assertEquals(1.0f, byFreq[5210]!!.height, 0.001f)
        assertTrue(byFreq[5180]!!.height > 0.8f)
        assertTrue(byFreq[5200]!!.height > 0.8f)
        assertTrue(byFreq[5220]!!.height > 0.8f)
        assertTrue(byFreq[5240]!!.height > 0.8f)
        // 带外为零
        assertEquals(0f, byFreq[5150]!!.height, 0.001f)
        assertEquals(0f, byFreq[5270]!!.height, 0.001f)
    }

    @Test
    fun `弱信号_高度被压低`() {
        val strong = WifiDataFactory.create(
            band = "2.4 GHz", frequency = 2412, primaryChannel = 1,
            channelWidth = ChannelWidth.WIDTH_20MHZ, centerFreq0 = 2412, rssi = -30,
        )
        val weak = WifiDataFactory.create(
            band = "2.4 GHz", frequency = 2437, primaryChannel = 6,
            channelWidth = ChannelWidth.WIDTH_20MHZ, centerFreq0 = 2437, rssi = -90,
        )
        val points = SpectrumAnalyzer.analyze(listOf(strong, weak), "2.4 GHz")
        val byFreq = points.associateBy { it.frequencyMhz }

        assertTrue(byFreq[2437]!!.height < byFreq[2412]!!.height)
    }

    @Test
    fun `无centerFreq0时_回退到主信道频率`() {
        val ap = WifiDataFactory.create(
            band = "2.4 GHz",
            frequency = 2437,
            primaryChannel = 6,
            channelWidth = ChannelWidth.WIDTH_20MHZ,
            centerFreq0 = 0,
            rssi = -30,
        )
        val points = SpectrumAnalyzer.analyze(listOf(ap), "2.4 GHz")
        val byFreq = points.associateBy { it.frequencyMhz }
        assertEquals(1.0f, byFreq[2437]!!.height, 0.001f)
    }

    @Test
    fun `analyzePerAp_返回每个AP的独立频谱`() {
        val a = WifiDataFactory.create(
            bssid = "00:11:22:33:44:01", band = "2.4 GHz", frequency = 2412,
            primaryChannel = 1, channelWidth = ChannelWidth.WIDTH_20MHZ,
            centerFreq0 = 2412, rssi = -30,
        )
        val b = WifiDataFactory.create(
            bssid = "00:11:22:33:44:02", band = "2.4 GHz", frequency = 2437,
            primaryChannel = 6, channelWidth = ChannelWidth.WIDTH_20MHZ,
            centerFreq0 = 2437, rssi = -30,
        )

        val perAp = SpectrumAnalyzer.analyzePerAp(listOf(a, b), "2.4 GHz")

        assertEquals(setOf("00:11:22:33:44:01", "00:11:22:33:44:02"), perAp.keys)
        assertEquals(1.0f, perAp["00:11:22:33:44:01"]!!.associateBy { it.frequencyMhz }[2412]!!.height, 0.001f)
        assertEquals(1.0f, perAp["00:11:22:33:44:02"]!!.associateBy { it.frequencyMhz }[2437]!!.height, 0.001f)
    }

    @Test
    fun `apBands_80加80_返回两个频段`() {
        val ap = WifiDataFactory.create(
            band = "5 GHz", frequency = 5180, primaryChannel = 36,
            channelWidth = ChannelWidth.WIDTH_80MHZ_PLUS,
            centerFreq0 = 5210, centerFreq1 = 5775, rssi = -55,
        )

        val bands = SpectrumAnalyzer.apBands(listOf(ap), "5 GHz")

        assertEquals(1, bands.size)
        assertEquals(2, bands[0].segments.size)
        assertEquals(5210, bands[0].segments[0].centerMhz)
        assertEquals(5775, bands[0].segments[1].centerMhz)
    }

    @Test
    fun `apBands_按信号强度降序`() {
        val weak = WifiDataFactory.create(
            bssid = "00:11:22:33:44:01", band = "2.4 GHz", rssi = -80,
        )
        val strong = WifiDataFactory.create(
            bssid = "00:11:22:33:44:02", band = "2.4 GHz", rssi = -40,
        )

        val bands = SpectrumAnalyzer.apBands(listOf(weak, strong), "2.4 GHz")

        assertEquals("00:11:22:33:44:02", bands[0].bssid)
        assertEquals("00:11:22:33:44:01", bands[1].bssid)
    }
}
