package top.pingwcy.wifianalyzer.domain

import top.pingwcy.wifianalyzer.model.WifiData

/** 一个候选信道的干扰评估。 */
data class CandidateChannel(
    val channel: Int,
    val load: Double,   // 等效干扰负载（考虑相邻信道重叠）
    val apCount: Int,   // 对该信道有显著干扰（权重 ≥ 0.5）的 AP 数量
)

/** 最佳信道推荐结果。 */
data class ChannelRecommendation(
    val band: String,
    val recommended: Int,
    val candidates: List<CandidateChannel>,
)

/**
 * 信道推荐器：在给定频段内，从「非重叠 / 非 DFS」的候选信道中，
 * 选出真实干扰最小（含相邻信道重叠）的一个。
 *
 * 候选信道策略：
 *  - 2.4GHz：1 / 6 / 11（标准非重叠三信道）
 *  - 5GHz：UNII-1（36~48）+ UNII-3（149~161），避开 DFS 信道
 *  - 6GHz：PSC（Preferred Scanning Channel），间隔 80MHz
 */
object ChannelRecommender {

    private val candidateChannels = mapOf(
        "2.4 GHz" to listOf(1, 6, 11),
        "5 GHz" to listOf(36, 40, 44, 48, 149, 153, 157, 161),
        "6 GHz" to listOf(5, 21, 37, 53, 69, 85, 101, 117, 133, 149, 165, 181, 197, 213, 229),
    )

    fun recommend(wifiList: List<WifiData>, band: String): ChannelRecommendation {
        val channels = candidateChannels[band]
            ?: return ChannelRecommendation(band, 0, emptyList())

        val bandWifi = wifiList.filter { it.band == band }

        val candidates = channels.map { ch ->
            var load = 0.0
            var apCount = 0
            bandWifi.forEach { wifi ->
                val weight = ChannelCalculator.interferenceWeights(wifi)[ch] ?: 0.0
                if (weight > 0.0) {
                    load += weight
                    if (weight >= 0.5) apCount++
                }
            }
            CandidateChannel(channel = ch, load = load, apCount = apCount)
        }

        val recommended = candidates
            .minWithOrNull(compareBy<CandidateChannel> { it.load }.thenBy { it.apCount })
            ?.channel
            ?: channels.first()

        return ChannelRecommendation(band = band, recommended = recommended, candidates = candidates)
    }
}
