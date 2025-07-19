package com.tukandado.tukandadov2.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.tukandado.tukandadov2.ui.home.HomeScreen
import com.tukandado.tukandadov2.ui.home.ProfileScreen
import com.tukandado.tukandadov2.ui.lock.OpenLockScreen
import com.tukandado.tukandadov2.ui.login.LoginScreen

@Composable
fun NavGraph(navController: NavHostController, startDestination: String = "login") {
    NavHost(navController = navController, startDestination = startDestination) {
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Home.route) {
            HomeScreen(navController)
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
    }
}
