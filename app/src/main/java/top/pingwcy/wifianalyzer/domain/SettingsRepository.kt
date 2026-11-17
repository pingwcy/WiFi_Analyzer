package top.pingwcy.wifianalyzer.domain

import kotlinx.coroutines.flow.Flow

/**
 * 应用设置仓库抽象（framework 无关）。
 *
 * 提供可观察的设置项，便于 UI 读取与修改，也便于后续替换存储实现或做单元测试 mock。
 */
interface SettingsRepository {

    /** 是否在应用打开时自动扫描（默认开启）。 */
    val autoScanEnabled: Flow<Boolean>

    /** 应用内语言选择（默认跟随系统）。 */
    val appLanguage: Flow<AppLanguage>

    /** 是否开启前台持续扫描（默认关闭）。 */
    val continuousScanEnabled: Flow<Boolean>

    /** 更新自动扫描开关。 */
    suspend fun setAutoScanEnabled(enabled: Boolean)

    /** 更新应用内语言。 */
    suspend fun setAppLanguage(language: AppLanguage)

    /** 更新持续扫描开关。 */
    suspend fun setContinuousScanEnabled(enabled: Boolean)
}
