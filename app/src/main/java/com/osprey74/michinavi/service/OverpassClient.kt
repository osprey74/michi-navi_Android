package com.osprey74.michinavi.service

import com.osprey74.michinavi.model.PoiCategory
import com.osprey74.michinavi.model.PoiItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import android.util.Log
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

private const val TAG = "OverpassClient"

class OverpassClient {

    companion object {
        private const val OVERPASS_API_URL = "https://overpass-api.de/api/interpreter"
        private const val TIMEOUT_MS = 10_000
    }

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * 指定カテゴリのPOIをバウンディングボックス内で検索
     */
    suspend fun fetchPois(
        categories: List<PoiCategory>,
        south: Double,
        west: Double,
        north: Double,
        east: Double,
    ): List<PoiItem> = withContext(Dispatchers.IO) {
        if (categories.isEmpty()) return@withContext emptyList()

        val query = buildQuery(categories, south, west, north, east)
        val response = executeQuery(query)
        parseResponse(response, categories)
    }

    private fun buildQuery(
        categories: List<PoiCategory>,
        south: Double,
        west: Double,
        north: Double,
        east: Double,
    ): String {
        val bbox = "$south,$west,$north,$east"
        val filters = categories.joinToString("\n") { cat ->
            """  node["${cat.osmTag}"="${cat.osmValue}"]($bbox);"""
        }
        return """
            [out:json][timeout:${ TIMEOUT_MS / 1000 }];
            (
            $filters
            );
            out body;
        """.trimIndent()
    }

    private fun executeQuery(query: String): String {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val url = URL("$OVERPASS_API_URL?data=$encodedQuery")
        val connection = url.openConnection() as HttpURLConnection
        return try {
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.requestMethod = "GET"
            val code = connection.responseCode
            if (code != 200) {
                Log.w(TAG, "Overpass API returned $code")
                return ""
            }
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun parseResponse(
        responseBody: String,
        categories: List<PoiCategory>,
    ): List<PoiItem> {
        if (responseBody.isBlank()) return emptyList()

        val response = json.decodeFromString<OverpassResponse>(responseBody)
        val categoryByTag = categories.associateBy { "${it.osmTag}=${it.osmValue}" }

        return response.elements.mapNotNull { element ->
            val lat = element.lat ?: return@mapNotNull null
            val lon = element.lon ?: return@mapNotNull null
            val tags = element.tags ?: return@mapNotNull null

            val category = categoryByTag.entries.firstOrNull { (key, _) ->
                val (tag, value) = key.split("=")
                tags[tag] == value
            }?.value ?: return@mapNotNull null

            PoiItem(
                id = element.id,
                category = category,
                name = tags["name"],
                latitude = lat,
                longitude = lon,
            )
        }
    }

    @Serializable
    private data class OverpassResponse(
        val elements: List<OverpassElement> = emptyList(),
    )

    @Serializable
    private data class OverpassElement(
        val id: Long,
        val lat: Double? = null,
        val lon: Double? = null,
        val tags: Map<String, String>? = null,
    )
}
