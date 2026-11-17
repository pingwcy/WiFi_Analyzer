package top.pingwcy.wifianalyzer.ui.screen

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.pingwcy.wifianalyzer.R
import top.pingwcy.wifianalyzer.domain.AppLanguage
import top.pingwcy.wifianalyzer.ui.SortMode
import top.pingwcy.wifianalyzer.ui.WifiViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: WifiViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val activity = LocalActivity.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
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
            // 自动扫描开关（整行可点击）
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.setAutoScanEnabled(!uiState.autoScanEnabled) }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.settings_auto_scan_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        stringResource(R.string.settings_auto_scan_desc),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = uiState.autoScanEnabled,
                    onCheckedChange = viewModel::setAutoScanEnabled
                )
            }

            HorizontalDivider()

            // 语言选择
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.settings_language_title),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(4.dp))
            AppLanguage.entries.forEach { language ->
                val isSelected = uiState.appLanguage == language
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (!isSelected) {
                                viewModel.setAppLanguage(language)
                                // 触发 Activity 重建，使新语言立即生效
                                activity?.recreate()
                            }
                        }
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = {
                            if (!isSelected) {
                                viewModel.setAppLanguage(language)
                                activity?.recreate()
                            }
                        }
                    )
                    Text(
                        languageLabel(language),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }

            HorizontalDivider()

            // 默认视图模式
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.settings_view_mode_title),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = uiState.sortMode == SortMode.GROUPED,
                    onClick = { viewModel.setSortMode(SortMode.GROUPED) },
                    label = { Text(stringResource(R.string.settings_view_mode_grouped)) }
                )
                FilterChip(
                    selected = uiState.sortMode == SortMode.FLAT,
                    onClick = { viewModel.setSortMode(SortMode.FLAT) },
                    label = { Text(stringResource(R.string.settings_view_mode_flat)) }
                )
            }
        }
    }
}

@Composable
private fun languageLabel(language: AppLanguage): String = when (language) {
    AppLanguage.SYSTEM -> stringResource(R.string.settings_language_system)
    AppLanguage.CHINESE -> stringResource(R.string.settings_language_chinese)
    AppLanguage.ENGLISH -> stringResource(R.string.settings_language_english)
}
