package com.osprey74.michinavi.service

import android.app.Application

object ServiceLocator {

    private lateinit var application: Application

    val roadsideStationRepository by lazy { RoadsideStationRepository(application) }
    val roadsideStationService by lazy { RoadsideStationService(roadsideStationRepository) }
    val locationService by lazy { LocationService(application) }
    val settingsRepository by lazy { SettingsRepository(application) }
    val poiService by lazy { PoiService() }

    fun init(app: Application) {
        application = app
    }
}
