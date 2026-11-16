
package top.pingwcy.wifianalyzer.model

data class BeaconInfo(
    // 所有收到的 IE
    val rawIes: List<RawInformationElement> = emptyList(),

    // 国家信息
    val country: CountryInfo? = null,

    // 已有的 BSS Load
    val bssLoad: BssLoad? = null,

    // 速率
    val supportedRates: List<WifiRate> = emptyList(),

    // 安全
    val rsn: RsnInfo? = null,
    val rsnxe: ByteArray? = null,

    // 802.11n/ac/ax/be
    val ht: HtInfo? = null,
    val vht: VhtInfo? = null,
    val he: HeInfo? = null,
    val eht: EhtInfo? = null,

    // QoS
    val wmm: WmmInfo? = null,

    // 扩展信息
    val extendedCapabilities: ByteArray? = null,
    val powerEnvelope: ByteArray? = null,
    val mobilityDomain: ByteArray? = null,
    val vendorIes: List<VendorIe> = emptyList(),

    // 多 BSSID、邻居信息
    val rnr: List<ByteArray> = emptyList(),
    val multipleBssid: List<ByteArray> = emptyList(),

    // 其他尚未解析的信息
    val parseWarnings: List<String> = emptyList()
)

data class RawInformationElement(
    val id: Int,
    val idExt: Int?,
    val data: ByteArray
)

data class WifiRate(
    val mbps: Double,
    val basic: Boolean
)

data class CountryInfo(
    val countryCode: String,
    val environment: Char?,
    val triplets: List<CountryTriplet>
)

data class CountryTriplet(
    val firstChannel: Int,
    val channelCount: Int,
    val maxPowerDbm: Int? // 加上问号
)

data class RsnInfo(
    val version: Int,
    val groupCipher: String,
    val pairwiseCiphers: List<String>,
    val akmSuites: List<Int>,
    val preAuthentication: Boolean,
    val noPairwise: Boolean,
    val ptksaReplayCounter: Int,
    val mfpRequired: Boolean,
    val mfpCapable: Boolean,
    val gtkNotUsed: Boolean,
    val sppAmsdu: Boolean,
    val peerKeyEnabled: Boolean,
    val ocv: Boolean,
    val pmkids: List<ByteArray>,
    val groupMgmtCipher: String?
)

data class HtInfo(
    val capabilities: Int,
    val maxAmsduLength: Int?,
    val ampduParameters: Int?,
    val rxMcsSet: ByteArray?,
    val operation: ByteArray?,
    // 以下为新增字段
    val htExtendedCapabilities: Int? = null,
    val txBeamformingCapabilities: Long? = null, // 注意这里是 Long
    val aselCapability: Int? = null,
    val sgi20: Boolean = false,
    val sgi40: Boolean = false,
    val greenfield: Boolean = false,
    val ldpc: Boolean = false,
    val fortyMhzSupported: Boolean = false,
    val maxAmpduLength: Int? = null,
    val stbcRxStreams: Int = 0,
    // HT Operation 相关
    val primaryChannel: Int? = null,
    val secondaryChannelOffset: Int? = null,
    val channelWidth: Int? = null,
    val rifsMode: Boolean = false
)

data class VhtInfo(
    val capabilities: Long, // 注意这里改成 Long，解决 Incompatible types 报错
    val rxMcsMap: Int?,
    val txMcsMap: Int?,
    val operation: ByteArray?,
    // 以下为新增字段
    val maxMpduLength: Int? = null,
    val supportedChannelWidth: Int? = null,
    val rxLdpc: Boolean = false,
    val sgi80: Boolean = false,
    val sgi160: Boolean = false,
    val txStbc: Boolean = false,
    val rxStbc: Int = 0,
    val suBeamformer: Boolean = false,
    val muBeamformer: Boolean = false,
    // VHT Operation 相关
    val channelWidth: Int? = null,
    val centerFreqSeg0: Int? = null,
    val centerFreqSeg1: Int? = null,
    val basicMcs: Int? = null
)

data class HeInfo(
    val macCapabilities: ByteArray?,
    val phyCapabilities: ByteArray?,
    val rxMcsNss: ByteArray?,
    val operation: ByteArray?,
    // 新增
    val heSupported: Boolean = true,
    val twtRequester: Boolean = false,
    val twtResponder: Boolean = false,
    val muEdca: Boolean = false,
    val omControl: Boolean = false,
    val suBeamformer: Boolean = false,
    val suBeamformee: Boolean = false,
    val muBeamformer: Boolean = false,
    val channelWidth160: Boolean = false
)

data class EhtInfo(
    val macCapabilities: ByteArray?,
    val phyCapabilities: ByteArray?,
    val operation: ByteArray?,
    // 新增
    val epcsPriorityAccess: Boolean = false,
    val ehtOmControl: Boolean = false,
    val triggeredTxopSharing: Boolean = false,
    val support320Mhz: Boolean = false,
    val support4096Qam: Boolean = false,
    val channelWidth: Int? = null,
    val puncturingBitmap: Int? = null
)

data class WmmInfo(
    val version: Int,
    val qosInfo: Int,
    // 补充下面这些字段
    val uApsd: Boolean = false,
    val parameterized: Boolean = false,
    val acm: List<String>? = null // 如果之前不是 List<String>，请按原来的类型调整，这里给个安全的默认值
)

data class WmmAcParameter(
    val aci: Int,
    val acm: Boolean,
    val aifsn: Int,
    val ecwMin: Int,
    val ecwMax: Int,
    val txopLimit: Int
)

data class VendorIe(
    val oui: String,
    val vendorType: Int?,
    val data: ByteArray
)
