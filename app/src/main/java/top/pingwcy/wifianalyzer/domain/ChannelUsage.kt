package top.pingwcy.wifianalyzer.domain

/**
 * 单个信道的分析结果（领域产物）。
 *
 * 由 [ChannelAnalyzer] 生产、被 UI 消费。它聚合了「信道号 + 等效负载 + 网络列表 + 评分」，
 * 与 [ChannelScore] 同属 domain 层，避免 model 层反向依赖 domain。
 */
data class ChannelUsage(
    val channel: Int,
    val count: Int,          // 影响该信道的 AP 数量（含相邻信道贡献者）
    val load: Double,        // 等效负载 = Σ 干扰权重
    val networks: List<String>,
    val score: ChannelScore
)
