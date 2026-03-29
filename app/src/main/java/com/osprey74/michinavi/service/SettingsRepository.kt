package com.osprey74.michinavi.service

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.osprey74.michinavi.model.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val ZOOM_POSITION = stringPreferencesKey("zoom_position")
        val SHOW_GAS_STATIONS = booleanPreferencesKey("show_gas_stations")
        val SHOW_FOOD_MARKETS = booleanPreferencesKey("show_food_markets")
        val SHOW_RESTAURANTS = booleanPreferencesKey("show_restaurants")
        val SHOW_PARKING = booleanPreferencesKey("show_parking")
        val SHOW_RV_PARKS = booleanPreferencesKey("show_rv_parks")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            zoomPosition = prefs[Keys.ZOOM_POSITION] ?: "right",
            showGasStations = prefs[Keys.SHOW_GAS_STATIONS] ?: true,
            showFoodMarkets = prefs[Keys.SHOW_FOOD_MARKETS] ?: false,
            showRestaurants = prefs[Keys.SHOW_RESTAURANTS] ?: false,
            showParking = prefs[Keys.SHOW_PARKING] ?: false,
            showRvParks = prefs[Keys.SHOW_RV_PARKS] ?: true,
        )
    }

    suspend fun updateSettings(settings: AppSettings) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ZOOM_POSITION] = settings.zoomPosition
            prefs[Keys.SHOW_GAS_STATIONS] = settings.showGasStations
            prefs[Keys.SHOW_FOOD_MARKETS] = settings.showFoodMarkets
            prefs[Keys.SHOW_RESTAURANTS] = settings.showRestaurants
            prefs[Keys.SHOW_PARKING] = settings.showParking
            prefs[Keys.SHOW_RV_PARKS] = settings.showRvParks
        }
    }

    suspend fun updateZoomPosition(position: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ZOOM_POSITION] = position
        }
    }

    suspend fun updatePoiVisibility(
        gasStations: Boolean? = null,
        foodMarkets: Boolean? = null,
        restaurants: Boolean? = null,
        parking: Boolean? = null,
        rvParks: Boolean? = null,
    ) {
        context.dataStore.edit { prefs ->
            gasStations?.let { prefs[Keys.SHOW_GAS_STATIONS] = it }
            foodMarkets?.let { prefs[Keys.SHOW_FOOD_MARKETS] = it }
            restaurants?.let { prefs[Keys.SHOW_RESTAURANTS] = it }
            parking?.let { prefs[Keys.SHOW_PARKING] = it }
            rvParks?.let { prefs[Keys.SHOW_RV_PARKS] = it }
        }
    }
}
