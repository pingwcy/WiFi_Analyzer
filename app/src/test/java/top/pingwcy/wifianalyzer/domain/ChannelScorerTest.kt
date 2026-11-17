package top.pingwcy.wifianalyzer.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import top.pingwcy.wifianalyzer.model.BssLoad
import top.pingwcy.wifianalyzer.model.WifiData
import top.pingwcy.wifianalyzer.testutil.WifiDataFactory

class ChannelScorerTest {

    private fun w(wifi: WifiData, weight: Double = 1.0) = WeightedWifi(wifi, weight)

    @Test
    fun `空信道_直接满分`() {
        val score = ChannelScorer.scoreChannel(6, emptyList())

        assertEquals(100, score.score)
        assertEquals(Grade.EXCELLENT, score.grade)
        assertEquals(0, score.apCount)
        assertEquals(0.0, score.load, 0.0001)
        assertEquals(0, score.totalPenalty)
        assertEquals(0, score.apScores.size)
    }

    @Test
    fun `单AP_弱信号_无BssLoad_不扣分`() {
        val wifi = WifiDataFactory.create(rssi = -90)
        val score = ChannelScorer.scoreChannel(36, listOf(w(wifi)))

        assertEquals(100, score.score)
        assertEquals(Grade.EXCELLENT, score.grade)
        assertEquals(0, score.rssiPenalty)
        assertEquals(0, score.bssLoadPenalty)
    }

    @Test
    fun `单AP_强信号_扣25分`() {
        val wifi = WifiDataFactory.create(rssi = -40)
        val score = ChannelScorer.scoreChannel(36, listOf(w(wifi)))

        assertEquals(25, score.rssiPenalty)
        assertEquals(75, score.score)
        assertEquals(Grade.GOOD, score.grade)
    }

    @Test
    fun `AP数量扣分_每个额外AP扣8分`() {
        val aps = (1..3).map {
            WifiDataFactory.create(bssid = "00:11:22:33:44:0$it", rssi = -90)
        }
        val score = ChannelScorer.scoreChannel(36, aps.map { w(it) })

        // (3 - 1) * 8 = 16
        assertEquals(16, score.apCountPenalty)
        assertEquals(84, score.score)
    }

    @Test
    fun `AP数量扣分_封顶40`() {
        val aps = (1..7).map {
            WifiDataFactory.create(bssid = "00:11:22:33:44:0$it", rssi = -90)
        }
        val score = ChannelScorer.scoreChannel(36, aps.map { w(it) })

        // (7 - 1) * 8 = 48 -> 封顶 40
        assertEquals(40, score.apCountPenalty)
    }

    @Test
    fun `BssLoad满负载_扣30分`() {
        val wifi = WifiDataFactory.create(
            rssi = -90,
            bssLoad = BssLoad(
                stationCount = 32,
                channelUtilization = 255, // 255 -> 100%
                availableAdmissionCapacity = 0,
            ),
        )
        val score = ChannelScorer.scoreChannel(36, listOf(w(wifi)))

        assertEquals(30, score.bssLoadPenalty)
        assertEquals(100.0, score.avgUtilizationPct!!, 0.0001)
        assertEquals(70, score.score)
        assertEquals(Grade.GOOD, score.grade)
    }

    @Test
    fun `总扣分封顶100_得分不为负`() {
        // 大量 AP + 强信号 + 满负载，理论上远超 100 分
        val aps = (1..10).map {
            WifiDataFactory.create(
                bssid = "00:11:22:33:44:0$it",
                rssi = -40,
                bssLoad = BssLoad(8, 255, 0),
            )
        }
        val score = ChannelScorer.scoreChannel(36, aps.map { w(it) })

        assertEquals(40, score.apCountPenalty)
        assertEquals(25, score.rssiPenalty)
        assertEquals(30, score.bssLoadPenalty)
        assertEquals(95, score.totalPenalty)
        assertEquals(5, score.score)
        assertEquals(Grade.CROWDED, score.grade)
    }

    @Test
    fun `等级分级边界`() {
        // 单 AP 强信号 + 满负载：25 + 30 = 55 -> FAIR
        val fair = ChannelScorer.scoreChannel(
            36,
            listOf(w(WifiDataFactory.create(rssi = -40, bssLoad = BssLoad(8, 255, 0))))
        )
        assertEquals(Grade.FAIR, fair.grade)

        // 两个 AP 强信号 + 满负载：8 + 25 + 30 = 63 -> CROWDED
        val crowded = ChannelScorer.scoreChannel(
            36,
            listOf(
                w(WifiDataFactory.create(bssid = "00:11:22:33:44:01", rssi = -40, bssLoad = BssLoad(8, 255, 0))),
                w(WifiDataFactory.create(bssid = "00:11:22:33:44:02", rssi = -40, bssLoad = BssLoad(8, 255, 0))),
            )
        )
        assertEquals(Grade.CROWDED, crowded.grade)
    }

    @Test
    fun `AP明细_包含正确的RSSI和利用率`() {
        val wifi = WifiDataFactory.create(
            ssid = "Home",
            bssid = "AA:BB:CC:DD:EE:FF",
            rssi = -40,
            bssLoad = BssLoad(
                stationCount = 10,
                channelUtilization = 255,
                availableAdmissionCapacity = 20000,
            ),
        )
        val score = ChannelScorer.scoreChannel(36, listOf(w(wifi)))

        val ap = score.apScores.single()
        assertEquals("Home", ap.ssid)
        assertEquals(-40, ap.rssi)
        assertEquals(1.0, ap.weight, 0.0001)
        assertEquals(25, ap.rssiPenalty)
        assertEquals(100.0, ap.channelUtilizationPct!!, 0.0001)
        assertEquals(30, ap.bssLoadPenalty)
        assertEquals(10, ap.stationCount)
    }

    // ---------- 加权行为 ----------

    @Test
    fun `部分权重AP_等效负载不足1不扣数量分`() {
        val wifi = WifiDataFactory.create(rssi = -90)
        val score = ChannelScorer.scoreChannel(2, listOf(w(wifi, 0.75)))

        assertEquals(0.75, score.load, 0.0001)
        assertEquals(0, score.apCountPenalty)
        assertEquals(100, score.score)
    }

    @Test
    fun `相邻信道干扰_按等效负载扣分`() {
        val main = WifiDataFactory.create(bssid = "00:11:22:33:44:01", rssi = -90)
        val adjacent = WifiDataFactory.create(bssid = "00:11:22:33:44:02", rssi = -90)

        val score = ChannelScorer.scoreChannel(
            6,
            listOf(w(main, 1.0), w(adjacent, 0.75))
        )

        assertEquals(1.75, score.load, 0.0001)
        assertEquals(6, score.apCountPenalty) // (1.75 - 1) * 8 = 6
        assertEquals(94, score.score)
    }

    @Test
    fun `加权平均RSSI`() {
        val strong = WifiDataFactory.create(bssid = "00:11:22:33:44:01", rssi = -40)
        val weak = WifiDataFactory.create(bssid = "00:11:22:33:44:02", rssi = -90)

        val score = ChannelScorer.scoreChannel(
            6,
            listOf(w(strong, 0.5), w(weak, 0.5))
        )

        // (-40*0.5 + -90*0.5) / 1.0 = -65
        assertEquals(-65.0, score.avgRssi, 0.0001)
        assertEquals(11, score.rssiPenalty) // rssiPenaltyOf(-65) = 11
    }
}
