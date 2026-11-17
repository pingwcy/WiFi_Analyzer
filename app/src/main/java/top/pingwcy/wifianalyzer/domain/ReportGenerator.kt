package top.pingwcy.wifianalyzer.domain

import top.pingwcy.wifianalyzer.model.WifiData

/**
 * 扫描报告生成器：把当前扫描结果汇总为可分享/导出的纯文本。
 *
 * 报告使用英文标签（技术导出惯例），内容为结构化数据，便于跨语言阅读与二次处理。
 */
object ReportGenerator {

    fun generate(wifiList: List<WifiData>): String {
        val sb = StringBuilder()
        sb.appendLine("WiFi Analyzer Report")
        sb.appendLine("====================")
        sb.appendLine("Total: ${wifiList.size} AP(s)")
        sb.appendLine()

        listOf("2.4 GHz", "5 GHz", "6 GHz").forEach { band ->
            val bandAps = wifiList.filter { it.band == band }
            if (bandAps.isNotEmpty()) {
                val rec = ChannelRecommender.recommend(wifiList, band)
                sb.appendLine("[$band]")
                sb.appendLine("Best: CH ${rec.recommended}")
                sb.appendLine("APs: ${bandAps.size}")
                sb.appendLine()
            }
        }

        sb.appendLine("AP List (by signal):")
        wifiList.sortedByDescending { it.rssi }.forEach { w ->
            val ssid = w.ssid.ifBlank { "(hidden)" }
            val security = if (w.isOpen) "OPEN" else "SECURED"
            sb.appendLine("  $ssid | CH ${w.primaryChannel} | ${w.band} | ${w.rssi} dBm | $security")
        }

        return sb.toString().trimEnd()
    }
}
