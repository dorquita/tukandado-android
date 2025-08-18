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
            TukandadoV2Theme(dynamicColor = false) {
                val navController = rememberNavController()
                var isLoggedIn by remember { mutableStateOf<Boolean?>(null) }

                Log.d("IsLoggedIn:", isLoggedIn.toString())

                LaunchedEffect(Unit) {
                    /*val token = SessionManager(applicationContext).getAccessToken().first()
                    Log.d("token:", token.toString())
                    isLoggedIn = token != null*/

                    val sm = SessionManager(applicationContext)
                    isLoggedIn = sm.hasValidSessionOnce() // comprueba access y refresh no vacíos
                    keepSplash = false
                }

                when (isLoggedIn) {
                    true -> MainScreen()
                    false -> {
                        NavGraph(navController = navController)
                    }
                    null -> Text("Cargando...")
                }
            }
        }
    }
}