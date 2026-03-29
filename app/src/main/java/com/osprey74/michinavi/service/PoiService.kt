package com.osprey74.michinavi.service

import com.osprey74.michinavi.model.AppSettings
import com.osprey74.michinavi.model.PoiCategory
import com.osprey74.michinavi.model.PoiItem
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class PoiService {

    private val client = OverpassClient()
    private val mutex = Mutex()

    // メモリキャッシュ（カテゴリ→POIリスト）
    private val cache = mutableMapOf<PoiCategory, CacheEntry>()

    private data class CacheEntry(
        val items: List<PoiItem>,
        val south: Double,
        val west: Double,
        val north: Double,
        val east: Double,
        val timestamp: Long,
    ) {
        fun covers(s: Double, w: Double, n: Double, e: Double): Boolean =
            s >= south && w >= west && n <= north && e <= east

        fun isExpired(): Boolean =
            System.currentTimeMillis() - timestamp > TTL_MS
    }

    companion object {
        private const val TTL_MS = 24 * 60 * 60 * 1000L // 24時間
        private const val BBOX_PADDING = 0.02 // バウンディングボックスの余白（約2km）
    }

    /**
     * 設定に基づいて有効なカテゴリを返す
     */
    fun enabledCategories(settings: AppSettings): List<PoiCategory> = buildList {
        if (settings.showGasStations) add(PoiCategory.GAS_STATION)
        if (settings.showFoodMarkets) add(PoiCategory.CONVENIENCE_STORE)
        if (settings.showRestaurants) add(PoiCategory.RESTAURANT)
        if (settings.showParking) add(PoiCategory.PARKING)
        if (settings.showRvParks) add(PoiCategory.RV_PARK)
    }

    /**
     * ビューポート内のPOIを取得（キャッシュ活用）
     */
    suspend fun fetchPois(
        categories: List<PoiCategory>,
        centerLat: Double,
        centerLon: Double,
        latitudeDelta: Double,
        longitudeDelta: Double,
    ): List<PoiItem> = mutex.withLock {
        val south = centerLat - latitudeDelta / 2
        val north = centerLat + latitudeDelta / 2
        val west = centerLon - longitudeDelta / 2
        val east = centerLon + longitudeDelta / 2

        // キャッシュが有効なカテゴリと、再取得が必要なカテゴリに分ける
        val cachedItems = mutableListOf<PoiItem>()
        val uncachedCategories = mutableListOf<PoiCategory>()

        for (category in categories) {
            val entry = cache[category]
            if (entry != null && !entry.isExpired() && entry.covers(south, west, north, east)) {
                // キャッシュヒット: ビューポート内のアイテムのみ返す
                cachedItems += entry.items.filter {
                    it.latitude in south..north && it.longitude in west..east
                }
            } else {
                uncachedCategories += category
            }
        }

        if (uncachedCategories.isEmpty()) {
            return@withLock cachedItems
        }

        // 余白付きバウンディングボックスで取得（次回のスクロールに備える）
        val paddedSouth = south - BBOX_PADDING
        val paddedNorth = north + BBOX_PADDING
        val paddedWest = west - BBOX_PADDING
        val paddedEast = east + BBOX_PADDING

        val fetched = try {
            client.fetchPois(uncachedCategories, paddedSouth, paddedWest, paddedNorth, paddedEast)
        } catch (_: Exception) {
            emptyList()
        }

        // カテゴリ別にキャッシュ保存
        val fetchedByCategory = fetched.groupBy { it.category }
        for (category in uncachedCategories) {
            val items = fetchedByCategory[category] ?: emptyList()
            cache[category] = CacheEntry(
                items = items,
                south = paddedSouth,
                west = paddedWest,
                north = paddedNorth,
                east = paddedEast,
                timestamp = System.currentTimeMillis(),
            )
        }

        // ビューポート内のアイテムのみ返す
        val fetchedInView = fetched.filter {
            it.latitude in south..north && it.longitude in west..east
        }

        cachedItems + fetchedInView
    }

    fun clearCache() {
        cache.clear()
    }
}
