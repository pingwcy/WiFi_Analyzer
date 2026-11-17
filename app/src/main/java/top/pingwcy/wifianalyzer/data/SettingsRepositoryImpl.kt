package top.pingwcy.wifianalyzer.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import top.pingwcy.wifianalyzer.domain.AppLanguage
import top.pingwcy.wifianalyzer.domain.SettingsRepository

/**
 * 基于 SharedPreferences 的设置仓库实现。
 *
 * 设置项少且都是简单值，用 SharedPreferences 足够简单、同步读取、零额外依赖。
 */
class SettingsRepositoryImpl(context: Context) : SettingsRepository {

    private val prefs = context.applicationContext
        .getSharedPreferences(SETTINGS_PREFS_NAME, Context.MODE_PRIVATE)

    private val _autoScanEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_AUTO_SCAN_ENABLED, true)
    )

    private val _appLanguage = MutableStateFlow(
        AppLanguage.fromTag(prefs.getString(KEY_APP_LANGUAGE, null))
    )

    private val _continuousScanEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_CONTINUOUS_SCAN_ENABLED, false)
    )

    override val autoScanEnabled: Flow<Boolean> = _autoScanEnabled

    override val appLanguage: Flow<AppLanguage> = _appLanguage

    override val continuousScanEnabled: Flow<Boolean> = _continuousScanEnabled

    override suspend fun setAutoScanEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_SCAN_ENABLED, enabled).apply()
        _autoScanEnabled.value = enabled
    }

    override suspend fun setAppLanguage(language: AppLanguage) {
        prefs.edit().putString(KEY_APP_LANGUAGE, language.tag).apply()
        _appLanguage.value = language
    }

    override suspend fun setContinuousScanEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_CONTINUOUS_SCAN_ENABLED, enabled).apply()
        _continuousScanEnabled.value = enabled
    }
}
