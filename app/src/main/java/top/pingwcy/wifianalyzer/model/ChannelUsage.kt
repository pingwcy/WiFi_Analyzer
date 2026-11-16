package top.pingwcy.wifianalyzer.model

import top.pingwcy.wifianalyzer.domain.ChannelScore

data class ChannelUsage(
    val channel: Int,
    val count: Int,
    val networks: List<String>,
    val score: ChannelScore
)