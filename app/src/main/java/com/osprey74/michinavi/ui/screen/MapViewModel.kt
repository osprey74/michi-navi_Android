package com.osprey74.michinavi.ui.screen

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.osprey74.michinavi.model.AppSettings
import com.osprey74.michinavi.model.CountrySign
import com.osprey74.michinavi.model.NearbyStation
import com.osprey74.michinavi.model.RoadsideStation
import com.osprey74.michinavi.service.LocationState
import com.osprey74.michinavi.service.MapConstants
import com.osprey74.michinavi.service.ServiceLocator
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

    private val repository = ServiceLocator.roadsideStationRepository
    private val stationService = ServiceLocator.roadsideStationService
    val locationService = ServiceLocator.locationService
    private val settingsRepository = ServiceLocator.settingsRepository
    private val countrySignRepository = ServiceLocator.countrySignRepository
    private val countrySignService = ServiceLocator.countrySignService

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

    // 速度連動オートズーム
    private val _autoZoomLevel = MutableStateFlow(latitudeDeltaToZoomLevel(MapConstants.WIDE_ZOOM_DEGREES))
    val autoZoomLevel: StateFlow<Float> = _autoZoomLevel.asStateFlow()

    // 選択中の道の駅（詳細シート表示用）
    private val _selectedStation = MutableStateFlow<RoadsideStation?>(null)
    val selectedStation: StateFlow<RoadsideStation?> = _selectedStation.asStateFlow()

    // 走行中判定
    private val _isDriving = MutableStateFlow(false)
    val isDriving: StateFlow<Boolean> = _isDriving.asStateFlow()

    // 位置追従モード
    private val _isFollowingUser = MutableStateFlow(true)
    val isFollowingUser: StateFlow<Boolean> = _isFollowingUser.asStateFlow()

    // オートズーム一時停止
    private val _isAutoZoomPaused = MutableStateFlow(false)
    val isAutoZoomPaused: StateFlow<Boolean> = _isAutoZoomPaused.asStateFlow()

    // お気に入り
    val favoriteIds: StateFlow<Set<String>> = settingsRepository.favoriteIdsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    // 到達リスト
    val visitedIds: StateFlow<Set<String>> = settingsRepository.visitedIdsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    // --- カントリーサイン ---

    // ビューポート内CS
    private val _visibleSigns = MutableStateFlow<List<CountrySign>>(emptyList())
    val visibleSigns: StateFlow<List<CountrySign>> = _visibleSigns.asStateFlow()

    // 選択中のCS（詳細シート表示用）
    private val _selectedSign = MutableStateFlow<CountrySign?>(null)
    val selectedSign: StateFlow<CountrySign?> = _selectedSign.asStateFlow()

    // 地図フォーカス要求（CSの位置にカメラ移動）
    private val _mapFocusSign = MutableStateFlow<CountrySign?>(null)
    val mapFocusSign: StateFlow<CountrySign?> = _mapFocusSign.asStateFlow()

    // CS お気に入り
    val favoriteSignIds: StateFlow<Set<String>> = settingsRepository.favoriteSignIdsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    // CS 踏破リスト
    val visitedSignIds: StateFlow<Set<String>> = settingsRepository.visitedSignIdsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    // CS マーカー表示
    val showCountrySignMarkers: StateFlow<Boolean> = settingsRepository.showCountrySignMarkersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    init {
        // 位置情報の変化に応じて近隣道の駅を更新
        locationService.locationState
            .onEach { loc ->
                if (loc.latitude != 0.0 || loc.longitude != 0.0) {
                    updateNearby(loc)
                    updateAutoZoom(loc.speedKmh)
                    _isDriving.value = loc.speedKmh > MapConstants.SPEED_DRIVING_THRESHOLD_KMH
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

    fun onMapRegionChanged(
        centerLat: Double,
        centerLon: Double,
        latitudeDelta: Double,
        longitudeDelta: Double,
    ) {
        _visibleStations.value = stationService.updateVisibleStations(
            centerLat, centerLon, latitudeDelta, longitudeDelta,
        )
        if (showCountrySignMarkers.value) {
            _visibleSigns.value = countrySignService.updateVisibleSigns(
                centerLat, centerLon, latitudeDelta, longitudeDelta,
            )
        } else {
            _visibleSigns.value = emptyList()
        }
    }

    fun selectStation(station: RoadsideStation?) {
        _selectedSign.value = null // 相互排他
        _selectedStation.value = station
        if (station != null) {
            _isFollowingUser.value = false
        }
    }

    fun selectStationById(id: String) {
        _selectedSign.value = null
        _selectedStation.value = repository.allStations.find { it.id == id }
    }

    fun setFollowingUser(following: Boolean) {
        _isFollowingUser.value = following
    }

    fun pauseAutoZoom(durationMs: Long = 30_000L) {
        _isAutoZoomPaused.value = true
        viewModelScope.launch {
            kotlinx.coroutines.delay(durationMs)
            _isAutoZoomPaused.value = false
        }
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

    fun toggleVisited(stationId: String) {
        viewModelScope.launch {
            settingsRepository.toggleVisited(stationId)
        }
    }

    fun getFavoriteStations(): List<RoadsideStation> {
        val ids = favoriteIds.value
        return repository.allStations.filter { it.id in ids }
    }

    fun getVisitedStations(): List<RoadsideStation> {
        val ids = visitedIds.value
        return repository.allStations.filter { it.id in ids }
    }

    fun toggleZoomPosition() {
        viewModelScope.launch {
            val current = settings.value.zoomPosition
            val newPosition = if (current == "right") "left" else "right"
            settingsRepository.updateZoomPosition(newPosition)
        }
    }

    fun stationsGroupedByPrefecture() = stationService.stationsGroupedByPrefecture()

    val allStations: List<RoadsideStation> get() = repository.allStations

    // --- カントリーサイン操作 ---

    fun selectSign(sign: CountrySign?) {
        _selectedStation.value = null // 相互排他
        _selectedSign.value = sign
        if (sign != null) {
            _isFollowingUser.value = false
        }
    }

    fun selectSignById(id: String) {
        _selectedStation.value = null
        _selectedSign.value = countrySignRepository.allSigns.find { it.id == id }
    }

    fun focusSign(sign: CountrySign) {
        _mapFocusSign.value = sign
    }

    fun clearFocusSign() {
        _mapFocusSign.value = null
    }

    fun toggleFavoriteSign(signId: String) {
        viewModelScope.launch {
            settingsRepository.toggleFavoriteSign(signId)
        }
    }

    fun toggleVisitedSign(signId: String) {
        viewModelScope.launch {
            settingsRepository.toggleVisitedSign(signId)
        }
    }

    fun toggleShowCountrySignMarkers() {
        viewModelScope.launch {
            val current = showCountrySignMarkers.value
            settingsRepository.setShowCountrySignMarkers(!current)
        }
    }

    fun getFavoriteCountrySigns(): List<CountrySign> {
        val ids = favoriteSignIds.value
        return countrySignRepository.allSigns.filter { it.id in ids }
    }

    fun getVisitedCountrySigns(): List<CountrySign> {
        val ids = visitedSignIds.value
        return countrySignRepository.allSigns.filter { it.id in ids }
    }

    fun signsGroupedBySubprefecture() = countrySignService.signsGroupedBySubprefecture()

    fun drawRandomUnvisitedSign(): CountrySign? =
        countrySignService.drawRandomUnvisitedSign(visitedSignIds.value)

    val allSigns: List<CountrySign> get() = countrySignRepository.allSigns
}
