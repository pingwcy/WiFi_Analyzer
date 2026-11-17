package top.pingwcy.wifianalyzer.domain

import top.pingwcy.wifianalyzer.model.WifiData

/**
 * 数据仓库抽象：定义「获取 WiFi 扫描结果」这一能力。
 *
 * UI / ViewModel 只依赖这个接口，不关心底层是系统 API、数据库还是缓存，
 * 便于替换实现、mock 测试，也把 Android 框架细节隔离在 data 层。
 */
interface WifiRepository {

    /**
     * 执行一次 WiFi 扫描并返回按信号强度降序排列的结果。
     *
     * @param timeoutMs 等待扫描广播的超时时间。
     */
    suspend fun scan(timeoutMs: Long = 5000): List<WifiData>
}
