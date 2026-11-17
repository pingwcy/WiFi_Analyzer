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
import top.pingwcy.wifianalyzer.R
import top.pingwcy.wifianalyzer.model.*
import java.util.Locale

private fun ByteArray.toHexString(): String? {
    if (isEmpty()) return null
    return joinToString(" ") {
        "%02X".format(it.toInt() and 0xFF)
    }
}

@Composable
private fun RawDataText(
    data: ByteArray?,
    label: String
) {
    if (data == null) return

    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = data.toHexString() ?: stringResource(R.string.beacon_no_data),
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
private fun yesNo(value: Boolean): String =
    stringResource(if (value) R.string.yes else R.string.no)

@Composable
fun BeaconInfoSections(beacon: BeaconInfo?) {
    ExpandableDetailSection(
        title = stringResource(R.string.beacon_ie_details_title),
        initiallyExpanded = false
    ) {
        if (beacon == null) {
            Text(
                text = stringResource(R.string.beacon_info_unavailable),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            return@ExpandableDetailSection
        }

        // Country
        ExpandableDetailSection(
            title = stringResource(R.string.beacon_country_title)
        ) {
            val country = beacon.country

            if (country == null) {
                Text(stringResource(R.string.beacon_country_not_found))
            } else {
                BeaconRow(stringResource(R.string.beacon_country_code), country.countryCode)
                BeaconRow(
                    stringResource(R.string.beacon_environment),
                    country.environment?.toString() ?: stringResource(R.string.beacon_unspecified)
                )

                country.triplets.forEachIndexed { index, triplet ->
                    val power = triplet.maxPowerDbm?.toString()
                        ?: stringResource(R.string.beacon_unspecified)
                    BeaconRow(
                        stringResource(R.string.beacon_channel_range, index + 1),
                        stringResource(
                            R.string.beacon_country_triplet,
                            triplet.firstChannel,
                            triplet.channelCount,
                            power
                        )
                    )
                }
            }
        }

        // Rates
        ExpandableDetailSection(
            title = stringResource(R.string.beacon_rates_title)
        ) {
            if (beacon.supportedRates.isEmpty()) {
                Text(stringResource(R.string.beacon_no_rates))
            } else {
                beacon.supportedRates.forEachIndexed { index, rate ->
                    val rateText = "${rate.mbps} Mbps" +
                            if (rate.basic) stringResource(R.string.beacon_rate_basic) else ""
                    BeaconRow(
                        stringResource(R.string.beacon_rate_index, index + 1),
                        rateText
                    )
                }
            }
        }

        // RSN
        ExpandableDetailSection(
            title = stringResource(R.string.beacon_rsn_title)
        ) {
            val rsn = beacon.rsn

            if (rsn == null) {
                Text(stringResource(R.string.beacon_rsn_unavailable), style = MaterialTheme.typography.bodyMedium)
            } else {
                BeaconRow(
                    label = stringResource(R.string.beacon_rsn_version),
                    value = rsn.version.toString()
                )
                BeaconRow(
                    label = stringResource(R.string.beacon_group_cipher),
                    value = rsn.groupCipher.ifEmpty { stringResource(R.string.unknown) }
                )

                BeaconRow(
                    label = stringResource(R.string.beacon_pairwise_cipher),
                    value = rsn.pairwiseCiphers.joinToString().ifEmpty { stringResource(R.string.none) }
                )

                // AKM 名称是协议缩写，保持原文
                val akmNames = rsn.akmSuites.map {
                    when (it) {
                        2 -> "PSK"
                        8 -> "SAE"
                        18 -> "OWE"
                        1 -> "802.1X"
                        5 -> "802.1X-SHA256"
                        6 -> "PSK-SHA256"
                        else -> "AKM($it)"
                    }
                }
                BeaconRow(
                    label = stringResource(R.string.beacon_akm),
                    value = akmNames.joinToString().ifEmpty { stringResource(R.string.none) }
                )

                BeaconRow(
                    label = stringResource(R.string.beacon_mfp),
                    value = when {
                        rsn.mfpRequired -> stringResource(R.string.beacon_mfp_required)
                        rsn.mfpCapable -> stringResource(R.string.beacon_mfp_capable)
                        else -> stringResource(R.string.beacon_mfp_unsupported)
                    }
                )
            }

            // RSNXE
            val rsnxe = beacon.rsnxe
            if (rsnxe != null) {
                BeaconRow(
                    label = stringResource(R.string.beacon_rsnxe_version),
                    value = rsnxe.version.toString()
                )
                BeaconRow(
                    label = stringResource(R.string.beacon_rsnxe_capabilities),
                    value = rsnxe.capabilities.joinToString(", ")
                        .ifEmpty { stringResource(R.string.beacon_no_capabilities) }
                )
            }
        }

        // HT
        ExpandableDetailSection(
            title = stringResource(R.string.beacon_ht_title)
        ) {
            val ht = beacon.ht

            if (ht == null) {
                Text(stringResource(R.string.beacon_ht_unavailable))
            } else {
                BeaconRow("LDPC", yesNo(ht.ldpc))
                BeaconRow("SGI 20 MHz", yesNo(ht.sgi20))
                BeaconRow("SGI 40 MHz", yesNo(ht.sgi40))
                BeaconRow("Greenfield", yesNo(ht.greenfield))
                BeaconRow("40 MHz", yesNo(ht.fortyMhzSupported))
                ht.maxAmsduLength?.let {
                    BeaconRow(stringResource(R.string.beacon_max_amsdu), stringResource(R.string.beacon_bytes, it))
                }
                ht.maxAmpduLength?.let {
                    BeaconRow("Max A-MPDU", stringResource(R.string.beacon_bytes, it))
                }
                BeaconRow("STBC RX streams", ht.stbcRxStreams.toString())
                ht.primaryChannel?.let {
                    BeaconRow("HT 主信道", it.toString())
                }
                ht.secondaryChannelOffset?.let {
                    BeaconRow("次信道偏移", it.toString())
                }
                ht.channelWidth?.let {
                    BeaconRow(stringResource(R.string.label_channel_width), "$it MHz")
                }
                BeaconRow("RIFS 模式", yesNo(ht.rifsMode))

                RawDataText(ht.rxMcsSet, stringResource(R.string.beacon_rx_mcs))
                RawDataText(ht.operation, stringResource(R.string.beacon_ht_operation))
            }
        }

        // VHT
        ExpandableDetailSection(
            title = stringResource(R.string.beacon_vht_title)
        ) {
            val vht = beacon.vht

            if (vht == null) {
                Text(stringResource(R.string.beacon_vht_unavailable))
            } else {
                vht.maxMpduLength?.let {
                    BeaconRow("Max MPDU", stringResource(R.string.beacon_bytes, it))
                }
                vht.supportedChannelWidth?.let {
                    val text = when (it) {
                        1 -> "160 MHz"
                        2 -> "160 MHz + 80+80"
                        else -> "80 MHz"
                    }
                    BeaconRow("支持信道宽度", text)
                }
                BeaconRow("RX LDPC", yesNo(vht.rxLdpc))
                BeaconRow("SGI 80 MHz", yesNo(vht.sgi80))
                BeaconRow("SGI 160 MHz", yesNo(vht.sgi160))
                BeaconRow("TX STBC", yesNo(vht.txStbc))
                BeaconRow("RX STBC", vht.rxStbc.toString())
                BeaconRow("SU Beamformer", yesNo(vht.suBeamformer))
                BeaconRow("MU Beamformer", yesNo(vht.muBeamformer))

                vht.centerFreqSeg0?.let {
                    BeaconRow("中心频率段 0", "$it")
                }
                vht.centerFreqSeg1?.let {
                    BeaconRow("中心频率段 1", "$it")
                }

                RawDataText(vht.operation, stringResource(R.string.beacon_vht_operation))
            }
        }

        // HE
        ExpandableDetailSection(
            title = stringResource(R.string.beacon_he_title)
        ) {
            val he = beacon.he

            if (he == null) {
                Text(stringResource(R.string.beacon_he_unavailable))
            } else {
                BeaconRow("TWT Requester", yesNo(he.twtRequester))
                BeaconRow("TWT Responder", yesNo(he.twtResponder))
                BeaconRow("MU EDCA", yesNo(he.muEdca))
                BeaconRow("OM Control", yesNo(he.omControl))
                BeaconRow("SU Beamformer", yesNo(he.suBeamformer))
                BeaconRow("SU Beamformee", yesNo(he.suBeamformee))
                BeaconRow("MU Beamformer", yesNo(he.muBeamformer))
                BeaconRow("160 MHz", yesNo(he.channelWidth160))

                RawDataText(he.rxMcsNss, stringResource(R.string.beacon_he_rx_mcs))
                RawDataText(he.operation, stringResource(R.string.beacon_he_operation))
            }
        }

        // EHT
        ExpandableDetailSection(
            title = stringResource(R.string.beacon_eht_title)
        ) {
            val eht = beacon.eht

            if (eht == null) {
                Text(stringResource(R.string.beacon_eht_unavailable))
            } else {
                BeaconRow("EPCS Priority Access", yesNo(eht.epcsPriorityAccess))
                BeaconRow("EHT OM Control", yesNo(eht.ehtOmControl))
                BeaconRow("Triggered TXOP Sharing", yesNo(eht.triggeredTxopSharing))
                BeaconRow("320 MHz", yesNo(eht.support320Mhz))
                BeaconRow("4096-QAM", yesNo(eht.support4096Qam))
                eht.channelWidth?.let {
                    BeaconRow(stringResource(R.string.label_channel_width), "$it MHz")
                }
                eht.puncturingBitmap?.let {
                    BeaconRow("Puncturing bitmap", "0x%04X".format(it))
                }

                RawDataText(eht.operation, stringResource(R.string.beacon_eht_operation))
            }
        }

        // WMM
        ExpandableDetailSection(
            title = stringResource(R.string.beacon_wmm_title)
        ) {
            val wmm = beacon.wmm

            if (wmm == null) {
                Text(stringResource(R.string.beacon_wmm_unavailable))
            } else {
                BeaconRow(label = stringResource(R.string.beacon_wmm_version), value = wmm.version.toString())
                BeaconRow(
                    label = stringResource(R.string.beacon_uapsd),
                    value = yesNo(wmm.uApsd)
                )
                BeaconRow(
                    label = stringResource(R.string.beacon_parameterized),
                    value = yesNo(wmm.parameterized)
                )
            }
        }

        // Extended Capabilities
        ExpandableDetailSection(
            title = stringResource(R.string.beacon_ext_cap_title)
        ) {
            val extCaps = beacon.extendedCapabilities
            if (extCaps == null) {
                Text(stringResource(R.string.beacon_no_capabilities))
            } else {
                BeaconRow(
                    label = stringResource(R.string.beacon_ext_capabilities),
                    value = extCaps.capabilities.joinToString(", ")
                        .ifEmpty { stringResource(R.string.beacon_no_capabilities) }
                )
                RawDataText(extCaps.raw, stringResource(R.string.beacon_ext_cap_raw))
            }
        }

        // Power Envelope
        ExpandableDetailSection(
            title = stringResource(R.string.beacon_power_envelope_title)
        ) {
            val power = beacon.powerEnvelope
            if (power == null) {
                Text(stringResource(R.string.beacon_power_unavailable))
            } else {
                val undefined = stringResource(R.string.beacon_power_unavailable)
                fun fmt(v: Double?): String =
                    v?.let { String.format(Locale.US, "%.1f", it) } ?: undefined

                BeaconRow(
                    label = stringResource(R.string.beacon_power_max),
                    value = stringResource(R.string.beacon_power_value, fmt(power.maxTransmitPowerDbm))
                )
                BeaconRow(
                    label = stringResource(R.string.beacon_power_local),
                    value = stringResource(R.string.beacon_power_value, fmt(power.localMaxTransmitPowerDbm))
                )

                power.regulatoryClientInfos.forEachIndexed { index, client ->
                    val cat = client.maxTransmitPowerCategory?.toString() ?: undefined
                    BeaconRow(
                        label = stringResource(R.string.beacon_power_client, index + 1),
                        value = stringResource(
                            R.string.beacon_power_value,
                            fmt(client.maxTransmitPowerDbm)
                        ) + " · " + stringResource(R.string.beacon_power_category, cat)
                    )
                }
            }
        }

        // Mobility Domain
        ExpandableDetailSection(
            title = stringResource(R.string.beacon_mobility_title)
        ) {
            val md = beacon.mobilityDomain
            if (md == null) {
                Text(stringResource(R.string.beacon_unspecified))
            } else {
                BeaconRow(
                    label = stringResource(R.string.beacon_mdid),
                    value = "0x%04X".format(md.mdid)
                )
                BeaconRow(
                    label = stringResource(R.string.beacon_ft_over_ds),
                    value = yesNo(md.fastBssTransitionOverDs)
                )
                BeaconRow(
                    label = stringResource(R.string.beacon_rrp),
                    value = yesNo(md.resourceRequestProtocolCapability)
                )
            }
        }

        // Vendor IE
        ExpandableDetailSection(
            title = stringResource(R.string.beacon_vendor_title)
        ) {
            if (beacon.vendorIes.isEmpty()) {
                Text(stringResource(R.string.beacon_vendor_unavailable))
            } else {
                beacon.vendorIes.forEachIndexed { index, vendor ->
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        BeaconRow(
                            stringResource(R.string.beacon_vendor_index, index + 1),
                            vendor.oui
                        )

                        vendor.vendorType?.let {
                            BeaconRow(stringResource(R.string.beacon_vendor_type), it.toString())
                        }

                        RawDataText(vendor.data, stringResource(R.string.beacon_vendor_data))
                    }
                }
            }
        }

        // RNR
        ExpandableDetailSection(
            title = stringResource(R.string.beacon_rnr_title)
        ) {
            if (beacon.rnr.isEmpty()) {
                Text(stringResource(R.string.beacon_rnr_unavailable))
            } else {
                beacon.rnr.forEachIndexed { index, neighbor ->
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        BeaconRow(
                            stringResource(R.string.beacon_rnr_index, index + 1),
                            neighbor.bssid ?: stringResource(R.string.unknown)
                        )
                        neighbor.shortSsid?.let {
                            BeaconRow(stringResource(R.string.beacon_rnr_short_ssid), it)
                        }
                        BeaconRow(
                            stringResource(R.string.beacon_rnr_channel, neighbor.channelNumber, neighbor.operatingClass),
                            ""
                        )
                        BeaconRow(
                            stringResource(R.string.beacon_rnr_tbtt),
                            "${neighbor.tbttOffsetTu} TU"
                        )
                        BeaconRow(stringResource(R.string.beacon_rnr_same_ssid), yesNo(neighbor.sameSsid))
                        BeaconRow(stringResource(R.string.beacon_rnr_multiple_bssid), yesNo(neighbor.multipleBssid))
                        BeaconRow(stringResource(R.string.beacon_rnr_transmitted), yesNo(neighbor.transmittedBssid))
                    }
                }
            }
        }

        // Multiple BSSID
        ExpandableDetailSection(
            title = stringResource(R.string.beacon_mbssid_title)
        ) {
            if (beacon.multipleBssid.isEmpty()) {
                Text(stringResource(R.string.beacon_mbssid_unavailable))
            } else {
                beacon.multipleBssid.forEach { element ->
                    BeaconRow(
                        label = stringResource(R.string.beacon_mbssid_max_indicator),
                        value = element.maxBssidIndicator.toString()
                    )
                    element.profiles.forEachIndexed { index, profile ->
                        BeaconRow(
                            label = stringResource(R.string.beacon_mbssid_profile, index + 1, profile.subelementId),
                            value = ""
                        )
                    }
                }
            }
        }

        // All raw IEs
        ExpandableDetailSection(
            title = stringResource(R.string.beacon_raw_title)
        ) {
            if (beacon.rawIes.isEmpty()) {
                Text(stringResource(R.string.beacon_raw_unavailable))
            } else {
                beacon.rawIes.forEachIndexed { index, ie ->
                    Column(
                        modifier = Modifier.padding(vertical = 6.dp)
                    ) {
                        BeaconRow(
                            stringResource(R.string.beacon_ie_index, index + 1),
                            stringResource(R.string.beacon_ie_id, ie.id) +
                                    (ie.idExt?.let { stringResource(R.string.beacon_ie_ext_id, it) } ?: "")
                        )

                        RawDataText(ie.data, stringResource(R.string.beacon_ie_body))
                    }
                }
            }
        }

        // Warnings
        ExpandableDetailSection(
            title = stringResource(R.string.beacon_warnings_title)
        ) {
            if (beacon.parseWarnings.isEmpty()) {
                Text(stringResource(R.string.beacon_no_warnings))
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
