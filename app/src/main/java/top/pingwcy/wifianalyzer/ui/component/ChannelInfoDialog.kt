package top.pingwcy.wifianalyzer.ui.component

import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.pingwcy.wifianalyzer.R
import top.pingwcy.wifianalyzer.data.AllowedChannelsProvider

/**
 * 展示当前设备监管域允许使用的信道列表。
 *
 * 需要 Android 14（API 34）+ 以及 NEARBY_WIFI_DEVICES 权限；低版本或拿不到时优雅降级提示。
 */
@Composable
fun ChannelInfoDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current

    val allowedByBand = remember(context) {
        AllowedChannelsProvider.allowedChannelsByBand(context)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_channel_title)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(stringResource(R.string.dialog_current_domain))
                Text(
                    stringResource(R.string.dialog_country_code_unknown),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.dialog_allowed_channels))
                Spacer(Modifier.height(6.dp))

                when {
                    Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
                        Text(
                            stringResource(R.string.dialog_android_too_old),
                            color = Color.Red,
                            fontSize = 12.sp
                        )
                    }

                    allowedByBand.isEmpty() -> {
                        Text(
                            stringResource(R.string.dialog_no_channels),
                            color = Color.Red,
                            fontSize = 12.sp
                        )
                    }

                    else -> {
                        Text(
                            stringResource(R.string.dialog_channel_count, allowedByBand.values.sumOf { it.size }),
                            color = Color(0xFF4CAF50)
                        )
                        Spacer(Modifier.height(8.dp))

                        allowedByBand.forEach { (band, channels) ->
                            val bandName = when (band) {
                                "2.4 GHz" -> stringResource(R.string.band_2g)
                                "5 GHz" -> stringResource(R.string.band_5g)
                                "6 GHz" -> stringResource(R.string.band_6g)
                                else -> stringResource(R.string.band_other)
                            }
                            channels.forEach { allowed ->
                                Text(
                                    stringResource(
                                        R.string.dialog_channel_line,
                                        bandName,
                                        allowed.channel,
                                        allowed.frequencyMhz
                                    ),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.dialog_note),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_ok)) }
        }
    )
}


