package top.pingwcy.wifianalyzer.data

import top.pingwcy.wifianalyzer.model.*

/**
 * Beacon 管理帧信息元素（IE）的纯 Kotlin 解码器。
 *
 * 不依赖任何 Android SDK，输入为 [IeData] 列表，输出 [BeaconInfo]。
 * 便于纯 JVM 单元测试，也消除了原先解析器里共享可变状态的问题。
 */
object BeaconIeDecoder {

    private const val ID_SUPPORTED_RATES = 1
    private const val ID_COUNTRY = 7
    private const val ID_BSS_LOAD = 11
    private const val ID_HT_CAP = 45
    private const val ID_RSN = 48
    private const val ID_EXT_RATES = 50
    private const val ID_MOBILITY_DOMAIN = 54
    private const val ID_HT_OPERATION = 61
    private const val ID_MULTIPLE_BSSID = 71
    private const val ID_EXT_CAP = 127
    private const val ID_RSNXE = 244
    private const val ID_RNR = 201
    private const val ID_VHT_CAP = 191
    private const val ID_VHT_OPERATION = 192
    private const val ID_TX_POWER = 195
    private const val ID_VENDOR = 221
    private const val ID_EXTENSION = 255

    // Extension IDs
    private const val EXT_HE_CAP = 35
    private const val EXT_HE_OPERATION = 36
    private const val EXT_HE_MU_EDCA = 38
    private const val EXT_SPATIAL_REUSE = 39
    private const val EXT_HE_6GHZ = 59
    private const val EXT_EHT_CAP = 108
    private const val EXT_EHT_OPERATION = 109

    fun decode(elements: List<IeData>): BeaconInfo {
        val raw = mutableListOf<RawInformationElement>()
        val warnings = mutableListOf<String>()
        val rates = mutableListOf<WifiRate>()
        val vendors = mutableListOf<VendorIe>()

        val rnr = mutableListOf<RnrNeighborAp>()
        val multipleBssid = mutableListOf<MultipleBssidElement>()

        var country: CountryInfo? = null
        var bssLoad: BssLoad? = null
        var rsn: RsnInfo? = null
        var rsnxe: RsnxeInfo? = null

        var ht: HtInfo? = null
        var vht: VhtInfo? = null
        var he: HeInfo? = null
        var eht: EhtInfo? = null
        var wmm: WmmInfo? = null

        var extCaps: ExtendedCapabilitiesInfo? = null
        var powerEnvelope: PowerEnvelopeInfo? = null
        var mobilityDomain: MobilityDomainInfo? = null

        for (ie in elements) {
            val id = ie.id
            val idExt = if (id == ID_EXTENSION) ie.idExt else -1
            val data = ie.bytes

            // 无论是否认识，都先保存
            raw += RawInformationElement(
                id = id,
                idExt = idExt.takeIf { it >= 0 },
                data = data.copyOf()
            )

            try {
                when (id) {
                    ID_SUPPORTED_RATES,
                    ID_EXT_RATES -> {
                        rates += parseRates(data)
                    }

                    ID_COUNTRY -> {
                        country = parseCountry(data)
                    }

                    ID_BSS_LOAD -> {
                        bssLoad = parseBssLoad(data)
                    }

                    ID_RSN -> {
                        rsn = parseRsn(data)
                    }

                    ID_RSNXE -> {
                        rsnxe = parseRsnxe(data)
                    }

                    ID_HT_CAP -> {
                        ht = parseHtCapabilities(data, ht)
                    }

                    ID_HT_OPERATION -> {
                        ht = parseHtOperation(data, ht)
                    }

                    ID_VHT_CAP -> {
                        vht = parseVhtCapabilities(data, vht)
                    }

                    ID_VHT_OPERATION -> {
                        vht = parseVhtOperation(data, vht)
                    }

                    ID_EXT_CAP -> {
                        extCaps = parseExtendedCapabilities(data)
                    }

                    ID_TX_POWER -> {
                        powerEnvelope = parsePowerEnvelope(data)
                    }

                    ID_MOBILITY_DOMAIN -> {
                        mobilityDomain = parseMobilityDomain(data)
                    }

                    ID_MULTIPLE_BSSID -> {
                        multipleBssid += parseMultipleBssid(data)
                    }

                    ID_RNR -> {
                        rnr += parseRnr(data)
                    }

                    ID_VENDOR -> {
                        val v = parseVendor(data)
                        if (v != null) {
                            vendors += v
                            if (v.oui == "00:50:F2" && v.vendorType == 2 && data.size >= 6) {
                                val subtype = data[4].toInt() and 0xFF
                                val version = data[5].toInt() and 0xFF
                                val isParameterized = subtype == 1
                                if (isParameterized && data.size >= 8) {
                                    val qos = readU16LE(data, 6) ?: 0
                                    wmm = WmmInfo(
                                        version = version,
                                        qosInfo = qos,
                                        uApsd = (qos and 0x80) != 0,
                                        parameterized = true,
                                        acm = null
                                    )
                                } else {
                                    wmm = WmmInfo(
                                        version = version,
                                        qosInfo = readU16LE(data, 6) ?: 0,
                                        uApsd = false,
                                        parameterized = false,
                                        acm = null
                                    )
                                }
                            }
                        }
                    }

                    ID_EXTENSION -> {
                        when (idExt) {
                            EXT_HE_CAP -> {
                                he = parseHeCapabilities(data, he)
                            }

                            EXT_HE_OPERATION -> {
                                he = (he ?: emptyHe()).copy(operation = data.copyOf())
                            }

                            EXT_HE_MU_EDCA,
                            EXT_SPATIAL_REUSE,
                            EXT_HE_6GHZ -> {
                                // 保留在 raw 中
                            }

                            EXT_EHT_CAP -> {
                                eht = parseEhtCapabilities(data, eht)
                            }

                            EXT_EHT_OPERATION -> {
                                eht = parseEhtOperation(data, eht)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                warnings += "IE=$id EXT=$idExt: ${e.message}"
            }
        }

        return BeaconInfo(
            rawIes = raw,
            country = country,
            bssLoad = bssLoad,
            supportedRates = rates,
            rsn = rsn,
            rsnxe = rsnxe,
            ht = ht,
            vht = vht,
            he = he,
            eht = eht,
            wmm = wmm,
            extendedCapabilities = extCaps,
            powerEnvelope = powerEnvelope,
            mobilityDomain = mobilityDomain,
            vendorIes = vendors,
            rnr = rnr,
            multipleBssid = multipleBssid,
            parseWarnings = warnings,
        )
    }

    // ============ 各 IE 解析 ============

    private fun parseRsnxe(data: ByteArray): RsnxeInfo? {
        if (data.size < 4) return null
        val version = readU16LE(data, 0) ?: return null
        val caps = readU16LE(data, 2) ?: return null

        val capabilities = mutableListOf<String>()
        if (caps and 0x0001 != 0) capabilities += "BSS Transition"
        if (caps and 0x0002 != 0) capabilities += "PeerKey Enabled"
        if (caps and 0x0004 != 0) capabilities += "SAE PWE (hash-to-curve)"
        if (caps and 0x0008 != 0) capabilities += "Beacon Protection"
        if (caps and 0x0010 != 0) capabilities += "Transition Disable"
        if (caps and 0x0020 != 0) capabilities += "Secure LTF"
        if (caps and 0x0040 != 0) capabilities += "Protected EHT Action"
        if (caps and 0x0080 != 0) capabilities += "EHT Link Maintenance"

        return RsnxeInfo(version = version, capabilities = capabilities)
    }

    private fun parseExtendedCapabilities(data: ByteArray): ExtendedCapabilitiesInfo {
        // 位号 -> 描述；bit 从 0 开始，LSB first
        val knownBits = mapOf(
            4 to "BSS Transition",
            39 to "OCV (Operating Channel Validation)",
            44 to "SAE PWE Method",
            46 to "Beacon Protection",
            77 to "TWT Requester",
            78 to "TWT Responder",
        )

        val capabilities = mutableListOf<String>()
        data.forEachIndexed { byteIndex, byte ->
            var b = byte.toInt() and 0xFF
            var bitInByte = 0
            while (b != 0) {
                if (b and 0x01 != 0) {
                    val bit = byteIndex * 8 + bitInByte
                    knownBits[bit]?.let { capabilities += it }
                }
                b = b ushr 1
                bitInByte++
            }
        }

        return ExtendedCapabilitiesInfo(capabilities = capabilities, raw = data.copyOf())
    }

    private fun parsePowerEnvelope(data: ByteArray): PowerEnvelopeInfo? {
        if (data.size < 2) return null

        // 0.5 dBm 步进；-128 (0x80) 表示未定义
        fun decodePower(value: Int): Double? {
            if (value == -128) return null
            return value / 2.0
        }

        val maxPower = decodePower(data[0].toInt())
        val localMaxPower = decodePower(data[1].toInt())

        val clientInfos = mutableListOf<RegulatoryClientInfo>()
        var off = 2
        while (off + 1 < data.size) {
            val clientPower = decodePower(data[off].toInt())
            val category = data[off + 1].toInt() and 0xFF
            val catValue = if (category == 0) null else category
            clientInfos += RegulatoryClientInfo(
                maxTransmitPowerDbm = clientPower,
                maxTransmitPowerCategory = catValue,
            )
            off += 2
        }

        return PowerEnvelopeInfo(
            maxTransmitPowerDbm = maxPower,
            localMaxTransmitPowerDbm = localMaxPower,
            regulatoryClientInfos = clientInfos,
        )
    }

    private fun parseMobilityDomain(data: ByteArray): MobilityDomainInfo? {
        if (data.size < 3) return null
        val mdid = readU16LE(data, 0) ?: return null
        val ft = data[2].toInt() and 0xFF
        return MobilityDomainInfo(
            mdid = mdid,
            fastBssTransitionOverDs = (ft and 0x01) != 0,
            resourceRequestProtocolCapability = (ft and 0x02) != 0,
        )
    }

    private fun parseRnr(data: ByteArray): List<RnrNeighborAp> {
        val result = mutableListOf<RnrNeighborAp>()
        var off = 0
        while (off + 3 <= data.size) {
            val header = data[off].toInt() and 0xFF
            val tbttOffset = header and 0x3F
            val infoCount = ((header shr 6) and 0x03) + 1

            val operatingClass = data[off + 1].toInt() and 0xFF
            val channelNumber = data[off + 2].toInt() and 0xFF
            off += 3

            var parsed = false
            repeat(infoCount) {
                // 每个 TBTT info 至少 13 字节：offset(1) + bssid(6) + short ssid(4) + params(1) + psd(1)
                if (off + 13 > data.size) break

                val bssidBytes = data.copyOfRange(off + 1, off + 7)
                val bssid = bssidBytes.toBssidString()
                val shortSsidBytes = data.copyOfRange(off + 7, off + 11)
                val shortSsid = shortSsidBytes.toShortSsidString()
                val params = data[off + 11].toInt() and 0xFF

                result += RnrNeighborAp(
                    tbttOffsetTu = tbttOffset,
                    operatingClass = operatingClass,
                    channelNumber = channelNumber,
                    bssid = bssid,
                    shortSsid = shortSsid,
                    sameSsid = (params and 0x01) != 0,
                    multipleBssid = (params and 0x02) != 0,
                    transmittedBssid = (params and 0x04) != 0,
                )
                parsed = true
                off += 13
            }

            // 单个 TBTT info 是 13 字节；若上面循环未完整消费，说明结构异常，停止
            if (!parsed) break
        }
        return result
    }

    private fun parseMultipleBssid(data: ByteArray): MultipleBssidElement {
        if (data.isEmpty()) return MultipleBssidElement(0, emptyList())
        val maxBssidIndicator = data[0].toInt() and 0xFF

        val profiles = mutableListOf<MultipleBssidProfile>()
        var off = 1
        while (off + 1 < data.size) {
            val subId = data[off].toInt() and 0xFF
            val subLen = data[off + 1].toInt() and 0xFF
            if (off + 2 + subLen > data.size) break
            val subData = data.copyOfRange(off + 2, off + 2 + subLen)
            profiles += MultipleBssidProfile(subelementId = subId, data = subData)
            off += 2 + subLen
        }

        return MultipleBssidElement(maxBssidIndicator = maxBssidIndicator, profiles = profiles)
    }

    private fun parseHtCapabilities(data: ByteArray, existing: HtInfo?): HtInfo {
        if (data.size >= 26) {
            val caps = readU16LE(data, 0) ?: 0
            val ampdu = data[2].toInt() and 0xFF
            val mcs = data.copyOfRange(3, 19)
            val htExt = readU16LE(data, 19)
            val txBf = readU32LE(data, 21)
            val asel = if (data.size >= 28) readU16LE(data, 25) else null

            val maxAmsdu = when ((caps shr 0) and 0x3) {
                0 -> 3839
                1 -> 7935
                else -> null
            }
            val maxAmpdu = when ((caps shr 1) and 0x3) {
                0 -> 8191
                1 -> 16383
                2 -> 32767
                3 -> 65535
                else -> null
            }
            val sgi20 = (caps and 0x0020) != 0
            val sgi40 = (caps and 0x0040) != 0
            val htGreenfield = (caps and 0x0010) != 0
            val ldpc = (caps and 0x0002) != 0
            val fortyMhz = (caps and 0x0400) != 0
            val stbc = ((caps shr 5) and 0x3)

            return HtInfo(
                capabilities = caps,
                maxAmsduLength = maxAmsdu,
                ampduParameters = ampdu,
                rxMcsSet = mcs,
                operation = existing?.operation,
                htExtendedCapabilities = htExt,
                txBeamformingCapabilities = txBf?.toLong(),
                aselCapability = asel,
                sgi20 = sgi20, sgi40 = sgi40,
                greenfield = htGreenfield, ldpc = ldpc,
                fortyMhzSupported = fortyMhz,
                maxAmpduLength = maxAmpdu,
                stbcRxStreams = stbc
            )
        }
        return HtInfo(
            capabilities = readU16LE(data, 0) ?: 0,
            maxAmsduLength = null,
            ampduParameters = data.getOrNull(2)?.toInt()?.and(0xFF),
            rxMcsSet = data.copyOfRangeSafe(3, 19),
            operation = existing?.operation
        )
    }

    private fun parseHtOperation(data: ByteArray, existing: HtInfo?): HtInfo {
        return if (data.size >= 22) {
            (existing ?: emptyHt()).copy(
                operation = data.copyOf(),
                primaryChannel = data[0].toInt() and 0xFF,
                secondaryChannelOffset = (data[1].toInt() shr 0) and 0x3,
                channelWidth = if (((data[1].toInt() shr 2) and 0x3) == 1) 40 else 20,
                rifsMode = (data[1].toInt() and 0x10) != 0
            )
        } else {
            (existing ?: emptyHt()).copy(operation = data.copyOf())
        }
    }

    private fun parseVhtCapabilities(data: ByteArray, existing: VhtInfo?): VhtInfo {
        if (data.size >= 12) {
            val cap = readU32LE(data, 0)?.toLong() ?: 0L
            val rxMcs = readU16LE(data, 4)
            val txMcs = readU16LE(data, 8)
            val maxMpdu = when ((cap shr 0) and 0x3) {
                0L -> 7991; 1L -> 11454; 2L -> 11454; else -> null
            }
            val chanWidth = (cap shr 2) and 0x3
            val rxLdpc = (cap and 0x10L) != 0L
            val sgi80 = (cap and 0x20L) != 0L
            val sgi160 = (cap and 0x40L) != 0L
            val txStbc = (cap and 0x80L) != 0L
            val rxStbc = ((cap shr 8) and 0x7).toInt()
            val suBeamformer = (cap and 0x0800L) != 0L
            val muBeamformer = (cap and 0x10000L) != 0L
            return VhtInfo(
                capabilities = cap,
                rxMcsMap = rxMcs,
                txMcsMap = txMcs,
                operation = existing?.operation,
                maxMpduLength = maxMpdu,
                supportedChannelWidth = chanWidth.toInt(),
                rxLdpc = rxLdpc, sgi80 = sgi80, sgi160 = sgi160,
                txStbc = txStbc, rxStbc = rxStbc,
                suBeamformer = suBeamformer,
                muBeamformer = muBeamformer
            )
        }
        return VhtInfo(
            capabilities = readU32LE(data, 0)?.toLong() ?: 0L,
            rxMcsMap = null,
            txMcsMap = null,
            operation = existing?.operation
        )
    }

    private fun parseVhtOperation(data: ByteArray, existing: VhtInfo?): VhtInfo {
        return if (data.size >= 5) {
            val width = data[0].toInt() and 0xFF
            (existing ?: emptyVht()).copy(
                operation = data.copyOf(),
                channelWidth = width,
                centerFreqSeg0 = data[1].toInt() and 0xFF,
                centerFreqSeg1 = data[2].toInt() and 0xFF,
                basicMcs = readU16LE(data, 3)
            )
        } else {
            (existing ?: emptyVht()).copy(operation = data.copyOf())
        }
    }

    private fun parseHeCapabilities(data: ByteArray, existing: HeInfo?): HeInfo {
        if (data.size >= 6 + 11 + 4) {
            val mac = data.copyOfRange(0, 6)
            val phy = data.copyOfRange(6, 17)
            val mcs = data.copyOfRange(17, 21)
            return (existing ?: emptyHe()).copy(
                macCapabilities = mac,
                phyCapabilities = phy,
                rxMcsNss = mcs,
                operation = existing?.operation,
                heSupported = true,
                twtRequester = (mac[0].toInt() and 0x01) != 0,
                twtResponder = (mac[0].toInt() and 0x02) != 0,
                muEdca = (mac[2].toInt() and 0x80) != 0,
                omControl = (mac[3].toInt() and 0x40) != 0,
                suBeamformer = (phy[0].toInt() and 0x01) != 0,
                suBeamformee = (phy[0].toInt() and 0x02) != 0,
                muBeamformer = (phy[0].toInt() and 0x04) != 0,
                channelWidth160 = (phy[1].toInt() and 0x80) != 0
            )
        }
        return (existing ?: emptyHe()).copy(operation = existing?.operation)
    }

    private fun parseEhtCapabilities(data: ByteArray, existing: EhtInfo?): EhtInfo {
        if (data.size >= 11) {
            val mac = data.copyOfRange(0, 2)
            val phy = data.copyOfRange(2, 11)
            val macU16 = readU16LE(mac, 0) ?: 0
            return (existing ?: emptyEht()).copy(
                macCapabilities = mac,
                phyCapabilities = phy,
                operation = existing?.operation,
                epcsPriorityAccess = (macU16 and 0x0001) != 0,
                ehtOmControl = (macU16 and 0x0002) != 0,
                triggeredTxopSharing = (macU16 and 0x0004) != 0,
                support320Mhz = (phy[0].toInt() and 0x01) != 0,
                support4096Qam = (phy[5].toInt() and 0x01) != 0
            )
        }
        return (existing ?: emptyEht()).copy(operation = existing?.operation)
    }

    private fun parseEhtOperation(data: ByteArray, existing: EhtInfo?): EhtInfo {
        return if (data.size >= 5) {
            val params = data[0].toInt() and 0xFF
            val widthCode = (params shr 1) and 0x7
            (existing ?: emptyEht()).copy(
                operation = data.copyOf(),
                channelWidth = when (widthCode) {
                    0 -> 20; 1 -> 40; 2 -> 80; 3 -> 160; 4 -> 320; else -> null
                },
                puncturingBitmap = readU16LE(data, 1)
            )
        } else {
            (existing ?: emptyEht()).copy(operation = data.copyOf())
        }
    }

    private fun parseCountry(data: ByteArray): CountryInfo? {
        if (data.size < 3) return null
        val code = String(data.copyOfRange(0, 2), Charsets.US_ASCII)
        if (!code.matches(Regex("[A-Z]{2}"))) return null
        val envByte = data[2].toInt() and 0xFF
        val env = when (envByte) {
            'I'.code, 'O'.code, ' '.code -> envByte.toChar()
            else -> null
        }

        val triplets = mutableListOf<CountryTriplet>()
        var off = 3
        while (off + 2 < data.size) {
            val first = data[off].toInt() and 0xFF
            if (first == 201) {
                off += 3
                continue
            }
            val count = data[off + 1].toInt() and 0xFF
            val power = data[off + 2].toInt() and 0xFF
            triplets += CountryTriplet(
                firstChannel = first,
                channelCount = count,
                maxPowerDbm = if (power == 0xFF) null else power
            )
            off += 3
        }
        return CountryInfo(code, env, triplets)
    }

    private fun parseRsn(data: ByteArray): RsnInfo? {
        if (data.size < 2) return null
        var off = 0

        val version = readU16LE(data, off) ?: return null
        off += 2

        if (off + 4 > data.size) return null
        val groupCipher = ouiToString(data, off)
        off += 4

        val pairwiseCount = readU16LE(data, off) ?: return null
        off += 2
        val pairwise = mutableListOf<String>()
        repeat(pairwiseCount) {
            if (off + 4 > data.size) return null
            pairwise += ouiToString(data, off); off += 4
        }

        val akmCount = readU16LE(data, off) ?: return null
        off += 2
        val akms = mutableListOf<Int>()
        repeat(akmCount) {
            if (off + 4 > data.size) return null
            val type = data[off + 3].toInt() and 0xFF
            akms += type
            off += 4
        }

        var rsnCaps = 0
        if (off + 2 <= data.size) {
            rsnCaps = readU16LE(data, off) ?: 0
            off += 2
        }

        val pmkidList = mutableListOf<ByteArray>()
        if (off + 2 <= data.size) {
            val pmkidCount = readU16LE(data, off) ?: 0
            off += 2
            repeat(pmkidCount) {
                if (off + 16 > data.size) return@repeat
                pmkidList += data.copyOfRange(off, off + 16); off += 16
            }
        }

        val groupMgmtCipher = if (off + 4 <= data.size) {
            ouiToString(data, off)
        } else null

        val preAuth = (rsnCaps and 0x0001) != 0
        val noPairwise = (rsnCaps and 0x0002) != 0
        val ptksaReplayCounter = ((rsnCaps shr 2) and 0x3)
        val mfpRequired = (rsnCaps and 0x0040) != 0
        val mfpCapable = (rsnCaps and 0x0080) != 0
        val gtkNotUsed = (rsnCaps and 0x0400) != 0
        val sppAmsdu = (rsnCaps and 0x0800) != 0
        val peerKeyEnabled = (rsnCaps and 0x0200) != 0
        val ocv = (rsnCaps and 0x4000) != 0

        return RsnInfo(
            version = version,
            groupCipher = groupCipher,
            pairwiseCiphers = pairwise,
            akmSuites = akms,
            preAuthentication = preAuth,
            noPairwise = noPairwise,
            ptksaReplayCounter = ptksaReplayCounter,
            mfpRequired = mfpRequired,
            mfpCapable = mfpCapable,
            gtkNotUsed = gtkNotUsed,
            sppAmsdu = sppAmsdu,
            peerKeyEnabled = peerKeyEnabled,
            ocv = ocv,
            pmkids = pmkidList,
            groupMgmtCipher = groupMgmtCipher
        )
    }

    private fun parseRates(data: ByteArray): List<WifiRate> =
        data.map {
            val value = it.toInt() and 0xFF
            WifiRate(
                mbps = (value and 0x7F) * 0.5,
                basic = (value and 0x80) != 0
            )
        }

    private fun parseBssLoad(data: ByteArray): BssLoad? {
        if (data.size < 5) return null
        return BssLoad(
            stationCount = readU16LE(data, 0) ?: return null,
            channelUtilization = data[2].toInt() and 0xFF,
            availableAdmissionCapacity = readU16LE(data, 3) ?: return null
        )
    }

    private fun parseVendor(data: ByteArray): VendorIe? {
        if (data.size < 3) return null
        val oui = data.take(3).joinToString(":") {
            "%02X".format(it.toInt() and 0xFF)
        }
        return VendorIe(
            oui = oui,
            vendorType = data.getOrNull(3)?.toInt()?.and(0xFF),
            data = data.copyOf()
        )
    }

    // ============ 工具函数 ============

    private fun ouiToString(b: ByteArray, off: Int): String =
        "%02X:%02X:%02X".format(
            b[off].toInt() and 0xFF,
            b[off + 1].toInt() and 0xFF,
            b[off + 2].toInt() and 0xFF
        )

    private fun ByteArray.toBssidString(): String =
        joinToString(":") { "%02X".format(it.toInt() and 0xFF) }

    private fun ByteArray.toShortSsidString(): String {
        val printable = all { it.toInt() in 0x20..0x7E }
        return if (printable) {
            String(this, Charsets.US_ASCII)
        } else {
            "0x" + joinToString("") { "%02X".format(it.toInt() and 0xFF) }
        }
    }

    private fun readU16LE(data: ByteArray, offset: Int): Int? {
        if (offset < 0 || offset + 1 >= data.size) return null
        return (data[offset].toInt() and 0xFF) or
                ((data[offset + 1].toInt() and 0xFF) shl 8)
    }

    private fun readU32LE(data: ByteArray, offset: Int): Int? {
        if (offset < 0 || offset + 3 >= data.size) return null
        return (data[offset].toInt() and 0xFF) or
                ((data[offset + 1].toInt() and 0xFF) shl 8) or
                ((data[offset + 2].toInt() and 0xFF) shl 16) or
                ((data[offset + 3].toInt() and 0xFF) shl 24)
    }

    private fun ByteArray.copyOfRangeSafe(from: Int, to: Int): ByteArray? {
        if (from < 0 || to > size || from > to) return null
        return copyOfRange(from, to)
    }

    private fun emptyHe() = HeInfo(
        macCapabilities = null,
        phyCapabilities = null,
        rxMcsNss = null,
        operation = null
    )

    private fun emptyVht() = VhtInfo(
        capabilities = 0L,
        rxMcsMap = null,
        txMcsMap = null,
        operation = null
    )

    private fun emptyEht() = EhtInfo(
        macCapabilities = null,
        phyCapabilities = null,
        operation = null
    )

    private fun emptyHt() = HtInfo(
        capabilities = 0,
        maxAmsduLength = null,
        ampduParameters = null,
        rxMcsSet = null,
        operation = null
    )
}
