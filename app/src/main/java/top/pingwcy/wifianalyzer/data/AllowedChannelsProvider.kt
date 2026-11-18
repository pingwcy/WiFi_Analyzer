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

    // WifiManager.getAllowedChannels 的 band 参数取值：
    // 1 = 2.4GHz，6 = 5GHz（含 DFS），8 = 6GHz
    private val BANDS = listOf(
        1 to "2.4 GHz",
        6 to "5 GHz",
        8 to "6 GHz",
    )

    fun allowedChannelsByBand(context: Context): Map<String, List<AllowedChannel>> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return emptyMap()

        val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val result = mutableMapOf<String, List<AllowedChannel>>()

        for ((band, bandName) in BANDS) {
            try {
                val channels = wm.getAllowedChannels(band, WifiAvailableChannel.OP_MODE_STA)
                    .mapNotNull { wac ->
                        val ch = ChannelCalculator.freqToChannel(wac.frequencyMhz)
                        if (ch > 0) AllowedChannel(wac.frequencyMhz, ch) else null
                    }
                    .distinctBy { it.channel }
                    .sortedBy { it.frequencyMhz }

                if (channels.isNotEmpty()) result[bandName] = channels
            } catch (e: SecurityException) {
                Log.e(TAG, "Missing NEARBY_WIFI_DEVICES permission", e)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to query allowed channels for band=$band", e)
            }
        }

        return result
    }

    private const val TAG = "AllowedChannels"
}
