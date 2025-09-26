package com.tukandado.tukandadov2.ui.admin

import CompactChip
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.Instant.now
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.math.min

/* -----------------------------------------------------------
 * DATA & STATE
 * ----------------------------------------------------------- */

data class ClubUi @RequiresApi(Build.VERSION_CODES.O) constructor(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val city: String,
    val address: String,
    val phone: String? = null,
    val hasGateway: Boolean = true,
    val zonesCount: Int = 0,
    val locksTotal: Int = 0,
    val locksOffline: Int = 0,
    val lowBatteries: Int = 0,
    val occupancyPct: Int = 0,
    val lastSyncAt: Instant
)

data class ClubsState(
    val isLoading: Boolean = true,
    val query: String = "",
    val onlyIncidents: Boolean = false,
    val onlyGateway: Boolean = false,
    val clubs: List<ClubUi> = emptyList()
)

class ClubsViewModel : ViewModel() {
    var state by mutableStateOf(ClubsState())
        private set

    @RequiresApi(Build.VERSION_CODES.O)
    fun refresh() {
        // Simulación de carga (sustituye por tu repo/API)
        val now = now()
        state = state.copy(isLoading = true)
        kotlinx.coroutines.GlobalScope.launch {
            delay(350)
            val demo = listOf(
                ClubUi(
                    name = "Tukandado Central", city = "Barcelona",
                    address = "C/ Diputació 88", phone = "+34 600 123 456",
                    hasGateway = true, zonesCount = 6, locksTotal = 58,
                    locksOffline = 1, lowBatteries = 3, occupancyPct = 42,
                    lastSyncAt = now.minusSeconds(900)
                ),
                ClubUi(
                    name = "Gimnasio Río", city = "Madrid",
                    address = "Av. de la Paz 12", phone = "+34 699 000 111",
                    hasGateway = false, zonesCount = 3, locksTotal = 21,
                    locksOffline = 0, lowBatteries = 1, occupancyPct = 67,
                    lastSyncAt = now.minusSeconds(3600)
                ),
                ClubUi(
                    name = "Sport Nueve", city = "Valencia",
                    address = "Gran Vía 201",
                    hasGateway = true, zonesCount = 4, locksTotal = 33,
                    locksOffline = 2, lowBatteries = 0, occupancyPct = 18,
                    lastSyncAt = now.minusSeconds(120)
                )
            )
            state = state.copy(isLoading = false, clubs = demo)
        }
    }

    fun setQuery(q: String) { state = state.copy(query = q) }
    fun toggleIncidents() { state = state.copy(onlyIncidents = !state.onlyIncidents) }
    fun toggleGateway() { state = state.copy(onlyGateway = !state.onlyGateway) }
}

/* -----------------------------------------------------------
 * SCREEN
 * ----------------------------------------------------------- */

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClubsScreen(
    navController: NavController,
    setHeaderAction: (label: String?, enabled: Boolean, onClick: (() -> Unit)?) -> Unit = { _, _, _ -> }
) {
    val vm: ClubsViewModel = viewModel()
    val s = vm.state
    val scope = rememberCoroutineScope()

    // Acción en AppBar
    LaunchedEffect(s.isLoading) {
        setHeaderAction("Refrescar", !s.isLoading) { vm.refresh() }
    }
    // Primera carga
    LaunchedEffect(Unit) { vm.refresh() }
    // Limpiar acción al salir
    DisposableEffect(Unit) { onDispose { setHeaderAction(null, true, null) } }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .statusBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(Modifier.height(4.dp))

        // Buscador
        OutlinedTextField(
            value = s.query,
            onValueChange = vm::setQuery,
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            singleLine = true,
            placeholder = { Text("Buscar centro…") }
        )

        // Filtros
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = s.onlyIncidents,
                onClick = vm::toggleIncidents,
                label = { Text("Con incidencias") },
                leadingIcon = {
                    Icon(
                        if (s.onlyIncidents) Icons.Outlined.ReportGmailerrorred else Icons.Outlined.ReportProblem,
                        contentDescription = null
                    )
                }
            )
            CompactChip{
                FilterChip(
                    selected = s.onlyGateway,
                    onClick = vm::toggleGateway,
                    label = { Text("Con gateway") },
                    leadingIcon = { Icon(Icons.Outlined.Hub, contentDescription = null) }
                )
            }
        }

        if (s.isLoading) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }

        // Lista
        val filtered = remember(s.clubs, s.query, s.onlyIncidents, s.onlyGateway) {
            s.clubs
                .filter {
                    val q = s.query.trim().lowercase()
                    (q.isBlank() || it.name.lowercase().contains(q) || it.city.lowercase().contains(q))
                }
                .filter { if (s.onlyGateway) it.hasGateway else true }
                .filter {
                    if (s.onlyIncidents) (it.locksOffline > 0 || it.lowBatteries > 0) else true
                }
        }

        if (!s.isLoading && filtered.isEmpty()) {
            EmptyState()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(filtered, key = { it.id }) { club ->
                    ClubCard(
                        club = club,
                        onOpenLocks = {
                            // Navega a la lista de candados de ese centro (ajusta si tienes ruta)
                            navController.navigate("locks") // o tu ruta concreta con filtros
                        },
                        onOpenDetail = {
                            navController.navigate("club/${club.id}")
                        }
                    )
                }
            }
        }
    }
}

/* -----------------------------------------------------------
 * COMPONENTS
 * ----------------------------------------------------------- */

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun ClubCard(
    club: ClubUi,
    onOpenLocks: () -> Unit,
    onOpenDetail: () -> Unit
) {
    val fontScale = LocalConfiguration.current.fontScale
    val compact = fontScale >= 1.3f  // umbral de zoom “abuelo”

    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Place, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(club.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val subtitle = buildString {
                        append(club.city)
                        append(" • ")
                        append(club.address)
                    }
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                StatusBadge(offline = club.locksOffline, lowBat = club.lowBatteries, compact = compact)
            }

            // KPIs inline
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                KpiPill(Icons.Outlined.Lock, "${club.locksTotal}", "Candados", compact = compact)
                KpiPill(Icons.Outlined.PowerOff, "${club.locksOffline}", "Offline",
                    highlight = club.locksOffline > 0, compact = compact)
                KpiPill(Icons.Outlined.BatteryAlert, "${club.lowBatteries}", "<20%",
                    highlight = club.lowBatteries > 0, compact = compact)
                KpiPill(Icons.Outlined.Hub, if (club.hasGateway) "Sí" else "No", "Gateway",
                    highlight = !club.hasGateway, compact = compact)
            }

            // Ocupación
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Ocupación", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (compact) {
                    Spacer(Modifier.width(8.dp))
                    Text("${club.occupancyPct}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (!compact) {
                Text("${club.occupancyPct}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            LinearProgressIndicator(
                progress = { (club.occupancyPct.coerceIn(0, 100) / 100f) },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            Text("${club.occupancyPct}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            // Footer acciones
            if (compact) {
                Column(
                    Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(onClick = onOpenLocks, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Outlined.Lock, contentDescription = null); Spacer(Modifier.width(8.dp)); Text("Ver candados", maxLines = 1)
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onOpenDetail, modifier = Modifier.weight(1f)) {
                            Text("Detalles", maxLines = 1, softWrap = false)
                        }
                        if (club.phone != null) {
                            TextButton(onClick = { /* dialer */ }, modifier = Modifier.weight(1f), enabled = false) {
                                Icon(Icons.Outlined.Call, contentDescription = null); Spacer(Modifier.width(6.dp)); Text("Contacto", maxLines = 1)
                            }
                        }
                    }
                }
            } else {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(onClick = onOpenLocks, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Outlined.Lock, contentDescription = null)
                        Spacer(Modifier.width(8.dp)); Text("Ver")
                    }
                    TextButton(onClick = onOpenDetail) { Text("Detalles") }
                    if (club.phone != null) {
                        TextButton(onClick = { /* dialer */ }, enabled = false) {
                            Icon(Icons.Outlined.Call, contentDescription = null)
                            Spacer(Modifier.width(6.dp)); Text("Contacto")
                        }
                    }
                }
            }


            // Meta
            Text(
                "Última sync: ${formatInstant(club.lastSyncAt)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun KpiPill(
    icon: ImageVector,
    value: String,
    label: String,
    highlight: Boolean = false,
    compact: Boolean = false
) {
    // capamos fontScale SOLO en la píldora
    val d = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(d.density, fontScale = min(d.fontScale, 1.15f))
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            tonalElevation = if (highlight) 2.dp else 0.dp,
            color = if (highlight) MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ) {
            Row(
                Modifier
                    .padding(horizontal = 10.dp, vertical = 8.dp)
                    .heightIn(min = 36.dp), // tactilidad
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, contentDescription = null,
                    tint = if (highlight) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(6.dp))
                Text(
                    value,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1, softWrap = false
                )
                if (!compact) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}


@Composable
private fun StatusBadge(offline: Int, lowBat: Int, compact: Boolean) {
    val text = when {
        offline > 0 || lowBat > 0 -> if (compact) "Incid." else "Incidencias"
        else -> if (compact) "OK" else "Sin incidencias"
    }

    // capar fontScale SOLO aquí para que no se rompa el chip
    val d = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(d.density, fontScale = min(d.fontScale, 1.15f))
    ) {
        Surface(
            color = if (offline > 0 || lowBat > 0) MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
            else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(999.dp)
        ) {
            Row(
                Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.ErrorOutline,
                    contentDescription = null,
                    tint = if (offline > 0 || lowBat > 0) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}


@Composable
private fun EmptyState() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Outlined.Place, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(8.dp))
            Text("Sin centros para mostrar", style = MaterialTheme.typography.titleSmall)
            Text("Ajusta los filtros o añade un centro.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/* -----------------------------------------------------------
 * DETAIL (auxiliar)
 * ----------------------------------------------------------- */

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ClubDetailScreen(
    clubId: String,
    navController: NavController,
    setHeaderAction: (label: String?, enabled: Boolean, onClick: (() -> Unit)?) -> Unit = { _, _, _ -> }
) {
    // Demo estático; en real, fetch por clubId
    LaunchedEffect(Unit) { setHeaderAction(null, true, null) }
    DisposableEffect(Unit) { onDispose { setHeaderAction(null, true, null) } }

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Centro", style = MaterialTheme.typography.titleMedium)
        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Zonas: 6  •  Candados: 58", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Dirección: C/ Demo 123 · Ciudad", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Horario hoy: 08:00–22:00", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text("Zonas", style = MaterialTheme.typography.titleSmall)
        listOf("Vestuario A", "Vestuario B", "Piscina", "Cross", "Sauna", "Premium").forEach {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                ListItem(
                    leadingContent = {
                        Box(
                            modifier = Modifier.size(36.dp).clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Outlined.GridView, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                    },
                    headlineContent = { Text(it) },
                    supportingContent = { Text("Candados en zona: 8", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    trailingContent = {
                        TextButton(onClick = { /* nav a candados con filtro de zona */ }) { Text("Ver") }
                    }
                )
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}

/* -----------------------------------------------------------
 * UTIL
 * ----------------------------------------------------------- */

@RequiresApi(Build.VERSION_CODES.O)
private fun formatInstant(instant: Instant): String =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault()).format(instant)
