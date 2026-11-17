package top.pingwcy.wifianalyzer.ui

import android.content.Context
import android.content.res.Configuration
import top.pingwcy.wifianalyzer.data.KEY_APP_LANGUAGE
import top.pingwcy.wifianalyzer.data.SETTINGS_PREFS_NAME
import top.pingwcy.wifianalyzer.domain.AppLanguage
import java.util.Locale

/**
 * 应用内语言切换的 locale 工具。
 *
 * 通过 [Context.createConfigurationContext] 覆盖 locale（适用于纯 Compose + ComponentActivity，
 * 无需引入 appcompat 依赖）；切换语言后由 Activity 重建触发重新读取。
 */
object LocaleHelper {

    /** 读取当前保存的语言（未保存或跟随系统时返回 [AppLanguage.SYSTEM]）。 */
    fun readLanguage(context: Context): AppLanguage {
        val prefs = context.getSharedPreferences(SETTINGS_PREFS_NAME, Context.MODE_PRIVATE)
        return AppLanguage.fromTag(prefs.getString(KEY_APP_LANGUAGE, null))
    }

    /** 返回应用了指定语言的 Context；跟随系统时原样返回。 */
    fun wrap(context: Context, language: AppLanguage): Context {
        if (language == AppLanguage.SYSTEM) return context
        val config = Configuration(context.resources.configuration).apply {
            setLocale(Locale.forLanguageTag(language.tag))
        }
        return context.createConfigurationContext(config)
    }
}
