package com.tukandado.tukandadov2.navigation

import android.net.Uri
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.tukandado.tukandadov2.BuildConfig
import com.tukandado.tukandadov2.data.SessionManager
import com.tukandado.tukandadov2.ui.booking.ActiveBookingScreen
import com.tukandado.tukandadov2.ui.components.layout.AdminLockControlScreen
import com.tukandado.tukandadov2.ui.home.HomeScreen
import com.tukandado.tukandadov2.ui.home.ProfileScreen
import com.tukandado.tukandadov2.ui.lock.OpenLockScreen
import com.tukandado.tukandadov2.ui.login.LoginScreen

@Composable
fun NavGraph(navController: NavHostController) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }

    // Si estás maquetando, siembra sesión en debug
    LaunchedEffect(Unit) {
        if (BuildConfig.BYPASS_LOGIN) {
            sessionManager.ensureDevToken()
        }
    }

    val hasSession by sessionManager.hasValidSessionFlow.collectAsState(initial = false)
    val startDestination = when {
        BuildConfig.BYPASS_LOGIN -> Screen.Home.route // o la ruta que quieras maquetar
        hasSession -> Screen.Home.route
        else -> "login"
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable("login") {
            LoginScreen(
                onLoginSuccess = { user ->
                    val active = user.activeBooking
                    if (active != null) {
                        val encodedName = Uri.encode(active.lockId.lockName)
                        val encodedAlias = Uri.encode(active.lockId.lockAlias)
                        val encodedData = Uri.encode(active.lockId.lockData)
                        val encodedMac = Uri.encode(active.lockId.lockMac)

                        navController.navigate("activeReservation/$encodedName/$encodedAlias/$encodedData/$encodedMac") {
                            popUpTo("login") { inclusive = true }
                            launchSingleTop = true
                        }
                    } else {
                        navController.navigate(Screen.Home.route) {
                            popUpTo("login") { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                }
            )
        }

        composable(Screen.Home.route) { OpenLockScreen(navController) }
        composable(Screen.Locks.route) { OpenLockScreen(navController) }
        composable(Screen.Clubs.route) { HomeScreen(navController) }
        composable(Screen.Profile.route) { ProfileScreen(navController) }

        composable(
            route = "activeReservation/{lockName}/{lockAlias}/{lockData}/{lockMac}",
            arguments = listOf(
                navArgument("lockName") { type = NavType.StringType },
                navArgument("lockAlias") { type = NavType.StringType },
                navArgument("lockData") { type = NavType.StringType },
                navArgument("lockMac") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val lockName = backStackEntry.arguments?.getString("lockName") ?: ""
            val lockAlias = backStackEntry.arguments?.getString("lockAlias") ?: ""
            val lockData = backStackEntry.arguments?.getString("lockData") ?: ""
            val lockMac = backStackEntry.arguments?.getString("lockMac") ?: ""

            ActiveBookingScreen(
                lockName = lockName,
                lockAlias = lockAlias,
                lockData = lockData,
                lockMac = lockMac,
                onRelease = { navController.popBackStack() }
            )
        }

        composable(
            route = "adminLock/{lockName}/{lockAlias}/{lockData}/{lockMac}",
            arguments = listOf(
                navArgument("lockName"){ type = NavType.StringType },
                navArgument("lockAlias"){ type = NavType.StringType },
                navArgument("lockData"){ type = NavType.StringType },
                navArgument("lockMac"){ type = NavType.StringType },
            )
        ) { backStackEntry ->
            val name  = backStackEntry.arguments?.getString("lockName").orEmpty()
            val alias = backStackEntry.arguments?.getString("lockAlias").orEmpty()
            val data  = backStackEntry.arguments?.getString("lockData").orEmpty()
            val mac   = backStackEntry.arguments?.getString("lockMac").orEmpty()

            AdminLockControlScreen(
                lockName = name,
                lockAlias = alias,
                lockData = data,
                lockMac = mac,
                onBack = { navController.popBackStack() }
            )
        }

    }
}