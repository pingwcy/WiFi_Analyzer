package top.pingwcy.wifianalyzer.data

import android.annotation.SuppressLint
import android.net.wifi.ScanResult
import android.os.Build
import top.pingwcy.wifianalyzer.domain.ChannelCalculator
import top.pingwcy.wifianalyzer.domain.WifiStandards
import top.pingwcy.wifianalyzer.model.WifiData

@SuppressLint("MissingPermission")
fun ScanResult.toWifiData(): WifiData {
    val freq = this.frequency
    val chWidth = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) this.channelWidth else 0
    val cf0 = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) this.centerFreq0 else 0
    val cf1 = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) this.centerFreq1 else 0

    return WifiData(
        ssid = this.SSID.ifEmpty { "隐藏网络" },
        bssid = this.BSSID ?: "未知",
        frequency = freq,
        rssi = this.level,
        channelWidth = chWidth,
        centerFreq0 = cf0,
        centerFreq1 = cf1,
        primaryChannel = ChannelCalculator.freqToChannel(freq),
        band = ChannelCalculator.freqToBand(freq),
        wifiStandard = WifiStandards.toString(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) this.wifiStandard else -1
        ),
        capabilities = this.capabilities ?: "",
        isHidden = this.SSID.isEmpty(),
        countryCode = this.getCountryCode()
    )
}

private fun ScanResult.getCountryCode(): String {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return "不支持"
    return try {
        val countryIe = this.informationElements?.firstOrNull { it.id == 7 } ?: return "未广播"
        val buffer = countryIe.bytes.duplicate()
        if (buffer.remaining() < 3) return "数据不完整"
        val country = ByteArray(2)
        buffer.get(country)
        String(country, Charsets.US_ASCII).takeIf { it.matches(Regex("[A-Z]{2}")) } ?: "无效数据"
    } catch (e: Exception) {
        "读取失败"
    }
}