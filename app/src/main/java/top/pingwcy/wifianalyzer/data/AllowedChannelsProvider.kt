package top.pingwcy.wifianalyzer.data

import android.content.Context
import android.net.wifi.WifiAvailableChannel
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import top.pingwcy.wifianalyzer.domain.ChannelCalculator

/** 设备监管域允许使用的单个信道。 */
data class AllowedChannel(
    val frequencyMhz: Int,
    val channel: Int,
)

/**
 * 读取设备监管域允许使用的信道（Android 14 / API 34+）。
 *
 * 返回「频段 -> 允许信道列表」。只在成功读取到非空结果时才会包含对应频段；
 * API 34 以下、权限缺失或读取失败时返回空 Map，调用方应回退到全量信道表。
 */
object AllowedChannelsProvider {

    fun allowedChannelsByBand(context: Context): Map<String, List<AllowedChannel>> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return emptyMap()
        if (!hasNearbyWifiDevicesPermission(context)) return emptyMap()

        val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val result = mutableMapOf<String, List<AllowedChannel>>()

        for (band in WifiBand.entries) {
            try {
                val channels = wm.getAllowedChannels(band.allowedChannelsBand, WifiAvailableChannel.OP_MODE_STA)
                    .mapNotNull { wac ->
                        val ch = ChannelCalculator.freqToChannel(wac.frequencyMhz)
                        if (ch > 0) AllowedChannel(wac.frequencyMhz, ch) else null
                    }
                    .distinctBy { it.channel }
                    .sortedBy { it.frequencyMhz }

                if (channels.isNotEmpty()) result[band.label] = channels
            } catch (e: UnsupportedOperationException) {
                // 框架不支持 / 监管域禁用该频段（例如中国大陆 6GHz），跳过。
                Log.w(TAG, "Unsupported band=${band.label}", e)
            } catch (e: SecurityException) {
                Log.e(TAG, "Missing NEARBY_WIFI_DEVICES permission", e)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to query allowed channels for band=${band.label}", e)
            }
        }

        return result
    }

    private const val TAG = "AllowedChannels"
}
