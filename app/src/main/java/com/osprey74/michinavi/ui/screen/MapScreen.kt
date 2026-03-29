package com.osprey74.michinavi.ui.screen

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.Style
import org.ramani.compose.CameraPosition
import org.ramani.compose.MapLibre
import org.ramani.compose.Symbol

private const val OPENFREEMAP_STYLE = "https://tiles.openfreemap.org/styles/liberty"

// 日本の中心付近（東京）
private val DEFAULT_POSITION = LatLng(35.681236, 139.767125)
private const val DEFAULT_ZOOM = 10.0

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(viewModel: MapViewModel = viewModel()) {
    val locationState by viewModel.locationState.collectAsState()
    val visibleStations by viewModel.visibleStations.collectAsState()
    val poiItems by viewModel.poiItems.collectAsState()
    val selectedStation by viewModel.selectedStation.collectAsState()

    var permissionGranted by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissionGranted = permissions.values.any { it }
    }

    // パーミッションリクエスト
    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            )
        )
    }

    // パーミッション取得後に位置情報の更新を開始
    LaunchedEffect(permissionGranted) {
        if (permissionGranted) {
            viewModel.locationService.locationUpdates().collect {}
        }
    }

    val cameraPosition = rememberSaveable {
        CameraPosition(
            target = DEFAULT_POSITION,
            zoom = DEFAULT_ZOOM,
        )
    }

    // 現在地が取得できたらカメラを移動
    val hasLocation = locationState.latitude != 0.0 || locationState.longitude != 0.0
    LaunchedEffect(hasLocation) {
        if (hasLocation) {
            cameraPosition.target = LatLng(locationState.latitude, locationState.longitude)
        }
    }

    // 詳細シート
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            MapLibre(
                modifier = Modifier.fillMaxSize(),
                styleBuilder = Style.Builder().fromUri(OPENFREEMAP_STYLE),
                cameraPosition = cameraPosition,
            ) {
                // 道の駅ピン（タップで詳細シート表示）
                visibleStations.forEach { station ->
                    Symbol(
                        center = LatLng(station.latitude, station.longitude),
                        size = 0.8f,
                        color = "#2196F3",
                        onClick = {
                            viewModel.selectStation(station)
                            true
                        },
                    )
                }

                // POIピン（GS / コンビニ / レストラン / 駐車場 / RVパーク）
                poiItems.forEach { poi ->
                    Symbol(
                        center = LatLng(poi.latitude, poi.longitude),
                        size = 0.6f,
                        color = poi.category.color,
                    )
                }
            }
        }

        // 道の駅詳細ボトムシート
        selectedStation?.let { station ->
            StationDetailSheet(
                station = station,
                sheetState = sheetState,
                onDismiss = { viewModel.selectStation(null) },
            )
        }
    }
}
