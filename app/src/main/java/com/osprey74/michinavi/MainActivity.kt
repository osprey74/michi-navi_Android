package com.osprey74.michinavi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.osprey74.michinavi.ui.screen.MapScreen
import com.osprey74.michinavi.ui.theme.MichiNaviTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MichiNaviTheme {
                MapScreen()
            }
        }
    }
}
