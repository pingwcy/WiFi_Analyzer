package top.pingwcy.wifianalyzer.domain

import top.pingwcy.wifianalyzer.model.BeaconInfo
import top.pingwcy.wifianalyzer.model.BssLoad
import top.pingwcy.wifianalyzer.model.CountryInfo
import top.pingwcy.wifianalyzer.model.EhtInfo
import top.pingwcy.wifianalyzer.model.ExtendedCapabilitiesInfo
import top.pingwcy.wifianalyzer.model.HeInfo
import top.pingwcy.wifianalyzer.model.HtInfo
import top.pingwcy.wifianalyzer.model.MobilityDomainInfo
import top.pingwcy.wifianalyzer.model.PowerEnvelopeInfo
import top.pingwcy.wifianalyzer.model.RnrNeighborAp
import top.pingwcy.wifianalyzer.model.RsnInfo
import top.pingwcy.wifianalyzer.model.RsnxeInfo
import top.pingwcy.wifianalyzer.model.VhtInfo
import top.pingwcy.wifianalyzer.model.WifiData
import top.pingwcy.wifianalyzer.model.WifiRate
import top.pingwcy.wifianalyzer.model.WmmInfo

/**
 * 扫描报告生成器：把当前扫描结果导出为结构化 JSON。
 *
 * JSON 便于程序二次解析，同时字段尽量覆盖详情页可展示的信息，
 * 包括基础信息、信号/信道、BSS Load 以及已解析的 Beacon IE。
 */
object ReportGenerator {

    private const val FORMAT = "wifi-analyzer-report"
    private const val FORMAT_VERSION = 1

    fun generate(wifiList: List<WifiData>): String {
        val sorted = wifiList.sortedByDescending { it.rssi }

        val bands = listOf("2.4 GHz", "5 GHz", "6 GHz").mapNotNull { band ->
            val aps = sorted.filter { it.band == band }
            if (aps.isEmpty()) null
            else obj(
                "band" to str(band),
                "apCount" to num(aps.size),
                "bestChannel" to num(ChannelRecommender.recommend(sorted, band).recommended),
            )
        }

        val aps = sorted.map { apJson(it) }

        return buildString {
            appendLine("{")
            appendLine("  \"format\": ${str(FORMAT)},")
            appendLine("  \"formatVersion\": ${num(FORMAT_VERSION)},")
            appendLine("  \"totalAps\": ${num(sorted.size)},")
            appendLine("  \"bands\": ${arr(bands)},")
            appendLine("  \"aps\": ${arr(aps)}")
            append("}")
        }
    }

    // ============ AP 及 Beacon 各字段 ============

    private fun apJson(w: WifiData): String {
        val entries = mutableListOf<Pair<String, String>>(
            "ssid" to str(w.ssid),
            "bssid" to str(w.bssid),
            "hidden" to bool(w.isHidden),
            "security" to str(if (w.isOpen) "OPEN" else "SECURED"),
            "band" to str(w.band),
            "frequencyMhz" to num(w.frequency),
            "rssiDbm" to num(w.rssi),
            "primaryChannel" to num(w.primaryChannel),
            "channelWidth" to str(WifiStandards.widthText(w.channelWidth)),
            "centerFreq0Mhz" to num(w.centerFreq0),
            "centerFreq1Mhz" to num(w.centerFreq1),
            "wifiStandard" to str(w.wifiStandard),
            "capabilities" to str(w.capabilities),
            "countryCode" to str(w.countryCode),
            "occupiedChannels" to arr(ChannelCalculator.occupiedChannels(w).map { num(it) }),
        )

        w.bssLoad?.let { entries += "bssLoad" to bssLoadJson(it) }
        w.beaconInfo?.let { entries += "beacon" to beaconJson(it) }

        return obj(entries)
    }

    private fun bssLoadJson(b: BssLoad): String = obj(
        "stationCount" to num(b.stationCount),
        "channelUtilization" to num(b.channelUtilization),
        "availableAdmissionCapacity" to num(b.availableAdmissionCapacity),
    )

    private fun beaconJson(b: BeaconInfo): String {
        val entries = mutableListOf<Pair<String, String>>()
        b.country?.let { entries += "country" to countryJson(it) }
        if (b.supportedRates.isNotEmpty()) {
            entries += "supportedRates" to arr(b.supportedRates.map { rateJson(it) })
        }
        b.rsn?.let { entries += "rsn" to rsnJson(it) }
        b.rsnxe?.let { entries += "rsnxe" to rsnxeJson(it) }
        b.ht?.let { entries += "ht" to htJson(it) }
        b.vht?.let { entries += "vht" to vhtJson(it) }
        b.he?.let { entries += "he" to heJson(it) }
        b.eht?.let { entries += "eht" to ehtJson(it) }
        b.wmm?.let { entries += "wmm" to wmmJson(it) }
        b.extendedCapabilities?.let { entries += "extendedCapabilities" to extCapsJson(it) }
        b.powerEnvelope?.let { entries += "powerEnvelope" to powerEnvelopeJson(it) }
        b.mobilityDomain?.let { entries += "mobilityDomain" to mobilityJson(it) }
        if (b.rnr.isNotEmpty()) {
            entries += "rnr" to arr(b.rnr.map { rnrJson(it) })
        }
        return obj(entries)
    }

    private fun rateJson(r: WifiRate): String {
        val entries = mutableListOf<Pair<String, String>>(
            "mbps" to num(r.mbps),
            "basic" to bool(r.basic),
        )
        r.membershipSelector?.let { entries += "membershipSelector" to str(it) }
        return obj(entries)
    }

    private fun countryJson(c: CountryInfo): String = obj(
        "countryCode" to str(c.countryCode),
        "environment" to str(c.environment?.name),
        "triplets" to arr(c.triplets.map { t ->
            obj(
                "firstChannel" to num(t.firstChannel),
                "endChannel" to num(t.endChannel),
                "channelCount" to num(t.channelCount),
                "maxPowerDbm" to num(t.maxPowerDbm),
            )
        }),
    )

    private fun rsnJson(r: RsnInfo): String = obj(
        "version" to num(r.version),
        "groupCipher" to str(r.groupCipher),
        "pairwiseCiphers" to arr(r.pairwiseCiphers.map { str(it) }),
        "akmSuites" to arr(r.akmSuites.map { str(it) }),
        "preAuthentication" to bool(r.preAuthentication),
        "noPairwise" to bool(r.noPairwise),
        "ptksaReplayCounter" to num(r.ptksaReplayCounter),
        "gtkReplayCounter" to num(r.gtkReplayCounter),
        "mfpRequired" to bool(r.mfpRequired),
        "mfpCapable" to bool(r.mfpCapable),
        "peerKeyEnabled" to bool(r.peerKeyEnabled),
        "sppAmsduCapable" to bool(r.sppAmsduCapable),
        "sppAmsduRequired" to bool(r.sppAmsduRequired),
        "extendedKeyId" to bool(r.extendedKeyId),
        "ocv" to bool(r.ocv),
        "groupMgmtCipher" to str(r.groupMgmtCipher),
    )

    private fun rsnxeJson(r: RsnxeInfo): String = obj(
        "version" to num(r.version),
        "capabilities" to arr(r.capabilities.map { str(it) }),
    )

    private fun htJson(h: HtInfo): String {
        val entries = mutableListOf<Pair<String, String>>(
            "ldpc" to bool(h.ldpc),
            "fortyMhzSupported" to bool(h.fortyMhzSupported),
            "greenfield" to bool(h.greenfield),
            "sgi20" to bool(h.sgi20),
            "sgi40" to bool(h.sgi40),
            "txStbc" to bool(h.txStbc),
            "stbcRxStreams" to num(h.stbcRxStreams),
            "htDelayedBlockAck" to bool(h.htDelayedBlockAck),
            "dsssCck40MHz" to bool(h.dsssCck40MHz),
            "fortyMhzIntolerant" to bool(h.fortyMhzIntolerant),
            "lsigTxopProtection" to bool(h.lsigTxopProtection),
            "rifsMode" to bool(h.rifsMode),
        )
        h.smPowerSave?.let { entries += "smPowerSave" to num(it) }
        h.maxAmsduLength?.let { entries += "maxAmsduLength" to num(it) }
        h.maxAmpduLength?.let { entries += "maxAmpduLength" to num(it) }
        h.ampduSpacing?.let { entries += "ampduSpacing" to num(it) }
        h.rxNss?.let { entries += "rxNss" to num(it) }
        h.primaryChannel?.let { entries += "primaryChannel" to num(it) }
        h.secondaryChannelOffset?.let { entries += "secondaryChannelOffset" to num(it) }
        h.channelWidth?.let { entries += "channelWidth" to num(it) }
        return obj(entries)
    }

    private fun vhtJson(v: VhtInfo): String {
        val entries = mutableListOf<Pair<String, String>>(
            "rxLdpc" to bool(v.rxLdpc),
            "sgi80" to bool(v.sgi80),
            "sgi160" to bool(v.sgi160),
            "txStbc" to bool(v.txStbc),
            "rxStbc" to num(v.rxStbc),
            "suBeamformer" to bool(v.suBeamformer),
            "suBeamformee" to bool(v.suBeamformee),
            "muBeamformer" to bool(v.muBeamformer),
            "muBeamformee" to bool(v.muBeamformee),
            "vhtTxopPs" to bool(v.vhtTxopPs),
            "htcVht" to bool(v.htcVht),
            "rxAntennaPatternConsistency" to bool(v.rxAntennaPatternConsistency),
            "txAntennaPatternConsistency" to bool(v.txAntennaPatternConsistency),
        )
        v.maxMpduLength?.let { entries += "maxMpduLength" to num(it) }
        v.supportedChannelWidth?.let { entries += "supportedChannelWidth" to num(it) }
        v.rxNss?.let { entries += "rxNss" to num(it) }
        v.txNss?.let { entries += "txNss" to num(it) }
        return obj(entries)
    }

    private fun heJson(h: HeInfo): String {
        val entries = mutableListOf<Pair<String, String>>(
            "htcHeSupported" to bool(h.htcHeSupported),
            "twtRequester" to bool(h.twtRequester),
            "twtResponder" to bool(h.twtResponder),
            "omControl" to bool(h.omControl),
            "he40_2_4GHz" to bool(h.he40_2_4GHz),
            "he40_80_5GHz" to bool(h.he40_80_5GHz),
            "he160_5GHz" to bool(h.he160_5GHz),
            "he160_8080_5GHz" to bool(h.he160_8080_5GHz),
            "suBeamformer" to bool(h.suBeamformer),
            "suBeamformee" to bool(h.suBeamformee),
            "muBeamformer" to bool(h.muBeamformer),
        )
        h.rxNss?.let { entries += "rxNss" to num(it) }
        h.txNss?.let { entries += "txNss" to num(it) }
        return obj(entries)
    }

    private fun ehtJson(e: EhtInfo): String {
        val entries = mutableListOf<Pair<String, String>>(
            "epcsPriorityAccess" to bool(e.epcsPriorityAccess),
            "ehtOmControl" to bool(e.ehtOmControl),
            "triggeredTxopSharing" to bool(e.triggeredTxopSharing),
            "support320Mhz" to bool(e.support320Mhz),
            "support4096Qam" to bool(e.support4096Qam),
        )
        e.channelWidth?.let { entries += "channelWidth" to num(it) }
        e.puncturingBitmap?.let { entries += "puncturingBitmap" to num(it) }
        return obj(entries)
    }

    private fun wmmJson(w: WmmInfo): String = obj(
        "version" to num(w.version),
        "uApsd" to bool(w.uApsd),
        "parameterized" to bool(w.parameterized),
    )

    private fun extCapsJson(e: ExtendedCapabilitiesInfo): String = obj(
        "capabilities" to arr(e.capabilities.map { str(it) }),
    )

    private fun powerEnvelopeJson(p: PowerEnvelopeInfo): String = obj(
        "maxTransmitPowersDbm" to arr(p.maxTransmitPowersDbm.map { num(it) }),
    )

    private fun mobilityJson(m: MobilityDomainInfo): String = obj(
        "mdid" to num(m.mdid),
        "fastBssTransitionOverDs" to bool(m.fastBssTransitionOverDs),
        "resourceRequestProtocolCapability" to bool(m.resourceRequestProtocolCapability),
    )

    private fun rnrJson(r: RnrNeighborAp): String = obj(
        "bssid" to str(r.bssid),
        "channelNumber" to num(r.channelNumber),
        "operatingClass" to num(r.operatingClass),
        "tbttOffsetTu" to num(r.tbttOffsetTu),
        "shortSsid" to str(r.shortSsid),
        "sameSsid" to bool(r.sameSsid),
        "multipleBssid" to bool(r.multipleBssid),
        "transmittedBssid" to bool(r.transmittedBssid),
    )

    // ============ JSON 工具 ============

    private fun str(v: String?): String = v?.let { "\"${it.jsonEscape()}\"" } ?: "null"
    private fun num(v: Int?): String = v?.toString() ?: "null"
    private fun num(v: Double?): String =
        v?.let { if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString() } ?: "null"
    private fun bool(v: Boolean): String = v.toString()

    private fun arr(elements: List<String>): String =
        elements.joinToString(",", "[", "]")

    private fun obj(entries: List<Pair<String, String>>): String =
        entries.joinToString(",", "{", "}") { (k, v) -> "\"${k.jsonEscape()}\": $v" }

    private fun obj(vararg entries: Pair<String, String>): String = obj(entries.toList())

    private fun String.jsonEscape(): String {
        val sb = StringBuilder(length + 8)
        for (c in this) {
            when (c) {
                '"' -> sb.append("\\\"")
                '\\' -> sb.append("\\\\")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                '\b' -> sb.append("\\b")
                '\u000C' -> sb.append("\\f")
                else -> if (c < ' ') {
                    sb.append("\\u%04x".format(c.code))
                } else {
                    sb.append(c)
                }
            }
        }
        return sb.toString()
    }
}
