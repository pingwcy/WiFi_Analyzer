package top.pingwcy.wifianalyzer.domain

import top.pingwcy.wifianalyzer.model.BssLoad
import top.pingwcy.wifianalyzer.model.WifiData

/** 单个 AP 的评分明细 */
data class ApScore(
    val ssid: String,
    val bssid: String,
    val rssi: Int,
    val rssiPenalty: Int,            // 强度扣分
    val bssLoadPenalty: Int,         // BSS Load 扣分
    val channelUtilizationPct: Double?, // 信道利用率（0~100），无 BSS Load 时 null
    val stationCount: Int?,
)

/** 一个信道的综合评分 */
data class ChannelScore(
    val channel: Int,
    // 三个维度的原始汇总
    val apCount: Int,
    val avgRssi: Double,             // 平均信号（越强越负，干扰越大）
    val avgUtilizationPct: Double?,  // 有 BSS Load 的 AP 平均利用率
    // 三个维度扣分
    val apCountPenalty: Int,
    val rssiPenalty: Int,
    val bssLoadPenalty: Int,
    // 最终
    val totalPenalty: Int,
    val score: Int,                  // 0~100
    val grade: String,               // 优秀/良好/一般/拥挤
    val apScores: List<ApScore>,     // 每个 AP 明细（点进去看）
)

object ChannelScorer {

    /**
     * 评分规则（可调）：
     *  - AP 数量：0 个 0 分扣；每多 1 个扣 8 分，封顶 40 分
     *  - 平均 RSSI：>= -50 扣 25；每弱 5dBm 少扣约 4 分（-50 -> -85 区间线性），封顶 30
     *  - BSS Load 平均利用率：0% 扣 0，每 10% 扣 3 分，封顶 30
     * 三个维度相加 = 总扣分（封顶 100），得分 = 100 - 总扣分
     */
    fun scoreChannel(channel: Int, networks: List<WifiData>): ChannelScore {
        val apCount = networks.size
        // ---------- 空信道：直接满分，不参与任何扣分 ----------
        if (apCount == 0) {
            return ChannelScore(
                channel = channel,
                apCount = 0,
                avgRssi = 0.0,
                avgUtilizationPct = null,
                apCountPenalty = 0,
                rssiPenalty = 0,
                bssLoadPenalty = 0,
                totalPenalty = 0,
                score = 100,
                grade = "优秀",
                apScores = emptyList()
            )
        }

        // ---------- AP 数量扣分 ----------
        val apCountPenalty = when {
            apCount <= 1 -> 0
            else -> ((apCount - 1) * 8).coerceAtMost(40)
        }

        // ---------- RSSI 扣分（逐个 AP 算，汇总取平均） ----------
        val apScores = networks.map { w ->
            val rssiPenalty = rssiPenaltyOf(w.rssi)
            val util = w.bssLoad?.let { it.channelUtilization * 100.0 / 255.0 }
            val bssPenalty = util?.let { bssLoadPenaltyOf(it) } ?: 0
            ApScore(
                ssid = w.ssid,
                bssid = w.bssid,
                rssi = w.rssi,
                rssiPenalty = rssiPenalty,
                bssLoadPenalty = bssPenalty,
                channelUtilizationPct = util,
                stationCount = w.bssLoad?.stationCount
            )
        }

        val avgRssi = networks.map { it.rssi }.average()
        // 用平均 RSSI 换算整体扣分（与逐个略不同，避免一个超强 AP 就爆表）
        val rssiPenalty = rssiPenaltyOf(avgRssi.toInt()).coerceAtMost(30)

        // ---------- BSS Load 扣分 ----------
        val utils = networks.mapNotNull { w ->
            w.bssLoad?.let { it.channelUtilization * 100.0 / 255.0 }
        }
        val avgUtil = if (utils.isEmpty()) null else utils.average()
        val bssLoadPenalty = avgUtil?.let { bssLoadPenaltyOf(it).coerceAtMost(30) } ?: 0

        val totalPenalty = (apCountPenalty + rssiPenalty + bssLoadPenalty).coerceIn(0, 100)
        val score = (100 - totalPenalty).coerceIn(0, 100)

        val grade = when {
            score >= 80 -> "优秀"
            score >= 60 -> "良好"
            score >= 40 -> "一般"
            else -> "拥挤"
        }

        return ChannelScore(
            channel = channel,
            apCount = apCount,
            avgRssi = avgRssi,
            avgUtilizationPct = avgUtil,
            apCountPenalty = apCountPenalty,
            rssiPenalty = rssiPenalty,
            bssLoadPenalty = bssLoadPenalty,
            totalPenalty = totalPenalty,
            score = score,
            grade = grade,
            apScores = apScores
        )
    }

    /** RSSI -> 扣分：-50 及以上最狠；-85 以下基本不扣 */
    private fun rssiPenaltyOf(rssi: Int): Int = when {
        rssi >= -50 -> 25
        rssi >= -55 -> 20
        rssi >= -60 -> 15
        rssi >= -65 -> 11
        rssi >= -70 -> 7
        rssi >= -75 -> 4
        rssi >= -80 -> 2
        else -> 0
    }

    /** 利用率% -> 扣分 */
    private fun bssLoadPenaltyOf(utilPct: Double): Int {
        if (utilPct <= 0) return 0
        return (utilPct / 10.0 * 3.0).toInt().coerceAtMost(30)
    }
}