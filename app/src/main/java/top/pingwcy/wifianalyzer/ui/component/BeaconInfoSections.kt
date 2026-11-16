
package top.pingwcy.wifianalyzer.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.pingwcy.wifianalyzer.model.*

private fun ByteArray.toHexString(): String {
    if (isEmpty()) return "无数据"
    return joinToString(" ") {
        "%02X".format(it.toInt() and 0xFF)
    }
}

private fun Int.hexString(): String =
    "0x%04X".format(this)

@Composable
private fun RawDataText(
    data: ByteArray?,
    label: String = "原始数据"
) {
    if (data == null) return

    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = data.toHexString(),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun BeaconRow(
    label: String,
    value: String
) {
    DetailRow(label, value)
}

@Composable
fun BeaconInfoSections(beacon: BeaconInfo?) {
    ExpandableDetailSection(
        title = "Beacon IE 详细信息",
        initiallyExpanded = false
    ) {
        if (beacon == null) {
            Text(
                text = "当前设备没有提供可读取的 Beacon IE 信息",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            return@ExpandableDetailSection
        }

        // Country
        ExpandableDetailSection(
            title = "Country / 国家与监管信息"
        ) {
            val country = beacon.country

            if (country == null) {
                Text("未获取到 Country IE")
            } else {
                BeaconRow("国家代码", country.countryCode)
                BeaconRow(
                    "环境",
                    country.environment?.toString() ?: "未指定"
                )

                country.triplets.forEachIndexed { index, triplet ->
                    BeaconRow(
                        "信道范围 ${index + 1}",
                        "起始信道 ${triplet.firstChannel}，" +
                                "数量 ${triplet.channelCount}，" +
                                "最大功率 ${triplet.maxPowerDbm} dBm"
                    )
                }
            }
        }

        // Rates
        ExpandableDetailSection(
            title = "Supported Rates / 支持速率"
        ) {
            if (beacon.supportedRates.isEmpty()) {
                Text("没有速率信息")
            } else {
                beacon.supportedRates.forEachIndexed { index, rate ->
                    BeaconRow(
                        "速率 ${index + 1}",
                        "${rate.mbps} Mbps" +
                                if (rate.basic) "（基本速率）" else ""
                    )
                }
            }
        }

        // RSN
        ExpandableDetailSection(
            title = "RSN / RSNXE 安全信息"
        ) {
            val rsn = beacon.rsn

            if (rsn == null) {
                Text("RSN 尚未解析或未广播", style = MaterialTheme.typography.bodyMedium)
            } else {
                BeaconRow(label = "RSN 版本", value = rsn.version.toString())
                BeaconRow(label = "组播加密", value = rsn.groupCipher ?: "未知")

                BeaconRow(
                    label = "单播加密",
                    value = rsn.pairwiseCiphers.joinToString().ifEmpty { "无" }
                )

                // 解析 AKM
                val akmNames = rsn.akmSuites.map {
                    when(it) {
                        2 -> "PSK"
                        8 -> "SAE"
                        18 -> "OWE"
                        1 -> "802.1X"
                        else -> "AKM($it)"
                    }
                }
                BeaconRow(label = "密钥管理(AKM)", value = akmNames.joinToString().ifEmpty { "无" })

                // 注意：名字改成了 mfpCapable 和 mfpRequired
                BeaconRow(
                    label = "受保护管理帧(MFP)",
                    value = when {
                        rsn.mfpRequired -> "必需"
                        rsn.mfpCapable -> "支持"
                        else -> "不支持"
                    }
                )
            }
            RawDataText(beacon.rsnxe, "RSNXE 原始数据")
        }

        // HT
        ExpandableDetailSection(
            title = "HT Capabilities / 802.11n"
        ) {
            val ht = beacon.ht

            if (ht == null) {
                Text("没有 HT 信息")
            } else {
                BeaconRow(
                    "HT Capabilities",
                    ht.capabilities.hexString()
                )

                ht.maxAmsduLength?.let {
                    BeaconRow("最大 A-MSDU 长度", "$it bytes")
                }

                ht.ampduParameters?.let {
                    BeaconRow("A-MPDU 参数", it.hexString())
                }

                RawDataText(ht.rxMcsSet, "接收 MCS 集合")
                RawDataText(ht.operation, "HT Operation")
            }
        }

        // VHT
        ExpandableDetailSection(
            title = "VHT Capabilities / 802.11ac"
        ) {
            val vht = beacon.vht

            if (vht == null) {
                Text("没有 VHT 信息")
            } else {
                BeaconRow(
                    "VHT Capabilities",
                    "0x%08X".format(vht.capabilities)
                )

                vht.rxMcsMap?.let {
                    BeaconRow("RX MCS Map", it.hexString())
                }

                vht.txMcsMap?.let {
                    BeaconRow("TX MCS Map", it.hexString())
                }

                RawDataText(vht.operation, "VHT Operation")
            }
        }

        // HE
        ExpandableDetailSection(
            title = "HE / 802.11ax"
        ) {
            val he = beacon.he

            if (he == null) {
                Text("没有 HE 信息")
            } else {
                Text(
                    "当前解析器保存了 HE IE 原始内容，" +
                            "尚未完成 MAC / PHY 位域拆解。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )

                RawDataText(
                    he.macCapabilities,
                    "HE Capabilities IE 原始数据"
                )

                RawDataText(
                    he.phyCapabilities,
                    "HE PHY 原始数据"
                )

                RawDataText(
                    he.rxMcsNss,
                    "HE RX MCS / NSS"
                )

                RawDataText(
                    he.operation,
                    "HE Operation"
                )
            }
        }

        // EHT
        ExpandableDetailSection(
            title = "EHT / 802.11be"
        ) {
            val eht = beacon.eht

            if (eht == null) {
                Text("没有 EHT 信息")
            } else {
                Text(
                    "当前解析器保存了 EHT IE 原始内容，" +
                            "尚未完成 MAC / PHY 位域拆解。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )

                RawDataText(
                    eht.macCapabilities,
                    "EHT Capabilities IE 原始数据"
                )

                RawDataText(
                    eht.phyCapabilities,
                    "EHT PHY 原始数据"
                )

                RawDataText(
                    eht.operation,
                    "EHT Operation"
                )
            }
        }

        // WMM
        ExpandableDetailSection(
            title = "WMM / QoS"
        ) {
            val wmm = beacon.wmm

            if (wmm == null) {
                Text("WMM 尚未解析或未广播")
            } else {
                BeaconRow(label = "WMM 版本", value = wmm.version.toString())
                BeaconRow(label = "U-APSD", value = if (wmm.uApsd) "支持" else "不支持")
                BeaconRow(label = "参数化", value = if (wmm.parameterized) "是" else "否")
            }        }

        // Extended Capabilities
        ExpandableDetailSection(
            title = "Extended Capabilities"
        ) {
            RawDataText(
                beacon.extendedCapabilities,
                "扩展能力原始数据"
            )
        }

        // Power Envelope
        ExpandableDetailSection(
            title = "Transmit Power Envelope"
        ) {
            RawDataText(
                beacon.powerEnvelope,
                "发射功率原始数据"
            )
        }

        // Mobility Domain
        ExpandableDetailSection(
            title = "Mobility Domain / 802.11r"
        ) {
            RawDataText(
                beacon.mobilityDomain,
                "Mobility Domain 原始数据"
            )
        }

        // Vendor IE
        ExpandableDetailSection(
            title = "Vendor Specific / 厂商信息"
        ) {
            if (beacon.vendorIes.isEmpty()) {
                Text("没有厂商 IE")
            } else {
                beacon.vendorIes.forEachIndexed { index, vendor ->
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        BeaconRow(
                            "厂商 IE ${index + 1}",
                            vendor.oui
                        )

                        vendor.vendorType?.let {
                            BeaconRow("Vendor Type", it.toString())
                        }

                        RawDataText(vendor.data, "Vendor IE 数据")
                    }
                }
            }
        }

        // RNR
        ExpandableDetailSection(
            title = "RNR / Reduced Neighbor Report"
        ) {
            if (beacon.rnr.isEmpty()) {
                Text("没有 RNR 信息")
            } else {
                beacon.rnr.forEachIndexed { index, data ->
                    RawDataText(data, "RNR ${index + 1}")
                }
            }
        }

        // Multiple BSSID
        ExpandableDetailSection(
            title = "Multiple BSSID"
        ) {
            if (beacon.multipleBssid.isEmpty()) {
                Text("没有 Multiple BSSID 信息")
            } else {
                beacon.multipleBssid.forEachIndexed { index, data ->
                    RawDataText(data, "Multiple BSSID ${index + 1}")
                }
            }
        }

        // All raw IEs
        ExpandableDetailSection(
            title = "Raw Information Elements / 全部原始 IE"
        ) {
            if (beacon.rawIes.isEmpty()) {
                Text("没有原始 IE 数据")
            } else {
                beacon.rawIes.forEachIndexed { index, ie ->
                    Column(
                        modifier = Modifier.padding(vertical = 6.dp)
                    ) {
                        BeaconRow(
                            "IE ${index + 1}",
                            "ID=${ie.id}" +
                                    (ie.idExt?.let { "，Extension ID=$it" } ?: "")
                        )

                        RawDataText(ie.data, "Body")
                    }
                }
            }
        }

        // Warnings
        ExpandableDetailSection(
            title = "Parser Warnings / 解析警告"
        ) {
            if (beacon.parseWarnings.isEmpty()) {
                Text("没有解析错误")
            } else {
                beacon.parseWarnings.forEachIndexed { index, warning ->
                    Text(
                        "${index + 1}. $warning",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(vertical = 3.dp)
                    )
                }
            }
        }
    }
}
