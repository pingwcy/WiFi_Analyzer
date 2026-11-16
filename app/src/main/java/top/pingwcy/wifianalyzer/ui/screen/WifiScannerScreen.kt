package top.pingwcy.wifianalyzer.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import top.pingwcy.wifianalyzer.ui.component.FilterChipRow
import top.pingwcy.wifianalyzer.ui.component.WifiCard
import top.pingwcy.wifianalyzer.data.WifiScanner
import top.pingwcy.wifianalyzer.model.BandFilter
import top.pingwcy.wifianalyzer.model.WifiData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WifiScannerScreen(
    wifiList: List<WifiData>,
    isScanning: Boolean,
    onListChange: (List<WifiData>) -> Unit,
    onScanningChange: (Boolean) -> Unit,
    onItemClick: (WifiData) -> Unit,
    onOpenChannelAnalysis: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    //    筛选状态（放这里即可，切换详情页回来时由 remember 保留在组合中；
    //   若想跨页面保留，可提升到 WifiScannerApp，见下方说明）
    var bandFilter by remember { mutableStateOf(BandFilter.ALL) }

    val permissionsToRequest = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.NEARBY_WIFI_DEVICES
        else
            Manifest.permission.ACCESS_FINE_LOCATION
    )

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            scope.launch {
                onScanningChange(true)
                try {
                    val scanner = WifiScanner(context)
                    val results = scanner.scan()
                    onListChange(results)
                    if (results.isEmpty()) {
                        Toast.makeText(context, "未扫描到 WiFi，请检查 GPS 定位开关是否打开", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "扫描失败：${e.message}", Toast.LENGTH_LONG).show()
                    onListChange(emptyList())
                } finally {
                    onScanningChange(false)
                }
            } // 结束 scope.launch
        } else {
            Toast.makeText(context, "你拒绝了权限，无法扫描", Toast.LENGTH_SHORT).show()
            onScanningChange(false)
        }
    } // 结束 permissionLauncher

        // 按频段过滤（每次重组重新计算，列表很小，性能无压力）
        val filteredList = remember(wifiList, bandFilter) {
            wifiList.filter(bandFilter.match)
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
                )        }
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
                            scope.launch {
                                onScanningChange(true)
                                try {
                                    val scanner = WifiScanner(context)
                                    val results = scanner.scan()
                                    onListChange(results)
                                    if (results.isEmpty()) {
                                        Toast.makeText(context, "未扫描到 WiFi，请检查 GPS 定位开关是否打开", Toast.LENGTH_LONG).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "扫描失败：${e.message}", Toast.LENGTH_LONG).show()
                                    onListChange(emptyList())
                                } finally {
                                    onScanningChange(false)
                                }
                            }
                        } else {
                            permissionLauncher.launch(permissionsToRequest)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isScanning
                ) {
                    Text(if (isScanning) "扫描中..." else "开始扫描 WiFi")
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ★ 新增：筛选按钮行
                FilterChipRow(
                    selected = bandFilter,
                    onSelect = { bandFilter = it }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // ★ 新增：数量提示（可选，体验更好）
                Text(
                    text = "共 ${filteredList.size} 个网络" +
                            if (bandFilter != BandFilter.ALL) "（已筛选：${bandFilter.label}）" else "",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // ★ 改成 filteredList
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filteredList) { wifi ->
                        WifiCard(wifi = wifi, onClick = { onItemClick(wifi) })
                    }
                }
            }
        }
    }
