package com.tukandado.tukandadov2.ui.bookings

import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.tukandado.tukandadov2.api.ActiveBooking
import com.tukandado.tukandadov2.data.SessionManager
import com.tukandado.tukandadov2.navigation.Screen
import com.tukandado.tukandadov2.viewmodel.BookingViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

/* ---------------------------------------------------------------------------------------------- */
/* STATE & VM                                                                                     */
/* ---------------------------------------------------------------------------------------------- */

enum class BookingState { ACTIVE, UPCOMING, PAST, CANCELED }

data class BookingItem(
    val id: String = UUID.randomUUID().toString(),
    val centerName: String,
    val zoneName: String,
    val startsAt: Instant,
    val endsAt: Instant,
    val state: BookingState
)

data class BookingsClientState(
    val isLoading: Boolean = true,
    val upcoming: List<BookingItem> = emptyList(),
    val past: List<BookingItem> = emptyList()
)

class BookingsClientViewModel : ViewModel() {
    var state by mutableStateOf(BookingsClientState())
        private set

    @RequiresApi(Build.VERSION_CODES.O)
    fun refresh() {
        state = state.copy(isLoading = true)
        // Simula tus llamadas reales a API/repos (solo próximas/pasadas; SIN activa demo)
        kotlinx.coroutines.GlobalScope.launch {
            delay(450)
            val now = Instant.now()
            val upcoming = listOf(
                BookingItem(
                    centerName = "Gimnasio Río",
                    zoneName = "Sala Funcional",
                    startsAt = now.plusSeconds(2 * 60 * 60),
                    endsAt = now.plusSeconds(3 * 60 * 60),
                    state = BookingState.UPCOMING
                ),
                BookingItem(
                    centerName = "Sport Nueve",
                    zoneName = "Pista 2",
                    startsAt = now.plusSeconds(26 * 60 * 60),
                    endsAt = now.plusSeconds(27 * 60 * 60),
                    state = BookingState.UPCOMING
                )
            )
            val past = listOf(
                BookingItem(
                    centerName = "Centro Avenida",
                    zoneName = "Sala Cardio",
                    startsAt = now.minusSeconds(26 * 60 * 60),
                    endsAt = now.minusSeconds(25 * 60 * 60),
                    state = BookingState.PAST
                ),
                BookingItem(
                    centerName = "Tukandado Central",
                    zoneName = "Vestuarios · Zona B",
                    startsAt = now.minusSeconds(4 * 24 * 60 * 60),
                    endsAt = now.minusSeconds(4 * 24 * 60 * 60 - 60 * 60),
                    state = BookingState.PAST
                )
            )
            state = BookingsClientState(
                isLoading = false,
                upcoming = upcoming,
                past = past
            )
        }
    }

    fun cancelBooking(bookingId: String) {
        val u = state.upcoming.filterNot { it.id == bookingId }
        state = state.copy(upcoming = u)
    }
}

/* ---------------------------------------------------------------------------------------------- */
/* SCREEN                                                                                         */
/* ---------------------------------------------------------------------------------------------- */

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingsClientScreen(
    navController: NavController,
    setHeaderAction: (label: String?, enabled: Boolean, onClick: (() -> Unit)?) -> Unit = { _, _, _ -> }
) {
    val vm: BookingsClientViewModel = viewModel()
    val s = vm.state

    // Acción del AppBar: Refrescar
    LaunchedEffect(s.isLoading) {
        setHeaderAction("Refrescar", !s.isLoading) { vm.refresh() }
    }
    // Primera carga
    LaunchedEffect(Unit) { vm.refresh() }
    // Limpia al salir
    DisposableEffect(Unit) { onDispose { setHeaderAction(null, true, null) } }

    var search by remember { mutableStateOf("") }
    var pendingCancelId by remember { mutableStateOf<String?>(null) }

    // Reserva activa REAL desde SessionManager (idéntico al DashboardClient)
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val activeBooking: ActiveBooking? by sessionManager
        .getActiveBooking()
        .collectAsState(initial = null)

    Log.d("BookingsClientScreen", "activeBooking = $activeBooking")

    // Filtro simple por texto (para listas demo)
    fun matchesQuery(b: BookingItem): Boolean {
        val q = search.trim().lowercase()
        if (q.isBlank()) return true
        return b.centerName.lowercase().contains(q) ||
                b.zoneName.lowercase().contains(q) ||
                formatInstant(b.startsAt).lowercase().contains(q)
    }

    val upcomingFiltered = remember(search, s.upcoming) { s.upcoming.filter(::matchesQuery) }
    val pastFiltered = remember(search, s.past) { s.past.filter(::matchesQuery) }
    val scope = rememberCoroutineScope()
    val bookingViewModel: BookingViewModel = viewModel()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            if (s.isLoading) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        }

        // Título + buscador
        item {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                singleLine = true,
                placeholder = { Text("Buscar por centro, sala, fecha…") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // ------------------------------- Activa (SOLO REAL) -------------------------------
        item { SectionHeader("Activa") }
        item {
            if (activeBooking != null) {
                val b = activeBooking!!
                ActiveBookingCardReal(
                    booking = b,
                    onDetails = {
                        val id  = Uri.encode(b.lockId._id)
                        val name  = Uri.encode(b.lockId.lockName)
                        val alias = Uri.encode(b.lockId.lockAlias)
                        val data  = Uri.encode(b.lockId.lockData)
                        val mac   = Uri.encode(b.lockId.lockMac)

                        if (b.lockId.lockData.isNullOrBlank() || b.lockId.lockMac.isNullOrBlank()) {
                            navController.navigate("bookings")
                        } else {
                            navController.navigate("activeReservation/$id/$name/$alias/$data/$mac")
                        }
                    },
                    onRelease = {
                        // Aquí navegarías tras liberar en backend; mantenemos coherencia con el DashboardClient
                        scope.launch {
                            val ok = bookingViewModel.endBooking(context)  // <-- debe limpiar activeBooking en DataStore
                            if (ok) {
                                navController.navigate(Screen.Locks.route)
                                Toast.makeText(context, "Reserva finalizada con éxito", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "No se pudo finalizar la reserva", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                )
            } else {
                OutlinedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    ListItem(
                        headlineContent = { Text("Sin reserva activa") },
                        supportingContent = { Text("Cuando empieces una reserva la verás aquí.") }
                    )
                }
            }
        }

        // ----------------------------- Próximas -------------------------------
        item { SectionHeader("Próximas") }
        if (upcomingFiltered.isEmpty()) {
            item {
                OutlinedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    ListItem(
                        headlineContent = { Text("No tienes próximas reservas") },
                        supportingContent = { Text("Ve a Reservar para crear una.") }
                    )
                }
            }
        } else {
            items(upcomingFiltered, key = { it.id }) { b ->
                UpcomingRow(
                    b = b,
                    onCancel = { pendingCancelId = b.id }
                )
            }
        }

        // ------------------------------ Pasadas -------------------------------
        item { SectionHeader("Pasadas") }
        if (pastFiltered.isEmpty()) {
            item {
                OutlinedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    ListItem(
                        headlineContent = { Text("Aún no hay pasadas") },
                        supportingContent = { Text("Tus reservas finalizadas aparecerán aquí.") }
                    )
                }
            }
        } else {
            items(pastFiltered, key = { it.id }) { b ->
                PastRow(
                    b = b,
                    onRepeat = { navController.navigate("reserve") }
                )
            }
        }
    }

    // Confirmación de cancelación (para próximas)
    if (pendingCancelId != null) {
        AlertDialog(
            onDismissRequest = { pendingCancelId = null },
            title = { Text("Cancelar reserva") },
            text = { Text("¿Seguro que quieres cancelar esta reserva?") },
            confirmButton = {
                TextButton(onClick = {
                    vm.cancelBooking(pendingCancelId!!)
                    pendingCancelId = null
                }) { Text("Cancelar") }
            },
            dismissButton = {
                TextButton(onClick = { pendingCancelId = null }) { Text("Volver") }
            }
        )
    }
}

/* ---------------------------------------------------------------------------------------------- */
/* SECTIONS & CARDS                                                                               */
/* ---------------------------------------------------------------------------------------------- */

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun ActiveBookingCardReal(
    booking: ActiveBooking,
    onDetails: () -> Unit,
    onRelease: () -> Unit
) {
    val centerName = booking.lockId.lockName
    val zoneName = booking.lockId.lockAlias.ifBlank { "Zona" }
    val start = parseInstantOrNull(booking.startTime)
    val end = booking.endTime?.let { parseInstantOrNull(it) }

    ElevatedCard(shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Reserva activa", style = MaterialTheme.typography.titleMedium)
            Text(zoneName, style = MaterialTheme.typography.bodyLarge)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(6.dp))
                val range = buildString {
                    append(start?.let { formatInstant(it) } ?: "Inicio")
                    append(" – ")
                    append(end?.let { formatTime(it) } ?: "en curso")
                }
                Text(range, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (end != null) {
                val left = remaining(end)
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        "Termina en $left",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onDetails, shape = RoundedCornerShape(10.dp)) { Text("Ver detalles") }
                TextButton(onClick = onRelease) { Text("Liberar") }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun UpcomingRow(
    b: BookingItem,
    onCancel: () -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar fecha
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "${b.centerName} • ${b.zoneName}",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${formatInstant(b.startsAt)} – ${formatTime(b.endsAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(
                onClick = onCancel,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Cancelar")
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun PastRow(
    b: BookingItem,
    onRepeat: () -> Unit
) {
    OutlinedCard(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar fecha
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Event, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "${b.centerName} • ${b.zoneName}",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${formatInstant(b.startsAt)} – ${formatTime(b.endsAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = onRepeat) { Text("Repetir") }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
    )
}

/* ---------------------------------------------------------------------------------------------- */
/* UTIL                                                                                           */
/* ---------------------------------------------------------------------------------------------- */

@RequiresApi(Build.VERSION_CODES.O)
private fun parseInstantOrNull(s: String?): Instant? = try {
    if (s.isNullOrBlank()) null else Instant.parse(s)
} catch (_: Throwable) {
    null
}

@RequiresApi(Build.VERSION_CODES.O)
private fun formatInstant(instant: Instant): String {
    val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault())
    return fmt.format(instant)
}

@RequiresApi(Build.VERSION_CODES.O)
private fun formatTime(instant: Instant): String {
    val fmt = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
    return fmt.format(instant)
}

@RequiresApi(Build.VERSION_CODES.O)
private fun remaining(endsAt: Instant): String {
    val now = Instant.now()
    val d = Duration.between(now, endsAt)
    val totalMin = d.toMinutes().coerceAtLeast(0)
    val h = totalMin / 60
    val m = totalMin % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}