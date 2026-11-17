package top.pingwcy.wifianalyzer.domain

import top.pingwcy.wifianalyzer.model.WifiData

/** 单个 AP 对某个信道的加权贡献（weight 表示干扰强度，0..1） */
data class WeightedWifi(
    val wifi: WifiData,
    val weight: Double,
)

/** 单个 AP 的评分明细 */
data class ApScore(
    val ssid: String,
    val bssid: String,
    val rssi: Int,
    val weight: Double,              // 该 AP 在此信道的干扰权重（0..1）
    val rssiPenalty: Int,            // 强度扣分
    val bssLoadPenalty: Int,         // BSS Load 扣分
    val channelUtilizationPct: Double?, // 信道利用率（0~100），无 BSS Load 时 null
    val stationCount: Int?,
)

/** 一个信道的综合评分 */
data class ChannelScore(
    val channel: Int,
    val grade: Grade,
    val apCount: Int,                // 影响该信道的 AP 数量（含相邻信道贡献者）
    val load: Double,                // 等效负载 = Σ 干扰权重
    val avgRssi: Double,             // 加权平均信号（越强越负，干扰越大）
    val avgUtilizationPct: Double?,  // 有 BSS Load 的 AP 的加权平均利用率
    // 三个维度扣分
    val apCountPenalty: Int,
    val rssiPenalty: Int,
    val bssLoadPenalty: Int,
    // 最终
    val totalPenalty: Int,
    val score: Int,                  // 0~100
    val apScores: List<ApScore>,     // 每个 AP 明细（点进去看）
)

enum class Grade {
    EXCELLENT, GOOD, FAIR, CROWDED
}

object ChannelScorer {

    /**
     * 评分规则（可调）：
     *  - AP 数量（等效负载）：≤1.0 不扣分；每多 1.0 扣 8 分，封顶 40 分
     *  - 平均 RSSI：>= -50 扣 25；每弱 5dBm 少扣约 4 分，封顶 30
     *  - BSS Load 平均利用率：0% 扣 0，每 10% 扣 3 分，封顶 30
     * 三个维度相加 = 总扣分（封顶 100），得分 = 100 - 总扣分。
     *
     * 其中 RSSI / BSS Load / 数量均按 [WeightedWifi.weight] 做加权，
     * 从而真实反映 2.4GHz 相邻信道干扰等频谱重叠。
     */
    fun scoreChannel(channel: Int, networks: List<WeightedWifi>): ChannelScore {
        val apCount = networks.size
        val load = networks.sumOf { it.weight }

        // ---------- 空信道：直接满分，不参与任何扣分 ----------
        if (apCount == 0 || load <= 0.0) {
            return ChannelScore(
                channel = channel,
                apCount = 0,
                load = 0.0,
                avgRssi = 0.0,
                avgUtilizationPct = null,
                apCountPenalty = 0,
                rssiPenalty = 0,
                bssLoadPenalty = 0,
                totalPenalty = 0,
                score = 100,
                grade = Grade.EXCELLENT,
                apScores = emptyList()
            )
        }

        // ---------- AP 数量（等效负载）扣分 ----------
        val apCountPenalty = if (load <= 1.0) {
            0
        } else {
            ((load - 1.0) * 8.0).toInt().coerceAtMost(40)
        }

        // ---------- RSSI 扣分（逐个 AP 算，汇总取加权平均） ----------
        val apScores = networks.map { n ->
            val w = n.wifi
            val rssiPenalty = rssiPenaltyOf(w.rssi)
            val util = w.bssLoad?.let { it.channelUtilization * 100.0 / 255.0 }
            val bssPenalty = util?.let { bssLoadPenaltyOf(it) } ?: 0
            ApScore(
                ssid = w.ssid,
                bssid = w.bssid,
                rssi = w.rssi,
                weight = n.weight,
                rssiPenalty = rssiPenalty,
                bssLoadPenalty = bssPenalty,
                channelUtilizationPct = util,
                stationCount = w.bssLoad?.stationCount
            )
        }

        val avgRssi = networks.sumOf { it.wifi.rssi * it.weight } / load
        // 用加权平均 RSSI 换算整体扣分（避免一个超强 AP 就爆表）
        val rssiPenalty = rssiPenaltyOf(avgRssi.toInt()).coerceAtMost(30)

        // ---------- BSS Load 扣分（只对提供 BSS Load 的 AP 做加权平均） ----------
        val utilContributors = networks.filter { it.wifi.bssLoad != null }
        val utilWeightSum = utilContributors.sumOf { it.weight }
        val avgUtil = if (utilWeightSum > 0.0) {
            utilContributors.sumOf {
                it.weight * (it.wifi.bssLoad!!.channelUtilization * 100.0 / 255.0)
            } / utilWeightSum
        } else {
            null
        }
        val bssLoadPenalty = avgUtil?.let { bssLoadPenaltyOf(it).coerceAtMost(30) } ?: 0

        val totalPenalty = (apCountPenalty + rssiPenalty + bssLoadPenalty).coerceIn(0, 100)
        val score = (100 - totalPenalty).coerceIn(0, 100)

        val grade = when {
            score >= 80 -> Grade.EXCELLENT
            score >= 60 -> Grade.GOOD
            score >= 40 -> Grade.FAIR
            else -> Grade.CROWDED
        }

        return ChannelScore(
            channel = channel,
            apCount = apCount,
            load = load,
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
