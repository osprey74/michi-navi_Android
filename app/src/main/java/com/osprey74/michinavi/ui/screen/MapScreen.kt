package com.osprey74.michinavi.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.Style
import org.ramani.compose.CameraPosition
import org.ramani.compose.MapLibre

private const val OPENFREEMAP_STYLE = "https://tiles.openfreemap.org/styles/liberty"

// 日本の中心付近（東京）
private val DEFAULT_POSITION = LatLng(35.681236, 139.767125)
private const val DEFAULT_ZOOM = 10.0

@Composable
fun MapScreen() {
    val cameraPosition = rememberSaveable {
        CameraPosition(
            target = DEFAULT_POSITION,
            zoom = DEFAULT_ZOOM,
        )
    }

    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            MapLibre(
                modifier = Modifier.fillMaxSize(),
                styleBuilder = Style.Builder().fromUri(OPENFREEMAP_STYLE),
                cameraPosition = cameraPosition,
            )
        }
    }
}
