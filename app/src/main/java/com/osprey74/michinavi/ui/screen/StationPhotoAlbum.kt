package com.osprey74.michinavi.ui.screen

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.osprey74.michinavi.service.StationPhotoStore

/**
 * 道の駅ごとのフォトアルバム — 3枚タイルグリッド
 *
 * - 空タイルをタップ: フォトピッカーで写真を選択し保存
 * - 写真タイルをタップ: フルスクリーン表示（左右スワイプ対応）
 * - 写真タイルを長押し: 削除確認ダイアログ
 */
@Composable
fun StationPhotoAlbum(stationId: String) {
    val context = LocalContext.current
    val store = remember { StationPhotoStore(context) }

    var photos by remember { mutableStateOf(store.loadPhotos(stationId)) }
    var addingSlot by remember { mutableIntStateOf(-1) }
    var deletingSlot by remember { mutableIntStateOf(-1) }
    var fullscreenSlot by remember { mutableIntStateOf(-1) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        if (uri != null && addingSlot >= 0) {
            store.savePhoto(uri, addingSlot, stationId)
            photos = store.loadPhotos(stationId)
        }
        addingSlot = -1
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for (slot in 0 until StationPhotoStore.MAX_PHOTOS) {
            Box(modifier = Modifier.weight(1f)) {
                PhotoTile(
                    bitmap = photos[slot],
                    onTap = {
                        if (photos[slot] != null) {
                            fullscreenSlot = slot
                        } else {
                            addingSlot = slot
                            photoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                        }
                    },
                    onLongPress = {
                        if (photos[slot] != null) {
                            deletingSlot = slot
                        }
                    },
                )
            }
        }
    }

    // 削除確認ダイアログ
    if (deletingSlot >= 0) {
        AlertDialog(
            onDismissRequest = { deletingSlot = -1 },
            title = { Text("写真の削除") },
            text = { Text("この写真を削除しますか？") },
            confirmButton = {
                TextButton(onClick = {
                    store.deletePhoto(deletingSlot, stationId)
                    photos = store.loadPhotos(stationId)
                    deletingSlot = -1
                }) {
                    Text("削除")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingSlot = -1 }) {
                    Text("キャンセル")
                }
            },
        )
    }

    // フルスクリーン表示
    if (fullscreenSlot >= 0) {
        val filledPhotos = photos.filterNotNull()
        val initialIndex = photos.take(fullscreenSlot).count { it != null }
        PhotoFullscreenDialog(
            photos = filledPhotos,
            initialIndex = initialIndex,
            onDismiss = { fullscreenSlot = -1 },
        )
    }
}

@Composable
private fun PhotoTile(
    bitmap: Bitmap?,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
) {
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp))
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { onTap() },
                        onLongPress = { onLongPress() },
                    )
                },
        )
    } else {
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { onTap() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.AddCircle,
                contentDescription = "写真を追加",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp),
            )
        }
    }
}

@Composable
private fun PhotoFullscreenDialog(
    photos: List<Bitmap>,
    initialIndex: Int,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            val pagerState = rememberPagerState(
                initialPage = initialIndex.coerceIn(0, (photos.size - 1).coerceAtLeast(0)),
                pageCount = { photos.size },
            )

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                Image(
                    bitmap = photos[page].asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            // 閉じるボタン
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "閉じる",
                    tint = Color.White,
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color.Black.copy(alpha = 0.4f), CircleShape),
                )
            }

            // ページインジケーター
            if (photos.size > 1) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    repeat(photos.size) { index ->
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    color = if (index == pagerState.currentPage) Color.White
                                    else Color.White.copy(alpha = 0.4f),
                                    shape = CircleShape,
                                ),
                        )
                    }
                }
            }
        }
    }
}
