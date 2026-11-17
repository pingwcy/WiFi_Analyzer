package top.pingwcy.wifianalyzer.ui.theme

import androidx.compose.ui.graphics.Color

/** 根据 RSSI（dBm）返回信号强度的语义颜色：强=绿、中=橙、弱=红。 */
fun signalColor(rssi: Int): Color = when {
    rssi >= -50 -> Color(0xFF4CAF50)
    rssi >= -70 -> Color(0xFFFF9800)
    else -> Color(0xFFF44336)
}

/** 根据信道综合评分（0~100）返回语义颜色：分越高越偏绿。 */
fun scoreColor(score: Int): Color = when {
    score >= 80 -> Color(0xFF4CAF50)
    score >= 60 -> Color(0xFF8BC34A)
    score >= 40 -> Color(0xFFFF9800)
    else -> Color(0xFFF44336)
}
