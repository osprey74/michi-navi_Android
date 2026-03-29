package com.osprey74.michinavi.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.osprey74.michinavi.model.RoadsideStation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StationPickerScreen(
    viewModel: MapViewModel,
    onStationSelected: (RoadsideStation) -> Unit,
    onBack: () -> Unit,
) {
    val grouped = remember { viewModel.stationsGroupedByPrefecture() }

    var selectedPrefecture by remember { mutableStateOf<String?>(null) }
    var selectedMunicipality by remember { mutableStateOf<String?>(null) }

    val title = when {
        selectedMunicipality != null -> selectedMunicipality!!
        selectedPrefecture != null -> selectedPrefecture!!
        else -> "道の駅を選択"
    }

    val onNavigateBack: () -> Unit = when {
        selectedMunicipality != null -> {
            { selectedMunicipality = null }
        }
        selectedPrefecture != null -> {
            { selectedPrefecture = null }
        }
        else -> onBack
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "戻る",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        when {
            // 第3階層: 駅選択
            selectedPrefecture != null && selectedMunicipality != null -> {
                val stations = grouped[selectedPrefecture]
                    ?.get(selectedMunicipality)
                    ?: emptyList()
                StationList(
                    stations = stations,
                    onStationClick = onStationSelected,
                    modifier = Modifier.padding(innerPadding),
                )
            }
            // 第2階層: 市町村選択
            selectedPrefecture != null -> {
                val municipalities = grouped[selectedPrefecture]?.keys?.toList() ?: emptyList()
                NavigationList(
                    items = municipalities,
                    onItemClick = { selectedMunicipality = it },
                    modifier = Modifier.padding(innerPadding),
                )
            }
            // 第1階層: 都道府県選択
            else -> {
                val prefectures = grouped.keys.toList()
                NavigationList(
                    items = prefectures,
                    onItemClick = { selectedPrefecture = it },
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }
    }
}

@Composable
private fun NavigationList(
    items: List<String>,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(items) { item ->
            ListItem(
                headlineContent = { Text(item) },
                trailingContent = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                    )
                },
                modifier = Modifier.clickable { onItemClick(item) },
            )
        }
    }
}

@Composable
private fun StationList(
    stations: List<RoadsideStation>,
    onStationClick: (RoadsideStation) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(stations, key = { it.id }) { station ->
            ListItem(
                headlineContent = { Text(station.name) },
                supportingContent = {
                    station.roadName?.let { Text(it) }
                },
                modifier = Modifier.clickable { onStationClick(station) },
            )
        }
    }
}
