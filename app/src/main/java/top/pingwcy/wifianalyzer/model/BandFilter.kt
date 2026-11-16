package top.pingwcy.wifianalyzer.model

enum class BandFilter(val label: String, val match: (WifiData) -> Boolean) {
    ALL("全部", { true }),
    BAND_24("2.4G", { it.band == "2.4 GHz" }),
    BAND_5("5G",   { it.band == "5 GHz" }),
    BAND_6("6G",   { it.band == "6 GHz" })
}
