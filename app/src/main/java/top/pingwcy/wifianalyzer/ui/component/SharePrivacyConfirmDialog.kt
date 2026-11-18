package top.pingwcy.wifianalyzer.ui.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import top.pingwcy.wifianalyzer.R

/**
 * 分享前的隐私确认弹窗。
 *
 * 报告/崩溃日志可能包含 SSID、BSSID、RSSI 等可推断位置的信息，分享前应征得用户同意。
 */
@Composable
fun SharePrivacyConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.share_privacy_title)) },
        text = { Text(stringResource(R.string.share_privacy_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.share_privacy_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.share_privacy_cancel))
            }
        }
    )
}
