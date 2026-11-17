package top.pingwcy.wifianalyzer.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.pingwcy.wifianalyzer.R
import top.pingwcy.wifianalyzer.ui.component.DetailRow
import top.pingwcy.wifianalyzer.ui.component.DetailSection
import top.pingwcy.wifianalyzer.domain.ChannelCalculator
import top.pingwcy.wifianalyzer.domain.WifiStandards
import top.pingwcy.wifianalyzer.model.WifiData
import top.pingwcy.wifianalyzer.ui.component.BeaconInfoSections
import top.pingwcy.wifianalyzer.ui.theme.signalColor
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WifiDetailScreen(wifi: WifiData, onBack: () -> Unit) {
    // 处理 SSID 为空的情况
    val displaySsid = if (wifi.ssid.isBlank()) stringResource(R.string.hidden_network) else wifi.ssid
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(displaySsid) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.btn_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            DetailSection(title = stringResource(R.string.section_basic_info)) {
                DetailRow(stringResource(R.string.label_ssid), displaySsid)
                DetailRow(stringResource(R.string.label_bssid), wifi.bssid)
                DetailRow(
                    stringResource(R.string.label_is_hidden),
                    stringResource(if (wifi.isHidden) R.string.yes else R.string.no)
                )
                DetailRow(stringResource(R.string.label_band), wifi.band)
                DetailRow(stringResource(R.string.label_wifi_standard), wifi.wifiStandard)
                DetailRow(stringResource(R.string.label_ap_country_code), wifi.countryCode)
            }

            Spacer(modifier = Modifier.height(16.dp))

            DetailSection(title = stringResource(R.string.section_signal_channel)) {
                DetailRow(
                    stringResource(R.string.label_signal_strength),
                    stringResource(R.string.value_dbm, wifi.rssi),
                    valueColor = signalColor(wifi.rssi)
                )
                DetailRow(
                    stringResource(R.string.label_primary_freq),
                    stringResource(R.string.value_mhz, wifi.frequency)
                )
                DetailRow(
                    stringResource(R.string.label_primary_channel),
                    wifi.primaryChannel.toString() // 不需要单位，直接 toString 即可
                )
                DetailRow(
                    stringResource(R.string.label_channel_width),
                    WifiStandards.widthText(wifi.channelWidth)
                )
                DetailRow(
                    stringResource(R.string.label_center_freq_0),
                    if (wifi.centerFreq0 > 0) stringResource(R.string.value_mhz, wifi.centerFreq0)
                    else stringResource(R.string.none)
                )
                DetailRow(
                    stringResource(R.string.label_center_freq_1),
                    if (wifi.centerFreq1 > 0) stringResource(R.string.value_mhz, wifi.centerFreq1)
                    else stringResource(R.string.none)
                )

                val occChannels = ChannelCalculator.occupiedChannels(wifi)
                val occText = occChannels.joinToString(" ")
                DetailRow(
                    stringResource(R.string.label_occupied_channels),
                    stringResource(R.string.value_occupied_channels, occText, occChannels.size)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            DetailSection(title = stringResource(R.string.section_capabilities)) {
                Text(
                    text = wifi.capabilities.ifEmpty { stringResource(R.string.unknown) },
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            DetailSection(title = stringResource(R.string.section_bss_load)) {
                val bssLoad = wifi.bssLoad
                if (bssLoad != null) {
                    DetailRow(
                        stringResource(R.string.label_station_count),
                        bssLoad.stationCount.toString()
                    )

                    val utilStr = "%.2f".format(bssLoad.channelUtilization * 100.0 / 255.0)
                    DetailRow(
                        stringResource(R.string.label_channel_utilization),
                        stringResource(R.string.value_channel_utilization, utilStr, bssLoad.channelUtilization)
                    )

                    DetailRow(
                        stringResource(R.string.label_available_capacity),
                        stringResource(R.string.value_available_capacity, bssLoad.availableAdmissionCapacity)
                    )
                } else {
                    Text(
                        text = stringResource(R.string.bss_load_unavailable),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            BeaconInfoSections(
                beacon = wifi.beaconInfo
            )
        }
    }
}