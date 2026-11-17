package top.pingwcy.wifianalyzer.data

import android.annotation.SuppressLint
import android.net.wifi.ScanResult
import android.os.Build
import top.pingwcy.wifianalyzer.domain.ChannelCalculator
import top.pingwcy.wifianalyzer.domain.WifiStandards
import top.pingwcy.wifianalyzer.model.ChannelWidth
import top.pingwcy.wifianalyzer.model.WifiData
import java.nio.ByteOrder
import top.pingwcy.wifianalyzer.model.BssLoad


private fun ScanResult.getBssLoad(): BssLoad? {
    // informationElements 从 Android 11（API 30）开始提供
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
        return null
    }

    return try {
        // BSS Load Element ID = 11
        val element = this.informationElements
            ?.firstOrNull { it.id == 11 }
            ?: return null

        val buffer = element.bytes
            .duplicate()
            .order(ByteOrder.LITTLE_ENDIAN)

        // BSS Load 的有效数据长度为 5 Bytes
        if (buffer.remaining() < 5) {
            return null
        }

        // Station Count: 2 Bytes, unsigned
        val stationCount =
            buffer.short.toInt() and 0xFFFF

        // Channel Utilization: 1 Byte, unsigned
        val channelUtilization =
            buffer.get().toInt() and 0xFF

        // Available Admission Capacity: 2 Bytes, unsigned
        val availableAdmissionCapacity =
            buffer.short.toInt() and 0xFFFF

        BssLoad(
            stationCount = stationCount,
            channelUtilization = channelUtilization,
            availableAdmissionCapacity = availableAdmissionCapacity
        )

    } catch (e: Exception) {
        null
    }
}

@SuppressLint("MissingPermission")
fun ScanResult.toWifiData(): WifiData {
    val freq = this.frequency
    val chWidth = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) this.channelWidth else ScanResult.CHANNEL_WIDTH_20MHZ
    val cf0 = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) this.centerFreq0 else 0
    val cf1 = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) this.centerFreq1 else 0
    val beacon = if (
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
    ) {
        BeaconIeParser.parse(this.informationElements)
    } else {
        null
    }
    return WifiData(
        ssid = this.SSID,
        bssid = this.BSSID ?: "",
        frequency = freq,
        rssi = this.level,
        channelWidth = chWidth.toChannelWidth(),
        centerFreq0 = cf0,
        centerFreq1 = cf1,
        primaryChannel = ChannelCalculator.freqToChannel(freq),
        band = ChannelCalculator.freqToBand(freq),
        wifiStandard = WifiStandards.toString(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) this.wifiStandard else -1
        ),
        capabilities = this.capabilities ?: "",
        isHidden = this.SSID.isEmpty(),
        countryCode = this.getCountryCode(),
        bssLoad = this.getBssLoad(),
        beaconInfo = beacon
    )
}

private fun Int.toChannelWidth(): ChannelWidth = when (this) {
    ScanResult.CHANNEL_WIDTH_40MHZ -> ChannelWidth.WIDTH_40MHZ
    ScanResult.CHANNEL_WIDTH_80MHZ -> ChannelWidth.WIDTH_80MHZ
    ScanResult.CHANNEL_WIDTH_160MHZ -> ChannelWidth.WIDTH_160MHZ
    ScanResult.CHANNEL_WIDTH_80MHZ_PLUS_MHZ -> ChannelWidth.WIDTH_80MHZ_PLUS
    else -> ChannelWidth.WIDTH_20MHZ
}

private fun ScanResult.getCountryCode(): String {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return "" // 返回空串代表不支持
    return try {
        val countryIe = this.informationElements?.firstOrNull { it.id == 7 } ?: return "" // 未广播返回空
        val buffer = countryIe.bytes.duplicate()
        if (buffer.remaining() < 3) return "" // 不完整
        val country = ByteArray(2)
        buffer.get(country)
        String(country, Charsets.US_ASCII).takeIf { it.matches(Regex("[A-Z]{2}")) } ?: "" // 无效返回空
    } catch (e: Exception) {
        "" // 读取失败返回空
    }
}
