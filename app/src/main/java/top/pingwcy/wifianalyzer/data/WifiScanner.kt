package top.pingwcy.wifianalyzer.data

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import top.pingwcy.wifianalyzer.domain.WifiRepository
import top.pingwcy.wifianalyzer.model.WifiData
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

class WifiScanner(private val context: Context) : WifiRepository {

    private val wifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    @SuppressLint("MissingPermission")
    override suspend fun scan(timeoutMs: Long): List<WifiData> {
        // 发起扫描并等待「结果可用」广播；startScan 失败或超时都回退到系统缓存的 scanResults。
        withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine { cont ->
                val unregistered = AtomicBoolean(false)

                val receiver = object : BroadcastReceiver() {
                    override fun onReceive(ctx: Context, intent: Intent) {
                        if (intent.action == WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) {
                            if (unregistered.compareAndSet(false, true)) {
                                runCatching { ctx.unregisterReceiver(this) }
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
        }

        return wifiManager.scanResults
            .map { it.toWifiData() }
            .sortedByDescending { it.rssi }
    }
}
