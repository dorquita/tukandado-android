package com.tukandado.tukandadov2.navigation

import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.tukandado.tukandadov2.BuildConfig
import com.tukandado.tukandadov2.data.SessionManager
import com.tukandado.tukandadov2.ui.admin.ConfigurationScreen
import com.tukandado.tukandadov2.ui.admin.EKeyScreen
import com.tukandado.tukandadov2.ui.admin.PasscodeCreateScreen
import com.tukandado.tukandadov2.ui.admin.PasscodeDetailScreen
import com.tukandado.tukandadov2.ui.admin.PasscodeListScreen
import com.tukandado.tukandadov2.ui.admin.RFIDScreen
import com.tukandado.tukandadov2.ui.admin.RegistersScreen
import com.tukandado.tukandadov2.ui.booking.ActiveBookingScreen
import com.tukandado.tukandadov2.ui.components.layout.AdminLockControlScreen
import com.tukandado.tukandadov2.ui.home.HomeScreen
import com.tukandado.tukandadov2.ui.home.ProfileScreen
import com.tukandado.tukandadov2.ui.lock.OpenLockScreen
import com.tukandado.tukandadov2.ui.login.LoginScreen

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun NavGraph(
    navController: NavHostController,
    setHeaderAction: (label: String?, enabled: Boolean, onClick: (() -> Unit)?) -> Unit = { _, _, _ -> }
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }

    // Si estás maquetando, siembra sesión en debug
    LaunchedEffect(Unit) {
        Log.d("Entro launched del nav grap", "he entrado")
        if (BuildConfig.BYPASS_LOGIN) {
            Log.d("Entro BYPASS_LOGIN del nav grap", BuildConfig.BYPASS_LOGIN.toString())
            sessionManager.ensureDevToken()
        }
    }

    val hasSession by sessionManager.hasValidSessionFlow.collectAsState(initial = false)
    Log.d("Tiene sesión?", hasSession.toString())

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

        composable("configuration") { ConfigurationScreen(navController) }
        composable("registers") { RegistersScreen(navController) }
        composable("rfid") { RFIDScreen(navController) }
        //composable("passwords") { PasscodeScreen() Screen(navController) }

        /*composable(
            route = "passcodes/{lockId}",
            arguments = listOf(
                navArgument("lockId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val lockId = backStackEntry.arguments?.getString("lockId").orEmpty()

            PasscodeScreen(
                lockId = lockId,
                onBack = { navController.popBackStack() },
                navController = navController
            )
        }*/

        composable(
            route = "ekeys/{lockId}",
            arguments = listOf(
                navArgument("lockId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val lockId = backStackEntry.arguments?.getString("lockId").orEmpty()

            EKeyScreen(
                lockId = lockId,
                onBack = { navController.popBackStack() },
                navController = navController
            )
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

            ActiveBookingScreen(
                lockName = lockName,
                lockAlias = lockAlias,
                lockData = lockData,
                lockMac = lockMac,
                onRelease = { navController.popBackStack() }
            )
        }

        composable(
            route = "adminLock/{lockName}/{lockAlias}/{lockData}/{lockMac}/{lockId}",
            arguments = listOf(
                navArgument("lockName"){ type = NavType.StringType },
                navArgument("lockAlias"){ type = NavType.StringType },
                navArgument("lockData"){ type = NavType.StringType },
                navArgument("lockMac"){ type = NavType.StringType },
                navArgument("lockId"){ type = NavType.StringType },
            )
        ) { backStackEntry ->
            val name  = backStackEntry.arguments?.getString("lockName").orEmpty()
            val alias = backStackEntry.arguments?.getString("lockAlias").orEmpty()
            val data  = backStackEntry.arguments?.getString("lockData").orEmpty()
            val mac   = backStackEntry.arguments?.getString("lockMac").orEmpty()
            val lockId   = backStackEntry.arguments?.getString("lockId").orEmpty()
            AdminLockControlScreen(
                lockName = name,
                lockAlias = alias,
                lockData = data,
                lockMac = mac,
                lockId = lockId,
                onBack = { navController.popBackStack() },
                navController = navController
            )
        }

        // en tu NavHost
        composable(
            route = "passcodes/{lockId}/{lockData}/{lockMac}",
            arguments = listOf(
                navArgument("lockId")   { type = NavType.StringType },
                navArgument("lockData") { type = NavType.StringType },
                navArgument("lockMac")  { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val lockId   = backStackEntry.arguments?.getString("lockId").orEmpty()
            val lockData = backStackEntry.arguments?.getString("lockData").orEmpty()
            val lockMac  = backStackEntry.arguments?.getString("lockMac").orEmpty()

            PasscodeListScreen(
                lockId = lockId,
                lockData = lockData,
                lockMac = lockMac,
                navController = navController,
                setHeaderAction = setHeaderAction,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "passcodes/create/{lockId}/{lockData}/{lockMac}",
            arguments = listOf(
                navArgument("lockId")   { type = NavType.StringType },
                navArgument("lockData") { type = NavType.StringType },
                navArgument("lockMac")  { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val lockId   = backStackEntry.arguments?.getString("lockId").orEmpty()
            val lockData = backStackEntry.arguments?.getString("lockData").orEmpty()
            val lockMac  = backStackEntry.arguments?.getString("lockMac").orEmpty()

            PasscodeCreateScreen(
                lockId = lockId,
                lockData = lockData,
                lockMac = lockMac,
                navController = navController,
                onBack = { navController.popBackStack() },
                setHeaderAction = setHeaderAction
            )
        }

        composable(
            route = "passcodes/{passcodeId}/detail/{lockId}/{lockData}/{lockMac}/",
            arguments = listOf(
                navArgument("passcodeId") { type = NavType.StringType },
                navArgument("lockId") { type = NavType.StringType },
                navArgument("lockData") { type = NavType.StringType },
                navArgument("lockMac") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val passcodeId = backStackEntry.arguments?.getString("passcodeId").orEmpty()
            val lockId = backStackEntry.arguments?.getString("lockId").orEmpty()
            val lockData = backStackEntry.arguments?.getString("lockData").orEmpty()
            val lockMac = backStackEntry.arguments?.getString("lockMac").orEmpty()
            PasscodeDetailScreen(
                passcodeId = passcodeId,
                navController = navController,
                lockId = lockId,
                lockDataJson = lockData,
                lockMac = lockMac,
                onBack = { navController.popBackStack() }
            )
        }
    }
}