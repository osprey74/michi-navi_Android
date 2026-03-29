package com.osprey74.michinavi.auto

import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.CarIcon
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.osprey74.michinavi.service.ServiceLocator

class NearbyStationsScreen(carContext: CarContext) : Screen(carContext) {

    private val handler = Handler(Looper.getMainLooper())
    private val refreshRunnable = object : Runnable {
        override fun run() {
            invalidate()
            handler.postDelayed(this, 5_000L)
        }
    }

    init {
        handler.postDelayed(refreshRunnable, 5_000L)
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                handler.removeCallbacks(refreshRunnable)
            }
        })
    }

    override fun onGetTemplate(): Template {
        val locationService = ServiceLocator.locationService
        val stationService = ServiceLocator.roadsideStationService
        val loc = locationService.locationState.value

        val (_, nearbyStations) = stationService.updateNearbyStations(
            lat = loc.latitude,
            lon = loc.longitude,
            heading = loc.heading,
            speedKmh = loc.speedKmh,
        )

        val listBuilder = ItemList.Builder()

        if (nearbyStations.isEmpty()) {
            listBuilder.setNoItemsMessage("近くに道の駅が見つかりません")
        } else {
            // Android Auto は ItemList 最大6件
            nearbyStations.take(6).forEach { nearby ->
                listBuilder.addItem(
                    Row.Builder()
                        .setTitle(nearby.station.name)
                        .addText("${nearby.distanceText} · ${nearby.cardinalDirection}")
                        .addText(nearby.station.roadName ?: "")
                        .setOnClickListener {
                            val uri = Uri.parse(
                                "google.navigation:q=${nearby.station.latitude},${nearby.station.longitude}"
                            )
                            carContext.startCarApp(Intent(Intent.ACTION_VIEW, uri))
                        }
                        .build()
                )
            }
        }

        return ListTemplate.Builder()
            .setTitle("道の駅")
            .setHeaderAction(Action.APP_ICON)
            .setActionStrip(
                androidx.car.app.model.ActionStrip.Builder()
                    .addAction(
                        Action.Builder()
                            .setTitle("情報")
                            .setOnClickListener {
                                screenManager.push(DriveInfoScreen(carContext))
                            }
                            .build()
                    )
                    .build()
            )
            .setSingleList(listBuilder.build())
            .build()
    }

}
