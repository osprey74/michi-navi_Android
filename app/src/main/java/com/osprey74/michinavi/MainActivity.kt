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
import com.osprey74.michinavi.ui.screen.RandomSignDrawScreen
import com.osprey74.michinavi.ui.screen.SettingsScreen
import com.osprey74.michinavi.ui.screen.UnifiedPickerScreen
import com.osprey74.michinavi.ui.theme.MichiNaviTheme

private enum class Screen { Map, Settings, Picker, RandomDraw }

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
                        onOpenPicker = { currentScreen = Screen.Picker },
                    )
                    Screen.Settings -> SettingsScreen(
                        viewModel = viewModel,
                        onBack = { currentScreen = Screen.Map },
                    )
                    Screen.Picker -> UnifiedPickerScreen(
                        viewModel = viewModel,
                        onStationSelected = { station ->
                            viewModel.selectStation(station)
                            currentScreen = Screen.Map
                        },
                        onSignSelected = { sign ->
                            viewModel.selectSign(sign)
                            viewModel.focusSign(sign)
                            currentScreen = Screen.Map
                        },
                        onOpenRandomDraw = { currentScreen = Screen.RandomDraw },
                        onBack = { currentScreen = Screen.Map },
                    )
                    Screen.RandomDraw -> RandomSignDrawScreen(
                        viewModel = viewModel,
                        onShowOnMap = { sign ->
                            viewModel.focusSign(sign)
                            currentScreen = Screen.Map
                        },
                        onShowDetail = { sign ->
                            viewModel.selectSign(sign)
                            currentScreen = Screen.Map
                        },
                        onBack = { currentScreen = Screen.Picker },
                    )
                }
            }
        }
    }
}
