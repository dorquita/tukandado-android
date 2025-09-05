package com.tukandado.tukandadov2.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.LockReset
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.tukandado.tukandadov2.BuildConfig
import com.tukandado.tukandadov2.data.SessionManager
import com.tukandado.tukandadov2.viewmodel.LoginViewModel
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(navController: NavController) {
    val context = LocalContext.current
    val viewModel: LoginViewModel = viewModel()
    val sessionManager = remember { SessionManager(context) }
    val scope = rememberCoroutineScope()

    // Role desde la sesión
    val role by sessionManager.getRole().collectAsState(initial = "client")
    val isAdmin = role == "admin" || role == "superadmin"

    val name by sessionManager.getUserEmail().collectAsState(initial = "")

    // Tema desde DataStore (misma fuente que MainActivity)
    val isDarkPref by sessionManager.getDarkTheme().collectAsState(initial = null)
    val isDark = isDarkPref ?: androidx.compose.foundation.isSystemInDarkTheme()


    // Estado UI local (preferencias demo)
    var language by remember { mutableStateOf("Español") }

    // Selector de centro (solo admins) – hardcodeado
    val clubOptions = listOf("Tukandado Central", "Gimnasio Río", "Sport Nueve")
    var selectedClub by remember { mutableStateOf(clubOptions.first()) }

    // Diálogo de diagnóstico (placeholder)
    var showDiag by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()) // <-- ahora sí hace scroll
            .padding(horizontal = 16.dp)
            .statusBarsPadding()
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // ----- Cuenta -----
        ElevatedCard(shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Avatar sencillo
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = role?.first()?.uppercase() ?: "",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(name.toString(), style = MaterialTheme.typography.titleMedium)
                        Text(
                            when (role) {
                                "superadmin" -> "Superadministrador"
                                "admin" -> "Administrador"
                                else -> "Cliente"
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    RoleBadge(role.toString())
                }

                if (isAdmin) {
                    // Selector de centro (dropdown sencillo)
                    ClubDropdownField(
                        label = "Centro activo",
                        value = selectedClub,
                        options = clubOptions,
                        onSelected = { selectedClub = it }
                    )
                }
            }
        }

        // ----- Preferencias -----
        OutlinedCard(shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Preferencias", style = MaterialTheme.typography.titleMedium)
                SettingRow(
                    icon = Icons.Outlined.LightMode,
                    title = "Tema oscuro",
                    subtitle = "Cambia entre claro/oscuro",
                    trailing = {
                        Switch(
                            checked = isDark,
                            onCheckedChange = { value ->
                                scope.launch { sessionManager.setDarkTheme(value) }
                            }
                        )
                    }
                )
                Divider()
                SettingRow(
                    icon = Icons.Outlined.Language,
                    title = "Idioma",
                    subtitle = language,
                    trailing = {
                        LanguageDropdown(
                            value = language,
                            options = listOf("Español", "English", "Català"),
                            onSelected = { language = it }
                        )
                    }
                )
            }
        }

        // ----- Conectividad & diagnóstico -----
        OutlinedCard(shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Conectividad y diagnóstico", style = MaterialTheme.typography.titleMedium)
                SettingRow(
                    icon = Icons.Outlined.Bluetooth,
                    title = "Diagnóstico rápido",
                    subtitle = "Comprueba BLE/GPS/red y tiempo del sistema",
                    trailing = {
                        TextButton(onClick = { showDiag = true }) { Text("Ejecutar") }
                    }
                )
            }
        }

        // ----- Seguridad -----
        OutlinedCard(shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Seguridad", style = MaterialTheme.typography.titleMedium)
                SettingRow(
                    icon = Icons.Outlined.LockReset,
                    title = "Cambiar contraseña",
                    subtitle = "Redirige a configuración",
                    trailing = {
                        TextButton(onClick = { navController.navigate("configuration") }) { Text("Abrir") }
                    }
                )
            }
        }

        // ----- Acerca de -----
        AboutCard()

        // ----- Logout -----
        Button(
            onClick = {
                scope.launch {
                    sessionManager.clearSession()
                    navController.navigate("login") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Outlined.Logout, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Cerrar sesión")
        }
        Spacer(Modifier.height(8.dp))
    }

    // Diálogo de diagnóstico (placeholder)
    if (showDiag) {
        AlertDialog(
            onDismissRequest = { showDiag = false },
            title = { Text("Diagnóstico rápido") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("• Bluetooth: pendiente de comprobación")
                    Text("• GPS: pendiente de comprobación")
                    Text("• Red: OK")
                    Text("• Hora sistema: OK")
                }
            },
            confirmButton = { TextButton(onClick = { showDiag = false }) { Text("Cerrar") } }
        )
    }
}

/* --------------------------- COMPONENTES --------------------------- */

@Composable
private fun RoleBadge(role: String) {
    val (bg, fg, label) = when (role) {
        "superadmin" -> Triple(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
            MaterialTheme.colorScheme.primary,
            "Superadmin"
        )
        "admin" -> Triple(
            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
            MaterialTheme.colorScheme.tertiary,
            "Admin"
        )
        else -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            "Cliente"
        )
    }
    androidx.compose.material3.Surface(
        color = bg,
        shape = RoundedCornerShape(999.dp)
    ) {
        Text(
            text = label,
            color = fg,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
private fun ClubDropdownField(
    label: String,
    value: String,
    options: List<String>,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            leadingIcon = { Icon(Icons.Outlined.Place, contentDescription = null) },
            trailingIcon = { Icon(Icons.Outlined.Info, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt) },
                    onClick = {
                        onSelected(opt)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun LanguageDropdown(
    value: String,
    options: List<String>,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            leadingIcon = { Icon(Icons.Outlined.Language, contentDescription = null) },
            trailingIcon = { Icon(Icons.Outlined.Info, contentDescription = null) },
            modifier = Modifier
                .widthIn(min = 160.dp)
                .clickable { expanded = true }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt) },
                    onClick = {
                        onSelected(opt)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun SettingRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String? = null,
    trailing: @Composable () -> Unit = {}
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        trailing()
    }
}

@Composable
private fun AboutCard() {
    OutlinedCard(shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Acerca de", style = MaterialTheme.typography.titleMedium)
            ListItem(
                leadingContent = {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                headlineContent = { Text("Tukandado") },
                supportingContent = {
                    Text(
                        "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) • ${BuildConfig.BUILD_TYPE.uppercase()}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )
        }
    }
}