package com.osprey74.michinavi.ui.screen

import android.Manifest
import android.graphics.Color as AndroidColor
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point

private const val OPENFREEMAP_STYLE = "https://tiles.openfreemap.org/styles/liberty"

private val DEFAULT_POSITION = LatLng(35.681236, 139.767125)
private const val DEFAULT_ZOOM = 10.0

private const val STATION_SOURCE = "station-source"
private const val STATION_LAYER = "station-layer"
private const val POI_SOURCE = "poi-source"
private const val POI_LAYER = "poi-layer"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: MapViewModel,
    onOpenSettings: () -> Unit = {},
    onOpenStationPicker: () -> Unit = {},
) {
    val context = LocalContext.current
    val locationState by viewModel.locationState.collectAsState()
    val visibleStations by viewModel.visibleStations.collectAsState()
    val poiItems by viewModel.poiItems.collectAsState()
    val selectedStation by viewModel.selectedStation.collectAsState()
    val favoriteIds by viewModel.favoriteIds.collectAsState()

    var permissionGranted by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissionGranted = permissions.values.any { it }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            )
        )
    }

    LaunchedEffect(permissionGranted) {
        if (permissionGranted) {
            viewModel.locationService.locationUpdates().collect {}
        }
    }

    // MapView & MapLibreMap の参照を保持
    val mapViewRef = remember { mutableStateOf<MapView?>(null) }
    var mapRef by remember { mutableStateOf<MapLibreMap?>(null) }
    var styleReady by remember { mutableStateOf(false) }

    // ライフサイクル管理
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            val mv = mapViewRef.value ?: return@LifecycleEventObserver
            when (event) {
                Lifecycle.Event.ON_START -> mv.onStart()
                Lifecycle.Event.ON_RESUME -> mv.onResume()
                Lifecycle.Event.ON_PAUSE -> mv.onPause()
                Lifecycle.Event.ON_STOP -> mv.onStop()
                Lifecycle.Event.ON_DESTROY -> mv.onDestroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // 現在地が取得できたらカメラを移動
    val hasLocation = locationState.latitude != 0.0 || locationState.longitude != 0.0
    LaunchedEffect(hasLocation) {
        if (hasLocation) {
            mapRef?.animateCamera(
                CameraUpdateFactory.newLatLng(
                    LatLng(locationState.latitude, locationState.longitude)
                )
            )
        }
    }

    // 選択された道の駅へカメラを移動
    LaunchedEffect(selectedStation) {
        selectedStation?.let { station ->
            mapRef?.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(station.latitude, station.longitude), 14.0
                )
            )
        }
    }

    // 道の駅マーカー更新
    LaunchedEffect(visibleStations, styleReady) {
        if (!styleReady) return@LaunchedEffect
        mapRef?.style?.let { style ->
            val source = style.getSourceAs<GeoJsonSource>(STATION_SOURCE) ?: return@let
            val features = visibleStations.map { station ->
                Feature.fromGeometry(
                    Point.fromLngLat(station.longitude, station.latitude)
                ).apply {
                    addStringProperty("id", station.id)
                    addStringProperty("name", station.name)
                }
            }
            source.setGeoJson(FeatureCollection.fromFeatures(features))
        }
    }

    // POIマーカー更新
    LaunchedEffect(poiItems, styleReady) {
        if (!styleReady) return@LaunchedEffect
        mapRef?.style?.let { style ->
            val source = style.getSourceAs<GeoJsonSource>(POI_SOURCE) ?: return@let
            val features = poiItems.map { poi ->
                Feature.fromGeometry(
                    Point.fromLngLat(poi.longitude, poi.latitude)
                ).apply {
                    addStringProperty("color", poi.category.color)
                }
            }
            source.setGeoJson(FeatureCollection.fromFeatures(features))
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    MapLibre.getInstance(ctx)
                    MapView(ctx).also { mapView ->
                        mapViewRef.value = mapView
                        mapView.onCreate(null)
                        mapView.getMapAsync { map ->
                            mapRef = map

                            map.setStyle(Style.Builder().fromUri(OPENFREEMAP_STYLE)) { style ->
                                // 道の駅ソース & レイヤー
                                style.addSource(GeoJsonSource(STATION_SOURCE))
                                style.addLayer(
                                    CircleLayer(STATION_LAYER, STATION_SOURCE).withProperties(
                                        PropertyFactory.circleRadius(8f),
                                        PropertyFactory.circleColor(AndroidColor.parseColor("#2196F3")),
                                        PropertyFactory.circleStrokeWidth(2f),
                                        PropertyFactory.circleStrokeColor(AndroidColor.WHITE),
                                    )
                                )

                                // POIソース & レイヤー
                                style.addSource(GeoJsonSource(POI_SOURCE))
                                style.addLayer(
                                    CircleLayer(POI_LAYER, POI_SOURCE).withProperties(
                                        PropertyFactory.circleRadius(5f),
                                        PropertyFactory.circleColor(
                                            Expression.get("color")
                                        ),
                                        PropertyFactory.circleStrokeWidth(1f),
                                        PropertyFactory.circleStrokeColor(AndroidColor.WHITE),
                                    )
                                )

                                styleReady = true
                            }

                            // 初期カメラ位置
                            map.moveCamera(
                                CameraUpdateFactory.newLatLngZoom(DEFAULT_POSITION, DEFAULT_ZOOM)
                            )

                            // 道の駅タップ検出
                            map.addOnMapClickListener { point ->
                                val screenPoint = map.projection.toScreenLocation(point)
                                val features = map.queryRenderedFeatures(screenPoint, STATION_LAYER)
                                if (features.isNotEmpty()) {
                                    val stationId = features[0].getStringProperty("id")
                                    val station = visibleStations.find { it.id == stationId }
                                    if (station != null) {
                                        viewModel.selectStation(station)
                                        return@addOnMapClickListener true
                                    }
                                }
                                false
                            }
                        }
                    }
                },
            )

            // 右上ボタン群
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(16.dp),
            ) {
                FloatingActionButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.size(40.dp),
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "設定",
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                FloatingActionButton(
                    onClick = onOpenStationPicker,
                    modifier = Modifier.size(40.dp),
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ) {
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = "道の駅を選択",
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }

        // 道の駅詳細ボトムシート
        selectedStation?.let { station ->
            StationDetailSheet(
                station = station,
                sheetState = sheetState,
                isFavorite = station.id in favoriteIds,
                onToggleFavorite = { viewModel.toggleFavorite(station.id) },
                onDismiss = { viewModel.selectStation(null) },
            )
        }
    }
}
