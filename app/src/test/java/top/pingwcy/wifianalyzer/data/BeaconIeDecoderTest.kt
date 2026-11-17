package top.pingwcy.wifianalyzer.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BeaconIeDecoderTest {

    @Test
    fun `空列表_返回空BeaconInfo`() {
        val info = BeaconIeDecoder.decode(emptyList())
        assertTrue(info.rawIes.isEmpty())
        assertNull(info.country)
        assertNull(info.bssLoad)
        assertNull(info.rsn)
        assertTrue(info.supportedRates.isEmpty())
    }

    @Test
    fun `解析SupportedRates`() {
        // 0x82 = basic + 2*0.5=1Mbps；0x84 = basic + 4*0.5=2Mbps；0x0B = 5.5Mbps 非 basic
        val info = BeaconIeDecoder.decode(
            listOf(IeData(id = 1, idExt = -1, bytes = byteArrayOf(0x82.toByte(), 0x84.toByte(), 0x0B)))
        )

        assertEquals(3, info.supportedRates.size)
        assertEquals(1.0, info.supportedRates[0].mbps, 0.001)
        assertTrue(info.supportedRates[0].basic)
        assertEquals(2.0, info.supportedRates[1].mbps, 0.001)
        assertTrue(info.supportedRates[1].basic)
        assertEquals(5.5, info.supportedRates[2].mbps, 0.001)
        assertTrue(!info.supportedRates[2].basic)
    }

    @Test
    fun `解析Country_有效国家码`() {
        // "CN" + 环境 0x20 + triplet(起始信道1, 11个信道, 最大功率20dBm)
        val bytes = byteArrayOf(
            'C'.code.toByte(), 'N'.code.toByte(), 0x20,
            0x01, 0x0B, 0x14
        )
        val info = BeaconIeDecoder.decode(listOf(IeData(id = 7, idExt = -1, bytes = bytes)))

        assertNotNull(info.country)
        assertEquals("CN", info.country!!.countryCode)
        assertEquals(1, info.country!!.triplets.size)
        assertEquals(1, info.country!!.triplets[0].firstChannel)
        assertEquals(11, info.country!!.triplets[0].channelCount)
        assertEquals(20, info.country!!.triplets[0].maxPowerDbm)
    }

    @Test
    fun `解析Country_无效国家码_返回null`() {
        // "12" 非法国家码
        val bytes = byteArrayOf('1'.code.toByte(), '2'.code.toByte(), 0x20)
        val info = BeaconIeDecoder.decode(listOf(IeData(id = 7, idExt = -1, bytes = bytes)))
        assertNull(info.country)
    }

    @Test
    fun `解析BssLoad`() {
        // station count = 5 (LE)，channel utilization = 100，available capacity = 20000 (0x4E20 LE)
        val bytes = byteArrayOf(0x05, 0x00, 0x64, 0x20.toByte(), 0x4E.toByte())
        val info = BeaconIeDecoder.decode(listOf(IeData(id = 11, idExt = -1, bytes = bytes)))

        assertNotNull(info.bssLoad)
        assertEquals(5, info.bssLoad!!.stationCount)
        assertEquals(100, info.bssLoad!!.channelUtilization)
        assertEquals(20000, info.bssLoad!!.availableAdmissionCapacity)
    }

    @Test
    fun `解析Rsn_WPA2_PSK`() {
        // RSN IE：version=1, group cipher=CCMP(00-0F-AC-04), pairwise=CCMP, AKM=PSK(02), caps=0x0000
        val bytes = byteArrayOf(
            0x01, 0x00,                     // version
            0x00, 0x0F, 0xAC.toByte(), 0x04,  // group cipher
            0x01, 0x00,                     // pairwise count
            0x00, 0x0F, 0xAC.toByte(), 0x04,  // pairwise
            0x01, 0x00,                     // akm count
            0x00, 0x0F, 0xAC.toByte(), 0x02,  // akm (PSK)
            0x00, 0x00,                     // rsn caps
        )
        val info = BeaconIeDecoder.decode(listOf(IeData(id = 48, idExt = -1, bytes = bytes)))

        assertNotNull(info.rsn)
        assertEquals(1, info.rsn!!.version)
        assertEquals("00:0F:AC", info.rsn!!.groupCipher)
        assertEquals(listOf("00:0F:AC"), info.rsn!!.pairwiseCiphers)
        assertEquals(listOf(2), info.rsn!!.akmSuites)
    }

    @Test
    fun `解析Rsn_MFP能力位`() {
        val bytes = byteArrayOf(
            0x01, 0x00,
            0x00, 0x0F, 0x30.toByte(), 0x01,   // group cipher (任意)
            0x00, 0x00,                     // pairwise count = 0
            0x00, 0x00,                     // akm count = 0
            0x80.toByte(), 0x00,             // rsn caps: MFP capable (0x0080)
        )
        val info = BeaconIeDecoder.decode(listOf(IeData(id = 48, idExt = -1, bytes = bytes)))

        assertNotNull(info.rsn)
        assertTrue(info.rsn!!.mfpCapable)
        assertTrue(!info.rsn!!.mfpRequired)
    }

    @Test
    fun `解析数据过短_不抛异常并记录警告`() {
        // BSS Load 需要至少 5 字节，这里只有 2 字节 -> 返回 null，且整体不崩溃
        val info = BeaconIeDecoder.decode(
            listOf(IeData(id = 11, idExt = -1, bytes = byteArrayOf(0x01, 0x02)))
        )
        assertNull(info.bssLoad)
    }
}
