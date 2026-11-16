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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.pingwcy.wifianalyzer.R
import top.pingwcy.wifianalyzer.model.WifiData

@Composable
fun WifiGroupHeader(
    ssid: String,
    count: Int,
    strongest: WifiData,
    expanded: Boolean,
    onToggle: () -> Unit,
    onOpenDetail: (WifiData) -> Unit,
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
                // ✅ 替换 contentDescription
                contentDescription = stringResource(
                    if (expanded) R.string.group_collapse else R.string.group_expand
                ),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = ssid,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1
                )
                // ✅ 替换带参数的副标题
                Text(
                    text = stringResource(
                        R.string.group_subtitle,
                        count,
                        strongest.rssi,
                        strongest.band
                    ),
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
            // 右侧放一个"查看最强"的按钮
            TextButton(onClick = { onOpenDetail(strongest) }) {
                // ✅ 替换按钮文字
                Text(stringResource(R.string.btn_detail), fontSize = 12.sp)
            }
        }
    }
}