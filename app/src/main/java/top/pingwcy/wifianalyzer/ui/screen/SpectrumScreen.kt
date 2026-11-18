package top.pingwcy.wifianalyzer.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.pingwcy.wifianalyzer.R
import top.pingwcy.wifianalyzer.data.AllowedChannelsProvider
import top.pingwcy.wifianalyzer.data.BandAvailability
import top.pingwcy.wifianalyzer.data.WifiBandSupport
import top.pingwcy.wifianalyzer.domain.ApBand
import top.pingwcy.wifianalyzer.domain.ChannelCalculator
import top.pingwcy.wifianalyzer.domain.SpectrumAnalyzer
import top.pingwcy.wifianalyzer.domain.WifiStandards
import top.pingwcy.wifianalyzer.model.WifiData
import top.pingwcy.wifianalyzer.ui.component.BandNotSupportedHint
import top.pingwcy.wifianalyzer.ui.component.ChannelMark
import top.pingwcy.wifianalyzer.ui.component.SpectrumChart

/** 各频段的关键信道标注（频率 MHz -> 信道号）。 */
private val channelMarksByBand: Map<String, List<ChannelMark>> = mapOf(
    "2.4 GHz" to listOf(
        ChannelMark(2412, "1"),
        ChannelMark(2437, "6"),
        ChannelMark(2462, "11"),
    ),
    "5 GHz" to listOf(
        ChannelMark(5180, "36"),
        ChannelMark(5220, "44"),
        ChannelMark(5320, "64"),
        ChannelMark(5500, "100"),
        ChannelMark(5745, "149"),
        ChannelMark(5825, "165"),
    ),
    "6 GHz" to listOf(
        ChannelMark(5955, "1"),
        ChannelMark(6175, "45"),
        ChannelMark(6415, "93"),
        ChannelMark(6655, "141"),
        ChannelMark(6895, "189"),
    ),
)

/** 给每个 AP 分配一个稳定的身份色（按列表顺序循环）。 */
private val apColors = listOf(
    Color(0xFF4285F4),
    Color(0xFF34A853),
    Color(0xFFFBBC05),
    Color(0xFFEA4335),
    Color(0xFF8E24AA),
    Color(0xFF00ACC1),
    Color(0xFF6D4C41),
    Color(0xFF5E35B1),
    Color(0xFF00897B),
    Color(0xFFF4511E),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpectrumScreen(
    wifiList: List<WifiData>,
    onBack: () -> Unit,
) {
    var band by remember { mutableStateOf("2.4 GHz") }
    var selectedBssid by remember(band) { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val allowedChannelsByBand = remember(context) {
        AllowedChannelsProvider.allowedChannelsByBand(context)
            .mapValues { (_, list) -> list.map { it.channel }.toSet() }
    }
    val bandAllowedChannels = allowedChannelsByBand[band]?.takeIf { it.isNotEmpty() }

    val points = remember(wifiList, band) { SpectrumAnalyzer.analyze(wifiList, band) }
    val apBands = remember(wifiList, band) { SpectrumAnalyzer.apBands(wifiList, band) }
    val apSpectra = remember(wifiList, band) { SpectrumAnalyzer.analyzePerAp(wifiList, band) }
    val colorByBssid = remember(apBands) {
        apBands.mapIndexed { index, ap -> ap.bssid to apColors[index % apColors.size] }.toMap()
    }

    val highlightedSpectrum = selectedBssid?.let { apSpectra[it] }
    val highlightColor = selectedBssid?.let { colorByBssid[it] }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.spectrum_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.btn_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // 频段切换
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val options = listOf(
                    "2.4 GHz" to R.string.band_2g,
                    "5 GHz" to R.string.band_5g,
                    "6 GHz" to R.string.band_6g,
                )
                options.forEach { (value, labelRes) ->
                    FilterChip(
                        selected = band == value,
                        onClick = { band = value },
                        label = { Text(stringResource(labelRes)) }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            when (WifiBandSupport.availability(context, band)) {
                BandAvailability.HARDWARE_UNSUPPORTED -> {
                    BandNotSupportedHint(band, regulatoryOnly = false)
                    return@Column
                }
                BandAvailability.REGULATORY_UNSUPPORTED -> {
                    BandNotSupportedHint(band, regulatoryOnly = true)
                    return@Column
                }
                BandAvailability.SUPPORTED -> Unit
            }

            if (wifiList.isEmpty()) {
                Text(
                    stringResource(R.string.no_data_hint),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                return@Column
            }

            SpectrumChart(
                points = points,
                channelMarks = channelMarksByBand[band].orEmpty().filter { mark ->
                    bandAllowedChannels == null ||
                        ChannelCalculator.freqToChannel(mark.frequencyMhz) in bandAllowedChannels
                },
                highlightedSpectrum = highlightedSpectrum,
                highlightColor = highlightColor ?: MaterialTheme.colorScheme.primary,
            )

            Spacer(Modifier.height(8.dp))

            Text(
                stringResource(R.string.spectrum_caption),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(12.dp))

            Text(
                stringResource(R.string.spectrum_ap_list_title),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(4.dp))

            if (apBands.isEmpty()) {
                Text(
                    stringResource(R.string.spectrum_no_ap_hint),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                return@Column
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(apBands, key = { it.bssid }) { ap ->
                    ApLegendRow(
                        ap = ap,
                        color = colorByBssid[ap.bssid] ?: MaterialTheme.colorScheme.primary,
                        selected = selectedBssid == ap.bssid,
                        onClick = {
                            selectedBssid = if (selectedBssid == ap.bssid) null else ap.bssid
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ApLegendRow(
    ap: ApBand,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .background(color, CircleShape)
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = ap.ssid.ifBlank { stringResource(R.string.hidden_network) },
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
            )
            Text(
                text = stringResource(
                    R.string.spectrum_ap_subtitle,
                    ap.primaryChannel,
                    WifiStandards.widthText(ap.channelWidth),
                    ap.rssi,
                ),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = color,
            )
        }
    }
}
