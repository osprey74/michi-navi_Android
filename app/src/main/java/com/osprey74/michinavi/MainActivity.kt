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
import com.osprey74.michinavi.ui.theme.MichiNaviTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MichiNaviTheme {
                val viewModel: MapViewModel = viewModel()
                var showSettings by remember { mutableStateOf(false) }

                if (showSettings) {
                    SettingsScreen(
                        viewModel = viewModel,
                        onBack = { showSettings = false },
                    )
                } else {
                    MapScreen(
                        viewModel = viewModel,
                        onOpenSettings = { showSettings = true },
                    )
                }
            }
        }
    }
}
