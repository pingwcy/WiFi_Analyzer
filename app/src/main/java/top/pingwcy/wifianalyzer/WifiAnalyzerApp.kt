package top.pingwcy.wifianalyzer

import android.app.Application
import top.pingwcy.wifianalyzer.data.CrashLogger

class WifiAnalyzerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // 尽早安装崩溃捕获，覆盖 Application 之后的整个生命周期
        CrashLogger.init(this)
    }
}
