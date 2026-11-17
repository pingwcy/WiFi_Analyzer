package top.pingwcy.wifianalyzer.model

/** 某个 AP 在某一时刻的信号强度采样，用于绘制信号历史趋势。 */
data class RssiSample(
    val timestampMs: Long,
    val rssi: Int,
)
