package com.tukandado.tukandadov2.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Inicio", Icons.Default.Home)
    object Locks : Screen("locks", "Candados", Icons.Default.Lock)
    object Clubs : Screen("clubs", "Centros", Icons.Default.Place)
    object Profile : Screen("profile", "Perfil", Icons.Default.Person)
}
