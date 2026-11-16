package top.pingwcy.wifianalyzer.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import top.pingwcy.wifianalyzer.model.BandFilter
import top.pingwcy.wifianalyzer.model.WifiData
import top.pingwcy.wifianalyzer.ui.WifiViewModel
import top.pingwcy.wifianalyzer.ui.component.FilterChipRow
import top.pingwcy.wifianalyzer.ui.component.WifiCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WifiScannerScreen(
    // 删掉了原来的 onListChange 和 onScanningChange，因为 ViewModel 接管了
    onItemClick: (WifiData) -> Unit,
    onOpenChannelAnalysis: () -> Unit,
    // 获取 ViewModel 实例
    viewModel: WifiViewModel = viewModel()
) {
    val context = LocalContext.current

    // 订阅 ViewModel 的状态流
    val uiState by viewModel.uiState.collectAsState()

    var bandFilter by remember { mutableStateOf(BandFilter.ALL) }

    val permissionsToRequest = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.NEARBY_WIFI_DEVICES
        else
            Manifest.permission.ACCESS_FINE_LOCATION
    )

    // 处理 ViewModel 发过来的报错信息
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            // 直接让 ViewModel 去扫描
            viewModel.scan(context)
        } else {
            Toast.makeText(context, "你拒绝了权限，无法扫描", Toast.LENGTH_SHORT).show()
        }
    }

    // 过滤列表
    val filteredList = remember(uiState.wifiList, bandFilter) {
        uiState.wifiList.filter(bandFilter.match)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("WiFi 扫描器") },
                actions = {
                    TextButton(onClick = onOpenChannelAnalysis) {
                        Text("信道分析")
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
                        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
                    }
                    if (allGranted) {
                        // 直接让 ViewModel 去扫描
                        viewModel.scan(context)
                    } else {
                        permissionLauncher.launch(permissionsToRequest)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isScanning // 用 ViewModel 里的状态
            ) {
                Text(if (uiState.isScanning) "扫描中..." else "开始扫描 WiFi")
            }

            Spacer(modifier = Modifier.height(12.dp))

            FilterChipRow(
                selected = bandFilter,
                onSelect = { bandFilter = it }
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "共 ${filteredList.size} 个网络" +
                        if (bandFilter != BandFilter.ALL) "（已筛选：${bandFilter.label}）" else "",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filteredList) { wifi ->
                    WifiCard(wifi = wifi, onClick = { onItemClick(wifi) })
                }
            }
        }
    }
}