package top.pingwcy.wifianalyzer.ui.component

import android.content.Context
import android.net.wifi.WifiAvailableChannel
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
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

/**
 * 展示当前设备监管域允许使用的信道列表。
 *
 * 需要 Android 14（API 34）+ 以及 NEARBY_WIFI_DEVICES 权限；低版本或拿不到时优雅降级提示。
 */
@Composable
fun ChannelInfoDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current

    val wm = remember(context) {
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    }

    val supportedFrequencies: List<Int> = remember(wm) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val result = mutableListOf<Int>()
            // 1=2.4G, 6=5G(含DFS), 8=6G
            for (band in listOf(1, 6, 8)) {
                try {
                    result += wm.getAllowedChannels(band, WifiAvailableChannel.OP_MODE_STA)
                        .map { it.frequencyMhz }
                } catch (e: SecurityException) {
                    Log.e("ChannelInfo", "Missing NEARBY_WIFI_DEVICES permission", e)
                } catch (e: Exception) {
                    Log.e("ChannelInfo", "Failed to get band=$band", e)
                }
            }
            result.distinct().sorted()
        } else {
            emptyList()
        }
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

                    supportedFrequencies.isEmpty() -> {
                        Text(
                            stringResource(R.string.dialog_no_channels),
                            color = Color.Red,
                            fontSize = 12.sp
                        )
                    }

                    else -> {
                        Text(
                            stringResource(R.string.dialog_channel_count, supportedFrequencies.size),
                            color = Color(0xFF4CAF50)
                        )
                        Spacer(Modifier.height(8.dp))

                        supportedFrequencies.forEach { freq ->
                            val channel = frequencyToChannel(freq)
                            val bandName = when (freq) {
                                in 2400..2500 -> stringResource(R.string.band_2g)
                                in 4900..5900 -> stringResource(R.string.band_5g)
                                in 5925..7125 -> stringResource(R.string.band_6g)
                                else -> stringResource(R.string.band_other)
                            }
                            Text(
                                stringResource(R.string.dialog_channel_line, bandName, channel, freq),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
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

private fun frequencyToChannel(freq: Int): Int = when (freq) {
    in 2412..2472 -> (freq - 2407) / 5
    2484 -> 14
    in 5000..5895 -> (freq - 5000) / 5
    in 5955..7115 -> (freq - 5950) / 5
    else -> -1
}
