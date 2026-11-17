package top.pingwcy.wifianalyzer.domain

/**
 * 应用内语言选项（framework 无关）。
 *
 * @property tag IETF 语言标签；[SYSTEM] 使用空标签表示跟随系统。
 */
enum class AppLanguage(val tag: String) {
    SYSTEM(""),
    CHINESE("zh"),
    ENGLISH("en");

    companion object {
        fun fromTag(tag: String?): AppLanguage =
            entries.firstOrNull { it.tag == tag } ?: SYSTEM
    }
}
