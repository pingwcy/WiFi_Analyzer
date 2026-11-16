package top.pingwcy.wifianalyzer.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import top.pingwcy.wifianalyzer.model.BandFilter
import androidx.compose.ui.res.stringResource
import top.pingwcy.wifianalyzer.R

// 筛选按钮组件
@Composable
fun FilterChipRow(
    selected: BandFilter,
    onSelect: (BandFilter) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        BandFilter.entries.forEach { filter ->
            FilterChip(
                selected = selected == filter,
                onClick = { onSelect(filter) },
                label = { Text(stringResource(filter.labelRes)) }
            )
        }
    }
}
