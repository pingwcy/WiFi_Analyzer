package top.pingwcy.wifianalyzer.data

import android.net.wifi.ScanResult
import top.pingwcy.wifianalyzer.model.BeaconInfo

/**
 * 薄适配层：把 Android 的 [ScanResult.InformationElement] 转换成纯数据 [IeData]，
 * 再交给 [BeaconIeDecoder] 解析。Android 依赖被隔离在此处。
 */
object BeaconIeParser {

    private const val ID_EXTENSION = 255

    fun parse(elements: List<ScanResult.InformationElement>): BeaconInfo {
        val ieData = elements.map { e ->
            val idExt = if (e.id == ID_EXTENSION) e.idExt else -1
            val bytes = e.bytes.let { buf ->
                val dup = buf.duplicate()
                ByteArray(dup.remaining()).also { dup.get(it) }
            }
            IeData(id = e.id, idExt = idExt, bytes = bytes)
        }
        return BeaconIeDecoder.decode(ieData)
    }
}
