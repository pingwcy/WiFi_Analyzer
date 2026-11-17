package top.pingwcy.wifianalyzer.data

import android.content.Context
import android.os.Build
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 本地崩溃日志器（零第三方依赖、零网络，契合「完全本地运行」的定位）。
 *
 * - [init]：在 Application 启动时安装全局未捕获异常处理器，把崩溃写入本地文件。
 * - 下次启动时由 UI 层调用 [hasCrashReport] 检测，提示用户是否分享日志。
 */
object CrashLogger {

    private const val CRASH_DIR = "crashes"

    /** 安装崩溃捕获处理器（仅调用一次，通常在 Application.onCreate）。 */
    fun init(context: Context) {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                writeCrash(context.applicationContext, thread, throwable)
            } catch (_: Throwable) {
                // 崩溃日志写入失败不应影响系统默认的崩溃处理流程
            }
            // 交给系统默认 handler（显示崩溃对话框 / 终止进程）
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    /** 是否存在待处理的崩溃日志。 */
    fun hasCrashReport(context: Context): Boolean =
        crashFiles(context).isNotEmpty()

    /** 读取所有崩溃日志，拼接为纯文本（用于分享）。 */
    fun readCrashReports(context: Context): String =
        crashFiles(context).joinToString("\n\n${"=".repeat(60)}\n\n") { it.readText() }

    /** 清除所有崩溃日志（分享或忽略后调用）。 */
    fun clear(context: Context) {
        crashFiles(context).forEach { it.delete() }
    }

    private fun crashFiles(context: Context): List<File> {
        val dir = File(context.filesDir, CRASH_DIR)
        if (!dir.exists() || !dir.isDirectory) return emptyList()
        return dir.listFiles()
            ?.filter { it.isFile && it.extension == "txt" }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    private fun writeCrash(context: Context, thread: Thread, throwable: Throwable) {
        val dir = File(context.filesDir, CRASH_DIR)
        if (!dir.exists()) dir.mkdirs()

        val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val fileFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        val now = Date()
        val file = File(dir, "crash-${fileFormat.format(now)}.txt")

        val content = buildString {
            appendLine("WiFi Analyzer Crash Report")
            appendLine("=========================")
            appendLine("Time: ${timeFormat.format(now)}")
            appendLine("App: ${context.packageName}")
            appendLine("Version: ${versionName(context)}")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("Thread: ${thread.name}")
            appendLine()
            append(throwable.stackTraceToString())
        }
        file.writeText(content)
    }

    private fun versionName(context: Context): String =
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "unknown"
}
