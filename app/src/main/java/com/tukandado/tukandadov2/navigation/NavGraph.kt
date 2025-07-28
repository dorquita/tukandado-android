package com.tukandado.tukandadov2.navigation

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.tukandado.tukandadov2.ui.booking.ActiveBookingScreen
import com.tukandado.tukandadov2.ui.home.HomeScreen
import com.tukandado.tukandadov2.ui.home.ProfileScreen
import com.tukandado.tukandadov2.ui.lock.OpenLockScreen
import com.tukandado.tukandadov2.ui.login.LoginScreen
import android.net.Uri
//import com.tukandado.tukandadov2.viewmodel.LockViewModel

@Composable
fun NavGraph(navController: NavHostController, startDestination: String = "login") {
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
                        }
                    } else {
                        navController.navigate(Screen.Home.route) {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                }
            )
        }
        composable(Screen.Home.route) {
            OpenLockScreen(navController)
        }
        composable(Screen.Locks.route) {
            OpenLockScreen(navController)
        }
        composable(Screen.Clubs.route) {
            HomeScreen(navController)
        }
        composable(Screen.Profile.route) {
            ProfileScreen(navController)
        }
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

            Log.d("DEBUG Nav: Lock lockName es??", lockName + "")
            Log.d("DEBUG Nav: Lock lockAlias es??", lockAlias + "")
            Log.d("DEBUG Nav: Lock lockData es??", lockData + "")
            Log.d("DEBUG Nav: Lock lockMac es??", lockMac + "")

            ActiveBookingScreen(
                lockName = lockName,
                lockAlias = lockAlias,
                lockData = lockData,
                lockMac = lockMac,
                onRelease = {
                    navController.popBackStack() // vuelve atrás al liberar
                }
            )
        }
    }
}
