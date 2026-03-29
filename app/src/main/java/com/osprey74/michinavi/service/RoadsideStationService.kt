package com.osprey74.michinavi.service

import com.osprey74.michinavi.model.NearbyStation
import com.osprey74.michinavi.model.RoadsideStation

class RoadsideStationService(
    private val repository: RoadsideStationRepository,
) {
    private val allStations: List<RoadsideStation>
        get() = repository.allStations

    /**
     * 近隣道の駅を検索し、走行状態に応じてフィルタリング
     * @return Pair(stationsInRange: 範囲内全件, nearbyStations: 表示用リスト)
     */
    fun updateNearbyStations(
        lat: Double,
        lon: Double,
        heading: Double,
        speedKmh: Double = 0.0,
        maxDistanceKm: Double = 100.0,
        maxResults: Int = 10,
    ): Pair<List<NearbyStation>, List<NearbyStation>> {
        val allInRange = allStations
            .map { station ->
                val dist = GeoUtils.haversine(lat, lon, station.latitude, station.longitude)
                val brg = GeoUtils.bearing(lat, lon, station.latitude, station.longitude)
                NearbyStation(station, dist, brg)
            }
            .filter { it.distanceKm <= maxDistanceKm }
            .sortedBy { it.distanceKm }

        val isDriving = speedKmh > 5.0
        val nearbyStations = if (isDriving) {
            // 走行中: 前方±45°のみ
            allInRange.filter { GeoUtils.isAhead(heading, it.bearing) }
                .take(maxResults)
        } else {
            // 停車中: 全方位から近い順
            allInRange.take(maxResults)
        }

        return Pair(allInRange, nearbyStations)
    }

    /**
     * 表示領域内の道の駅をフィルタ（ビューポート連動）
     */
    fun updateVisibleStations(
        centerLat: Double,
        centerLon: Double,
        latitudeDelta: Double,
        longitudeDelta: Double,
    ): List<RoadsideStation> {
        val minLat = centerLat - latitudeDelta / 2
        val maxLat = centerLat + latitudeDelta / 2
        val minLon = centerLon - longitudeDelta / 2
        val maxLon = centerLon + longitudeDelta / 2
        return allStations.filter { station ->
            station.latitude in minLat..maxLat &&
                station.longitude in minLon..maxLon
        }
    }

    /**
     * 都道府県→市町村→駅のグループ化
     */
    fun stationsGroupedByPrefecture(): Map<String, Map<String, List<RoadsideStation>>> {
        return allStations
            .groupBy { it.prefecture ?: "不明" }
            .toSortedMap(compareBy { RoadsideStationRepository.prefectureSortOrder(it) })
            .mapValues { (_, stations) ->
                stations.groupBy { it.municipality ?: "不明" }
            }
    }
}
