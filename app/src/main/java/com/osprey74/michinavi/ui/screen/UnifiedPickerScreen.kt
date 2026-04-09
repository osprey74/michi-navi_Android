package com.osprey74.michinavi.ui.screen

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Beenhere
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Signpost
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.osprey74.michinavi.model.CountrySign
import com.osprey74.michinavi.model.RoadsideStation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedPickerScreen(
    viewModel: MapViewModel,
    onStationSelected: (RoadsideStation) -> Unit,
    onSignSelected: (CountrySign) -> Unit,
    onCloseToMapStation: (RoadsideStation) -> Unit,
    onCloseToMapSign: (CountrySign) -> Unit,
    onOpenRandomDraw: () -> Unit,
    onBack: () -> Unit,
) {
    val showCountrySignMarkers by viewModel.showCountrySignMarkers.collectAsState()
    var segment by remember { mutableIntStateOf(0) }

    // 道の駅 state
    val stationGrouped = remember { viewModel.stationsGroupedByPrefecture() }
    val favoriteIds by viewModel.favoriteIds.collectAsState()
    val visitedIds by viewModel.visitedIds.collectAsState()
    var stationTab by remember { mutableIntStateOf(0) }
    var selectedPrefecture by remember { mutableStateOf<String?>(null) }
    var selectedMunicipality by remember { mutableStateOf<String?>(null) }
    // 道の駅インライン詳細
    var detailStation by remember { mutableStateOf<RoadsideStation?>(null) }

    // CS state
    val favoriteSignIds by viewModel.favoriteSignIds.collectAsState()
    val visitedSignIds by viewModel.visitedSignIds.collectAsState()
    var signTab by remember { mutableIntStateOf(0) }
    var selectedSubprefecture by remember { mutableStateOf<String?>(null) }
    // CSインライン詳細
    var detailSign by remember { mutableStateOf<CountrySign?>(null) }

    // タイトル
    val title = if (detailStation != null) {
        detailStation!!.name
    } else if (detailSign != null) {
        detailSign!!.name
    } else if (segment == 0) {
        when {
            selectedMunicipality != null -> selectedMunicipality!!
            selectedPrefecture != null -> selectedPrefecture!!
            else -> "リスト"
        }
    } else {
        when {
            selectedSubprefecture != null -> selectedSubprefecture!!
            else -> "リスト"
        }
    }

    // 戻る処理
    val onNavigateBack: () -> Unit = when {
        detailStation != null -> { { detailStation = null } }
        detailSign != null -> { { detailSign = null } }
        segment == 0 && selectedMunicipality != null -> { { selectedMunicipality = null } }
        segment == 0 && selectedPrefecture != null -> { { selectedPrefecture = null } }
        segment == 1 && selectedSubprefecture != null -> { { selectedSubprefecture = null } }
        else -> onBack
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "戻る")
                    }
                },
                actions = {
                    if (detailStation != null) {
                        IconButton(onClick = { onCloseToMapStation(detailStation!!) }) {
                            Icon(Icons.Default.Close, "閉じる")
                        }
                    } else if (detailSign != null) {
                        IconButton(onClick = { onCloseToMapSign(detailSign!!) }) {
                            Icon(Icons.Default.Close, "閉じる")
                        }
                    } else if (segment == 1 && showCountrySignMarkers) {
                        IconButton(onClick = onOpenRandomDraw) {
                            Icon(Icons.Default.Style, "ランダムカード")
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            // 詳細表示中はセグメント/タブを非表示
            if (detailStation != null) {
                // 道の駅インライン詳細
                val station = detailStation!!
                StationDetailContent(
                    station = station,
                    isFavorite = station.id in favoriteIds,
                    isVisited = station.id in visitedIds,
                    onToggleFavorite = { viewModel.toggleFavorite(station.id) },
                    onToggleVisited = { viewModel.toggleVisited(station.id) },
                    onShowOnMap = {
                        onStationSelected(station)
                    },
                )
            } else if (detailSign != null) {
                // CSインライン詳細
                val sign = detailSign!!
                CountrySignDetailContent(
                    sign = sign,
                    isFavorite = sign.id in favoriteSignIds,
                    isVisited = sign.id in visitedSignIds,
                    onToggleFavorite = { viewModel.toggleFavoriteSign(sign.id) },
                    onToggleVisited = { viewModel.toggleVisitedSign(sign.id) },
                    onShowOnMap = {
                        onSignSelected(sign)
                    },
                )
            } else {
                // セグメント切替
                if (showCountrySignMarkers) {
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        SegmentedButton(
                            selected = segment == 0,
                            onClick = { segment = 0 },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                            icon = { Icon(Icons.Default.Map, null, modifier = Modifier.size(18.dp)) },
                        ) { Text("道の駅") }
                        SegmentedButton(
                            selected = segment == 1,
                            onClick = { segment = 1 },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                            icon = { Icon(Icons.Default.Signpost, null, modifier = Modifier.size(18.dp)) },
                        ) { Text("カントリーサイン") }
                    }
                }

                if (segment == 0) {
                    StationTabContent(
                        viewModel = viewModel,
                        grouped = stationGrouped,
                        favoriteIds = favoriteIds,
                        visitedIds = visitedIds,
                        selectedTab = stationTab,
                        onTabChange = { stationTab = it },
                        selectedPrefecture = selectedPrefecture,
                        onPrefectureSelected = { selectedPrefecture = it },
                        selectedMunicipality = selectedMunicipality,
                        onMunicipalitySelected = { selectedMunicipality = it },
                        onStationSelected = { detailStation = it },
                    )
                } else {
                    SignTabContent(
                        viewModel = viewModel,
                        favoriteSignIds = favoriteSignIds,
                        visitedSignIds = visitedSignIds,
                        selectedTab = signTab,
                        onTabChange = { signTab = it },
                        selectedSubprefecture = selectedSubprefecture,
                        onSubprefectureSelected = { selectedSubprefecture = it },
                        onSignSelected = { detailSign = it },
                    )
                }
            }
        }
    }
}

// ==================== 道の駅タブコンテンツ ====================

@Composable
private fun StationTabContent(
    viewModel: MapViewModel,
    grouped: Map<String, Map<String, List<RoadsideStation>>>,
    favoriteIds: Set<String>,
    visitedIds: Set<String>,
    selectedTab: Int,
    onTabChange: (Int) -> Unit,
    selectedPrefecture: String?,
    onPrefectureSelected: (String?) -> Unit,
    selectedMunicipality: String?,
    onMunicipalitySelected: (String?) -> Unit,
    onStationSelected: (RoadsideStation) -> Unit,
) {
    TabRow(selectedTabIndex = selectedTab) {
        Tab(selected = selectedTab == 0, onClick = { onTabChange(0) }, text = { Text("一覧") })
        Tab(
            selected = selectedTab == 1, onClick = { onTabChange(1) },
            icon = { Icon(Icons.Default.Favorite, null) }, text = { Text("お気に入り") },
        )
        Tab(
            selected = selectedTab == 2, onClick = { onTabChange(2) },
            icon = { Icon(Icons.Default.Beenhere, null, tint = Color(0xFF2196F3)) }, text = { Text("踏破済み") },
        )
    }

    when (selectedTab) {
        0 -> {
            when {
                selectedPrefecture != null && selectedMunicipality != null -> {
                    val stations = grouped[selectedPrefecture]?.get(selectedMunicipality) ?: emptyList()
                    StationListSection(stations = stations, onStationClick = onStationSelected)
                }
                selectedPrefecture != null -> {
                    val municipalities = grouped[selectedPrefecture]?.keys?.toList() ?: emptyList()
                    NavigationList(items = municipalities, onItemClick = { onMunicipalitySelected(it) })
                }
                else -> {
                    val prefectures = grouped.keys.toList()
                    NavigationList(items = prefectures, onItemClick = { onPrefectureSelected(it) })
                }
            }
        }
        1 -> {
            val favorites = remember(favoriteIds) { viewModel.getFavoriteStations() }
            if (favorites.isEmpty()) SimpleEmptyState("お気に入りの道の駅はまだありません")
            else StationListSection(stations = favorites, onStationClick = onStationSelected)
        }
        2 -> {
            val visited = remember(visitedIds) { viewModel.getVisitedStations() }
            if (visited.isEmpty()) SimpleEmptyState("踏破した道の駅はまだありません")
            else StationListSection(stations = visited, onStationClick = onStationSelected)
        }
    }
}

// ==================== カントリーサインタブコンテンツ ====================

@Composable
private fun SignTabContent(
    viewModel: MapViewModel,
    favoriteSignIds: Set<String>,
    visitedSignIds: Set<String>,
    selectedTab: Int,
    onTabChange: (Int) -> Unit,
    selectedSubprefecture: String?,
    onSubprefectureSelected: (String?) -> Unit,
    onSignSelected: (CountrySign) -> Unit,
) {
    TabRow(selectedTabIndex = selectedTab) {
        Tab(selected = selectedTab == 0, onClick = { onTabChange(0) }, text = { Text("すべて") }, icon = { Icon(Icons.Default.Map, null) })
        Tab(selected = selectedTab == 1, onClick = { onTabChange(1) }, text = { Text("お気に入り") }, icon = { Icon(Icons.Default.Favorite, null) })
        Tab(selected = selectedTab == 2, onClick = { onTabChange(2) }, text = { Text("踏破済み") }, icon = { Icon(Icons.Default.Beenhere, null) })
    }

    when (selectedTab) {
        0 -> {
            val grouped = remember { viewModel.signsGroupedBySubprefecture() }
            if (selectedSubprefecture == null) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(grouped.keys.toList()) { sub ->
                        val count = grouped[sub]?.size ?: 0
                        ListItem(
                            headlineContent = { Text(sub) },
                            supportingContent = { Text("$count 市町村") },
                            trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
                            modifier = Modifier.clickable { onSubprefectureSelected(sub) },
                        )
                    }
                }
            } else {
                val signs = grouped[selectedSubprefecture] ?: emptyList()
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(signs) { sign ->
                        SignListRow(sign = sign, isFavorite = sign.id in favoriteSignIds, isVisited = sign.id in visitedSignIds, onClick = { onSignSelected(sign) })
                    }
                }
            }
        }
        1 -> {
            val favorites = remember(favoriteSignIds) { viewModel.getFavoriteCountrySigns() }
            if (favorites.isEmpty()) EmptyStateWithIcon(icon = Icons.Default.Favorite, title = "お気に入りなし", message = "詳細画面でハートボタンをタップすると追加されます")
            else SignListSection(signs = favorites, favoriteIds = favoriteSignIds, visitedIds = visitedSignIds, onSignSelected = onSignSelected)
        }
        2 -> {
            val visited = remember(visitedSignIds) { viewModel.getVisitedCountrySigns() }
            if (visited.isEmpty()) EmptyStateWithIcon(icon = Icons.Default.Beenhere, title = "踏破記録なし", message = "詳細画面でチェックボタンをタップすると追加されます")
            else SignListSection(signs = visited, favoriteIds = favoriteSignIds, visitedIds = visitedSignIds, onSignSelected = onSignSelected)
        }
    }
}

// ==================== 共通コンポーネント ====================

@Composable
private fun SignListRow(sign: CountrySign, isFavorite: Boolean, isVisited: Boolean, onClick: () -> Unit) {
    val context = LocalContext.current
    val thumbnail = remember(sign.imageName) {
        sign.imageName?.let { name ->
            try { context.assets.open("country_signs/$name.jpg").use { BitmapFactory.decodeStream(it) } } catch (_: Exception) { null }
        }
    }
    ListItem(
        headlineContent = { Text(sign.name) },
        supportingContent = { Text("${sign.subprefectureOffice} / ${sign.municipalityType}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        leadingContent = {
            if (thumbnail != null) {
                Image(bitmap = thumbnail.asImageBitmap(), contentDescription = sign.name, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(6.dp)), contentScale = ContentScale.Fit)
            } else {
                Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Image, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        trailingContent = {
            if (isFavorite || isVisited) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (isFavorite) Icon(Icons.Default.Favorite, "お気に入り", modifier = Modifier.size(18.dp), tint = Color(0xFFF44336))
                    if (isVisited) Icon(Icons.Default.Beenhere, "踏破済み", modifier = Modifier.size(18.dp), tint = Color(0xFF2196F3))
                }
            }
        },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
private fun SignListSection(signs: List<CountrySign>, favoriteIds: Set<String>, visitedIds: Set<String>, onSignSelected: (CountrySign) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(signs) { sign ->
            SignListRow(sign = sign, isFavorite = sign.id in favoriteIds, isVisited = sign.id in visitedIds, onClick = { onSignSelected(sign) })
        }
    }
}

@Composable
private fun NavigationList(items: List<String>, onItemClick: (String) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(items) { item ->
            ListItem(
                headlineContent = { Text(item) },
                trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
                modifier = Modifier.clickable { onItemClick(item) },
            )
        }
    }
}

@Composable
private fun StationListSection(stations: List<RoadsideStation>, onStationClick: (RoadsideStation) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(stations, key = { it.id }) { station ->
            ListItem(
                headlineContent = { Text(station.name) },
                supportingContent = { station.roadName?.let { Text(it) } },
                modifier = Modifier.clickable { onStationClick(station) },
            )
        }
    }
}

@Composable
private fun SimpleEmptyState(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun EmptyStateWithIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, message: String) {
    Column(modifier = Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 16.dp))
        Text(text = message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
    }
}
