package top.pingwcy.wifianalyzer.data

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.os.SystemClock
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import top.pingwcy.wifianalyzer.model.WifiData
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

class WifiScanner(private val context: Context) {

    private val wifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    @SuppressLint("MissingPermission")
    suspend fun scan(timeoutMs: Long = 5000): List<WifiData> {
        // 扫描前快照：BSSID -> timestamp，用于差集判断"本次新结果"
        val beforeSnapshot: Map<String, Long> =
            wifiManager.scanResults.associate { it.BSSID to it.timestamp }

        // ScanResult.timestamp 基准是开机后的 elapsedRealtime（微秒）
        val startRealtimeUs = SystemClock.elapsedRealtime() * 1000

        val gotBroadcast: Boolean = withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine { cont ->
                val unregistered = AtomicBoolean(false)

                val receiver = object : BroadcastReceiver() {
                    override fun onReceive(ctx: Context, intent: Intent) {
                        if (intent.action == WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) {
                            if (unregistered.compareAndSet(false, true)) {
                                runCatching { context.unregisterReceiver(this) } // this = 本接收器
                            }
                            if (cont.isActive) cont.resume(true)
                        }
                    }
                }

                cont.invokeOnCancellation {
                    if (unregistered.compareAndSet(false, true)) {
                        runCatching { context.unregisterReceiver(receiver) }
                    }
                }

                context.registerReceiver(
                    receiver,
                    IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
                )

                if (!wifiManager.startScan()) {
                    if (unregistered.compareAndSet(false, true)) {
                        runCatching { context.unregisterReceiver(receiver) }
                    }
                    if (cont.isActive) cont.resume(false)
                }
            }
        } ?: false // 超时视为未拿到本次结果

        val all = wifiManager.scanResults

        // 只有确实收到广播，才认为 scanResults 可能包含本次新结果
        val fresh = if (gotBroadcast) {
            all.filter { r ->
                val prevTs = beforeSnapshot[r.BSSID]
                // 扫描前没有该 BSSID，或 timestamp 比扫描前更新
                prevTs == null || r.timestamp > prevTs || r.timestamp >= startRealtimeUs
            }
        } else {
            emptyList()
        }

        // 与原逻辑一致：本次无新结果则回退到全部缓存
        return (if (fresh.isNotEmpty()) fresh else all)
            .map { it.toWifiData() }
            .sortedByDescending { it.rssi }
    }
}