package top.pingwcy.wifianalyzer.model

data class BeaconInfo(
    // 所有收到的 IE
    val rawIes: List<RawInformationElement> = emptyList(),

    // 国家信息
    val country: CountryInfo? = null,

    // BSS Load
    val bssLoad: BssLoad? = null,

    // 速率
    val supportedRates: List<WifiRate> = emptyList(),

    // 安全
    val rsn: RsnInfo? = null,
    val rsnxe: RsnxeInfo? = null,

    // 802.11n/ac/ax/be
    val ht: HtInfo? = null,
    val vht: VhtInfo? = null,
    val he: HeInfo? = null,
    val eht: EhtInfo? = null,

    // QoS
    val wmm: WmmInfo? = null,

    // 扩展信息
    val extendedCapabilities: ExtendedCapabilitiesInfo? = null,
    val powerEnvelope: PowerEnvelopeInfo? = null,
    val mobilityDomain: MobilityDomainInfo? = null,
    val vendorIes: List<VendorIe> = emptyList(),

    // 多 BSSID、邻居信息
    val rnr: List<RnrNeighborAp> = emptyList(),
    val multipleBssid: List<MultipleBssidElement> = emptyList(),

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
    val maxPowerDbm: Int?
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

/** RSN Extension（元素 244）的解析结果。 */
data class RsnxeInfo(
    val version: Int,
    val capabilities: List<String>,  // 已置位的能力位（人类可读）
)

/** Extended Capabilities（元素 127）的解析结果。 */
data class ExtendedCapabilitiesInfo(
    val capabilities: List<String>,  // 已置位的能力位（人类可读）
    val raw: ByteArray,
)

/** Transmit Power Envelope（元素 195）的解析结果。 */
data class PowerEnvelopeInfo(
    val maxTransmitPowerDbm: Double?,        // 本地最大发射功率（dBm）
    val localMaxTransmitPowerDbm: Double?,   // 本地每 20MHz 最大发射功率（dBm）
    val regulatoryClientInfos: List<RegulatoryClientInfo>,
)

data class RegulatoryClientInfo(
    val maxTransmitPowerDbm: Double?,        // 客户端最大发射功率（dBm），0 表示未定义
    val maxTransmitPowerCategory: Int?,      // 功率类别（0 表示未定义）
)

/** Mobility Domain（元素 54，802.11r）的解析结果。 */
data class MobilityDomainInfo(
    val mdid: Int,
    val fastBssTransitionOverDs: Boolean,
    val resourceRequestProtocolCapability: Boolean,
)

/** Reduced Neighbor Report（元素 201）中的一个邻居 AP。 */
data class RnrNeighborAp(
    val tbttOffsetTu: Int,          // TBTT offset（TU）
    val operatingClass: Int,        // 操作类别
    val channelNumber: Int,         // 信道号
    val bssid: String?,             // 邻居 AP 的 BSSID
    val shortSsid: String?,         // Short-SSID（4 字节，ASCII 或 hex）
    val sameSsid: Boolean,          // 是否与本 AP 同 SSID
    val multipleBssid: Boolean,     // 是否属于 Multiple BSSID
    val transmittedBssid: Boolean,  // 是否为 Transmitted BSSID
)

/** Multiple BSSID（元素 71）的解析结果。 */
data class MultipleBssidElement(
    val maxBssidIndicator: Int,
    val profiles: List<MultipleBssidProfile>,
)

data class MultipleBssidProfile(
    val subelementId: Int,      // 子元素 ID（0 = Non-transmitted BSSID Profile）
    val data: ByteArray,        // 子元素原始内容
)

data class HtInfo(
    val capabilities: Int,
    val maxAmsduLength: Int?,
    val ampduParameters: Int?,
    val rxMcsSet: ByteArray?,
    val operation: ByteArray?,
    val htExtendedCapabilities: Int? = null,
    val txBeamformingCapabilities: Long? = null,
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
    val capabilities: Long,
    val rxMcsMap: Int?,
    val txMcsMap: Int?,
    val operation: ByteArray?,
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
    val uApsd: Boolean = false,
    val parameterized: Boolean = false,
    val acm: List<String>? = null
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
