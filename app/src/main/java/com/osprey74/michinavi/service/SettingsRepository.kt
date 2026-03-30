package com.osprey74.michinavi.service

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.osprey74.michinavi.model.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val ZOOM_POSITION = stringPreferencesKey("zoom_position")
        val MAP_TILE_TYPE = stringPreferencesKey("map_tile_type")
        val GOOGLE_MAPS_API_KEY = stringPreferencesKey("google_maps_api_key")
        val FAVORITE_STATION_IDS = stringSetPreferencesKey("favorite_station_ids")
        val VISITED_STATION_IDS = stringSetPreferencesKey("visited_station_ids")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            zoomPosition = prefs[Keys.ZOOM_POSITION] ?: "right",
            mapTileType = prefs[Keys.MAP_TILE_TYPE] ?: "gsi_pale",
            googleMapsApiKey = prefs[Keys.GOOGLE_MAPS_API_KEY] ?: "",
        )
    }

    suspend fun updateSettings(settings: AppSettings) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ZOOM_POSITION] = settings.zoomPosition
            prefs[Keys.MAP_TILE_TYPE] = settings.mapTileType
            prefs[Keys.GOOGLE_MAPS_API_KEY] = settings.googleMapsApiKey
        }
    }

    suspend fun updateZoomPosition(position: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ZOOM_POSITION] = position
        }
    }

    // お気に入り
    val favoriteIdsFlow: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[Keys.FAVORITE_STATION_IDS] ?: emptySet()
    }

    suspend fun toggleFavorite(stationId: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.FAVORITE_STATION_IDS] ?: emptySet()
            prefs[Keys.FAVORITE_STATION_IDS] = if (stationId in current) {
                current - stationId
            } else {
                current + stationId
            }
        }
    }

    // 到達リスト
    val visitedIdsFlow: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[Keys.VISITED_STATION_IDS] ?: emptySet()
    }

    suspend fun toggleVisited(stationId: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.VISITED_STATION_IDS] ?: emptySet()
            prefs[Keys.VISITED_STATION_IDS] = if (stationId in current) {
                current - stationId
            } else {
                current + stationId
            }
        }
    }
}
