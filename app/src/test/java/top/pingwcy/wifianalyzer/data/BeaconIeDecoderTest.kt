package top.pingwcy.wifianalyzer.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import top.pingwcy.wifianalyzer.model.CountryEnvironment

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
        assertEquals("CCMP (AES)", info.rsn!!.groupCipher)
        assertEquals(listOf("CCMP (AES)"), info.rsn!!.pairwiseCiphers)
        assertEquals(listOf(2), info.rsn!!.akmSuites)
    }

    @Test
    fun `解析Rsn_TKIP组播和CCMP单播`() {
        // group=TKIP(00-0F-AC-02), pairwise=CCMP(00-0F-AC-04)
        val bytes = byteArrayOf(
            0x01, 0x00,
            0x00, 0x0F, 0xAC.toByte(), 0x02,   // group TKIP
            0x01, 0x00,
            0x00, 0x0F, 0xAC.toByte(), 0x04,   // pairwise CCMP
            0x01, 0x00,
            0x00, 0x0F, 0xAC.toByte(), 0x02,   // akm PSK
            0x00, 0x00,
        )
        val info = BeaconIeDecoder.decode(listOf(IeData(id = 48, idExt = -1, bytes = bytes)))

        assertNotNull(info.rsn)
        assertEquals("TKIP", info.rsn!!.groupCipher)
        assertEquals(listOf("CCMP (AES)"), info.rsn!!.pairwiseCiphers)
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

    @Test
    fun `解析Rsnxe_版本和能力位`() {
        // version=1, caps = 0x0009 (bit 0 BSS Transition + bit 3 Beacon Protection)
        val bytes = byteArrayOf(0x01, 0x00, 0x09, 0x00)
        val info = BeaconIeDecoder.decode(listOf(IeData(id = 244, idExt = -1, bytes = bytes)))

        assertNotNull(info.rsnxe)
        assertEquals(1, info.rsnxe!!.version)
        assertTrue(info.rsnxe!!.capabilities.contains("BSS Transition"))
        assertTrue(info.rsnxe!!.capabilities.contains("Beacon Protection"))
    }

    @Test
    fun `解析ExtendedCapabilities_关键能力位`() {
        // bit 4 (BSS Transition) + bit 39 (OCV) 置位
        // bit 4 -> byte 0 bit 4 = 0x10
        // bit 39 -> byte 4 bit 7 = 0x80
        val bytes = byteArrayOf(
            0x10, 0x00, 0x00, 0x00, 0x80.toByte(), 0x00,
        )
        val info = BeaconIeDecoder.decode(listOf(IeData(id = 127, idExt = -1, bytes = bytes)))

        assertNotNull(info.extendedCapabilities)
        assertTrue(info.extendedCapabilities!!.capabilities.contains("BSS Transition"))
        assertTrue(info.extendedCapabilities!!.capabilities.contains("OCV (Operating Channel Validation)"))
    }

    @Test
    fun `解析PowerEnvelope_功率值`() {
        // count=1, max=20dBm(0x28), per20=20dBm(0x28), client=18dBm(0x24) category 1
        val bytes = byteArrayOf(0x01, 0x28, 0x28, 0x24, 0x01)
        val info = BeaconIeDecoder.decode(listOf(IeData(id = 195, idExt = -1, bytes = bytes)))

        assertNotNull(info.powerEnvelope)
        assertEquals(listOf(20.0), info.powerEnvelope!!.maxTransmitPowersDbm)
        assertEquals(20.0, info.powerEnvelope!!.localMaxTransmitPowerDbm!!, 0.001)
        assertEquals(1, info.powerEnvelope!!.regulatoryClientInfos.size)
        assertEquals(18.0, info.powerEnvelope!!.regulatoryClientInfos[0].maxTransmitPowerDbm!!, 0.001)
        assertEquals(1, info.powerEnvelope!!.regulatoryClientInfos[0].maxTransmitPowerCategory)
    }

    @Test
    fun `解析PowerEnvelope_多信道宽度功率`() {
        // 真实抓包：count=2, 20MHz=24dBm(0x30), 40MHz=24dBm(0x30), per20=24dBm(0x30)
        val bytes = byteArrayOf(0x02, 0x30, 0x30, 0x30)
        val info = BeaconIeDecoder.decode(listOf(IeData(id = 195, idExt = -1, bytes = bytes)))

        assertNotNull(info.powerEnvelope)
        assertEquals(listOf(24.0, 24.0), info.powerEnvelope!!.maxTransmitPowersDbm)
        assertEquals(24.0, info.powerEnvelope!!.localMaxTransmitPowerDbm!!, 0.001)
        assertTrue(info.powerEnvelope!!.regulatoryClientInfos.isEmpty())
    }

    @Test
    fun `解析MobilityDomain_MDID和FT能力`() {
        // MDID = 0x1234, FT = 0x03 (bit0 FT over DS + bit1 RRP)
        val bytes = byteArrayOf(0x34, 0x12, 0x03)
        val info = BeaconIeDecoder.decode(listOf(IeData(id = 54, idExt = -1, bytes = bytes)))

        assertNotNull(info.mobilityDomain)
        assertEquals(0x1234, info.mobilityDomain!!.mdid)
        assertTrue(info.mobilityDomain!!.fastBssTransitionOverDs)
        assertTrue(info.mobilityDomain!!.resourceRequestProtocolCapability)
    }

    @Test
    fun `解析Rnr_邻居AP信息`() {
        // header: tbtt offset=0, count=0 (1 个 info)
        // operating class=131, channel=37
        // info: tbtt offset=0, bssid=aa:bb:cc:dd:ee:ff, short ssid="test", params=0x01(same ssid), psd=0
        val bytes = byteArrayOf(
            0x00,                       // header
            0x83.toByte(),              // operating class 131
            0x25,                       // channel 37
            0x00,                       // neighbor tbtt offset
            0xAA.toByte(), 0xBB.toByte(), 0xCC.toByte(), 0xDD.toByte(), 0xEE.toByte(), 0xFF.toByte(),
            't'.code.toByte(), 'e'.code.toByte(), 's'.code.toByte(), 't'.code.toByte(),
            0x01,                       // params: same ssid
            0x00,                       // psd
        )

        val info = BeaconIeDecoder.decode(listOf(IeData(id = 201, idExt = -1, bytes = bytes)))

        assertEquals(1, info.rnr.size)
        assertEquals("AA:BB:CC:DD:EE:FF", info.rnr[0].bssid)
        assertEquals("test", info.rnr[0].shortSsid)
        assertEquals(131, info.rnr[0].operatingClass)
        assertEquals(37, info.rnr[0].channelNumber)
        assertTrue(info.rnr[0].sameSsid)
    }

    @Test
    fun `解析MultipleBssid_MaxIndicator和Profile`() {
        // max bssid indicator = 3, subelement 0 len 2 data {0x01, 0x02}
        val bytes = byteArrayOf(0x03, 0x00, 0x02, 0x01, 0x02)
        val info = BeaconIeDecoder.decode(listOf(IeData(id = 71, idExt = -1, bytes = bytes)))

        assertEquals(1, info.multipleBssid.size)
        assertEquals(3, info.multipleBssid[0].maxBssidIndicator)
        assertEquals(1, info.multipleBssid[0].profiles.size)
        assertEquals(0, info.multipleBssid[0].profiles[0].subelementId)
        assertEquals(2, info.multipleBssid[0].profiles[0].data.size)
    }

    @Test
    fun `解析Country_环境字段`() {
        fun env(code: Char) = BeaconIeDecoder.decode(
            listOf(IeData(id = 7, idExt = -1, bytes = byteArrayOf('C'.code.toByte(), 'N'.code.toByte(), code.code.toByte())))
        ).country!!.environment

        assertEquals(CountryEnvironment.INDOOR, env('I'))
        assertEquals(CountryEnvironment.OUTDOOR, env('O'))
        assertEquals(CountryEnvironment.INDOOR_OUTDOOR, env('X'))
        assertNull(env(' '))
        assertNull(env('\u0000'))
    }

    @Test
    fun `解析Vht_MIMO空间流数量`() {
        // 真实抓包：Rx MCS=0xFFF5、Tx MCS=0xFFF5 -> 各支持 2 个 NSS
        val bytes = byteArrayOf(
            0xB1.toByte(), 0x21, 0x81.toByte(), 0x03,   // capabilities
            0xF5.toByte(), 0xFF.toByte(),               // rx mcs
            0x00, 0x00,                                 // rx highest
            0xF5.toByte(), 0xFF.toByte(),               // tx mcs
            0x00, 0x00,                                 // tx highest
        )
        val info = BeaconIeDecoder.decode(listOf(IeData(id = 191, idExt = -1, bytes = bytes)))

        assertNotNull(info.vht)
        assertEquals(2, info.vht!!.rxNss)
        assertEquals(2, info.vht!!.txNss)
    }

    @Test
    fun `解析He_MIMO空间流数量`() {
        // 真实抓包：≤80MHz MCS=FA FF FA FF -> Rx/Tx 各 2 个 NSS
        val bytes = byteArrayOf(
            0x09, 0x00, 0x08, 0x12, 0x00, 0x10,           // mac capabilities
            0x00, 0x20, 0x02, 0x40, 0x01, 0x00, 0x80.toByte(), 0x00, 0x00, 0x0C, 0x00, // phy capabilities
            0xFA.toByte(), 0xFF.toByte(), 0xFA.toByte(), 0xFF.toByte(), // mcs/nss set
        )
        val info = BeaconIeDecoder.decode(listOf(IeData(id = 255, idExt = 35, bytes = bytes)))

        assertNotNull(info.he)
        assertEquals(2, info.he!!.rxNss)
        assertEquals(2, info.he!!.txNss)
    }

    @Test
    fun `解析Ht_Rx空间流数量`() {
        // HT Capabilities：Rx MCS 0-15 全置位 -> 2 流
        val bytes = ByteArray(26)
        bytes[3] = 0xFF.toByte() // MCS 0-7
        bytes[4] = 0xFF.toByte() // MCS 8-15
        val info = BeaconIeDecoder.decode(listOf(IeData(id = 45, idExt = -1, bytes = bytes)))

        assertNotNull(info.ht)
        assertEquals(2, info.ht!!.rxNss)
    }
}
