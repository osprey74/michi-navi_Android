package com.osprey74.michinavi.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
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
            // 地図タイル
            SectionHeader("地図タイル")
            val baseTileOptions = listOf(
                "gsi_pale" to "国土地理院 淡色",
                "gsi_std" to "国土地理院 標準",
                "gsi_photo" to "国土地理院 航空写真",
                "openfreemap" to "OpenFreeMap",
            )
            val tileOptions = if (settings.googleMapsApiKey.isNotEmpty()) {
                baseTileOptions + ("google_maps" to "Google Maps")
            } else {
                baseTileOptions
            }
            tileOptions.forEach { (key, label) ->
                ListItem(
                    headlineContent = { Text(label) },
                    leadingContent = {
                        RadioButton(
                            selected = settings.mapTileType == key,
                            onClick = {
                                viewModel.updateSettings(settings.copy(mapTileType = key))
                            },
                        )
                    },
                    modifier = Modifier.padding(start = 0.dp),
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Google Maps API キー
            SectionHeader("Google Maps API キー")
            Text(
                text = "Google Maps の地図タイルを利用する場合に設定してください。APIキーを登録すると「地図タイル」にGoogle Mapsが追加されます。Map Tiles API を有効にしたキーが必要です。\n\nこれは高度な設定であり、サポート対象外です。APIキーの登録・管理はすべて自己責任で行ってください。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )

            var apiKeyInput by remember(settings.googleMapsApiKey) {
                mutableStateOf(settings.googleMapsApiKey)
            }
            var showApiKey by remember { mutableStateOf(false) }
            val isModified = apiKeyInput != settings.googleMapsApiKey

            OutlinedTextField(
                value = apiKeyInput,
                onValueChange = { apiKeyInput = it.trim() },
                label = { Text("APIキー") },
                placeholder = { Text("AIza...") },
                singleLine = true,
                visualTransformation = if (showApiKey) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                trailingIcon = {
                    IconButton(onClick = { showApiKey = !showApiKey }) {
                        Text(
                            text = if (showApiKey) "隠す" else "表示",
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )

            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilledTonalButton(
                    onClick = {
                        viewModel.updateSettings(settings.copy(googleMapsApiKey = apiKeyInput))
                    },
                    enabled = isModified,
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 4.dp),
                    )
                    Text("保存")
                }
                Spacer(modifier = Modifier.width(8.dp))
                if (settings.googleMapsApiKey.isNotEmpty()) {
                    FilledTonalButton(
                        onClick = {
                            apiKeyInput = ""
                            // Google Mapsタイル使用中なら、キー削除時にデフォルトに戻す
                            val newTileType = if (settings.mapTileType == "google_maps") "gsi_pale" else settings.mapTileType
                            viewModel.updateSettings(settings.copy(
                                googleMapsApiKey = "",
                                mapTileType = newTileType,
                            ))
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 4.dp),
                        )
                        Text("削除")
                    }
                }
            }

            if (settings.googleMapsApiKey.isNotEmpty()) {
                Text(
                    text = "登録済み",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // クレジット
            SectionHeader("クレジット")

            Text(
                text = "Michi-Navi v1.0.0",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
            )
            Text(
                text = "© 2026 osprey74",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
            )

            Spacer(modifier = Modifier.padding(bottom = 8.dp))

            Text(
                text = "道の駅データ出典：",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
            )
            Text(
                text = "一般社団法人 全国道の駅連絡会",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
            )
            val uriHandler = LocalUriHandler.current
            val michiNoEkiLink = buildAnnotatedString {
                pushStringAnnotation(tag = "URL", annotation = "http://www.michi-no-eki.jp/")
                withStyle(
                    SpanStyle(
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline,
                    ),
                ) {
                    append("http://www.michi-no-eki.jp/")
                }
                pop()
            }
            ClickableText(
                text = michiNoEkiLink,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                onClick = { offset ->
                    michiNoEkiLink.getStringAnnotations("URL", offset, offset)
                        .firstOrNull()?.let { uriHandler.openUri(it.item) }
                },
            )

            Spacer(modifier = Modifier.padding(bottom = 8.dp))

            val flaticonLink = buildAnnotatedString {
                pushStringAnnotation(tag = "URL", annotation = "https://www.flaticon.com/free-icons/navigation")
                withStyle(
                    SpanStyle(
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline,
                    ),
                ) {
                    append("Navigation icons created by ChilliColor - Flaticon")
                }
                pop()
            }
            ClickableText(
                text = flaticonLink,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                onClick = { offset ->
                    flaticonLink.getStringAnnotations("URL", offset, offset)
                        .firstOrNull()?.let { uriHandler.openUri(it.item) }
                },
            )

            // 下部余白
            Spacer(modifier = Modifier.padding(bottom = 16.dp))
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

