package com.osprey74.michinavi.ui.screen

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.osprey74.michinavi.model.AppSettings
import com.osprey74.michinavi.model.NearbyStation
import com.osprey74.michinavi.model.PoiItem
import com.osprey74.michinavi.model.RoadsideStation
import com.osprey74.michinavi.service.LocationService
import com.osprey74.michinavi.service.LocationState
import com.osprey74.michinavi.service.MapConstants
import com.osprey74.michinavi.service.PoiService
import com.osprey74.michinavi.service.RoadsideStationRepository
import com.osprey74.michinavi.service.RoadsideStationService
import com.osprey74.michinavi.service.SettingsRepository
import com.osprey74.michinavi.service.latitudeDeltaToZoomLevel
import com.osprey74.michinavi.service.zoomLevelForSpeed
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MapViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RoadsideStationRepository(application)
    private val stationService = RoadsideStationService(repository)
    val locationService = LocationService(application)
    private val settingsRepository = SettingsRepository(application)
    private val poiService = PoiService()

    // 位置情報
    val locationState: StateFlow<LocationState> = locationService.locationState

    // 設定
    val settings: StateFlow<AppSettings> = settingsRepository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    // 近隣道の駅（リスト表示用）
    private val _nearbyStations = MutableStateFlow<List<NearbyStation>>(emptyList())
    val nearbyStations: StateFlow<List<NearbyStation>> = _nearbyStations.asStateFlow()

    // 範囲内道の駅（地図ピン用）
    private val _stationsInRange = MutableStateFlow<List<NearbyStation>>(emptyList())
    val stationsInRange: StateFlow<List<NearbyStation>> = _stationsInRange.asStateFlow()

    // ビューポート内道の駅（地図表示用）
    private val _visibleStations = MutableStateFlow<List<RoadsideStation>>(emptyList())
    val visibleStations: StateFlow<List<RoadsideStation>> = _visibleStations.asStateFlow()

    // POI（施設スポット）
    private val _poiItems = MutableStateFlow<List<PoiItem>>(emptyList())
    val poiItems: StateFlow<List<PoiItem>> = _poiItems.asStateFlow()

    // 速度連動オートズーム
    private val _autoZoomLevel = MutableStateFlow(latitudeDeltaToZoomLevel(MapConstants.WIDE_ZOOM_DEGREES))
    val autoZoomLevel: StateFlow<Float> = _autoZoomLevel.asStateFlow()

    // 選択中の道の駅（詳細シート表示用）
    private val _selectedStation = MutableStateFlow<RoadsideStation?>(null)
    val selectedStation: StateFlow<RoadsideStation?> = _selectedStation.asStateFlow()

    // 位置追従モード
    private val _isFollowingUser = MutableStateFlow(true)
    val isFollowingUser: StateFlow<Boolean> = _isFollowingUser.asStateFlow()

    // お気に入り
    val favoriteIds: StateFlow<Set<String>> = settingsRepository.favoriteIdsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    init {
        // 位置情報の変化に応じて近隣道の駅を更新
        locationService.locationState
            .onEach { loc ->
                if (loc.latitude != 0.0 || loc.longitude != 0.0) {
                    updateNearby(loc)
                    updateAutoZoom(loc.speedKmh)
                }
            }
            .launchIn(viewModelScope)
    }

    private fun updateNearby(loc: LocationState) {
        val (inRange, nearby) = stationService.updateNearbyStations(
            lat = loc.latitude,
            lon = loc.longitude,
            heading = loc.heading,
            speedKmh = loc.speedKmh,
        )
        _stationsInRange.value = inRange
        _nearbyStations.value = nearby
    }

    private fun updateAutoZoom(speedKmh: Double) {
        val latDelta = zoomLevelForSpeed(speedKmh)
        _autoZoomLevel.value = latitudeDeltaToZoomLevel(latDelta)
    }

    /**
     * 地図のビューポートが変わった時に呼ばれる
     */
    fun onMapRegionChanged(
        centerLat: Double,
        centerLon: Double,
        latitudeDelta: Double,
        longitudeDelta: Double,
    ) {
        _visibleStations.value = stationService.updateVisibleStations(
            centerLat, centerLon, latitudeDelta, longitudeDelta,
        )
        // POI取得（デバウンスはUI側で行う）
        viewModelScope.launch {
            val categories = poiService.enabledCategories(settings.value)
            val items = poiService.fetchPois(
                categories, centerLat, centerLon, latitudeDelta, longitudeDelta,
            )
            _poiItems.value = items
        }
    }

    fun selectStation(station: RoadsideStation?) {
        _selectedStation.value = station
    }

    fun setFollowingUser(following: Boolean) {
        _isFollowingUser.value = following
    }

    fun updateSettings(settings: AppSettings) {
        viewModelScope.launch {
            settingsRepository.updateSettings(settings)
        }
    }

    fun toggleFavorite(stationId: String) {
        viewModelScope.launch {
            settingsRepository.toggleFavorite(stationId)
        }
    }

    fun getFavoriteStations(): List<RoadsideStation> {
        val ids = favoriteIds.value
        return repository.allStations.filter { it.id in ids }
    }

    fun toggleZoomPosition() {
        viewModelScope.launch {
            val current = settings.value.zoomPosition
            val newPosition = if (current == "right") "left" else "right"
            settingsRepository.updateZoomPosition(newPosition)
        }
    }

    /**
     * 都道府県→市町村→駅のグループ化（道の駅選択画面用）
     */
    fun stationsGroupedByPrefecture() = stationService.stationsGroupedByPrefecture()
}
