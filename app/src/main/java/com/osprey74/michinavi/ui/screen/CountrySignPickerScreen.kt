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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Beenhere
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountrySignPickerScreen(
    viewModel: MapViewModel,
    onSignSelected: (CountrySign) -> Unit,
    onBack: () -> Unit,
) {
    val favoriteSignIds by viewModel.favoriteSignIds.collectAsState()
    val visitedSignIds by viewModel.visitedSignIds.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    var selectedSubprefecture by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when {
                            selectedTab == 0 && selectedSubprefecture != null -> selectedSubprefecture!!
                            else -> "カントリーサイン"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (selectedTab == 0 && selectedSubprefecture != null) {
                            selectedSubprefecture = null
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "戻る")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0; selectedSubprefecture = null },
                    text = { Text("すべて") },
                    icon = { Icon(Icons.Default.Map, null) },
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("お気に入り") },
                    icon = { Icon(Icons.Default.Favorite, null) },
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("踏破済み") },
                    icon = { Icon(Icons.Default.Beenhere, null) },
                )
            }

            when (selectedTab) {
                0 -> {
                    val grouped = remember { viewModel.signsGroupedBySubprefecture() }
                    if (selectedSubprefecture == null) {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(grouped.keys.toList()) { subprefecture ->
                                val count = grouped[subprefecture]?.size ?: 0
                                ListItem(
                                    headlineContent = { Text(subprefecture) },
                                    supportingContent = { Text("$count 市町村") },
                                    modifier = Modifier.clickable {
                                        selectedSubprefecture = subprefecture
                                    },
                                )
                            }
                        }
                    } else {
                        val signs = grouped[selectedSubprefecture] ?: emptyList()
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(signs) { sign ->
                                SignListRow(
                                    sign = sign,
                                    isFavorite = sign.id in favoriteSignIds,
                                    isVisited = sign.id in visitedSignIds,
                                    onClick = { onSignSelected(sign) },
                                )
                            }
                        }
                    }
                }
                1 -> {
                    val favorites = remember(favoriteSignIds) {
                        viewModel.getFavoriteCountrySigns()
                    }
                    if (favorites.isEmpty()) {
                        EmptyState(
                            icon = Icons.Default.Favorite,
                            title = "お気に入りなし",
                            message = "詳細画面でハートボタンをタップすると追加されます",
                        )
                    } else {
                        SignList(
                            signs = favorites,
                            favoriteIds = favoriteSignIds,
                            visitedIds = visitedSignIds,
                            onSignSelected = onSignSelected,
                        )
                    }
                }
                2 -> {
                    val visited = remember(visitedSignIds) {
                        viewModel.getVisitedCountrySigns()
                    }
                    if (visited.isEmpty()) {
                        EmptyState(
                            icon = Icons.Default.Beenhere,
                            title = "踏破記録なし",
                            message = "詳細画面でチェックボタンをタップすると追加されます",
                        )
                    } else {
                        SignList(
                            signs = visited,
                            favoriteIds = favoriteSignIds,
                            visitedIds = visitedSignIds,
                            onSignSelected = onSignSelected,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SignListRow(
    sign: CountrySign,
    isFavorite: Boolean,
    isVisited: Boolean,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val thumbnail = remember(sign.imageName) {
        sign.imageName?.let { name ->
            try {
                context.assets.open("country_signs/$name.jpg").use {
                    BitmapFactory.decodeStream(it)
                }
            } catch (_: Exception) { null }
        }
    }

    ListItem(
        headlineContent = { Text(sign.name) },
        supportingContent = {
            Text(
                "${sign.subprefectureOffice} / ${sign.municipalityType}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        leadingContent = {
            if (thumbnail != null) {
                Image(
                    bitmap = thumbnail.asImageBitmap(),
                    contentDescription = sign.name,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    contentScale = ContentScale.Fit,
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.Image, null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        trailingContent = {
            if (isFavorite || isVisited) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (isFavorite) {
                        Icon(
                            Icons.Default.Favorite, "お気に入り",
                            modifier = Modifier.size(18.dp),
                            tint = Color(0xFFF44336),
                        )
                    }
                    if (isVisited) {
                        Icon(
                            Icons.Default.Beenhere, "踏破済み",
                            modifier = Modifier.size(18.dp),
                            tint = Color(0xFF2196F3),
                        )
                    }
                }
            }
        },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
private fun SignList(
    signs: List<CountrySign>,
    favoriteIds: Set<String>,
    visitedIds: Set<String>,
    onSignSelected: (CountrySign) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(signs) { sign ->
            SignListRow(
                sign = sign,
                isFavorite = sign.id in favoriteIds,
                isVisited = sign.id in visitedIds,
                onClick = { onSignSelected(sign) },
            )
        }
    }
}

@Composable
private fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    message: String,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}
