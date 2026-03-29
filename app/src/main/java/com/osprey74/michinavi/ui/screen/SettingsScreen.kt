package com.osprey74.michinavi.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MapViewModel,
    onBack: () -> Unit,
) {
    val settings by viewModel.settings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("設定") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "戻る",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            // ズームボタン位置
            SectionHeader("ズームボタン位置")
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                SegmentedButton(
                    selected = settings.zoomPosition == "left",
                    onClick = {
                        viewModel.updateSettings(settings.copy(zoomPosition = "left"))
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                ) {
                    Text("左")
                }
                SegmentedButton(
                    selected = settings.zoomPosition == "right",
                    onClick = {
                        viewModel.updateSettings(settings.copy(zoomPosition = "right"))
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                ) {
                    Text("右")
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // POI表示
            SectionHeader("施設表示")

            PoiToggle(
                label = "ガソリンスタンド",
                checked = settings.showGasStations,
                onCheckedChange = {
                    viewModel.updateSettings(settings.copy(showGasStations = it))
                },
            )
            PoiToggle(
                label = "コンビニ",
                checked = settings.showFoodMarkets,
                onCheckedChange = {
                    viewModel.updateSettings(settings.copy(showFoodMarkets = it))
                },
            )
            PoiToggle(
                label = "レストラン",
                checked = settings.showRestaurants,
                onCheckedChange = {
                    viewModel.updateSettings(settings.copy(showRestaurants = it))
                },
            )
            PoiToggle(
                label = "駐車場",
                checked = settings.showParking,
                onCheckedChange = {
                    viewModel.updateSettings(settings.copy(showParking = it))
                },
            )
            PoiToggle(
                label = "RVパーク",
                checked = settings.showRvParks,
                onCheckedChange = {
                    viewModel.updateSettings(settings.copy(showRvParks = it))
                },
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun PoiToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    ListItem(
        headlineContent = { Text(label) },
        trailingContent = {
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        },
    )
}
