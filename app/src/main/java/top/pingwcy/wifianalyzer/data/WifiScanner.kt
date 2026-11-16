package top.pingwcy.wifianalyzer.data

import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.WifiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import top.pingwcy.wifianalyzer.model.WifiData

class WifiScanner(private val context: Context) {

    @SuppressLint("MissingPermission")
    suspend fun scan(): List<WifiData> = withContext(Dispatchers.IO) {
        val wifiManager =
            context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        wifiManager.startScan()
        delay(1200)
        wifiManager.scanResults
            .map { it.toWifiData() }
            .sortedByDescending { it.rssi }
    }
}