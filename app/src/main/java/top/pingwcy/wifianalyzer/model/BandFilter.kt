package top.pingwcy.wifianalyzer.model
import androidx.compose.ui.res.stringResource
import top.pingwcy.wifianalyzer.R
import androidx.annotation.StringRes
enum class BandFilter(@StringRes val labelRes: Int, val match: (WifiData) -> Boolean) {
    ALL(R.string.band_all, { true }),
    BAND_2G(R.string.band_2g, { it.frequency in 2400..2500 }),
    BAND_5G(R.string.band_5g, { it.frequency in 4900..5900 }),
    BAND_6G(R.string.band_6g, { it.frequency in 5925..7125 })
}
