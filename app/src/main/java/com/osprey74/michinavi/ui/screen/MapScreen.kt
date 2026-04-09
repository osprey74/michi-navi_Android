package com.osprey74.michinavi.ui.screen

import android.Manifest
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Beenhere
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Signpost
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.maps.MapLibreMap.OnCameraMoveStartedListener
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point

private val DEFAULT_POSITION = LatLng(35.681236, 139.767125)
private const val DEFAULT_ZOOM = 9.4

private const val USER_LOCATION_SOURCE = "user-location-source"
private const val USER_LOCATION_LAYER = "user-location-layer"
private const val USER_LOCATION_OUTLINE_LAYER = "user-location-outline-layer"
private const val BOUNDARY_SOURCE = "hokkaido-boundary-source"
private const val BOUNDARY_LAYER = "hokkaido-boundary-layer"

private fun tileUrl(tileType: String, apiKey: String, session: String?): String {
    return when (tileType) {
        "gsi_std" -> "https://cyberjapandata.gsi.go.jp/xyz/std/{z}/{x}/{y}.png"
        "gsi_photo" -> "https://cyberjapandata.gsi.go.jp/xyz/seamlessphoto/{z}/{x}/{y}.jpg"
        "google_maps" ->
            if (session != null && apiKey.isNotEmpty())
                "https://tile.googleapis.com/v1/2dtiles/{z}/{x}/{y}?session=$session&key=$apiKey"
            else
                "https://cyberjapandata.gsi.go.jp/xyz/pale/{z}/{x}/{y}.png"
        else -> "https://cyberjapandata.gsi.go.jp/xyz/pale/{z}/{x}/{y}.png"
    }
}

/** OpenFreeMap はベクタータイルなので専用スタイルURLを使う */
private const val OFM_STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"

private fun isVectorTileType(tileType: String): Boolean = tileType == "openfreemap"

/**
 * 道の駅データ・全レイヤーを含む完全なスタイルJSONを構築
 */
private fun buildFullStyleJson(
    tileType: String,
    apiKey: String = "",
    session: String? = null,
): String {
    val tile = tileUrl(tileType, apiKey, session)
    return """
{
  "version": 8,
  "glyphs": "https://demotiles.maplibre.org/font/{fontstack}/{range}.pbf",
  "sources": {
    "raster-tiles": {
      "type": "raster",
      "tiles": ["$tile"],
      "tileSize": 256,
      "maxzoom": 18
    },
    "$USER_LOCATION_SOURCE": {
      "type": "geojson",
      "data": {"type": "FeatureCollection", "features": []}
    }
  },
  "layers": [
    {
      "id": "raster-layer",
      "type": "raster",
      "source": "raster-tiles"
    },
    {
      "id": "$USER_LOCATION_OUTLINE_LAYER",
      "type": "circle",
      "source": "$USER_LOCATION_SOURCE",
      "paint": {
        "circle-radius": 10,
        "circle-color": "#FFFFFF"
      }
    },
    {
      "id": "$USER_LOCATION_LAYER",
      "type": "circle",
      "source": "$USER_LOCATION_SOURCE",
      "paint": {
        "circle-radius": 7,
        "circle-color": "#4285F4"
      }
    }
  ]
}"""
}

/**
 * Google Maps Map Tiles API のセッショントークンを作成する
 */
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
                connection.disconnect()
                return@withContext null
            }
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()
            val regex = """"session"\s*:\s*"([^"]+)"""".toRegex()
            regex.find(body)?.groupValues?.get(1)
        } catch (_: Exception) {
            null
        }
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: MapViewModel,
    onOpenSettings: () -> Unit = {},
    onOpenPicker: () -> Unit = {},
) {
    val context = LocalContext.current
    val locationState by viewModel.locationState.collectAsState()
    val selectedStation by viewModel.selectedStation.collectAsState()
    val favoriteIds by viewModel.favoriteIds.collectAsState()
    val visitedIds by viewModel.visitedIds.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val isDriving by viewModel.isDriving.collectAsState()
    val autoZoomLevel by viewModel.autoZoomLevel.collectAsState()
    val isFollowingUser by viewModel.isFollowingUser.collectAsState()
    val isAutoZoomPaused by viewModel.isAutoZoomPaused.collectAsState()
    val selectedSign by viewModel.selectedSign.collectAsState()
    val favoriteSignIds by viewModel.favoriteSignIds.collectAsState()
    val visitedSignIds by viewModel.visitedSignIds.collectAsState()
    val showCountrySignMarkers by viewModel.showCountrySignMarkers.collectAsState()
    val mapFocusSign by viewModel.mapFocusSign.collectAsState()
    val mapFocusStation by viewModel.mapFocusStation.collectAsState()

    // マーカー更新用コルーチンスコープ（非同期データ: 位置情報）
    val markerScope = remember { mutableStateOf<CoroutineScope?>(null) }
    DisposableEffect(Unit) {
        onDispose { markerScope.value?.cancel() }
    }

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
    var debugZoom by remember { mutableStateOf(0.0) }
    // 道の駅スクリーン座標（Compose Canvas描画用）
    data class StationMarker(
        val x: Float, val y: Float, val name: String, val id: String,
        val isFavorite: Boolean, val isVisited: Boolean,
    )
    var stationMarkers by remember { mutableStateOf<List<StationMarker>>(emptyList()) }
    // カントリーサインスクリーン座標
    data class SignMarker(
        val x: Float, val y: Float, val name: String, val id: String,
        val isFavorite: Boolean, val isVisited: Boolean,
        val imageName: String?,
    )
    var signMarkers by remember { mutableStateOf<List<SignMarker>>(emptyList()) }
    var isMapMoving by remember { mutableStateOf(false) }
    var showStationMarkers by remember { mutableStateOf(true) }
    val allStations = remember { viewModel.allStations }
    val allSigns = remember { viewModel.allSigns }
    // CSサムネイルBitmapキャッシュ（40dp角丸、起動時に全件プリロード）
    val csThumbnailCache = remember {
        val cache = mutableMapOf<String, android.graphics.Bitmap>()
        val thumbSize = 80 // px (40dp * 2 for density)
        for (sign in viewModel.allSigns) {
            val name = sign.imageName ?: continue
            try {
                val original = context.assets.open("country_signs/$name.jpg").use {
                    android.graphics.BitmapFactory.decodeStream(it)
                } ?: continue
                // アスペクト比を保ってサムネイルに収める
                val scale = minOf(thumbSize.toFloat() / original.width, thumbSize.toFloat() / original.height)
                val w = (original.width * scale).toInt()
                val h = (original.height * scale).toInt()
                val scaled = android.graphics.Bitmap.createScaledBitmap(original, w, h, true)
                // 角丸クリッピング + 白背景
                val output = android.graphics.Bitmap.createBitmap(thumbSize, thumbSize, android.graphics.Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(output)
                val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
                // 白背景
                canvas.drawRoundRect(0f, 0f, thumbSize.toFloat(), thumbSize.toFloat(), 12f, 12f, paint.apply { color = android.graphics.Color.WHITE })
                // 角丸クリッピングして画像描画
                paint.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN)
                val left = (thumbSize - w) / 2f
                val top = (thumbSize - h) / 2f
                canvas.drawBitmap(scaled, left, top, paint)
                paint.xfermode = null
                // 枠線
                paint.style = android.graphics.Paint.Style.STROKE
                paint.strokeWidth = 2f
                paint.color = android.graphics.Color.parseColor("#E0E0E0")
                canvas.drawRoundRect(1f, 1f, thumbSize - 1f, thumbSize - 1f, 12f, 12f, paint)
                cache[sign.id] = output
                if (original !== scaled) original.recycle()
            } catch (_: Exception) {}
        }
        cache
    }
    // google_maps はセッション取得後に切り替えるため、初期値はフォールバック
    val initialTileType = remember {
        if (settings.mapTileType == "google_maps") "gsi_pale" else settings.mapTileType
    }
    var currentTileType by remember { mutableStateOf(initialTileType) }
    var googleMapsSession by remember { mutableStateOf<String?>(null) }

    // Google Maps セッション取得
    LaunchedEffect(settings.googleMapsApiKey, settings.mapTileType) {
        if (settings.mapTileType == "google_maps" && settings.googleMapsApiKey.isNotEmpty()) {
            googleMapsSession = createGoogleMapsSession(settings.googleMapsApiKey)
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
                    LatLng(station.latitude, station.longitude), 11.0
                )
            )
        }
    }

    // カントリーサインフォーカス要求でカメラ移動
    LaunchedEffect(mapFocusSign, mapRef) {
        val map = mapRef ?: return@LaunchedEffect
        mapFocusSign?.let { sign ->
            map.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(sign.centroidLat, sign.centroidLon), 11.0
                )
            )
            viewModel.clearFocusSign()
        }
    }

    // 道の駅フォーカス要求でカメラ移動（詳細シートは開かない）
    LaunchedEffect(mapFocusStation, mapRef) {
        val map = mapRef ?: return@LaunchedEffect
        mapFocusStation?.let { station ->
            map.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(station.latitude, station.longitude), 11.0
                )
            )
            viewModel.clearFocusStation()
        }
    }

    // 走行中カメラ制御（オートズーム + ヘディングアップ）— 走行中のみ連続追従
    LaunchedEffect(locationState, isDriving, isFollowingUser, isAutoZoomPaused) {
        if (!isDriving || !isFollowingUser || !hasLocation) return@LaunchedEffect
        mapRef?.let { map ->
            val builder = CameraPosition.Builder()
                .target(LatLng(locationState.latitude, locationState.longitude))
                .bearing(locationState.heading.toFloat().toDouble())
                .tilt(45.0)
            if (!isAutoZoomPaused) {
                builder.zoom(autoZoomLevel.toDouble())
            }
            map.animateCamera(CameraUpdateFactory.newCameraPosition(builder.build()), 500)
        }
    }

    // タイル切替 (mapRef をキーに含め、地図準備完了後にも再評価)
    LaunchedEffect(settings.mapTileType, googleMapsSession, mapRef) {
        // Google Maps選択時はセッション取得まで切り替えを待つ
        if (settings.mapTileType == "google_maps" && googleMapsSession == null) return@LaunchedEffect

        if (settings.mapTileType != currentTileType) {
            mapRef?.let { map ->
                styleReady = false
                // 旧スタイルのオブザーバーを即座にキャンセル（旧Styleへのアクセスを防ぐ）
                markerScope.value?.cancel()
                markerScope.value = null
                currentTileType = settings.mapTileType
                applyMapStyle(
                    map, context, settings.mapTileType,
                    googleMapsApiKey = settings.googleMapsApiKey,
                    googleMapsSession = googleMapsSession,
                ) { style ->
                    styleReady = true
                    startMarkerObservers(markerScope, style, viewModel)
                    // スタイル切替後にビューポート更新をトリガー（onCameraIdleは発火しないため）
                    val bounds = map.projection.visibleRegion.latLngBounds
                    val center = map.cameraPosition.target
                    if (center != null) {
                        viewModel.onMapRegionChanged(
                            centerLat = center.latitude,
                            centerLon = center.longitude,
                            latitudeDelta = bounds.latitudeSpan,
                            longitudeDelta = bounds.longitudeSpan,
                        )
                    }
                }
            }
        }
    }


    // CS マーカー設定変更時に境界線レイヤーの表示/非表示を切替
    LaunchedEffect(showCountrySignMarkers, mapRef) {
        mapRef?.style?.let { style ->
            try {
                style.getLayer(BOUNDARY_LAYER)?.setProperties(
                    PropertyFactory.visibility(
                        if (showCountrySignMarkers) Property.VISIBLE else Property.NONE
                    )
                )
            } catch (_: Exception) {}
        }
    }

    // お気に入り/到達リスト変更時にマーカーの状態を更新
    LaunchedEffect(favoriteIds, visitedIds, favoriteSignIds, visitedSignIds, showCountrySignMarkers, mapRef) {
        val map = mapRef ?: return@LaunchedEffect
        val bounds = map.projection.visibleRegion.latLngBounds
        val padLat = bounds.latitudeSpan * 0.1
        val padLon = bounds.longitudeSpan * 0.1
        stationMarkers = allStations
            .filter { s ->
                s.latitude in (bounds.latitudeSouth - padLat)..(bounds.latitudeNorth + padLat) &&
                    s.longitude in (bounds.longitudeWest - padLon)..(bounds.longitudeEast + padLon)
            }
            .map { s ->
                val sp = map.projection.toScreenLocation(LatLng(s.latitude, s.longitude))
                StationMarker(sp.x, sp.y, s.name, s.id,
                    isFavorite = s.id in favoriteIds,
                    isVisited = s.id in visitedIds)
            }
        signMarkers = if (showCountrySignMarkers) {
            allSigns
                .filter { s ->
                    s.centroidLat in (bounds.latitudeSouth - padLat)..(bounds.latitudeNorth + padLat) &&
                        s.centroidLon in (bounds.longitudeWest - padLon)..(bounds.longitudeEast + padLon)
                }
                .map { s ->
                    val sp = map.projection.toScreenLocation(LatLng(s.centroidLat, s.centroidLon))
                    SignMarker(sp.x, sp.y, s.name, s.id,
                        isFavorite = s.id in favoriteSignIds,
                        isVisited = s.id in visitedSignIds,
                        imageName = s.imageName)
                }
        } else emptyList()
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
                                map, ctx, initialTileType,
                            ) { style ->
                                styleReady = true
                                // 非同期データ（位置情報）の監視開始
                                startMarkerObservers(markerScope, style, viewModel)
                            }

                            // 初期カメラ位置: 選択駅 > 現在地 > デフォルト(東京)
                            val station = selectedStation
                            val loc = locationState
                            when {
                                station != null -> map.moveCamera(
                                    CameraUpdateFactory.newLatLngZoom(
                                        LatLng(station.latitude, station.longitude), 11.0
                                    )
                                )
                                loc.latitude != 0.0 || loc.longitude != 0.0 -> map.moveCamera(
                                    CameraUpdateFactory.newLatLngZoom(
                                        LatLng(loc.latitude, loc.longitude), 8.0
                                    )
                                )
                                else -> map.moveCamera(
                                    CameraUpdateFactory.newLatLngZoom(DEFAULT_POSITION, DEFAULT_ZOOM)
                                )
                            }

                            // 道の駅 + CS スクリーン座標を更新
                            val updateMarkers = {
                                val bounds = map.projection.visibleRegion.latLngBounds
                                val padLat = bounds.latitudeSpan * 0.1
                                val padLon = bounds.longitudeSpan * 0.1
                                stationMarkers = allStations
                                    .filter { s ->
                                        s.latitude in (bounds.latitudeSouth - padLat)..(bounds.latitudeNorth + padLat) &&
                                            s.longitude in (bounds.longitudeWest - padLon)..(bounds.longitudeEast + padLon)
                                    }
                                    .map { s ->
                                        val sp = map.projection.toScreenLocation(LatLng(s.latitude, s.longitude))
                                        StationMarker(sp.x, sp.y, s.name, s.id,
                                            isFavorite = s.id in favoriteIds,
                                            isVisited = s.id in visitedIds)
                                    }
                                // カントリーサインマーカー
                                signMarkers = if (showCountrySignMarkers) {
                                    allSigns
                                        .filter { s ->
                                            s.centroidLat in (bounds.latitudeSouth - padLat)..(bounds.latitudeNorth + padLat) &&
                                                s.centroidLon in (bounds.longitudeWest - padLon)..(bounds.longitudeEast + padLon)
                                        }
                                        .map { s ->
                                            val sp = map.projection.toScreenLocation(LatLng(s.centroidLat, s.centroidLon))
                                            SignMarker(sp.x, sp.y, s.name, s.id,
                                                isFavorite = s.id in favoriteSignIds,
                                                isVisited = s.id in visitedSignIds,
                                                imageName = s.imageName)
                                        }
                                } else emptyList()
                            }

                            // ビューポート変更時に道の駅描画更新
                            val notifyRegionChanged = {
                                val bounds = map.projection.visibleRegion.latLngBounds
                                val center = map.cameraPosition.target
                                if (center != null) {
                                    viewModel.onMapRegionChanged(
                                        centerLat = center.latitude,
                                        centerLon = center.longitude,
                                        latitudeDelta = bounds.latitudeSpan,
                                        longitudeDelta = bounds.longitudeSpan,
                                    )
                                }
                                updateMarkers()
                            }
                            map.addOnCameraIdleListener {
                                debugZoom = map.cameraPosition.zoom
                                isMapMoving = false
                                notifyRegionChanged()
                            }
                            // 初期カメラ位置設定後に道の駅を即表示
                            notifyRegionChanged()

                            // ユーザージェスチャーで追従モード解除 + マーカー非表示
                            map.addOnCameraMoveStartedListener { reason ->
                                isMapMoving = true
                                if (reason == OnCameraMoveStartedListener.REASON_API_GESTURE) {
                                    viewModel.setFollowingUser(false)
                                }
                            }

                            // タップ検出（道の駅 + カントリーサイン）
                            map.addOnMapClickListener { point ->
                                val screenPoint = map.projection.toScreenLocation(point)
                                val tapRadius = 30f
                                // 道の駅タップ
                                val tappedStation = stationMarkers.firstOrNull { m ->
                                    val dx = screenPoint.x - m.x
                                    val dy = screenPoint.y - m.y
                                    dx * dx + dy * dy < tapRadius * tapRadius
                                }
                                if (tappedStation != null) {
                                    viewModel.selectStationById(tappedStation.id)
                                    return@addOnMapClickListener true
                                }
                                // カントリーサインタップ
                                val tappedSign = signMarkers.firstOrNull { m ->
                                    val dx = screenPoint.x - m.x
                                    val dy = screenPoint.y - m.y
                                    dx * dx + dy * dy < tapRadius * tapRadius
                                }
                                if (tappedSign != null) {
                                    viewModel.selectSignById(tappedSign.id)
                                    return@addOnMapClickListener true
                                }
                                false
                            }
                        }
                    }
                },
            )

            // 道の駅マーカー（Material Icons + Canvas描画）
            // デフォルト=緑 location_on, お気に入り=赤 favorite, 到達=緑 beenhere, お気に入り+到達=赤 beenhere
            val iconLocationOn = rememberVectorPainter(Icons.Filled.LocationOn)
            val iconFavorite = rememberVectorPainter(Icons.Filled.Favorite)
            val iconBeenhere = rememberVectorPainter(Icons.Filled.Beenhere)
            val labelPaint = remember {
                android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    textSize = 32f
                    color = android.graphics.Color.parseColor("#E65100")
                    textAlign = android.graphics.Paint.Align.CENTER
                }
            }
            val labelHaloPaint = remember {
                android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    textSize = 32f
                    color = android.graphics.Color.WHITE
                    textAlign = android.graphics.Paint.Align.CENTER
                    style = android.graphics.Paint.Style.STROKE
                    strokeWidth = 4f
                }
            }
            val statusBarHeightPx = with(androidx.compose.ui.platform.LocalDensity.current) {
                val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
                if (resourceId > 0) context.resources.getDimensionPixelSize(resourceId).toFloat()
                else 24.dp.toPx()
            }
            if (!isMapMoving) {
                val colorDefault = Color(0xFFFF9800) // オレンジ
                val colorFavorite = Color(0xFFF44336) // 赤
                val colorVisited = Color(0xFF2196F3) // 青
                // CS ラベル用Paint
                val csLabelPaint = remember {
                    android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                        textSize = 24f
                        color = android.graphics.Color.parseColor("#424242")
                        textAlign = android.graphics.Paint.Align.CENTER
                        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                    }
                }
                val csLabelBgPaint = remember {
                    android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                        color = android.graphics.Color.argb(217, 255, 255, 255) // 白 85%
                        style = android.graphics.Paint.Style.FILL
                    }
                }
                // CS バッジ用Paint
                val badgeBgPaint = remember {
                    android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                        color = android.graphics.Color.WHITE
                        style = android.graphics.Paint.Style.FILL
                    }
                }
                val badgeFavPainter = rememberVectorPainter(Icons.Filled.Favorite)
                val badgeVisitPainter = rememberVectorPainter(Icons.Filled.Beenhere)
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    val iconSize = 26.dp.toPx()
                    val labelOffsetY = iconSize * 0.5f + 16.dp.toPx()
                    // 道の駅マーカー（showStationMarkers時のみ）
                    if (showStationMarkers) stationMarkers.forEach { m ->
                        if (m.y - iconSize < statusBarHeightPx && m.y < statusBarHeightPx) return@forEach
                        val fillColor = when {
                            m.isFavorite && m.isVisited -> colorFavorite
                            m.isFavorite -> colorFavorite
                            m.isVisited -> colorVisited
                            else -> colorDefault
                        }
                        val icon = when {
                            m.isVisited -> iconBeenhere
                            m.isFavorite -> iconFavorite
                            else -> iconLocationOn
                        }
                        translate(left = m.x - iconSize / 2f, top = m.y - iconSize) {
                            with(icon) {
                                draw(size = androidx.compose.ui.geometry.Size(iconSize, iconSize),
                                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(fillColor))
                            }
                        }
                    }
                    // カントリーサインマーカー（画像サムネイル + ラベル + バッジ）
                    val thumbSizePx = 40.dp.toPx()
                    val labelGap = 2.dp.toPx()
                    val labelPadH = 4.dp.toPx()
                    val labelPadV = 2.dp.toPx()
                    val badgeSize = 14.dp.toPx()
                    signMarkers.forEach { m ->
                        val totalHeight = thumbSizePx + labelGap + csLabelPaint.textSize + labelPadV * 2
                        if (m.y - totalHeight < statusBarHeightPx && m.y < statusBarHeightPx) return@forEach
                        drawIntoCanvas { canvas ->
                            val thumbLeft = m.x - thumbSizePx / 2f
                            val thumbTop = m.y - totalHeight / 2f
                            // サムネイル画像
                            val bitmap = csThumbnailCache[m.id]
                            if (bitmap != null) {
                                val srcRect = android.graphics.Rect(0, 0, bitmap.width, bitmap.height)
                                val dstRect = android.graphics.RectF(thumbLeft, thumbTop, thumbLeft + thumbSizePx, thumbTop + thumbSizePx)
                                canvas.nativeCanvas.drawBitmap(bitmap, srcRect, dstRect, null)
                            } else {
                                // プレースホルダー（グレー角丸）
                                val placeholderPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                                    color = android.graphics.Color.parseColor("#E0E0E0")
                                }
                                canvas.nativeCanvas.drawRoundRect(
                                    thumbLeft, thumbTop, thumbLeft + thumbSizePx, thumbTop + thumbSizePx,
                                    6.dp.toPx(), 6.dp.toPx(), placeholderPaint,
                                )
                            }
                            // バッジ（右上、お気に入り or 踏破）
                            if (m.isFavorite || m.isVisited) {
                                val bx = thumbLeft + thumbSizePx - badgeSize - 2.dp.toPx()
                                val by = thumbTop + 2.dp.toPx()
                                // 白丸背景
                                canvas.nativeCanvas.drawCircle(
                                    bx + badgeSize / 2f, by + badgeSize / 2f,
                                    badgeSize / 2f + 2.dp.toPx(), badgeBgPaint,
                                )
                            }
                            // ラベル背景 + テキスト
                            val labelY = thumbTop + thumbSizePx + labelGap
                            val textWidth = csLabelPaint.measureText(m.name)
                            val bgWidth = textWidth + labelPadH * 2
                            val bgHeight = csLabelPaint.textSize + labelPadV * 2
                            val bgLeft = m.x - bgWidth / 2f
                            canvas.nativeCanvas.drawRoundRect(
                                bgLeft, labelY, bgLeft + bgWidth, labelY + bgHeight,
                                3.dp.toPx(), 3.dp.toPx(), csLabelBgPaint,
                            )
                            canvas.nativeCanvas.drawText(
                                m.name, m.x, labelY + labelPadV + csLabelPaint.textSize * 0.85f,
                                csLabelPaint,
                            )
                        }
                        // バッジアイコン（Compose描画）
                        if (m.isFavorite || m.isVisited) {
                            val bx = m.x + thumbSizePx / 2f - badgeSize - 2.dp.toPx()
                            val by = m.y - (thumbSizePx + labelGap + csLabelPaint.textSize + 2.dp.toPx() * 2) / 2f + 2.dp.toPx()
                            val badgePainter = if (m.isFavorite) badgeFavPainter else badgeVisitPainter
                            val badgeColor = if (m.isFavorite) colorFavorite else colorVisited
                            translate(left = bx, top = by) {
                                with(badgePainter) {
                                    draw(
                                        size = androidx.compose.ui.geometry.Size(badgeSize, badgeSize),
                                        colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(badgeColor),
                                    )
                                }
                            }
                        }
                    }
                    // 道の駅ラベル
                    if (showStationMarkers && debugZoom >= 10) {
                        drawIntoCanvas { canvas ->
                            stationMarkers.forEach { m ->
                                canvas.nativeCanvas.drawText(m.name, m.x, m.y + labelOffsetY, labelHaloPaint)
                                canvas.nativeCanvas.drawText(m.name, m.x, m.y + labelOffsetY, labelPaint)
                            }
                        }
                    }
                }
            }

            // 走行中UI: ボタンサイズ・色の切替
            val buttonSize by animateDpAsState(if (isDriving) 56.dp else 44.dp, label = "buttonSize")
            val iconSize by animateDpAsState(if (isDriving) 28.dp else 22.dp, label = "iconSize")
            val drivingContainerColor by animateColorAsState(
                if (isDriving) Color.Black.copy(alpha = 0.85f)
                else MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                label = "containerColor",
            )
            val drivingContentColor by animateColorAsState(
                if (isDriving) Color.White
                else MaterialTheme.colorScheme.onSurface,
                label = "contentColor",
            )

            // 左上: 設定ボタン (iOS版準拠)
            FloatingActionButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(12.dp)
                    .size(buttonSize),
                containerColor = drivingContainerColor,
                contentColor = drivingContentColor,
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "設定",
                    modifier = Modifier.size(iconSize),
                )
            }

            // 右下: 操作ボタン群 (iOS版準拠: リスト / 道の駅トグル / CSトグル / 現在地)
            Column(
                modifier = Modifier
                    .align(
                        if (settings.zoomPosition == "left") Alignment.BottomStart
                        else Alignment.BottomEnd
                    )
                    .padding(12.dp)
                    .padding(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // リスト（統合ピッカー）
                FloatingActionButton(
                    onClick = onOpenPicker,
                    modifier = Modifier.size(buttonSize),
                    containerColor = drivingContainerColor,
                    contentColor = drivingContentColor,
                ) {
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = "リスト",
                        modifier = Modifier.size(iconSize),
                    )
                }
                // 道の駅 表示/非表示トグル
                FloatingActionButton(
                    onClick = { showStationMarkers = !showStationMarkers },
                    modifier = Modifier.size(buttonSize),
                    containerColor = if (showStationMarkers) {
                        if (isDriving) Color(0xDD1565C0) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                    } else {
                        drivingContainerColor
                    },
                    contentColor = if (showStationMarkers) {
                        if (isDriving) Color.White else MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        drivingContentColor
                    },
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "道の駅を表示/非表示",
                        modifier = Modifier.size(iconSize),
                    )
                }
                // カントリーサイン 表示/非表示トグル
                FloatingActionButton(
                    onClick = { viewModel.toggleShowCountrySignMarkers() },
                    modifier = Modifier.size(buttonSize),
                    containerColor = if (showCountrySignMarkers) {
                        if (isDriving) Color(0xDD1565C0) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                    } else {
                        drivingContainerColor
                    },
                    contentColor = if (showCountrySignMarkers) {
                        if (isDriving) Color.White else MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        drivingContentColor
                    },
                ) {
                    Icon(
                        imageVector = Icons.Default.Signpost,
                        contentDescription = "カントリーサインを表示/非表示",
                        modifier = Modifier.size(iconSize),
                    )
                }
                // 現在地に戻る
                FloatingActionButton(
                    onClick = {
                        if (hasLocation) {
                            val zoom = if (isDriving) autoZoomLevel.toDouble() else 8.0
                            val cp = CameraPosition.Builder()
                                .target(LatLng(locationState.latitude, locationState.longitude))
                                .zoom(zoom)
                                .bearing(if (isDriving) locationState.heading else 0.0)
                                .tilt(if (isDriving) 45.0 else 0.0)
                                .build()
                            mapRef?.animateCamera(
                                CameraUpdateFactory.newCameraPosition(cp),
                                500,
                            )
                            viewModel.setFollowingUser(true)
                        }
                    },
                    modifier = Modifier.size(buttonSize),
                    containerColor = if (hasLocation) {
                        if (isDriving) Color(0xDD1565C0) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                    } else {
                        drivingContainerColor
                    },
                    contentColor = if (hasLocation) {
                        if (isDriving) Color.White else MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        drivingContentColor.copy(alpha = 0.4f)
                    },
                ) {
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = "現在地に戻る",
                        modifier = Modifier.size(iconSize),
                    )
                }
            }

            // 速度HUD（走行中のみ表示）
            AnimatedVisibility(
                visible = isDriving,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp),
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Text(
                    text = "${locationState.speedKmh.toInt()} km/h",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier
                        .background(
                            Color.Black.copy(alpha = 0.7f),
                            RoundedCornerShape(16.dp),
                        )
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                )
            }

        }

        // 道の駅詳細ボトムシート
        selectedStation?.let { station ->
            StationDetailSheet(
                station = station,
                sheetState = sheetState,
                isFavorite = station.id in favoriteIds,
                isVisited = station.id in visitedIds,
                onToggleFavorite = { viewModel.toggleFavorite(station.id) },
                onToggleVisited = { viewModel.toggleVisited(station.id) },
                onDismiss = { viewModel.selectStation(null) },
            )
        }

        // カントリーサイン詳細ボトムシート
        val signSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        selectedSign?.let { sign ->
            CountrySignDetailSheet(
                sign = sign,
                sheetState = signSheetState,
                isFavorite = sign.id in favoriteSignIds,
                isVisited = sign.id in visitedSignIds,
                onToggleFavorite = { viewModel.toggleFavoriteSign(sign.id) },
                onToggleVisited = { viewModel.toggleVisitedSign(sign.id) },
                onDismiss = {
                    viewModel.focusSign(sign)
                    viewModel.selectSign(null)
                },
            )
        }
    }
}

private fun applyMapStyle(
    map: MapLibreMap,
    context: android.content.Context,
    tileType: String,
    googleMapsApiKey: String = "",
    googleMapsSession: String? = null,
    onReady: (Style) -> Unit,
) {
    if (isVectorTileType(tileType)) {
        map.setStyle(Style.Builder().fromUri(OFM_STYLE_URL)) { style ->
            addOverlayLayers(style)
            addBoundaryLayer(style, context)
            onReady(style)
        }
    } else {
        val styleJson = buildFullStyleJson(tileType, apiKey = googleMapsApiKey, session = googleMapsSession)
        map.setStyle(Style.Builder().fromJson(styleJson)) { style ->
            addBoundaryLayer(style, context)
            onReady(style)
        }
    }
}

/** 北海道市町村境界線レイヤーを追加する */
private fun addBoundaryLayer(style: Style, context: android.content.Context) {
    try {
        val geoJson = context.assets.open("hokkaido_boundaries_simplified.geojson")
            .bufferedReader().use { it.readText() }
        val fc = FeatureCollection.fromJson(geoJson)
        style.addSource(GeoJsonSource(BOUNDARY_SOURCE, fc))
        style.addLayer(
            LineLayer(BOUNDARY_LAYER, BOUNDARY_SOURCE).withProperties(
                PropertyFactory.lineColor("rgba(255, 59, 48, 0.7)"),
                PropertyFactory.lineWidth(1.5f),
            )
        )
    } catch (_: Exception) {
        // GeoJSON ファイルが見つからない場合は無視
    }
}

/** ベクタータイルスタイルに現在地レイヤーを追加する */
private fun addOverlayLayers(style: Style) {
    val emptyFc = FeatureCollection.fromFeatures(emptyList())
    style.addSource(GeoJsonSource(USER_LOCATION_SOURCE, emptyFc))

    // 現在地マーカー（白フチ）
    style.addLayer(
        org.maplibre.android.style.layers.CircleLayer(USER_LOCATION_OUTLINE_LAYER, USER_LOCATION_SOURCE).withProperties(
            org.maplibre.android.style.layers.PropertyFactory.circleRadius(10f),
            org.maplibre.android.style.layers.PropertyFactory.circleColor("#FFFFFF"),
        )
    )

    // 現在地マーカー（青丸）
    style.addLayer(
        org.maplibre.android.style.layers.CircleLayer(USER_LOCATION_LAYER, USER_LOCATION_SOURCE).withProperties(
            org.maplibre.android.style.layers.PropertyFactory.circleRadius(7f),
            org.maplibre.android.style.layers.PropertyFactory.circleColor("#4285F4"),
        )
    )
}

/** 非同期データ（現在地）の監視を開始 */
private fun startMarkerObservers(
    scopeHolder: androidx.compose.runtime.MutableState<CoroutineScope?>,
    style: Style,
    viewModel: MapViewModel,
) {
    scopeHolder.value?.cancel()
    scopeHolder.value = CoroutineScope(Dispatchers.Main + Job()).also { scope ->
        scope.launch {
            viewModel.locationState.collect { loc ->
                if (loc.latitude == 0.0 && loc.longitude == 0.0) return@collect
                val source = try {
                    style.getSourceAs<GeoJsonSource>(USER_LOCATION_SOURCE)
                } catch (_: IllegalStateException) { null }
                    ?: return@collect
                val point = Point.fromLngLat(loc.longitude, loc.latitude)
                source.setGeoJson(
                    FeatureCollection.fromFeatures(listOf(Feature.fromGeometry(point)))
                )
            }
        }
    }
}
