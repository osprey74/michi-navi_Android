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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Beenhere
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.outlined.Beenhere
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.osprey74.michinavi.model.RoadsideStation
import java.security.MessageDigest

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StationDetailSheet(
    station: RoadsideStation,
    sheetState: SheetState,
    isFavorite: Boolean,
    isVisited: Boolean,
    onToggleFavorite: () -> Unit,
    onToggleVisited: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
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

            // 名前 + お気に入り
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                Text(
                    text = station.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onToggleVisited) {
                    Icon(
                        imageVector = if (isVisited) Icons.Filled.Beenhere else Icons.Outlined.Beenhere,
                        contentDescription = if (isVisited) "到達解除" else "到達登録",
                        tint = if (isVisited) Color(0xFF2196F3) else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (isFavorite) "お気に入り解除" else "お気に入り登録",
                        tint = if (isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 住所（都道府県 + 市町村）
            val address = listOfNotNull(station.prefecture, station.municipality).joinToString(" ")
            if (address.isNotBlank()) {
                Text(
                    text = address,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // 路線名
            station.roadName?.let { road ->
                Text(
                    text = road,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 施設アイコン
            if (station.features.isNotEmpty()) {
                Text(
                    text = "施設・設備",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    station.features.forEach { key ->
                        val feature = Feature.fromKey(key)
                        if (feature != null) {
                            SuggestionChip(
                                onClick = {},
                                label = { Text(feature.label) },
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // ボタン行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // 公式サイト
                station.url?.let { url ->
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("公式サイト")
                    }
                }

                // 地図で開く（Google Maps Intent）
                Button(
                    onClick = {
                        val uri = Uri.parse(
                            "google.navigation:q=${station.latitude},${station.longitude}"
                        )
                        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                            setPackage("com.google.android.apps.maps")
                        }
                        if (intent.resolveActivity(context.packageManager) != null) {
                            context.startActivity(intent)
                        } else {
                            // Google Maps がなければブラウザで開く
                            val browserUri = Uri.parse(
                                "https://www.google.com/maps/dir/?api=1&destination=${station.latitude},${station.longitude}"
                            )
                            context.startActivity(Intent(Intent.ACTION_VIEW, browserUri))
                        }
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("ナビ開始")
                }
            }
        }
    }
}

/**
 * Wikimedia Commons の Special:FilePath/ URL を upload.wikimedia.org の直接URLに変換する。
 * 例: https://commons.wikimedia.org/wiki/Special:FilePath/Example.jpg
 *   → https://upload.wikimedia.org/wikipedia/commons/a/ab/Example.jpg
 */
private fun toWikimediaDirectUrl(url: String): String {
    val prefix = "https://commons.wikimedia.org/wiki/Special:FilePath/"
    if (!url.startsWith(prefix)) return url

    val filename = url.removePrefix(prefix).replace(' ', '_')
    val md5 = MessageDigest.getInstance("MD5")
        .digest(filename.toByteArray())
        .joinToString("") { "%02x".format(it) }

    return "https://upload.wikimedia.org/wikipedia/commons/${md5[0]}/${md5[0]}${md5[1]}/$filename"
}
