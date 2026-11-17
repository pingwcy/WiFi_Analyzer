package top.pingwcy.wifianalyzer

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import top.pingwcy.wifianalyzer.ui.LocaleHelper
import top.pingwcy.wifianalyzer.ui.WifiScannerApp
import top.pingwcy.wifianalyzer.ui.theme.WiFiAnalyzerTheme

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        // 在资源加载前应用保存的语言，保证 UI 首帧即用正确语言
        super.attachBaseContext(LocaleHelper.wrap(newBase, LocaleHelper.readLanguage(newBase)))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WiFiAnalyzerTheme {
                WifiScannerApp()
            }
        }
    }
}
