package top.pingwcy.wifianalyzer.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import top.pingwcy.wifianalyzer.testutil.WifiDataFactory

class ChannelCalculatorTest {

    // ---------- freqToChannel ----------

    @Test
    fun `freqToChannel_2_4GHz_映射正确`() {
        assertEquals(1, ChannelCalculator.freqToChannel(2412))
        assertEquals(6, ChannelCalculator.freqToChannel(2437))
        assertEquals(13, ChannelCalculator.freqToChannel(2472))
        assertEquals(14, ChannelCalculator.freqToChannel(2484))
    }

    @Test
    fun `freqToChannel_5GHz_映射正确`() {
        assertEquals(36, ChannelCalculator.freqToChannel(5180))
        assertEquals(64, ChannelCalculator.freqToChannel(5320))
        assertEquals(165, ChannelCalculator.freqToChannel(5825))
    }

    @Test
    fun `freqToChannel_6GHz_映射正确`() {
        assertEquals(1, ChannelCalculator.freqToChannel(5955))
        assertEquals(5, ChannelCalculator.freqToChannel(5975))
        assertEquals(93, ChannelCalculator.freqToChannel(6415))
    }

    @Test
    fun `freqToChannel_未知频率_返回0`() {
        assertEquals(0, ChannelCalculator.freqToChannel(3000))
        assertEquals(0, ChannelCalculator.freqToChannel(0))
    }

    // ---------- freqToBand ----------

    @Test
    fun `freqToBand_三个频段边界`() {
        assertEquals("2.4 GHz", ChannelCalculator.freqToBand(2400))
        assertEquals("2.4 GHz", ChannelCalculator.freqToBand(2500))
        assertEquals("5 GHz", ChannelCalculator.freqToBand(4900))
        assertEquals("5 GHz", ChannelCalculator.freqToBand(5900))
        assertEquals("6 GHz", ChannelCalculator.freqToBand(5925))
        assertEquals("6 GHz", ChannelCalculator.freqToBand(7125))
    }

    @Test
    fun `freqToBand_频段间隙返回未知`() {
        assertEquals("未知", ChannelCalculator.freqToBand(2399))
        assertEquals("未知", ChannelCalculator.freqToBand(5901))
    }

    // ---------- occupiedChannels：5/6GHz ----------

    @Test
    fun `occupiedChannels_5GHz_20MHz_仅占用自身`() {
        val wifi = WifiDataFactory.create(
            frequency = 5180,
            primaryChannel = 36,
            channelWidth = WifiDataFactory.WIDTH_20,
            centerFreq0 = 5180,
        )
        assertEquals(listOf(36), ChannelCalculator.occupiedChannels(wifi))
    }

    @Test
    fun `occupiedChannels_5GHz_40MHz_占用两个信道`() {
        val wifi = WifiDataFactory.create(
            frequency = 5180,
            primaryChannel = 36,
            channelWidth = WifiDataFactory.WIDTH_40,
            centerFreq0 = 5190, // 40MHz 中心 = 36/40 的中点
        )
        assertEquals(listOf(36, 40), ChannelCalculator.occupiedChannels(wifi))
    }

    @Test
    fun `occupiedChannels_5GHz_80MHz_占用四个信道`() {
        val wifi = WifiDataFactory.create(
            frequency = 5180,
            primaryChannel = 36,
            channelWidth = WifiDataFactory.WIDTH_80,
            centerFreq0 = 5210, // 80MHz 中心 = 36..48 的中点
        )
        assertEquals(listOf(36, 40, 44, 48), ChannelCalculator.occupiedChannels(wifi))
    }

    @Test
    fun `occupiedChannels_5GHz_160MHz_按标准段枚举八个信道`() {
        val wifi = WifiDataFactory.create(
            frequency = 5180,
            primaryChannel = 36,
            channelWidth = WifiDataFactory.WIDTH_160,
        )
        assertEquals(
            listOf(36, 40, 44, 48, 52, 56, 60, 64),
            ChannelCalculator.occupiedChannels(wifi)
        )
    }

    @Test
    fun `occupiedChannels_80加80_两段不连续占用`() {
        val wifi = WifiDataFactory.create(
            frequency = 5180,
            primaryChannel = 36,
            channelWidth = WifiDataFactory.WIDTH_80_PLUS_80,
            centerFreq0 = 5210, // 36..48
            centerFreq1 = 5775, // 149..161
        )
        assertEquals(
            listOf(36, 40, 44, 48, 149, 153, 157, 161),
            ChannelCalculator.occupiedChannels(wifi)
        )
    }

    @Test
    fun `occupiedChannels_6GHz_20MHz_仅占用自身`() {
        val wifi = WifiDataFactory.create(
            frequency = 5975,
            primaryChannel = 5,
            channelWidth = WifiDataFactory.WIDTH_20,
            centerFreq0 = 5975,
            band = "6 GHz",
        )
        assertEquals(listOf(5), ChannelCalculator.occupiedChannels(wifi))
    }

    // ---------- occupiedChannels：2.4GHz（绑定口径，不做频谱重叠展开） ----------

    @Test
    fun `occupiedChannels_2_4GHz_20MHz_仅占用主信道自身`() {
        val wifi = WifiDataFactory.create(
            frequency = 2412,
            primaryChannel = 1,
            channelWidth = WifiDataFactory.WIDTH_20,
            centerFreq0 = 2412,
            band = "2.4 GHz",
        )
        assertEquals(listOf(1), ChannelCalculator.occupiedChannels(wifi))
    }

    @Test
    fun `occupiedChannels_2_4GHz_40MHz_主信道在上方时向下绑定`() {
        // 主信道 5（2432）+ 扩展信道 1（2412），40MHz 中心 2422
        val wifi = WifiDataFactory.create(
            frequency = 2432,
            primaryChannel = 5,
            channelWidth = WifiDataFactory.WIDTH_40,
            centerFreq0 = 2422,
            band = "2.4 GHz",
        )
        assertEquals(listOf(1, 5), ChannelCalculator.occupiedChannels(wifi))
    }

    @Test
    fun `occupiedChannels_2_4GHz_40MHz_主信道在下方时向上绑定`() {
        // 主信道 1（2412）+ 扩展信道 5（2432），40MHz 中心 2422
        val wifi = WifiDataFactory.create(
            frequency = 2412,
            primaryChannel = 1,
            channelWidth = WifiDataFactory.WIDTH_40,
            centerFreq0 = 2422,
            band = "2.4 GHz",
        )
        assertEquals(listOf(1, 5), ChannelCalculator.occupiedChannels(wifi))
    }

    @Test
    fun `occupiedChannels_2_4GHz_40MHz_无中心频率时默认向上绑定`() {
        val wifi = WifiDataFactory.create(
            frequency = 2412,
            primaryChannel = 1,
            channelWidth = WifiDataFactory.WIDTH_40,
            centerFreq0 = 0,
            band = "2.4 GHz",
        )
        assertEquals(listOf(1, 5), ChannelCalculator.occupiedChannels(wifi))
    }

    @Test
    fun `occupiedChannels_2_4GHz_非法主信道_返回空`() {
        val wifi = WifiDataFactory.create(
            frequency = 2412,
            primaryChannel = 0,
            channelWidth = WifiDataFactory.WIDTH_20,
            band = "2.4 GHz",
        )
        assertEquals(0, ChannelCalculator.occupiedChannels(wifi).size)
    }

    // ---------- interferenceWeights（加权频谱贡献） ----------

    @Test
    fun `interferenceWeights_2_4GHz_20MHz_向相邻信道衰减扩散`() {
        val wifi = WifiDataFactory.create(
            frequency = 2412,
            primaryChannel = 1,
            channelWidth = WifiDataFactory.WIDTH_20,
            band = "2.4 GHz",
        )

        val weights = ChannelCalculator.interferenceWeights(wifi)

        assertEquals(mapOf(1 to 1.0, 2 to 0.75, 3 to 0.5, 4 to 0.25), weights)
    }

    @Test
    fun `interferenceWeights_5GHz_20MHz_仅自身`() {
        val wifi = WifiDataFactory.create(
            frequency = 5180,
            primaryChannel = 36,
            channelWidth = WifiDataFactory.WIDTH_20,
            centerFreq0 = 5180,
        )

        assertEquals(mapOf(36 to 1.0), ChannelCalculator.interferenceWeights(wifi))
    }

    @Test
    fun `interferenceWeights_5GHz_40MHz_两信道各1`() {
        val wifi = WifiDataFactory.create(
            frequency = 5180,
            primaryChannel = 36,
            channelWidth = WifiDataFactory.WIDTH_40,
            centerFreq0 = 5190,
        )

        assertEquals(mapOf(36 to 1.0, 40 to 1.0), ChannelCalculator.interferenceWeights(wifi))
    }

    @Test
    fun `interferenceWeights_2_4GHz_40MHz_双足迹合并并封顶`() {
        // 主信道 1 + 扩展信道 5，两段足迹合并后中间信道（1~5）均为满负载
        val wifi = WifiDataFactory.create(
            frequency = 2412,
            primaryChannel = 1,
            channelWidth = WifiDataFactory.WIDTH_40,
            centerFreq0 = 2422,
            band = "2.4 GHz",
        )

        val weights = ChannelCalculator.interferenceWeights(wifi)

        assertEquals(1.0, weights[1]!!, 0.0001)
        assertEquals(1.0, weights[2]!!, 0.0001)
        assertEquals(1.0, weights[3]!!, 0.0001)
        assertEquals(1.0, weights[4]!!, 0.0001)
        assertEquals(1.0, weights[5]!!, 0.0001)
        assertEquals(0.75, weights[6]!!, 0.0001)
        assertEquals(0.5, weights[7]!!, 0.0001)
        assertEquals(0.25, weights[8]!!, 0.0001)
    }
}
