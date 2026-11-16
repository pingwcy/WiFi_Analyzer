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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.pingwcy.wifianalyzer.ui.component.DetailRow
import top.pingwcy.wifianalyzer.ui.component.DetailSection
import top.pingwcy.wifianalyzer.domain.ChannelCalculator
import top.pingwcy.wifianalyzer.domain.WifiStandards
import top.pingwcy.wifianalyzer.model.WifiData
import kotlin.text.ifEmpty

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WifiDetailScreen(wifi: WifiData, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(wifi.ssid) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
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
            DetailSection(title = "基本信息") {
                DetailRow("SSID", wifi.ssid)
                DetailRow("BSSID", wifi.bssid)
                DetailRow("是否隐藏", if (wifi.isHidden) "是" else "否")
                DetailRow("频段", wifi.band)
                DetailRow("WiFi 标准", wifi.wifiStandard)
                DetailRow("AP Country Code", wifi.countryCode)
            }

            Spacer(modifier = Modifier.height(16.dp))

            DetailSection(title = "信号与信道") {
                DetailRow(
                    "信号强度",
                    "${wifi.rssi} dBm",
                    valueColor = when {
                        wifi.rssi >= -50 -> Color(0xFF4CAF50)
                        wifi.rssi >= -70 -> Color(0xFFFF9800)
                        else -> Color(0xFFF44336)
                    }
                )
                DetailRow("主信道频率", "${wifi.frequency} MHz")
                DetailRow("主信道号", "${wifi.primaryChannel}")
                DetailRow("信道宽度", WifiStandards.widthText(wifi.channelWidth))
                DetailRow("中心频率 0", if (wifi.centerFreq0 > 0) "${wifi.centerFreq0} MHz" else "无")
                DetailRow("中心频率 1", if (wifi.centerFreq1 > 0) "${wifi.centerFreq1} MHz" else "无")
                val occChannels = ChannelCalculator.occupiedChannels(wifi)
                DetailRow(
                    "占用信道",
                    "${occChannels.joinToString(" ")}（${occChannels.size} 个）"
                )            }

            Spacer(modifier = Modifier.height(16.dp))

            DetailSection(title = "能力 / 加密") {
                Text(
                    text = wifi.capabilities.ifEmpty { "未知" },
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            DetailSection(title = "BSS Load（网络负载）") {

                val bssLoad = wifi.bssLoad

                if (bssLoad != null) {

                    DetailRow(
                        "关联站点数量",
                        "${bssLoad.stationCount}"
                    )

                    DetailRow(
                        "信道利用率",
                        "${"%.2f".format(
                            bssLoad.channelUtilization * 100.0 / 255.0
                        )}% (${bssLoad.channelUtilization}/255)"
                    )

                    DetailRow(
                        "剩余接纳容量",
                        "${bssLoad.availableAdmissionCapacity} (32 μs/s)"
                    )

                } else {
                    Text(
                        text = "未获取到 BSS Load 信息",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }


        }
    }
}
