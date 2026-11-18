package top.pingwcy.wifianalyzer.domain

/**
 * 排序/视图模式（framework 无关，供 UI 与设置仓库共用）。
 *
 * @property tag 持久化标识；[GROUPED] 为默认值。
 */
enum class SortMode(val tag: String) {
    GROUPED("grouped"),   // 按 SSID 聚合，组间按组内最强 rssi 降序
    FLAT("flat");         // 全部展开，纯 rssi 降序

    companion object {
        fun fromTag(tag: String?): SortMode =
            entries.firstOrNull { it.tag == tag } ?: GROUPED
    }
}
