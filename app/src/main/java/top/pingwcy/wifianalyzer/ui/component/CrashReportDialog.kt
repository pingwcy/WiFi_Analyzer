package top.pingwcy.wifianalyzer.ui.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import top.pingwcy.wifianalyzer.R

/**
 * 检测到上次崩溃后的提示弹窗，让用户选择分享日志或忽略。
 */
@Composable
fun CrashReportDialog(
    onShare: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.crash_report_title)) },
        text = { Text(stringResource(R.string.crash_report_message)) },
        confirmButton = {
            TextButton(onClick = onShare) {
                Text(stringResource(R.string.crash_report_share))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.crash_report_dismiss))
            }
        }
    )
}
