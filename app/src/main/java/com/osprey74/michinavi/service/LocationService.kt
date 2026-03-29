package com.osprey74.michinavi.service

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow

data class LocationState(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val speedKmh: Double = 0.0,
    val heading: Double = 0.0,
    val accuracy: Float = 0f,
)

class LocationService(context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val sensorManager: SensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val _locationState = MutableStateFlow(LocationState())
    val locationState: StateFlow<LocationState> = _locationState.asStateFlow()

    private val locationRequest = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY,
        MapConstants.LOCATION_UPDATE_INTERVAL_MS,
    ).apply {
        setMinUpdateDistanceMeters(MapConstants.MIN_UPDATE_DISTANCE_M)
        setWaitForAccurateLocation(false)
    }.build()

    private var lastHeading: Double = 0.0

    private val compassListener = object : SensorEventListener {
        private val rotationMatrix = FloatArray(9)
        private val orientation = FloatArray(3)

        override fun onSensorChanged(event: SensorEvent) {
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            SensorManager.getOrientation(rotationMatrix, orientation)
            val azimuthDegrees = Math.toDegrees(orientation[0].toDouble())
            val normalized = (azimuthDegrees + 360) % 360

            // 5°以上変化で更新（iOS の headingFilter = 5 相当）
            if (kotlin.math.abs(normalized - lastHeading) >= MapConstants.HEADING_FILTER_DEGREES) {
                lastHeading = normalized
                _locationState.value = _locationState.value.copy(heading = normalized)
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    /**
     * 位置情報の更新を開始する Flow を返す。
     * collect を中止すると自動的に更新が停止される。
     */
    @SuppressLint("MissingPermission")
    fun locationUpdates(): Flow<LocationState> = callbackFlow {
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location: Location = result.lastLocation ?: return
                val speedKmh = if (location.speed < 0) 0.0 else location.speed * 3.6
                val newState = _locationState.value.copy(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    speedKmh = speedKmh,
                    accuracy = location.accuracy,
                )
                _locationState.value = newState
                trySend(newState)
            }
        }

        // 位置情報の更新を開始
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            callback,
            Looper.getMainLooper(),
        )

        // コンパスの更新を開始
        val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        rotationSensor?.let {
            sensorManager.registerListener(
                compassListener,
                it,
                SensorManager.SENSOR_DELAY_UI,
            )
        }

        awaitClose {
            fusedLocationClient.removeLocationUpdates(callback)
            sensorManager.unregisterListener(compassListener)
        }
    }
}
