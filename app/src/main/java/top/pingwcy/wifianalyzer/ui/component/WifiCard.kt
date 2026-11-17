package top.pingwcy.wifianalyzer.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import top.pingwcy.wifianalyzer.R
import top.pingwcy.wifianalyzer.model.WifiData
import top.pingwcy.wifianalyzer.domain.WifiStandards
import top.pingwcy.wifianalyzer.ui.theme.WiFiAnalyzerTheme
import top.pingwcy.wifianalyzer.ui.theme.signalColor

@Composable
fun WifiCard(wifi: WifiData, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 处理 SSID 为空的情况（隐藏网络）
            val displaySsid = if (wifi.ssid.isBlank()) {
                stringResource(R.string.hidden_network)
            } else {
                wifi.ssid
            }

            Text(
                text = displaySsid,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 带参数替换
                Text(
                    text = stringResource(R.string.wifi_card_frequency, wifi.frequency),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.wifi_card_signal, wifi.rssi),
                    color = signalColor(wifi.rssi)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.wifi_card_width, WifiStandards.widthText(wifi.channelWidth)),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.wifi_card_channel, wifi.primaryChannel),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // 处理国家码为空的情况
                val displayCountry = wifi.countryCode.ifBlank {
                    stringResource(R.string.unknown)
                }
                Text(
                    text = stringResource(R.string.wifi_card_country, displayCountry),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WifiCardPreview() {
    WiFiAnalyzerTheme {
        WifiCard(wifi = sampleWifi, onClick = {})
    }
}