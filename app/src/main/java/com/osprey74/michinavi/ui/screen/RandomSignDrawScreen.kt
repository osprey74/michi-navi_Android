package com.osprey74.michinavi.ui.screen

import android.graphics.BitmapFactory
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.osprey74.michinavi.model.CountrySign

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RandomSignDrawScreen(
    viewModel: MapViewModel,
    onShowOnMap: (CountrySign) -> Unit,
    onShowDetail: (CountrySign) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val visitedSignIds by viewModel.visitedSignIds.collectAsState()
    val totalSigns = remember { viewModel.allSigns.size }
    val visitedCount = visitedSignIds.size

    var drawnSign by remember { mutableStateOf<CountrySign?>(null) }
    // アニメーション用カウンター（ドローのたびにインクリメント）
    var drawCount by remember { mutableIntStateOf(0) }
    val animateTarget = drawCount > 0
    val scale by animateFloatAsState(
        targetValue = if (animateTarget) 1f else 0.92f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "cardScale",
    )
    val alpha by animateFloatAsState(
        targetValue = if (animateTarget) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "cardAlpha",
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ランダムカード") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "戻る")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 進捗表示
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "踏破済み: $visitedCount / $totalSigns",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { if (totalSigns > 0) visitedCount.toFloat() / totalSigns else 0f },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (visitedCount >= totalSigns) {
                // 全踏破達成
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "全${totalSigns}市町村を踏破しました!",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "北海道全市町村のカントリーサインを踏破しました。おめでとうございます!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.weight(1f))
            } else if (drawnSign == null) {
                // 初期状態: カードを引くボタン
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = {
                        drawnSign = viewModel.drawRandomUnvisitedSign()
                        drawCount++
                    },
                ) {
                    Icon(Icons.Default.Style, contentDescription = null)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("カードを引く")
                }
                Spacer(modifier = Modifier.weight(1f))
            } else {
                // カード表示
                val sign = drawnSign!!
                val bitmap = remember(sign.imageName) {
                    sign.imageName?.let { name ->
                        try {
                            context.assets.open("country_signs/$name.jpg").use {
                                BitmapFactory.decodeStream(it)
                            }
                        } catch (_: Exception) {
                            null
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // カード（springアニメーション付き）
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            this.alpha = alpha
                        }
                        .shadow(12.dp, RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = sign.name,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(260.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Fit,
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(260.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Image,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = sign.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "${sign.subprefectureOffice} / ${sign.municipalityType}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // アクションボタン
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = { onShowOnMap(sign) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("地図で見る")
                    }
                    OutlinedButton(
                        onClick = { onShowDetail(sign) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("詳細")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 「もう一度引く」を目立つ filled ボタンに
                Button(
                    onClick = {
                        drawCount++
                        drawnSign = viewModel.drawRandomUnvisitedSign()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.Style, contentDescription = null)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("もう一度引く")
                }
            }
        }
    }
}
