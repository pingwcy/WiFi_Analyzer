package top.pingwcy.wifianalyzer.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

/**
 * 设备 Wi-Fi 频段定义。
 *
 * [allowedChannelsBand] 是 WifiManager.getAllowedChannels / getUsableChannels 使用的 band 常量：
 * 1 = 2.4GHz，6 = 5GHz（含 DFS），8 = 6GHz。
 */
enum class WifiBand(val label: String, val allowedChannelsBand: Int) {
    BAND_2G("2.4 GHz", 1),
    BAND_5G("5 GHz", 6),
    BAND_6G("6 GHz", 8),
}

/** 是否已授予 NEARBY_WIFI_DEVICES 权限（Android 13+ 才存在该权限）。 */
internal fun hasNearbyWifiDevicesPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        (context.checkSelfPermission(Manifest.permission.NEARBY_WIFI_DEVICES) ==
            PackageManager.PERMISSION_GRANTED)
