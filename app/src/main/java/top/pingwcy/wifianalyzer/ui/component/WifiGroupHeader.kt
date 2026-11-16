package top.pingwcy.wifianalyzer.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.pingwcy.wifianalyzer.model.WifiData

@Composable
fun WifiGroupHeader(
    ssid: String,
    count: Int,
    strongest: WifiData,
    expanded: Boolean,
    onToggle: () -> Unit,
    onOpenDetail: (WifiData) -> Unit,   // 点击头部可看最强那个的详情（可选）
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onToggle() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "收起" else "展开",
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = ssid,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1
                )
                Text(
                    text = "$count 个热点 · 最强 ${strongest.rssi} dBm · ${strongest.band}",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
            // 右侧放一个"查看最强"的按钮，避免展开也能进详情
            TextButton(onClick = { onOpenDetail(strongest) }) {
                Text("详情", fontSize = 12.sp)
            }
        }
    }
}