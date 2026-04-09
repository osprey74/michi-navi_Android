package com.osprey74.michinavi.ui.screen

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Beenhere
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.outlined.Beenhere
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.osprey74.michinavi.model.Feature
import com.osprey74.michinavi.model.NearbyStation
import com.osprey74.michinavi.model.RoadsideStation
import java.security.MessageDigest

/** 地図上のマーカータップ時に使う BottomSheet 版 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StationDetailSheet(
    station: RoadsideStation,
    sheetState: SheetState,
    isFavorite: Boolean,
    isVisited: Boolean,
    nearby: NearbyStation? = null,
    onToggleFavorite: () -> Unit,
    onToggleVisited: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        StationDetailContent(
            station = station,
            isFavorite = isFavorite,
            isVisited = isVisited,
            nearby = nearby,
            onToggleFavorite = onToggleFavorite,
            onToggleVisited = onToggleVisited,
            onShowOnMap = null,
        )
    }
}

/** リスト内インライン表示・Sheet 両方で使える詳細コンテンツ */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StationDetailContent(
    station: RoadsideStation,
    isFavorite: Boolean,
    isVisited: Boolean,
    nearby: NearbyStation? = null,
    onToggleFavorite: () -> Unit,
    onToggleVisited: () -> Unit,
    onShowOnMap: (() -> Unit)?,
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 32.dp),
    ) {
        // 施設写真
        station.imageUrl?.let { url ->
            val directUrl = toWikimediaDirectUrl(url)
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(directUrl)
                    .addHeader("User-Agent", "MichiNavi/1.0 (Android)")
                    .crossfade(true)
                    .build(),
                contentDescription = station.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop,
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 名前
        Text(
            text = station.name,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(12.dp))

        // お気に入り・踏破ボタン
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = onToggleFavorite,
                modifier = Modifier.weight(1f),
                colors = if (isFavorite) {
                    ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                } else {
                    ButtonDefaults.outlinedButtonColors()
                },
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    tint = if (isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(if (isFavorite) "お気に入り済み" else "お気に入り")
            }
            OutlinedButton(
                onClick = onToggleVisited,
                modifier = Modifier.weight(1f),
                colors = if (isVisited) {
                    ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2196F3))
                } else {
                    ButtonDefaults.outlinedButtonColors()
                },
            ) {
                Icon(
                    imageVector = if (isVisited) Icons.Filled.Beenhere else Icons.Outlined.Beenhere,
                    contentDescription = null,
                    tint = if (isVisited) Color(0xFF2196F3) else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text("踏破済み")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(12.dp))

        // 基本情報
        nearby?.let { ns ->
            InfoRow("距離", ns.distanceText)
            InfoRow("方角", ns.cardinalDirection)
        }
        station.roadName?.let { road ->
            InfoRow("路線", road)
        }
        val address = listOfNotNull(station.prefecture, station.municipality).joinToString(" ")
        if (address.isNotBlank()) {
            InfoRow("所在地", address)
        }

        // 施設・設備
        if (station.features.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            Text(text = "施設・設備", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                station.features.forEach { key ->
                    val feature = Feature.fromKey(key)
                    if (feature != null) {
                        SuggestionChip(onClick = {}, label = { Text(feature.label) })
                    }
                }
            }
        }

        // フォトアルバム
        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(12.dp))

        Text(text = "フォトアルバム", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        StationPhotoAlbum(stationId = station.id)

        // 公式サイト・ナビボタン
        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(12.dp))

        station.url?.let { url ->
            OutlinedButton(
                onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("公式サイトを開く")
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        Button(
            onClick = {
                val uri = Uri.parse("google.navigation:q=${station.latitude},${station.longitude}")
                val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                    setPackage("com.google.android.apps.maps")
                }
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                } else {
                    val browserUri = Uri.parse(
                        "https://www.google.com/maps/dir/?api=1&destination=${station.latitude},${station.longitude}",
                    )
                    context.startActivity(Intent(Intent.ACTION_VIEW, browserUri))
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("この道の駅へナビ開始")
        }

        // 地図で見る（リスト内表示時のみ）
        onShowOnMap?.let { action ->
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(onClick = action, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Map, contentDescription = null)
                Spacer(modifier = Modifier.height(4.dp))
                Text("地図で見る")
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun toWikimediaDirectUrl(url: String): String {
    val prefix = "https://commons.wikimedia.org/wiki/Special:FilePath/"
    if (!url.startsWith(prefix)) return url
    val filename = url.removePrefix(prefix).replace(' ', '_')
    val md5 = MessageDigest.getInstance("MD5")
        .digest(filename.toByteArray())
        .joinToString("") { "%02x".format(it) }
    return "https://upload.wikimedia.org/wikipedia/commons/${md5[0]}/${md5[0]}${md5[1]}/$filename"
}
