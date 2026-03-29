package com.osprey74.michinavi.auto

import android.os.Handler
import android.os.Looper
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.osprey74.michinavi.service.ServiceLocator

class DriveInfoScreen(carContext: CarContext) : Screen(carContext) {

    private val handler = Handler(Looper.getMainLooper())
    private val refreshRunnable = object : Runnable {
        override fun run() {
            invalidate()
            handler.postDelayed(this, 2_000L)
        }
    }

    init {
        handler.postDelayed(refreshRunnable, 2_000L)
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                handler.removeCallbacks(refreshRunnable)
            }
        })
    }

    override fun onGetTemplate(): Template {
        val loc = ServiceLocator.locationService.locationState.value
        val speedText = "${loc.speedKmh.toInt()} km/h"

        val pane = Pane.Builder()
            .addRow(
                Row.Builder()
                    .setTitle("現在速度")
                    .addText(speedText)
                    .build()
            )
            .addRow(
                Row.Builder()
                    .setTitle("方角")
                    .addText(com.osprey74.michinavi.service.GeoUtils.cardinalDirection(loc.heading))
                    .build()
            )
            .build()

        return PaneTemplate.Builder(pane)
            .setTitle("ドライブ情報")
            .setHeaderAction(Action.BACK)
            .build()
    }

}
