package com.tukandado.tukandadov2.ui.lock

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.tukandado.tukandadov2.api.LockResponse
import com.tukandado.tukandadov2.data.SessionManager
import com.tukandado.tukandadov2.viewmodel.BookingViewModel
import com.tukandado.tukandadov2.viewmodel.LockViewModel
import kotlin.math.abs

private val CardShape: RoundedCornerShape = RoundedCornerShape(16.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpenLockScreen(
    navController: NavController,
    // Para poner el botón "Refrescar" en el HeaderBar
    setHeaderAction: (label: String?, enabled: Boolean, onClick: (() -> Unit)?) -> Unit = { _, _, _ -> }
) {
    val context = LocalContext.current
    val lockViewModel: LockViewModel = viewModel()
    val bookingViewModel: BookingViewModel = viewModel()
    val locks by lockViewModel.locks.collectAsState()
    val error by lockViewModel.error.collectAsState()

    var selectedLockName by remember { mutableStateOf<String?>(null) }
    var selectedLock by remember { mutableStateOf<LockResponse?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    val role by SessionManager(context).getRole().collectAsState(initial = "client")
    val isAdmin = role == "admin" || role == "superadmin"

    // Carga inicial
    LaunchedEffect(Unit) {
        lockViewModel.getAllLocks(context)
        // Acción del HeaderBar
        setHeaderAction("Refrescar", true) { lockViewModel.getAllLocks(context) }
    }
    // Limpia acción al salir
    DisposableEffect(Unit) { onDispose { setHeaderAction(null, true, null) } }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        // Admin mantiene título; cliente empieza directamente en el conteo
        if (isAdmin) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Candados",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        // Lista con UI condicional por rol
        LockListScreen(
            locks = locks,
            isAdmin = isAdmin,
            navController = navController,
            onTapNonAdmin = { lock ->
                // Cliente: confirmación antes de iniciar reserva
                val title = lock.lockAlias.takeIf { it.isNotBlank() } ?: lock.lockName
                selectedLockName = title
                selectedLock = lock
                showDialog = true
            }
        )

        if (error != null) {
            Spacer(Modifier.height(8.dp))
            OutlinedCard {
                ListItem(
                    headlineContent = { Text("Error") },
                    supportingContent = { Text(error ?: "") }
                )
            }
        }
    }

    // Diálogo de reserva para cliente (no admin)
    if (showDialog && selectedLockName != null) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Reserva") },
            text = { Text("¿Quieres iniciar la reserva \"$selectedLockName\"?") },
            confirmButton = {
                TextButton(onClick = {
                    showDialog = false
                    val encodedId = Uri.encode(selectedLock?._id)
                    val encodedName = Uri.encode(selectedLock?.lockName)
                    val encodedAlias = Uri.encode(selectedLock?.lockAlias)
                    val encodedData = Uri.encode(selectedLock?.lockData)
                    val encodedMac = Uri.encode(selectedLock?.lockMac)

                    selectedLock?.let {
                        bookingViewModel.startBooking(context, it._id!!)
                        navController.navigate(
                            "activeReservation/${encodedId}/${encodedName}/${encodedAlias}/${encodedData}/${encodedMac}"
                        )
                    }
                }) { Text("Sí") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Cancelar") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LockListScreen(
    locks: List<LockResponse>,
    isAdmin: Boolean,
    navController: NavController?,
    modifier: Modifier = Modifier,
    onTapNonAdmin: (LockResponse) -> Unit = {}
) {
    var search by remember { mutableStateOf("") }

    // Selector de club (hardcodeado, solo admin)
    val clubOptions = listOf("Todos los centros", "Tukandado Central", "Gimnasio Río", "Sport Nueve")
    var selectedClub by remember { mutableStateOf(clubOptions.first()) }

    // Filtros rápidos (solo admin)
    var onlyFree by remember { mutableStateOf(false) }
    var onlyBusy by remember { mutableStateOf(false) }
    var onlyGateway by remember { mutableStateOf(false) }

    Column(
        modifier
            .fillMaxSize()
            .padding(0.dp)
    ) {
        // ──────────────── UI de filtros solo para ADMIN ────────────────
        if (isAdmin) {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                placeholder = { Text("Buscar candado…") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            ClubSelector(
                clubOptions = clubOptions,
                selectedClub = selectedClub,
                onSelected = { selectedClub = it }
            )
            Spacer(Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = onlyFree,
                    onClick = {
                        onlyFree = !onlyFree
                        if (onlyFree) onlyBusy = false
                    },
                    label = { Text("Libres") }
                )
                FilterChip(
                    selected = onlyBusy,
                    onClick = {
                        onlyBusy = !onlyBusy
                        if (onlyBusy) onlyFree = false
                    },
                    label = { Text("Ocupados") }
                )
                FilterChip(
                    selected = onlyGateway,
                    onClick = { onlyGateway = !onlyGateway },
                    label = { Text("Gateway") },
                    leadingIcon = { Icon(Icons.Outlined.Hub, contentDescription = null) }
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        // ──────────────── Filtrado (forzando libres para CLIENTE) ────────────────
        val filtered = remember(search, selectedClub, onlyFree, onlyBusy, onlyGateway, locks, isAdmin) {
            locks
                .asSequence()
                .filter { l ->
                    // Búsqueda solo afecta si admin; cliente muestra todo (libre) sin buscar
                    if (!isAdmin) true
                    else {
                        val q = search.trim().lowercase()
                        if (q.isBlank()) true
                        else l.lockAlias.lowercase().contains(q) || l.lockName.lowercase().contains(q)
                    }
                }
                .filter { l ->
                    // Selector centro solo admin
                    if (!isAdmin) true
                    else if (selectedClub == "Todos los centros") true
                    else {
                        val tag = selectedClub.lowercase()
                        l.lockAlias.lowercase().contains(tag) || l.lockName.lowercase().contains(tag)
                    }
                }
                .filter { l ->
                    // Cliente: SIEMPRE solo libres
                    if (!isAdmin) !l.isInUse
                    else {
                        if (onlyFree) !l.isInUse
                        else if (onlyBusy) l.isInUse
                        else true
                    }
                }
                .filter { l ->
                    if (!isAdmin) true
                    else if (!onlyGateway) true
                    else hasGateway(l.lockMac)
                }
                .sortedBy { it.lockAlias.ifBlank { it.lockName }.lowercase() }
                .toList()
        }

        // ──────────────── Header de resultados ────────────────
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val count = filtered.size
            Text(
                "$count candado${if (count == 1) "" else "s"}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(8.dp))

        // ──────────────── Lista / vacíos ────────────────
        if (filtered.isEmpty()) {
            if (isAdmin) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ListItem(
                        headlineContent = { Text("Sin resultados") },
                        supportingContent = { Text("Ajusta la búsqueda o los filtros.") }
                    )
                }
            } else {
                // Cliente: mensaje específico cuando no hay libres
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ListItem(
                        headlineContent = { Text("Sin disponibilidad") },
                        supportingContent = {
                            Text("Actualmente todas las taquillas están ocupadas. Consulta con el administrador.")
                        }
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(filtered.size) { index ->
                    val lock = filtered[index]
                    LockRow(
                        lock = lock,
                        onClick = {
                            if (isAdmin) {
                                navController?.navigate(
                                    "adminLock/" +
                                            Uri.encode(lock.lockName) + "/" +
                                            Uri.encode(lock.lockAlias) + "/" +
                                            Uri.encode(lock.lockData) + "/" +
                                            Uri.encode(lock.lockMac) + "/" +
                                            Uri.encode(lock._id)
                                )
                            } else {
                                onTapNonAdmin(lock) // cliente: abre diálogo de confirmación en el padre
                            }
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClubSelector(
    clubOptions: List<String>,
    selectedClub: String,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selectedClub,
            onValueChange = {},
            readOnly = true,
            label = { Text("Centro") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            clubOptions.forEach { opt ->
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
private fun UsagePill(isInUse: Boolean) {
    val bg = if (isInUse) Color(0xFFFDECEC) else Color(0xFFE8F5E9)
    val fg = if (isInUse) Color(0xFFB00020) else Color(0xFF1B5E20)
    val text = if (isInUse) "Ocupado" else "Libre"

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text, color = fg, fontSize = MaterialTheme.typography.labelSmall.fontSize)
    }
}

@Composable
private fun LockRow(lock: LockResponse, onClick: () -> Unit) {
    ElevatedCard(
        shape = CardShape,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 88.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val title = lock.lockAlias.takeIf { it.isNotEmpty() } ?: lock.lockName

            // Avatar con inicial
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title.firstOrNull()?.uppercase() ?: "?",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleSmall
                )
            }

            Spacer(Modifier.width(12.dp))

            // Texto + chips
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))

                val remote = hasGateway(lock.lockMac)
                AssistChip(
                    onClick = {},
                    label = { Text(if (remote) "Remota" else "Solo BLE") },
                    enabled = false,
                    leadingIcon = {
                        Icon(
                            if (remote) Icons.Outlined.Hub else Icons.Outlined.Bluetooth,
                            contentDescription = null
                        )
                    }
                )
            }

            // Estado uso + batería (demo estable por hash)
            Column(horizontalAlignment = Alignment.End) {
                UsagePill(isInUse = lock.isInUse)
                Spacer(Modifier.height(6.dp))
                BatteryBadge(percent = pseudoBattery(lock))
            }
        }
    }
}

@Composable
private fun BatteryBadge(percent: Int?) {
    val p = (percent ?: 0).coerceIn(0, 100)

    val icon: ImageVector
    val tint: Color

    when {
        percent == null -> {
            icon = Icons.Default.BatteryStd
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        }
        p <= 10 -> {
            icon = Icons.Default.BatteryStd
            tint = Color(0xFFB00020)
        }
        p <= 30 -> {
            icon = Icons.Default.BatteryChargingFull
            tint = Color(0xFFFF6D00)
        }
        else -> {
            icon = Icons.Default.BatteryFull
            tint = Color(0xFF1B5E20)
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(4.dp))
        Text("$p%", style = MaterialTheme.typography.bodySmall, color = tint)
    }
}

/* ------------------------- UTIL ------------------------- */

private fun hasGateway(mac: String): Boolean {
    val d = mac.takeLast(1).toIntOrNull()
    return d != null && d % 2 == 0
}

private fun shortMac(mac: String): String =
    if (mac.length <= 5) mac else "••:${mac.takeLast(5)}"

private fun pseudoBattery(lock: LockResponse): Int {
    val base = (lock.lockMac.ifBlank { lock._id }.hashCode())
    return (abs(base) % 61) + 20 // 20..80
}