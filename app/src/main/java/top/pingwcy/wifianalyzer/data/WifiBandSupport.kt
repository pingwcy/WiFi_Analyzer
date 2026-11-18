package top.pingwcy.wifianalyzer.data

import android.content.Context
import android.net.wifi.WifiAvailableChannel
import android.net.wifi.WifiManager
import android.os.Build

/** 某个频段是否可用的判断结果。 */
enum class BandAvailability {
    /** 可用：硬件支持且监管域允许（或监管域无法判断时，仅按硬件支持判断）。 */
    SUPPORTED,

    /** 设备硬件不支持该频段。 */
    HARDWARE_UNSUPPORTED,

    /** 设备硬件支持，但当前监管域 / 系统不允许使用该频段。 */
    REGULATORY_UNSUPPORTED,
}

/**
 * 判断某个 Wi-Fi 频段是否真正可用。
 *
 * 与 [AllowedChannelsProvider]（监管域允许的信道明细）不同，这里回答「该频段能不能用」，
 * 并把「硬件不支持」和「监管域不允许」区分开，用于在 UI 上给用户明确提示，而不是直接隐藏入口。
 */
object WifiBandSupport {

    fun availability(context: Context, band: String): BandAvailability {
        val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        val hardwareSupported = when (band) {
            "2.4 GHz" -> true
            "5 GHz" -> wm.is5GHzBandSupported
            "6 GHz" -> wm.is6GHzBandSupported
            else -> false
        }
        if (!hardwareSupported) return BandAvailability.HARDWARE_UNSUPPORTED

        // 硬件支持后，再判断监管域是否允许（仅 API 34+ 提供公开 API）。
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val wifiBand = WifiBand.entries.firstOrNull { it.label == band }
                ?: return BandAvailability.SUPPORTED

            // 权限缺失时无法判断监管域，退化为「按硬件支持判断」，避免误报不支持。
            if (!hasNearbyWifiDevicesPermission(context)) return BandAvailability.SUPPORTED

            return try {
                val allowed = wm.getAllowedChannels(wifiBand.allowedChannelsBand, WifiAvailableChannel.OP_MODE_STA)
                if (allowed.isEmpty()) BandAvailability.REGULATORY_UNSUPPORTED
                else BandAvailability.SUPPORTED
            } catch (e: UnsupportedOperationException) {
                // 硬件上报支持，但框架拒绝提供该频段信道（常见于监管域禁用，例如中国大陆 6GHz）。
                BandAvailability.REGULATORY_UNSUPPORTED
            } catch (e: SecurityException) {
                BandAvailability.SUPPORTED
            } catch (e: Exception) {
                BandAvailability.SUPPORTED
            }
        }

        return BandAvailability.SUPPORTED
    }
}
