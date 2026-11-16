package top.pingwcy.wifianalyzer.model

data class WifiData(
        val ssid: String,
        val bssid: String,
        val frequency: Int,          // 主信道频率 MHz
        val rssi: Int,               // 信号强度 dBm
        val channelWidth: Int,       // 信道宽度枚举值（WifiManager.CHANNEL_WIDTH_*），非 MHz
        val centerFreq0: Int,        // 中心频率 0（MHz），0 表示无
        val centerFreq1: Int,        // 中心频率 1（MHz），0 表示无
        val primaryChannel: Int,     // 主信道号
        val band: String,            // 2.4GHz / 5GHz / 6GHz
        val wifiStandard: String,    // WiFi 4/5/6/7 等
        val capabilities: String,    // 加密/能力描述
        val isHidden: Boolean,
        val countryCode: String,  // AP 广播的 802.11d 国家码
        val bssLoad: BssLoad? = null
)

data class BssLoad(
        val stationCount: Int,
        val channelUtilization: Int,
        val availableAdmissionCapacity: Int
)

/**
 * 分组 key：
 * - 隐藏网络按 BSSID 单独成组（避免所有空 SSID 混在一起）
 * - 可见网络按 SSID + 加密能力分组（同名但加密类型不同的不合并）
 */
fun groupKey(w: WifiData): String =
        if (w.isHidden || w.ssid.isBlank()) "hidden:${w.bssid}"
        else "${w.ssid}|${w.capabilities}"
