package com.osprey74.michinavi.model

import com.osprey74.michinavi.service.GeoUtils

data class NearbyStation(
    val station: RoadsideStation,
    val distanceKm: Double,
    val bearing: Double,
) {
    val cardinalDirection: String
        get() = GeoUtils.cardinalDirection(bearing)

    val distanceText: String
        get() = if (distanceKm < 1.0) {
            "${(distanceKm * 1000).toInt()} m"
        } else {
            "%.1f km".format(distanceKm)
        }
}
