package com.osprey74.michinavi.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.rememberCameraPositionState

// 日本の中心付近（東京）
private val DEFAULT_POSITION = LatLng(35.681236, 139.767125)
private const val DEFAULT_ZOOM = 10f

@Composable
fun MapScreen() {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(DEFAULT_POSITION, DEFAULT_ZOOM)
    }

    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
            )
        }
    }
}
