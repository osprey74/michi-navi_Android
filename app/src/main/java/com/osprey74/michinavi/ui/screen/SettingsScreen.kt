package com.osprey74.michinavi.ui.screen

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Logout
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.osprey74.michinavi.BuildConfig
import com.osprey74.michinavi.service.DriveBackupService
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MapViewModel,
    onBack: () -> Unit,
) {
    val settings by viewModel.settings.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Google Sign-In
    val driveScope = Scope("https://www.googleapis.com/auth/drive.file")
    val signInOptions = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(driveScope)
            .build()
    }
    var googleAccount by remember { mutableStateOf(GoogleSignIn.getLastSignedInAccount(context)) }
    var syncState by remember { mutableStateOf<SyncState>(SyncState.Idle) }
    var lastBackupTime by remember { mutableStateOf<String?>(null) }

    // サインイン済みなら最終バックアップ日時を取得
    LaunchedEffect(googleAccount) {
        val account = googleAccount?.account ?: return@LaunchedEffect
        val service = DriveBackupService(context)
        lastBackupTime = service.getLastBackupTime(account)?.let { formatIso(it) }
    }

    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            try {
                googleAccount = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    .getResult(ApiException::class.java)
                syncState = SyncState.Idle
            } catch (e: ApiException) {
                syncState = SyncState.Error("サインインに失敗しました (${e.statusCode})")
            }
        }
    }

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

            // データ同期
            SectionHeader("データ同期")
            Text(
                text = "Googleドライブにお気に入り・踏破データとフォトアルバムをバックアップします。端末の故障時や機種変更時にデータを復元できます。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )

            if (googleAccount == null) {
                Button(
                    onClick = {
                        val client = GoogleSignIn.getClient(context, signInOptions)
                        signInLauncher.launch(client.signInIntent)
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text("Googleアカウントでサインイン")
                }
            } else {
                // アカウント情報
                ListItem(
                    headlineContent = { Text(googleAccount!!.email ?: "") },
                    supportingContent = {
                        Text(
                            if (lastBackupTime != null) "最終バックアップ: $lastBackupTime"
                            else "バックアップはまだありません",
                        )
                    },
                )

                // バックアップ / 復元
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = {
                            scope.launch {
                                syncState = SyncState.InProgress("バックアップ中…")
                                val service = DriveBackupService(context)
                                val result = service.backup(googleAccount!!.account!!)
                                if (result.isSuccess) {
                                    syncState = SyncState.Success("バックアップが完了しました")
                                    lastBackupTime = service.getLastBackupTime(googleAccount!!.account!!)
                                        ?.let { formatIso(it) }
                                } else {
                                    syncState = SyncState.Error(
                                        result.exceptionOrNull()?.message ?: "エラーが発生しました",
                                    )
                                }
                            }
                        },
                        enabled = syncState !is SyncState.InProgress,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Default.Backup, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("バックアップ")
                    }
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                syncState = SyncState.InProgress("復元中…")
                                val service = DriveBackupService(context)
                                val result = service.restore(googleAccount!!.account!!)
                                syncState = if (result.isSuccess) {
                                    SyncState.Success("データを復元しました")
                                } else {
                                    SyncState.Error(
                                        result.exceptionOrNull()?.message ?: "エラーが発生しました",
                                    )
                                }
                            }
                        },
                        enabled = syncState !is SyncState.InProgress,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Default.CloudDownload, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("復元")
                    }
                }

                // ステータス
                when (val state = syncState) {
                    is SyncState.InProgress -> {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(state.message, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    is SyncState.Success -> {
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                    is SyncState.Error -> {
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                    SyncState.Idle -> {}
                }

                // サインアウト
                TextButton(
                    onClick = {
                        GoogleSignIn.getClient(context, signInOptions).signOut()
                            .addOnCompleteListener {
                                googleAccount = null
                                lastBackupTime = null
                                syncState = SyncState.Idle
                            }
                    },
                    modifier = Modifier.padding(horizontal = 16.dp),
                ) {
                    Icon(Icons.Default.Logout, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("サインアウト")
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // クレジット
            SectionHeader("クレジット")

            Text(
                text = "Michi-Navi v${BuildConfig.VERSION_NAME}",
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
                text = "カントリーサイン画像出典：",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
            )
            Text(
                text = "北海道開発局（利用許諾済み）\n大空町・本別町・今金町（個別利用許諾済み）",
                style = MaterialTheme.typography.bodySmall,
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
                text = "一般社団法人 全国道の駅連絡会（利用許諾済み）",
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

private sealed class SyncState {
    data object Idle : SyncState()
    data class InProgress(val message: String) : SyncState()
    data class Success(val message: String) : SyncState()
    data class Error(val message: String) : SyncState()
}

private fun formatIso(iso: String): String = try {
    val zdt = ZonedDateTime.parse(iso)
    val jst = zdt.withZoneSameInstant(ZoneId.of("Asia/Tokyo"))
    jst.format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm", Locale.JAPAN))
} catch (_: Exception) {
    iso
}

