package com.osprey74.michinavi.auto

import android.content.Intent
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.osprey74.michinavi.service.ServiceLocator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MichiNaviCarSession : Session() {

    private val scope = CoroutineScope(Dispatchers.Main + Job())

    init {
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                scope.cancel()
            }
        })
    }

    override fun onCreateScreen(intent: Intent): Screen {
        // 位置情報の更新を開始
        scope.launch {
            ServiceLocator.locationService.locationUpdates().collect { loc ->
                if (loc.latitude != 0.0 || loc.longitude != 0.0) {
                    val service = ServiceLocator.roadsideStationService
                    service.updateNearbyStations(
                        lat = loc.latitude,
                        lon = loc.longitude,
                        heading = loc.heading,
                        speedKmh = loc.speedKmh,
                    )
                }
            }
        }
        return NearbyStationsScreen(carContext)
    }

}
