package com.tukandado.tukandadov2

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.tukandado.tukandadov2.data.SessionManager
import com.tukandado.tukandadov2.navigation.NavGraph
import com.tukandado.tukandadov2.ui.MainScreen
import com.tukandado.tukandadov2.ui.theme.TukandadoV2Theme
import kotlinx.coroutines.flow.first

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val permissionsToRequest = mutableListOf<String>()

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
            }

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN)
            }

            if (permissionsToRequest.isNotEmpty()) {
                ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), 123)
            }
        }

        setContent {
            TukandadoV2Theme {
                var isLoggedIn by remember { mutableStateOf<Boolean?>(null) }

                LaunchedEffect(Unit) {
                    val token = SessionManager(applicationContext).getAccessToken().first()
                    isLoggedIn = token != null
                }

                when (isLoggedIn) {
                    true -> MainScreen()
                    false -> {
                        val navController = rememberNavController()
                        NavGraph(navController = navController, startDestination = "login")
                    }
                    null -> Text("Cargando...")
                }
            }
        }
    }
}