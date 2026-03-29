package com.osprey74.michinavi.service

object MapConstants {
    // 1km ≈ 0.009度（緯度）
    const val WIDE_ZOOM_DEGREES = 120 * 0.009   // 1.08° = 短辺120km
    const val DETAIL_ZOOM_DEGREES = 0.3 * 0.009  // 0.0027° = 短辺300m

    // 位置情報更新設定
    const val LOCATION_UPDATE_INTERVAL_MS = 500L
    const val MIN_UPDATE_DISTANCE_M = 5f
    const val HEADING_FILTER_DEGREES = 5.0

    // 検索パラメータ
    const val AHEAD_THRESHOLD_DEGREES = 45.0
    const val SPEED_DRIVING_THRESHOLD_KMH = 5.0
    const val MAX_NEARBY_DISTANCE_KM = 100.0
    const val MAX_NEARBY_RESULTS = 10
}

/**
 * 速度に応じたズーム段階マッピング（MapKit latitudeDelta相当の度数を返す）
 *
 * 注意: MapLibre ではズームレベル（0–22）に変換が必要。
 * latitudeDeltaToZoomLevel() で変換すること。
 */
fun zoomLevelForSpeed(speedKmh: Double): Double {
    val fullSpan = MapConstants.WIDE_ZOOM_DEGREES
    return when {
        speedKmh < 5 -> fullSpan            // 停車中: 120km広域
        speedKmh < 30 -> fullSpan * 0.15     // 低速: 18km
        speedKmh < 60 -> fullSpan * 0.3      // 市街地: 36km
        speedKmh < 100 -> fullSpan * 0.5     // 一般道: 60km
        else -> fullSpan * 0.7               // 高速: 84km
    }
}

/**
 * MapKit風の緯度デルタ（度数）をMapLibreのズームレベルに変換
 */
fun latitudeDeltaToZoomLevel(latitudeDelta: Double): Float {
    // Google Maps: zoom = log2(360 / latitudeDelta)
    return (Math.log(360.0 / latitudeDelta) / Math.log(2.0)).toFloat()
}
