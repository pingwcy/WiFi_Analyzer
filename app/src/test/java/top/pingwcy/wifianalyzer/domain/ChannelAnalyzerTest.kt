package top.pingwcy.wifianalyzer.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import top.pingwcy.wifianalyzer.testutil.WifiDataFactory

class ChannelAnalyzerTest {

    @Test
    fun `按主信道统计_同信道AP聚合`() {
        val a = WifiDataFactory.create(ssid = "A", bssid = "00:11:22:33:44:01", primaryChannel = 36)
        val b = WifiDataFactory.create(ssid = "B", bssid = "00:11:22:33:44:02", primaryChannel = 36)

        val result = ChannelAnalyzer.analyze(listOf(a, b), band = null, useOccupied = false)

        assertEquals(1, result.size)
        assertEquals(36, result[0].channel)
        assertEquals(2, result[0].count)
        assertEquals(2.0, result[0].load, 0.0001)
        assertEquals(listOf("A", "B"), result[0].networks)
    }

    @Test
    fun `频段过滤_只保留目标频段`() {
        val g2 = WifiDataFactory.create(
            band = "2.4 GHz", frequency = 2412, primaryChannel = 1,
        )
        val g5 = WifiDataFactory.create(
            band = "5 GHz", primaryChannel = 36,
        )

        val result = ChannelAnalyzer.analyze(listOf(g2, g5), band = "2.4 GHz", useOccupied = false)

        assertEquals(1, result.size)
        assertEquals(1, result[0].channel)
    }

    @Test
    fun `占用信道口径_40MHz展开为两个信道`() {
        val ap = WifiDataFactory.create(
            frequency = 5180,
            primaryChannel = 36,
            channelWidth = WifiDataFactory.WIDTH_40,
            centerFreq0 = 5190,
        )

        val result = ChannelAnalyzer.analyze(listOf(ap), band = null, useOccupied = true)

        assertEquals(2, result.size)
        assertEquals(36, result[0].channel)
        assertEquals(40, result[1].channel)
        assertEquals(1, result[0].count)
        assertEquals(1, result[1].count)
        assertEquals(1.0, result[0].load, 0.0001)
        assertEquals(1.0, result[1].load, 0.0001)
    }

    @Test
    fun `显示空白信道_2_4GHz填满14个信道`() {
        val result = ChannelAnalyzer.analyze(
            emptyList(),
            band = "2.4 GHz",
            showEmpty = true,
        )

        assertEquals(14, result.size)
        assertEquals(1, result.first().channel)
        assertEquals(14, result.last().channel)
        // 空信道：count = 0，满分
        assertEquals(0, result[0].count)
        assertEquals(0.0, result[0].load, 0.0001)
        assertEquals(100, result[0].score.score)
    }

    @Test
    fun `忽略弱信号_过滤低于阈值的AP`() {
        val weak = WifiDataFactory.create(ssid = "weak", bssid = "00:11:22:33:44:01", rssi = -90, primaryChannel = 36)
        val strong = WifiDataFactory.create(ssid = "strong", bssid = "00:11:22:33:44:02", rssi = -70, primaryChannel = 36)

        val result = ChannelAnalyzer.analyze(
            listOf(weak, strong),
            band = null,
            useOccupied = false,
            ignoreWeak = true,
            weakThreshold = -82,
        )

        assertEquals(1, result.size)
        assertEquals(listOf("strong"), result[0].networks)
    }

    @Test
    fun `结果按信道号升序排列`() {
        val ch40 = WifiDataFactory.create(ssid = "B", bssid = "00:11:22:33:44:02", primaryChannel = 40)
        val ch36 = WifiDataFactory.create(ssid = "A", bssid = "00:11:22:33:44:01", primaryChannel = 36)

        val result = ChannelAnalyzer.analyze(listOf(ch40, ch36), band = null, useOccupied = false)

        assertEquals(listOf(36, 40), result.map { it.channel })
    }

    // ---------- 2.4GHz 相邻信道干扰 ----------

    @Test
    fun `2_4GHz_20MHz_相邻信道干扰_等效负载扩散`() {
        val ap = WifiDataFactory.create(
            band = "2.4 GHz", frequency = 2412, primaryChannel = 1,
            channelWidth = WifiDataFactory.WIDTH_20,
        )

        val result = ChannelAnalyzer.analyze(listOf(ap), band = "2.4 GHz", useOccupied = true)

        // 信道 1 影响 1/2/3/4
        assertEquals(listOf(1, 2, 3, 4), result.map { it.channel })

        val byChannel = result.associateBy { it.channel }
        assertEquals(1.0, byChannel[1]!!.load, 0.0001)
        assertEquals(0.75, byChannel[2]!!.load, 0.0001)
        assertEquals(0.5, byChannel[3]!!.load, 0.0001)
        assertEquals(0.25, byChannel[4]!!.load, 0.0001)
    }

    @Test
    fun `2_4GHz_信道1和6互不影响`() {
        val ch1 = WifiDataFactory.create(ssid = "A", bssid = "00:11:22:33:44:01", band = "2.4 GHz", frequency = 2412, primaryChannel = 1)
        val ch6 = WifiDataFactory.create(ssid = "B", bssid = "00:11:22:33:44:02", band = "2.4 GHz", frequency = 2437, primaryChannel = 6)

        val result = ChannelAnalyzer.analyze(listOf(ch1, ch6), band = "2.4 GHz", useOccupied = true)
        val byChannel = result.associateBy { it.channel }

        // 信道 1 只受 A 影响，信道 6 只受 B 影响（Δ=5，权重 0）
        assertEquals(listOf("A"), byChannel[1]!!.networks)
        assertEquals(listOf("B"), byChannel[6]!!.networks)
    }

    @Test
    fun `allChannelsOfBand_三个频段信道表`() {
        assertEquals((1..14).toList(), ChannelAnalyzer.allChannelsOfBand("2.4 GHz"))

        val ch5 = ChannelAnalyzer.allChannelsOfBand("5 GHz")
        assertEquals(28, ch5.size)
        assertEquals(36, ch5.first())
        assertEquals(177, ch5.last())

        assertEquals((1..233 step 4).toList(), ChannelAnalyzer.allChannelsOfBand("6 GHz"))
        assertEquals(emptyList<Int>(), ChannelAnalyzer.allChannelsOfBand("未知"))
    }

    @Test
    fun `显示空白信道_按可用信道过滤`() {
        val result = ChannelAnalyzer.analyze(
            emptyList(),
            band = "2.4 GHz",
            showEmpty = true,
            allowedChannelsByBand = mapOf("2.4 GHz" to setOf(1, 6, 11)),
        )

        assertEquals(listOf(1, 6, 11), result.map { it.channel })
    }

    @Test
    fun `未知可用信道_回退到全量信道表`() {
        val result = ChannelAnalyzer.analyze(
            emptyList(),
            band = "2.4 GHz",
            showEmpty = true,
            allowedChannelsByBand = emptyMap(),
        )

        assertEquals((1..14).toList(), result.map { it.channel })
    }
}
