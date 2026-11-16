package top.pingwcy.wifianalyzer.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.pingwcy.wifianalyzer.model.WifiData
import top.pingwcy.wifianalyzer.domain.WifiStandards

@Composable
fun WifiCard(wifi: WifiData, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = wifi.ssid,
                fontSize = 18.sp,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "频率: ${wifi.frequency} MHz", color = Color.Gray)
                Text(
                    text = "信号: ${wifi.rssi} dBm",
                    color = when {
                        wifi.rssi >= -50 -> Color(0xFF4CAF50)
                        wifi.rssi >= -70 -> Color(0xFFFF9800)
                        else -> Color(0xFFF44336)
                    }
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "频宽: ${WifiStandards.widthText(wifi.channelWidth)}", color = Color.Gray)
                Text(text = "信道: ${wifi.primaryChannel}", color = Color.Gray)
                Text(text = "国家: ${wifi.countryCode}", color = Color.Gray)
            }
        }
    }
}
