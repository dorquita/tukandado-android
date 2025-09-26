package com.tukandado.tukandadov2

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.tukandado.tukandadov2.data.SessionManager
import com.tukandado.tukandadov2.navigation.NavGraph
import com.tukandado.tukandadov2.ui.MainScreen
import com.tukandado.tukandadov2.ui.theme.TukandadoV2Theme
import kotlinx.coroutines.flow.first

class MainActivity : ComponentActivity() {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        // Instala el SplashScreen antes de todo
        val splashScreen = installSplashScreen()

        // Controla si mantener visible el splash hasta que cargue sesión
        var keepSplash = true
        splashScreen.setKeepOnScreenCondition { keepSplash }

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
            val sessionManager = remember { SessionManager(this) }

            // Si getDarkTheme() es Flow<Boolean?>:
            val isDarkPref by sessionManager.getDarkTheme().collectAsState(initial = null)
            val isDark = isDarkPref ?: androidx.compose.foundation.isSystemInDarkTheme()

            TukandadoV2Theme(darkTheme = isDark, dynamicColor = true) {
                val navController = rememberNavController()
                var isLoggedIn by remember { mutableStateOf<Boolean?>(null) }
                var initialRouteAfterLogin by remember { mutableStateOf<String?>(null) } // ⬅️ NUEVO

                LaunchedEffect(Unit) {
                    val sm = SessionManager(applicationContext)
                    isLoggedIn = sm.hasValidSessionOnce()
                    keepSplash = false
                }

                when (isLoggedIn) {
                    true -> MainScreen(initialRoute = initialRouteAfterLogin).also {
                        // evita re-navegar en recomposiciones
                        initialRouteAfterLogin = null
                    }
                    false -> {
                        // ⬇️ PASA ESTE LAMBDA
                        NavGraph(navController = navController, onLoggedIn = { route ->          // ⬅️ NUEVO callback con ruta
                            initialRouteAfterLogin = route // guarda dónde ir (Home o activeReservation/...)
                            isLoggedIn = true              // dispara el “root switch” a MainScreen()
                        })
                    }
                    null -> Text("Cargando...")
                }
            }
        }
    }
}