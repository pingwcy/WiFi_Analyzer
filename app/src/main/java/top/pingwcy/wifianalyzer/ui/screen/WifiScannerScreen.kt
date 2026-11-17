package top.pingwcy.wifianalyzer.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import top.pingwcy.wifianalyzer.R
import top.pingwcy.wifianalyzer.model.BandFilter
import top.pingwcy.wifianalyzer.model.WifiData
import top.pingwcy.wifianalyzer.model.groupKey
import top.pingwcy.wifianalyzer.ui.SortMode
import top.pingwcy.wifianalyzer.ui.WifiViewModel
import top.pingwcy.wifianalyzer.ui.component.ChannelInfoDialog
import top.pingwcy.wifianalyzer.ui.component.FilterChipRow
import top.pingwcy.wifianalyzer.ui.component.WifiCard
import top.pingwcy.wifianalyzer.ui.component.WifiGroupHeader

data class WifiGroup(
    val key: String,
    val ssid: String,          // 隐藏网络显示 "(隐藏网络)"
    val networks: List<WifiData> // 已按 rssi 降序
) {
    val strongest: WifiData get() = networks.first()
    val maxRssi: Int get() = strongest.rssi
    val count: Int get() = networks.size
}

fun buildWifiGroups(list: List<WifiData>): List<WifiGroup> =
    list.groupBy { groupKey(it) }
        .map { (key, group) ->
            val sorted = group.sortedByDescending { it.rssi }
            WifiGroup(
                key = key,
                ssid = sorted.first().ssid.takeIf { it.isNotBlank() } ?: "(隐藏网络)",
                networks = sorted
            )
        }
        // 组间按组内最强信号降序；信号相同再按 SSID 字母序，保证稳定
        .sortedWith(
            compareByDescending<WifiGroup> { it.maxRssi }
                .thenBy { it.ssid }
        )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WifiScannerScreen(
    onItemClick: (WifiData) -> Unit,
    onOpenChannelAnalysis: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: WifiViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val bandFilter = uiState.bandFilter
    var showMenu by remember { mutableStateOf(false) }
    var showChannelInfoDialog by remember { mutableStateOf(false) }

    val permissionsToRequest = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.NEARBY_WIFI_DEVICES
        else
            Manifest.permission.ACCESS_FINE_LOCATION
    )

    // 首次进入：请求一次「自动扫描」决策（由 ViewModel 结合设置与权限决定，且仅触发一次）
    LaunchedEffect(Unit) {
        val allGranted = permissionsToRequest.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        viewModel.requestInitialAutoScan(allGranted)
    }

    val errorMessage = uiState.errorResId?.let { resId ->
        uiState.errorArg?.let { stringResource(resId, it) } ?: stringResource(resId)
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            viewModel.scan()
        } else {
            Toast.makeText(context, R.string.permission_denied, Toast.LENGTH_SHORT).show()
        }
    }

    val filteredList = remember(uiState.wifiList, bandFilter) {
        uiState.wifiList.filter(bandFilter.match)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = { showMenu = !showMenu }) {
                        Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.menu_more))
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_channel_analysis)) },
                            onClick = {
                                showMenu = false
                                onOpenChannelAnalysis()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_local_channels)) },
                            onClick = {
                                showMenu = false
                                showChannelInfoDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_settings)) },
                            onClick = {
                                showMenu = false
                                onOpenSettings()
                            }
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
        ) {
            Button(
                onClick = {
                    val allGranted = permissionsToRequest.all {
                        ContextCompat.checkSelfPermission(
                            context,
                            it
                        ) == PackageManager.PERMISSION_GRANTED
                    }
                    if (allGranted) {
                        viewModel.scan()
                    } else {
                        permissionLauncher.launch(permissionsToRequest)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isScanning
            ) {
                if (uiState.isScanning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    if (uiState.isScanning) stringResource(R.string.btn_scanning)
                    else stringResource(R.string.btn_start_scan)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            FilterChipRow(
                selected = bandFilter,
                onSelect = { viewModel.setBandFilter(it) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            val groupedList = remember(filteredList) {
                buildWifiGroups(filteredList)
            }
            val flatSorted = remember(filteredList, uiState.sortMode) {
                if (uiState.sortMode == SortMode.FLAT)
                    filteredList.sortedByDescending { it.rssi }
                else emptyList()
            }

            val filterLabel = if (bandFilter != BandFilter.ALL)
                stringResource(bandFilter.labelRes)
            else null

            val baseText = when (uiState.sortMode) {
                SortMode.GROUPED -> stringResource(
                    R.string.count_grouped, groupedList.size, filteredList.size
                )
                SortMode.FLAT -> stringResource(
                    R.string.count_flat, filteredList.size
                )
            }

            val countText = if (filterLabel != null)
                baseText + stringResource(R.string.count_filtered_suffix, filterLabel)
            else
                baseText

            Text(
                text = countText, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                when {
                    // 扫描中且没有旧数据：显示加载中
                    uiState.isScanning && filteredList.isEmpty() -> {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(12.dp))
                            Text(stringResource(R.string.list_scanning_hint), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    // 无数据：持久空状态提示
                    filteredList.isEmpty() -> {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("📡", fontSize = 40.sp)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                stringResource(R.string.list_empty_hint),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 16.sp
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                stringResource(R.string.list_empty_action),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp
                            )
                        }
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            when (uiState.sortMode) {
                                SortMode.GROUPED -> {
                                    groupedList.forEach { group ->
                                        if (group.count == 1) {
                                            item(key = "ap:${group.strongest.bssid}") {
                                                WifiCard(
                                                    group.strongest,
                                                    onClick = { onItemClick(group.strongest) }
                                                )
                                            }
                                        } else {
                                            item(key = "group:${group.key}") {
                                                WifiGroupHeader(
                                                    ssid = group.ssid,
                                                    count = group.count,
                                                    strongest = group.strongest,
                                                    expanded = group.key in uiState.expandedKeys,
                                                    onToggle = { viewModel.toggleGroup(group.key) },
                                                    onOpenDetail = onItemClick
                                                )
                                            }
                                            if (group.key in uiState.expandedKeys) {
                                                items(group.networks, key = { "ap:${it.bssid}" }) { wifi ->
                                                    WifiCard(wifi = wifi, onClick = { onItemClick(wifi) })
                                                }
                                            }
                                        }
                                    }
                                }

                                SortMode.FLAT -> {
                                    items(flatSorted, key = { "ap:${it.bssid}" }) { wifi ->
                                        WifiCard(wifi = wifi, onClick = { onItemClick(wifi) })
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showChannelInfoDialog) {
        ChannelInfoDialog(onDismiss = { showChannelInfoDialog = false })
    }
}
