package top.pingwcy.wifianalyzer.model

data class ChannelUsage(
    val channel: Int,
    val count: Int,          // 该信道被占用次数
    val networks: List<String>  // 占用该信道的 SSID，用于详情
)