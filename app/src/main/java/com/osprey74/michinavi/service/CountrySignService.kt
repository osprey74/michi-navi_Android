package com.osprey74.michinavi.service

import com.osprey74.michinavi.model.CountrySign

class CountrySignService(
    private val repository: CountrySignRepository,
) {
    private val allSigns: List<CountrySign>
        get() = repository.allSigns

    /**
     * 表示領域内のカントリーサインをフィルタ（ビューポート連動）
     */
    fun updateVisibleSigns(
        centerLat: Double,
        centerLon: Double,
        latitudeDelta: Double,
        longitudeDelta: Double,
    ): List<CountrySign> {
        val minLat = centerLat - latitudeDelta / 2
        val maxLat = centerLat + latitudeDelta / 2
        val minLon = centerLon - longitudeDelta / 2
        val maxLon = centerLon + longitudeDelta / 2
        return allSigns.filter { sign ->
            sign.centroidLat in minLat..maxLat &&
                sign.centroidLon in minLon..maxLon
        }
    }

    /**
     * 振興局別グループ化
     */
    fun signsGroupedBySubprefecture(): Map<String, List<CountrySign>> {
        return allSigns
            .groupBy { it.subprefectureOffice }
            .toSortedMap(compareBy { CountrySignRepository.subprefectureSortOrder(it) })
    }

    /**
     * 未踏破サインからランダムに1件選択
     */
    fun drawRandomUnvisitedSign(visitedIds: Set<String>): CountrySign? {
        return allSigns.filter { it.id !in visitedIds }.randomOrNull()
    }
}
