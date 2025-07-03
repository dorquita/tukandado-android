package com.tukandado.tukandadov2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.tukandado.tukandadov2.navigation.NavGraph
import com.tukandado.tukandadov2.ui.theme.TukandadoV2Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TukandadoV2Theme {
                NavGraph()
            }
        }
    }
}