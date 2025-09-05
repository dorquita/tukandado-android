package com.tukandado.tukandadov2.navigation

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.tukandado.tukandadov2.BuildConfig
import com.tukandado.tukandadov2.data.SessionManager
import com.tukandado.tukandadov2.ui.admin.ClubDetailScreen
import com.tukandado.tukandadov2.ui.admin.ClubsScreen
import com.tukandado.tukandadov2.ui.admin.ConfigurationScreen
import com.tukandado.tukandadov2.ui.admin.EKeyScreen
import com.tukandado.tukandadov2.ui.admin.PasscodeCreateScreen
import com.tukandado.tukandadov2.ui.admin.PasscodeDetailScreen
import com.tukandado.tukandadov2.ui.admin.PasscodeListScreen
import com.tukandado.tukandadov2.ui.admin.RFIDScreen
import com.tukandado.tukandadov2.ui.admin.RegistersScreen
import com.tukandado.tukandadov2.ui.booking.ActiveBookingScreen
import com.tukandado.tukandadov2.ui.bookings.BookingsClientScreen
import com.tukandado.tukandadov2.ui.components.layout.AdminLockControlScreen
import com.tukandado.tukandadov2.ui.home.HomeScreen
import com.tukandado.tukandadov2.ui.home.ProfileScreen
import com.tukandado.tukandadov2.ui.lock.OpenLockScreen
import com.tukandado.tukandadov2.ui.login.LoginScreen
import com.tukandado.tukandadov2.ui.dashboard.DashboardScreen
import com.tukandado.tukandadov2.ui.dashboard.DashboardClientScreen
import com.tukandado.tukandadov2.viewmodel.BookingViewModel
import com.tukandado.tukandadov2.viewmodel.PasscodeViewModel
import kotlinx.coroutines.launch


@SuppressLint("CoroutineCreationDuringComposition")
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun NavGraph(
    navController: NavHostController,
    setHeaderAction: (label: String?, enabled: Boolean, onClick: (() -> Unit)?) -> Unit = { _, _, _ -> }
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val passcodeViewModel: PasscodeViewModel = viewModel()

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

    val scope = rememberCoroutineScope()

    NavHost(navController = navController, startDestination = startDestination) {
        composable("login") {
            LoginScreen(
                onLoginSuccess = { user ->
                    val active = user.activeBooking
                    if (active != null) {
                        val encodedId = Uri.encode(active.lockId._id)
                        val encodedName = Uri.encode(active.lockId.lockName)
                        val encodedAlias = Uri.encode(active.lockId.lockAlias)
                        val encodedData = Uri.encode(active.lockId.lockData)
                        val encodedMac = Uri.encode(active.lockId.lockMac)

                        navController.navigate("activeReservation/$encodedId/$encodedName/$encodedAlias/$encodedData/$encodedMac") {
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

        composable(Screen.Home.route) {
            // Lee el rol desde SessionManager (ROLE_KEY)
            val role by sessionManager.getRole().collectAsState(initial = "client")
            val isAdmin = role == "admin" || role == "superadmin"

            if (isAdmin) {
                // Dashboard de ADMIN (ya lo tienes)
                DashboardScreen(navController, setHeaderAction)
            } else {
                // Dashboard de CLIENTE (nuevo)
                DashboardClientScreen(navController, setHeaderAction)
            }
        }
        composable(Screen.Locks.route) {
            val context = LocalContext.current
            val sessionManager = remember { SessionManager(context) }

            val role by sessionManager.getRole().collectAsState(initial = "client")
            val activeBooking by sessionManager.getActiveBooking().collectAsState(initial = null)

            // Redirige automáticamente si es cliente y tiene reserva activa
            LaunchedEffect(role, activeBooking?._id) {
                val isAdmin = role == "admin" || role == "superadmin"
                if (!isAdmin && activeBooking != null) {
                    val ab = activeBooking!!
                    val encodedId = Uri.encode(ab.lockId._id)
                    val encodedName  = Uri.encode(ab.lockId.lockName)
                    val encodedAlias = Uri.encode(ab.lockId.lockAlias)
                    val encodedData  = Uri.encode(ab.lockId.lockData)
                    val encodedMac   = Uri.encode(ab.lockId.lockMac)

                    navController.navigate("activeReservation/$encodedId/$encodedName/$encodedAlias/$encodedData/$encodedMac") {
                        launchSingleTop = true
                    }
                }
            }
            // Mientras redirige, no pintes nada. Si no hay reserva activa o es admin, muestra la lista.
            val isAdmin = role == "admin" || role == "superadmin"
            if (activeBooking != null && !isAdmin) {
                // placeholder vacío durante la redirección
                Box(Modifier.fillMaxSize())
            } else {
                OpenLockScreen(navController, setHeaderAction)
            }
        }

        composable(Screen.Clubs.route) { HomeScreen(navController) }
        composable(Screen.Profile.route) { ProfileScreen(navController) }
        composable("bookings") {
            BookingsClientScreen(navController, setHeaderAction)
        }

        composable("configuration") { ConfigurationScreen(navController) }
        composable("registers") { RegistersScreen(navController) }
        composable("rfid") { RFIDScreen(navController) }

        composable(Screen.Clubs.route) {
            ClubsScreen(navController, setHeaderAction)
        }

        composable(
            route = "club/{clubId}",
            arguments = listOf(navArgument("clubId") { type = NavType.StringType })
        ) { backStackEntry ->
            val clubId = backStackEntry.arguments?.getString("clubId").orEmpty()
            ClubDetailScreen(
                clubId = clubId,
                navController = navController,
                setHeaderAction = setHeaderAction
            )
        }
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
            route = "activeReservation/{lockId}/{lockName}/{lockAlias}/{lockData}/{lockMac}",
            arguments = listOf(
                navArgument("lockId") { type = NavType.StringType },
                navArgument("lockName") { type = NavType.StringType },
                navArgument("lockAlias") { type = NavType.StringType },
                navArgument("lockData") { type = NavType.StringType },
                navArgument("lockMac") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val lockId = backStackEntry.arguments?.getString("lockId") ?: ""
            val lockName = backStackEntry.arguments?.getString("lockName") ?: ""
            val lockAlias = backStackEntry.arguments?.getString("lockAlias") ?: ""
            val lockData = backStackEntry.arguments?.getString("lockData") ?: ""
            val lockMac = backStackEntry.arguments?.getString("lockMac") ?: ""

            ActiveBookingScreen(
                lockName = lockName,
                lockAlias = lockAlias,
                lockData = lockData,
                lockMac = lockMac,
                onOpenPasscodes = {
                    val encId   = Uri.encode(lockId)
                    val encData = Uri.encode(lockData)
                    val encMac  = Uri.encode(lockMac)
                    navController.navigate("passcodes/create/$encId/$encData/$encMac")
                },
                onRelease = {
                    scope.launch {
                        // Llamada suspend
                        sessionManager.clearActiveBooking()

                        navController.navigate(Screen.Home.route) {
                            popUpTo("activeReservation/{lockId}/{lockName}/{lockAlias}/{lockData}/{lockMac}") {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                    }
                }
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