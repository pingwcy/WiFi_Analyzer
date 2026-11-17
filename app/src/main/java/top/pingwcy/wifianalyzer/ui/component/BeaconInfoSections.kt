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

private fun ByteArray.toHexString(): String? {
    if (isEmpty()) return null
    return joinToString(" ") {
        "%02X".format(it.toInt() and 0xFF)
    }
}

private fun Int.hexString(): String =
    "0x%04X".format(this)

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
            RawDataText(beacon.rsnxe, stringResource(R.string.beacon_rsnxe_raw))
        }

        // HT
        ExpandableDetailSection(
            title = stringResource(R.string.beacon_ht_title)
        ) {
            val ht = beacon.ht

            if (ht == null) {
                Text(stringResource(R.string.beacon_ht_unavailable))
            } else {
                BeaconRow(
                    stringResource(R.string.beacon_ht_capabilities),
                    ht.capabilities.hexString()
                )

                ht.maxAmsduLength?.let {
                    BeaconRow(stringResource(R.string.beacon_max_amsdu), stringResource(R.string.beacon_bytes, it))
                }

                ht.ampduParameters?.let {
                    BeaconRow(stringResource(R.string.beacon_ampdu_params), it.hexString())
                }

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
                BeaconRow(
                    stringResource(R.string.beacon_vht_capabilities),
                    "0x%08X".format(vht.capabilities)
                )

                vht.rxMcsMap?.let {
                    BeaconRow(stringResource(R.string.beacon_rx_mcs_map), it.hexString())
                }

                vht.txMcsMap?.let {
                    BeaconRow(stringResource(R.string.beacon_tx_mcs_map), it.hexString())
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
                Text(
                    stringResource(R.string.beacon_he_parser_note),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )

                RawDataText(
                    he.macCapabilities,
                    stringResource(R.string.beacon_he_cap_raw)
                )

                RawDataText(
                    he.phyCapabilities,
                    stringResource(R.string.beacon_he_phy_raw)
                )

                RawDataText(
                    he.rxMcsNss,
                    stringResource(R.string.beacon_he_rx_mcs)
                )

                RawDataText(
                    he.operation,
                    stringResource(R.string.beacon_he_operation)
                )
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
                Text(
                    stringResource(R.string.beacon_eht_parser_note),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )

                RawDataText(
                    eht.macCapabilities,
                    stringResource(R.string.beacon_eht_cap_raw)
                )

                RawDataText(
                    eht.phyCapabilities,
                    stringResource(R.string.beacon_eht_phy_raw)
                )

                RawDataText(
                    eht.operation,
                    stringResource(R.string.beacon_eht_operation)
                )
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
                    value = if (wmm.uApsd) stringResource(R.string.beacon_mfp_capable)
                    else stringResource(R.string.beacon_mfp_unsupported)
                )
                BeaconRow(
                    label = stringResource(R.string.beacon_parameterized),
                    value = if (wmm.parameterized) stringResource(R.string.yes) else stringResource(R.string.no)
                )
            }
        }

        // Extended Capabilities
        ExpandableDetailSection(
            title = stringResource(R.string.beacon_ext_cap_title)
        ) {
            RawDataText(
                beacon.extendedCapabilities,
                stringResource(R.string.beacon_ext_cap_raw)
            )
        }

        // Power Envelope
        ExpandableDetailSection(
            title = stringResource(R.string.beacon_power_envelope_title)
        ) {
            RawDataText(
                beacon.powerEnvelope,
                stringResource(R.string.beacon_power_envelope_raw)
            )
        }

        // Mobility Domain
        ExpandableDetailSection(
            title = stringResource(R.string.beacon_mobility_title)
        ) {
            RawDataText(
                beacon.mobilityDomain,
                stringResource(R.string.beacon_mobility_raw)
            )
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
                beacon.rnr.forEachIndexed { index, data ->
                    RawDataText(data, stringResource(R.string.beacon_rnr_index, index + 1))
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
                beacon.multipleBssid.forEachIndexed { index, data ->
                    RawDataText(data, stringResource(R.string.beacon_mbssid_index, index + 1))
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
