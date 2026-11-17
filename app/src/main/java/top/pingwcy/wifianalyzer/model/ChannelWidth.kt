package top.pingwcy.wifianalyzer.model

/**
 * WiFi 信道宽度枚举（framework 无关）。
 *
 * 用于替代 android.net.wifi.ScanResult.CHANNEL_WIDTH_* 在业务层（domain / model）的使用，
 * 使这些层不依赖 Android SDK，便于纯 JVM 单元测试与跨层复用。
 *
 * @property mhz 该宽度对应的 MHz 数；80+80 的每一段按 80MHz 计。
 */
enum class ChannelWidth(val mhz: Int) {
    WIDTH_20MHZ(20),
    WIDTH_40MHZ(40),
    WIDTH_80MHZ(80),
    WIDTH_160MHZ(160),
    WIDTH_80MHZ_PLUS(80),
}
