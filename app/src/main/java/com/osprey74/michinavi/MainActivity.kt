package com.osprey74.michinavi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.osprey74.michinavi.ui.screen.MapScreen
import com.osprey74.michinavi.ui.screen.MapViewModel
import com.osprey74.michinavi.ui.screen.SettingsScreen
import com.osprey74.michinavi.ui.screen.StationPickerScreen
import com.osprey74.michinavi.ui.theme.MichiNaviTheme

private enum class Screen { Map, Settings, StationPicker }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MichiNaviTheme {
                val viewModel: MapViewModel = viewModel()
                var currentScreen by remember { mutableStateOf(Screen.Map) }

                when (currentScreen) {
                    Screen.Map -> MapScreen(
                        viewModel = viewModel,
                        onOpenSettings = { currentScreen = Screen.Settings },
                        onOpenStationPicker = { currentScreen = Screen.StationPicker },
                    )
                    Screen.Settings -> SettingsScreen(
                        viewModel = viewModel,
                        onBack = { currentScreen = Screen.Map },
                    )
                    Screen.StationPicker -> StationPickerScreen(
                        viewModel = viewModel,
                        onStationSelected = { station ->
                            viewModel.selectStation(station)
                            currentScreen = Screen.Map
                        },
                        onBack = { currentScreen = Screen.Map },
                    )
                }
            }
        }
    }
}
