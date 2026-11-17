package top.pingwcy.wifianalyzer.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import top.pingwcy.wifianalyzer.model.ChannelWidth
import top.pingwcy.wifianalyzer.testutil.WifiDataFactory

class ChannelRecommenderTest {

    @Test
    fun `空列表_推荐首个候选信道`() {
        val r = ChannelRecommender.recommend(emptyList(), "2.4 GHz")
        assertEquals(1, r.recommended)
        assertTrue(r.candidates.all { it.load == 0.0 })
    }

    @Test
    fun `2_4GHz_信道1被占用_推荐6`() {
        val ap = WifiDataFactory.create(
            band = "2.4 GHz", frequency = 2412, primaryChannel = 1,
            channelWidth = ChannelWidth.WIDTH_20MHZ, centerFreq0 = 2412,
        )
        val r = ChannelRecommender.recommend(listOf(ap), "2.4 GHz")
        assertEquals(6, r.recommended)
    }

    @Test
    fun `2_4GHz_信道1和6被占用_推荐11`() {
        val ch1 = WifiDataFactory.create(
            bssid = "00:11:22:33:44:01", band = "2.4 GHz", frequency = 2412,
            primaryChannel = 1, channelWidth = ChannelWidth.WIDTH_20MHZ, centerFreq0 = 2412,
        )
        val ch6 = WifiDataFactory.create(
            bssid = "00:11:22:33:44:02", band = "2.4 GHz", frequency = 2437,
            primaryChannel = 6, channelWidth = ChannelWidth.WIDTH_20MHZ, centerFreq0 = 2437,
        )
        val r = ChannelRecommender.recommend(listOf(ch1, ch6), "2.4 GHz")
        assertEquals(11, r.recommended)
    }

    @Test
    fun `5GHz_信道36被占用_推荐40`() {
        val ap = WifiDataFactory.create(
            band = "5 GHz", frequency = 5180, primaryChannel = 36,
            channelWidth = ChannelWidth.WIDTH_20MHZ, centerFreq0 = 5180,
        )
        val r = ChannelRecommender.recommend(listOf(ap), "5 GHz")
        assertEquals(40, r.recommended)
    }

    @Test
    fun `未知频段_返回空推荐`() {
        val r = ChannelRecommender.recommend(emptyList(), "未知")
        assertEquals(0, r.recommended)
        assertEquals(emptyList<CandidateChannel>(), r.candidates)
    }

    @Test
    fun `候选信道_包含负载和AP数量`() {
        val ap = WifiDataFactory.create(
            band = "2.4 GHz", frequency = 2412, primaryChannel = 1,
            channelWidth = ChannelWidth.WIDTH_20MHZ, centerFreq0 = 2412, rssi = -40,
        )
        val r = ChannelRecommender.recommend(listOf(ap), "2.4 GHz")
        val ch1 = r.candidates.first { it.channel == 1 }
        assertEquals(1.0, ch1.load, 0.001)
        assertEquals(1, ch1.apCount)
    }
}
