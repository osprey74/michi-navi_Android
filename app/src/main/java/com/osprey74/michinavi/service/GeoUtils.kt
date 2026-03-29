package com.osprey74.michinavi.service

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

object GeoUtils {
    private const val EARTH_RADIUS_KM = 6371.0

    /**
     * Haversine公式による2点間の距離（km）
     */
    fun haversine(
        fromLat: Double, fromLon: Double,
        toLat: Double, toLon: Double,
    ): Double {
        val dLat = Math.toRadians(toLat - fromLat)
        val dLon = Math.toRadians(toLon - fromLon)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(fromLat)) *
            cos(Math.toRadians(toLat)) *
            sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_KM * c
    }

    /**
     * 方位角計算（0–360°）
     */
    fun bearing(
        fromLat: Double, fromLon: Double,
        toLat: Double, toLon: Double,
    ): Double {
        val dLon = Math.toRadians(toLon - fromLon)
        val lat1 = Math.toRadians(fromLat)
        val lat2 = Math.toRadians(toLat)
        val x = sin(dLon) * cos(lat2)
        val y = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
        val bearing = Math.toDegrees(atan2(x, y))
        return (bearing + 360) % 360
    }

    /**
     * 前方判定（進行方向に対して閾値角度以内か）
     */
    fun isAhead(heading: Double, bearingToTarget: Double, threshold: Double = 45.0): Boolean {
        var diff = bearingToTarget - heading
        while (diff > 180) diff -= 360.0
        while (diff < -180) diff += 360.0
        return abs(diff) <= threshold
    }

    /**
     * 16方位変換
     */
    private val CARDINALS = arrayOf(
        "N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
        "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW",
    )

    fun cardinalDirection(bearing: Double): String {
        val index = ((bearing + 11.25) % 360 / 22.5).toInt()
        return CARDINALS[index % 16]
    }
}
