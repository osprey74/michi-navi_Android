package com.osprey74.michinavi.ui.screen

import android.Manifest
import android.graphics.Color as AndroidColor
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.LocationOn
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

private val DEFAULT_POSITION = LatLng(35.681236, 139.767125)
private const val DEFAULT_ZOOM = 10.0

private const val STATION_SOURCE = "station-source"
private const val STATION_LAYER = "station-layer"
private const val POI_SOURCE = "poi-source"
private const val POI_LAYER = "poi-layer"
private const val USER_LOCATION_SOURCE = "user-location-source"
private const val USER_LOCATION_LAYER = "user-location-layer"
private const val USER_LOCATION_OUTLINE_LAYER = "user-location-outline-layer"

private fun buildStyleForTileType(
    tileType: String,
    googleMapsApiKey: String = "",
    googleMapsSession: String? = null,
): Style.Builder {
    return when (tileType) {
        "openfreemap" -> Style.Builder().fromUri("https://tiles.openfreemap.org/styles/liberty")
        "gsi_std" -> Style.Builder().fromJson(rasterStyleJson(
            "https://cyberjapandata.gsi.go.jp/xyz/std/{z}/{x}/{y}.png",
            attribution = "<a href='https://maps.gsi.go.jp/development/ichiran.html'>国土地理院</a>",
        ))
        "gsi_photo" -> Style.Builder().fromJson(rasterStyleJson(
            "https://cyberjapandata.gsi.go.jp/xyz/seamlessphoto/{z}/{x}/{y}.jpg",
            attribution = "<a href='https://maps.gsi.go.jp/development/ichiran.html'>国土地理院</a>",
        ))
        "google_maps" -> {
            if (googleMapsSession != null && googleMapsApiKey.isNotEmpty()) {
                Style.Builder().fromJson(rasterStyleJson(
                    "https://tile.googleapis.com/v1/2dtiles/{z}/{x}/{y}?session=$googleMapsSession&key=$googleMapsApiKey",
                    attribution = "Google",
                ))
            } else {
                // フォールバック: セッション未取得時は国土地理院淡色
                Style.Builder().fromJson(rasterStyleJson(
                    "https://cyberjapandata.gsi.go.jp/xyz/pale/{z}/{x}/{y}.png",
                    attribution = "<a href='https://maps.gsi.go.jp/development/ichiran.html'>国土地理院</a>",
                ))
            }
        }
        else -> Style.Builder().fromJson(rasterStyleJson(
            "https://cyberjapandata.gsi.go.jp/xyz/pale/{z}/{x}/{y}.png",
            attribution = "<a href='https://maps.gsi.go.jp/development/ichiran.html'>国土地理院</a>",
        ))
    }
}

private fun rasterStyleJson(tileUrl: String, attribution: String = ""): String = """
{
  "version": 8,
  "sources": {
    "raster-tiles": {
      "type": "raster",
      "tiles": ["$tileUrl"],
      "tileSize": 256,
      "maxzoom": 18,
      "attribution": "$attribution"
    }
  },
  "layers": [{
    "id": "raster-layer",
    "type": "raster",
    "source": "raster-tiles"
  }]
}
"""

/**
 * Google Maps Map Tiles API のセッショントークンを作成する
 */
private const val TAG = "MichiNavi"

private suspend fun createGoogleMapsSession(apiKey: String): String? =
    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val url = java.net.URL(
                "https://tile.googleapis.com/v1/createSession?key=$apiKey"
            )
            val connection = url.openConnection() as java.net.HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.outputStream.use { os ->
                os.write("""{"mapType":"roadmap","language":"ja","region":"JP"}""".toByteArray())
            }
            val responseCode = connection.responseCode
            if (responseCode != 200) {
                val errorBody = try {
                    connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "no error body"
                } catch (_: Exception) { "failed to read error" }
                android.util.Log.e(TAG, "Google Maps session failed: HTTP $responseCode — $errorBody")
                connection.disconnect()
                return@withContext null
            }
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()
            android.util.Log.d(TAG, "Google Maps session response: $body")
            val regex = """"session"\s*:\s*"([^"]+)"""".toRegex()
            val session = regex.find(body)?.groupValues?.get(1)
            if (session != null) {
                android.util.Log.d(TAG, "Google Maps session created successfully")
            } else {
                android.util.Log.e(TAG, "Google Maps session token not found in response")
            }
            session
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Google Maps session exception: ${e.message}", e)
            null
        }
    }

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
    val settings by viewModel.settings.collectAsState()

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

    val mapViewRef = remember { mutableStateOf<MapView?>(null) }
    var mapRef by remember { mutableStateOf<MapLibreMap?>(null) }
    var styleReady by remember { mutableStateOf(false) }
    // google_maps はセッション取得後に切り替えるため、初期値はフォールバック
    val initialTileType = remember {
        if (settings.mapTileType == "google_maps") "gsi_pale" else settings.mapTileType
    }
    var currentTileType by remember { mutableStateOf(initialTileType) }
    var googleMapsSession by remember { mutableStateOf<String?>(null) }

    // Google Maps セッション取得
    LaunchedEffect(settings.googleMapsApiKey, settings.mapTileType) {
        android.util.Log.d(TAG, "Session LaunchedEffect: tileType=${settings.mapTileType}, apiKey=${if (settings.googleMapsApiKey.isNotEmpty()) "set(${settings.googleMapsApiKey.length}chars)" else "empty"}")
        if (settings.mapTileType == "google_maps" && settings.googleMapsApiKey.isNotEmpty()) {
            android.util.Log.d(TAG, "Creating Google Maps session...")
            googleMapsSession = createGoogleMapsSession(settings.googleMapsApiKey)
            android.util.Log.d(TAG, "Session result: ${if (googleMapsSession != null) "success" else "failed"}")
        }
    }

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

    // 現在地が取得できたらカメラを移動（選択中の駅がない場合のみ）
    val hasLocation = locationState.latitude != 0.0 || locationState.longitude != 0.0
    LaunchedEffect(hasLocation) {
        if (hasLocation && selectedStation == null) {
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

    // タイル切替 (mapRef をキーに含め、地図準備完了後にも再評価)
    LaunchedEffect(settings.mapTileType, googleMapsSession, mapRef) {
        android.util.Log.d(TAG, "Tile LaunchedEffect: tileType=${settings.mapTileType}, currentTile=$currentTileType, session=${googleMapsSession != null}")
        // Google Maps選択時はセッション取得まで切り替えを待つ
        if (settings.mapTileType == "google_maps" && googleMapsSession == null) {
            android.util.Log.d(TAG, "Waiting for Google Maps session...")
            return@LaunchedEffect
        }

        if (settings.mapTileType != currentTileType) {
            android.util.Log.d(TAG, "Switching tile: $currentTileType -> ${settings.mapTileType}")
            mapRef?.let { map ->
                styleReady = false
                currentTileType = settings.mapTileType
                applyMapStyle(
                    map, settings.mapTileType,
                    settings.googleMapsApiKey, googleMapsSession,
                ) {
                    styleReady = true
                }
            }
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

    // 現在地マーカー更新
    LaunchedEffect(locationState, styleReady) {
        if (!styleReady) return@LaunchedEffect
        val loc = locationState
        if (loc.latitude == 0.0 && loc.longitude == 0.0) return@LaunchedEffect
        mapRef?.style?.let { style ->
            val source = style.getSourceAs<GeoJsonSource>(USER_LOCATION_SOURCE) ?: return@let
            val point = Point.fromLngLat(loc.longitude, loc.latitude)
            source.setGeoJson(FeatureCollection.fromFeatures(listOf(Feature.fromGeometry(point))))
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

                            applyMapStyle(
                                map, initialTileType,
                            ) {
                                styleReady = true
                            }

                            // 初期カメラ位置: 選択駅 > 現在地 > デフォルト(東京)
                            val station = selectedStation
                            val loc = locationState
                            when {
                                station != null -> map.moveCamera(
                                    CameraUpdateFactory.newLatLngZoom(
                                        LatLng(station.latitude, station.longitude), 14.0
                                    )
                                )
                                loc.latitude != 0.0 || loc.longitude != 0.0 -> map.moveCamera(
                                    CameraUpdateFactory.newLatLngZoom(
                                        LatLng(loc.latitude, loc.longitude), 14.0
                                    )
                                )
                                else -> map.moveCamera(
                                    CameraUpdateFactory.newLatLngZoom(DEFAULT_POSITION, DEFAULT_ZOOM)
                                )
                            }

                            // 道の駅タップ検出
                            map.addOnMapClickListener { point ->
                                val screenPoint = map.projection.toScreenLocation(point)
                                val features = map.queryRenderedFeatures(screenPoint, STATION_LAYER)
                                if (features.isNotEmpty()) {
                                    val stationId = features[0].getStringProperty("id")
                                    val found = visibleStations.find { it.id == stationId }
                                    if (found != null) {
                                        viewModel.selectStation(found)
                                        return@addOnMapClickListener true
                                    }
                                }
                                false
                            }
                        }
                    }
                },
            )

            // 左上: 設定ボタン (iOS版準拠)
            FloatingActionButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(12.dp)
                    .size(44.dp),
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                contentColor = MaterialTheme.colorScheme.onSurface,
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "設定",
                    modifier = Modifier.size(22.dp),
                )
            }

            // 右下: 操作ボタン群 (iOS版準拠)
            Column(
                modifier = Modifier
                    .align(
                        if (settings.zoomPosition == "left") Alignment.BottomStart
                        else Alignment.BottomEnd
                    )
                    .padding(12.dp)
                    .padding(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // 道の駅リスト
                FloatingActionButton(
                    onClick = onOpenStationPicker,
                    modifier = Modifier.size(44.dp),
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ) {
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = "道の駅を選択",
                        modifier = Modifier.size(22.dp),
                    )
                }
                // 現在地に戻る
                FloatingActionButton(
                    onClick = {
                        if (hasLocation) {
                            mapRef?.animateCamera(
                                CameraUpdateFactory.newLatLngZoom(
                                    LatLng(locationState.latitude, locationState.longitude),
                                    14.0,
                                )
                            )
                        }
                    },
                    modifier = Modifier.size(44.dp),
                    containerColor = if (hasLocation) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                    } else {
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    },
                    contentColor = if (hasLocation) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    },
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "現在地に戻る",
                        modifier = Modifier.size(22.dp),
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

private fun applyMapStyle(
    map: MapLibreMap,
    tileType: String,
    googleMapsApiKey: String = "",
    googleMapsSession: String? = null,
    onReady: () -> Unit,
) {
    map.setStyle(buildStyleForTileType(tileType, googleMapsApiKey, googleMapsSession)) { style ->
        style.addSource(GeoJsonSource(STATION_SOURCE))
        style.addLayer(
            CircleLayer(STATION_LAYER, STATION_SOURCE).withProperties(
                PropertyFactory.circleRadius(8f),
                PropertyFactory.circleColor(AndroidColor.parseColor("#2196F3")),
                PropertyFactory.circleStrokeWidth(2f),
                PropertyFactory.circleStrokeColor(AndroidColor.WHITE),
            )
        )

        style.addSource(GeoJsonSource(POI_SOURCE))
        style.addLayer(
            CircleLayer(POI_LAYER, POI_SOURCE).withProperties(
                PropertyFactory.circleRadius(5f),
                PropertyFactory.circleColor(Expression.get("color")),
                PropertyFactory.circleStrokeWidth(1f),
                PropertyFactory.circleStrokeColor(AndroidColor.WHITE),
            )
        )

        // 現在地マーカー（白フチ付き青丸）
        style.addSource(GeoJsonSource(USER_LOCATION_SOURCE))
        style.addLayer(
            CircleLayer(USER_LOCATION_OUTLINE_LAYER, USER_LOCATION_SOURCE).withProperties(
                PropertyFactory.circleRadius(10f),
                PropertyFactory.circleColor(AndroidColor.WHITE),
            )
        )
        style.addLayer(
            CircleLayer(USER_LOCATION_LAYER, USER_LOCATION_SOURCE).withProperties(
                PropertyFactory.circleRadius(7f),
                PropertyFactory.circleColor(AndroidColor.parseColor("#4285F4")),
            )
        )

        onReady()
    }
}
