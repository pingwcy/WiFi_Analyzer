package top.pingwcy.wifianalyzer.data

/** 一个信息元素（IE）的纯数据表示，作为 Beacon IE 解码器的输入。 */
data class IeData(
    val id: Int,
    val idExt: Int,   // 仅当 id == 255（Extension）时有意义，否则为 -1
    val bytes: ByteArray,
)
