package com.tukandado.tukandadov2.ui.dashboard

import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
/* STATE & VM (sugerencias/avisos; la reserva activa real viene del SessionManager)               */
/* ---------------------------------------------------------------------------------------------- */

enum class ClientNoticeSeverity { INFO, WARNING }

data class ClientNotice(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val subtitle: String? = null,
    val severity: ClientNoticeSeverity = ClientNoticeSeverity.INFO,
    val ctaLabel: String? = null
)

data class SuggestedCenter(
    val id: String,
    val name: String,
    val distanceKm: Double,
    val nextAvailability: String
)

data class DashboardClientUi(
    val isLoading: Boolean = true,
    val suggested: List<SuggestedCenter> = emptyList(),
    val notices: List<ClientNotice> = emptyList()
)

class DashboardClientViewModel : ViewModel() {
    var ui by mutableStateOf(DashboardClientUi())
        private set

    fun refresh() {
        viewModelScope.launch {
            ui = ui.copy(isLoading = true)
            delay(350)
            ui = DashboardClientUi(
                isLoading = false,
                suggested = listOf(
                    SuggestedCenter("c1", "Gimnasio Río",   0.8, "12:30"),
                    SuggestedCenter("c2", "Sport Nueve",    1.6, "13:00"),
                    SuggestedCenter("c3", "Centro Avenida", 2.1, "13:30"),
                ),
                notices = listOf(
                    ClientNotice(
                        title = "Recuerda llegar 5 min antes",
                        subtitle = "Evita esperas y solapes",
                        severity = ClientNoticeSeverity.INFO
                    ),
                    ClientNotice(
                        title = "Mantenimiento hoy 16:00–17:00",
                        subtitle = "Podría afectar al centro Río",
                        severity = ClientNoticeSeverity.WARNING,
                        ctaLabel = "Ver detalles"
                    )
                )
            )
        }
    }

    fun dismissNotice(id: String) {
        ui = ui.copy(notices = ui.notices.filterNot { it.id == id })
    }
}

/* ---------------------------------------------------------------------------------------------- */
/* SCREEN                                                                                         */
/* ---------------------------------------------------------------------------------------------- */

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardClientScreen(
    navController: NavController,
    setHeaderAction: (label: String?, enabled: Boolean, onClick: (() -> Unit)?) -> Unit = { _, _, _ -> }
) {
    val vm: DashboardClientViewModel = viewModel()

    // Sin acción en el AppBar del home de cliente
    LaunchedEffect(Unit) { setHeaderAction(null, true, null) }
    DisposableEffect(Unit) { onDispose { setHeaderAction(null, true, null) } }

    // Sugeridos/Avisos
    val ui = vm.ui
    LaunchedEffect(Unit) { vm.refresh() }

    // Reserva activa real desde SessionManager
    val context = androidx.compose.ui.platform.LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val activeBooking: ActiveBooking? by sessionManager
        .getActiveBooking()
        .collectAsState(initial = null)

    val scope = rememberCoroutineScope()
    val bookingViewModel: BookingViewModel = viewModel()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(bottom = 12.dp)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (ui.isLoading) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }

        // Bloque superior: reserva activa real o CTA reservar
        if (activeBooking == null) {
            HeroReserveCard(
                onReserve = { navController.navigate("locks") },
                onBookings = { navController.navigate("bookings") }
            )
        } else {
            val b = activeBooking!!
            ActiveBookingCardReal(
                booking = b,
                onDetails = {
                    // Navega primero a activeReservation con datos del lock
                    val lockId  = Uri.encode(b.lockId._id)
                    val name  = Uri.encode(b.lockId.lockName)
                    val alias = Uri.encode(b.lockId.lockAlias)
                    val data  = Uri.encode(b.lockId.lockData)
                    val mac   = Uri.encode(b.lockId.lockMac)

                    if (b.lockId.lockData.isNullOrBlank() || b.lockId.lockMac.isNullOrBlank()) {
                        navController.navigate("bookings")
                    } else {
                        navController.navigate("activeReservation/$lockId/$name/$alias/$data/$mac")
                    }
                },
                onRelease = {
                    scope.launch {
                        val ok = bookingViewModel.endBooking(context)  // <-- debe limpiar activeBooking en DataStore
                        if (ok) {
                            Toast.makeText(context, "Reserva finalizada con éxito", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "No se pudo finalizar la reserva", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            )
        }

        // Sugeridos
        SuggestedCentersRow(
            items = ui.suggested,
            onOpenCenter = { navController.navigate("reserve") }
        )

        // Accesos rápidos
        /*QuickActionsRow(
            items = listOf(
                QuickActionClient(icon = Icons.Outlined.CalendarMonth, label = "Reservar", enabled = true, onClick = {
                    navController.navigate("bookings")
                }),
                QuickActionClient(icon = Icons.Outlined.EventAvailable,  label = "Reservas",  enabled = true, onClick = {
                    navController.navigate("bookings")
                }),
                QuickActionClient(icon = Icons.Outlined.HelpOutline,     label = "Ayuda",     enabled = true, onClick = {
                    navController.navigate("profile")
                })
            )
        )*/

        // Avisos
        NoticesSection(
            notices = ui.notices,
            onPrimary = { notice ->
                if (notice.ctaLabel?.contains("detalle", true) == true) {
                    navController.navigate("bookings")
                } else {
                    navController.navigate("profile")
                }
            },
            onDismiss = { vm.dismissNotice(it) }
        )

        Spacer(Modifier.height(8.dp))
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
            Text(
                zoneName,
                style = MaterialTheme.typography.bodyLarge
            )
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

            // “Termina en …” solo si existe endTime
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
                Button(onClick = onDetails, shape = RoundedCornerShape(10.dp)) {
                    Text("Ver detalles")
                }
                TextButton(onClick = onRelease) {
                    Text("Liberar")
                }
            }
        }
    }
}

data class QuickActionClient(
    val icon: ImageVector,
    val label: String,
    val onClick: () -> Unit,
    val enabled: Boolean = true
)

@Composable
private fun HeroReserveCard(
    onReserve: () -> Unit,
    onBookings: () -> Unit
) {
    val fontScale = LocalConfiguration.current.fontScale
    val stack = fontScale >= 1.3f   // con texto grande, apilar

    ElevatedCard(shape = RoundedCornerShape(16.dp)) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("¿Listo para entrenar?", style = MaterialTheme.typography.titleMedium)
            Text(
                "Reserva tu espacio en segundos. Elige centro, franja y ¡listo!",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (stack) {
                // Botones en columna (no se rompen)
                Button(
                    onClick = onReserve,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Reservar ahora", maxLines = 1) }

                TextButton(
                    onClick = onBookings,
                    modifier = Modifier.align(Alignment.Start)
                ) {
                    // Evita saltos “M is / reser / vas”
                    Text(
                        "Mis reservas",
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                // Layout normal en fila
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = onReserve, shape = RoundedCornerShape(10.dp)) {
                        Text("Reservar ahora", maxLines = 1)
                    }
                    TextButton(onClick = onBookings) {
                        Text(
                            "Mis reservas",
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestedCentersRow(
    items: List<SuggestedCenter>,
    onOpenCenter: (SuggestedCenter) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader("Sugeridos cerca")
        if (items.isEmpty()) {
            OutlinedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                ListItem(
                    headlineContent = { Text("No hay sugerencias") },
                    supportingContent = { Text("Empieza reservando en tu centro favorito.") }
                )
            }
        } else {
            val scroll = rememberScrollState()
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scroll),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items.forEach { CenterCard(it) { onOpenCenter(it) } }
            }
        }
    }
}

@Composable
private fun CenterCard(item: SuggestedCenter, onClick: () -> Unit) {
    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.widthIn(min = 220.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Place, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(item.name, style = MaterialTheme.typography.titleSmall)
                    Text(
                        "${"%.1f".format(item.distanceKm)} km • Próx. ${item.nextAvailability}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            /*Button(
                onClick = onClick,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text("Reservar")
            }*/
        }
    }
}

@Composable
private fun QuickActionsRow(items: List<QuickActionClient>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader("Accesos rápidos")
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items.forEach { QuickActionButton(it, modifier = Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun QuickActionButton(item: QuickActionClient, modifier: Modifier = Modifier) {
    val contentColor =
        if (item.enabled) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        shape = RoundedCornerShape(14.dp),
        tonalElevation = if (item.enabled) 2.dp else 0.dp,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (item.enabled) 0.6f else 0.4f),
        modifier = modifier.clickable(enabled = item.enabled) { item.onClick() }
    ) {
        Row(
            Modifier
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(item.icon, contentDescription = item.label, tint = contentColor)
            Spacer(Modifier.width(8.dp))
            Text(item.label, color = contentColor, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/* Avisos */

@Composable
private fun NoticesSection(
    notices: List<ClientNotice>,
    onPrimary: (ClientNotice) -> Unit,
    onDismiss: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader("Avisos")
        if (notices.isEmpty()) {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                ListItem(
                    leadingContent = {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    headlineContent = { Text("Sin avisos") },
                    supportingContent = { Text("¡Todo en orden!") }
                )
            }
        } else {
            notices.forEach { n ->
                NoticeRow(n, onPrimary, onDismiss) // tu implementación existente
            }
        }
    }
}

@Composable
private fun NoticeRow(
    item: ClientNotice,
    onPrimary: (ClientNotice) -> Unit,
    onDismiss: (String) -> Unit
) {
    val fontScale = LocalConfiguration.current.fontScale
    val compact = fontScale >= 1.3f   // con texto grande, compactar

    val (bg, fg) = when (item.severity) {
        ClientNoticeSeverity.INFO    -> MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) to MaterialTheme.colorScheme.primary
        ClientNoticeSeverity.WARNING -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f) to MaterialTheme.colorScheme.tertiary
    }

    Surface(shape = RoundedCornerShape(16.dp), color = bg) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(fg.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Outlined.HelpOutline, contentDescription = null, tint = fg) }

                Spacer(Modifier.width(12.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        item.title,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = if (compact) 2 else 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    item.subtitle?.let {
                        Text(
                            it,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = if (compact) 2 else 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // En modo normal, las acciones caben a la derecha
                if (!compact) {
                    if (!item.ctaLabel.isNullOrBlank()) {
                        TextButton(onClick = { onPrimary(item) }) { Text(item.ctaLabel!!) }
                    }
                    TextButton(
                        onClick = { onDismiss(item.id) },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                    ) { Text("Ocultar") }
                }
            }

            // Con zoom grande, mueve las acciones a una segunda línea
            if (compact) {
                Spacer(Modifier.height(6.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!item.ctaLabel.isNullOrBlank()) {
                        TextButton(onClick = { onPrimary(item) }) {
                            Text(item.ctaLabel!!, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    TextButton(
                        onClick = { onDismiss(item.id) },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                    ) { Text("Ocultar", maxLines = 1) }
                }
            }
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