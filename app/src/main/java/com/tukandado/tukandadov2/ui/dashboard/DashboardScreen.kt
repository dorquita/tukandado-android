package com.tukandado.tukandadov2.ui.dashboard

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

/* ------------------------------------------------------------------------------------------------
 * STATE & VM
 * ------------------------------------------------------------------------------------------------*/

enum class AlertSeverity { CRITICAL, WARNING, INFO }

data class AlertItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val subtitle: String? = null,
    val severity: AlertSeverity,
    val ctaLabel: String? = null
)

data class ActivityItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val whenTs: Instant,
    val meta: String? = null // p.ej. “por Pau”, “Lock #123”
)

data class DashboardKpis(
    val occupancyPct: Int = 0,
    val activeBookings: Int = 0,
    val offlineLocks: Int = 0,
    val lowBatteries: Int = 0,
    val contraseñas24h: Int = 0
)

data class DashboardState(
    val isLoading: Boolean = true,
    val kpis: DashboardKpis = DashboardKpis(),
    val alerts: List<AlertItem> = emptyList(),
    val activity: List<ActivityItem> = emptyList()
)

class DashboardViewModel : ViewModel() {
    var state by mutableStateOf(DashboardState())
        private set

    @RequiresApi(Build.VERSION_CODES.O)
    fun refresh(context: Context) {
        viewModelScope.launch {
            state = state.copy(isLoading = true)
            // Simula carga: aquí irían tus llamadas reales a repos / API
            delay(450)

            val overSlaCount = 4            // ← demo: nº de tickets abiertos fuera de SLA
            val slaHours = 48               // ← demo: umbral SLA (horas)
            val now = Instant.now()
            state = DashboardState(
                isLoading = false,
                kpis = DashboardKpis(
                    occupancyPct = 37,
                    activeBookings = 12,
                    offlineLocks = 1,
                    lowBatteries = 3,
                    contraseñas24h = 26
                ),
                alerts = listOf(
                    AlertItem(
                        title = "3 candados con batería < 15%",
                        subtitle = "Reemplazo recomendado hoy",
                        severity = AlertSeverity.WARNING,
                        ctaLabel = "Ver candados"
                    ),
                    AlertItem(
                        title = "1 candado offline > 24h",
                        subtitle = "Posible problema de conexión",
                        severity = AlertSeverity.CRITICAL,
                        ctaLabel = "Diagnóstico"
                    ),
                    AlertItem(
                        title = "$overSlaCount incidencias abiertas > $slaHours h (SLA)",
                        subtitle = "Revisión y actuación recomendadas hoy",
                        severity = if (overSlaCount >= 3) AlertSeverity.CRITICAL else AlertSeverity.WARNING,
                        ctaLabel = "Escalar"
                    )
                ),
                activity = listOf(
                    ActivityItem("a1", "Reset de contraseñas (zona A)", now.minusSeconds(60 * 20), "por Admin"),
                    ActivityItem("a2", "Contraseña creada: vestuario B · #22516532", now.minusSeconds(60 * 60), "por Pau"),
                    ActivityItem("a3", "Incidencia cerrada: cerradura 12", now.minusSeconds(60 * 120), "Soporte"),
                )
            )
        }
    }

    fun acknowledgeAlert(id: String) {
        state = state.copy(alerts = state.alerts.filterNot { it.id == id })
    }
}

/* ------------------------------------------------------------------------------------------------
 * SCREEN
 * ------------------------------------------------------------------------------------------------*/

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    setHeaderAction: (label: String?, enabled: Boolean, onClick: (() -> Unit)?) -> Unit = { _, _, _ -> }
) {
    val vm: DashboardViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val context = LocalContext.current
    val s = vm.state

    val fontScale = LocalConfiguration.current.fontScale
    val compact = fontScale >= 1.3f   // umbral de “zoom muy grande”

    // Botón de acción del HeaderBar
    LaunchedEffect(s.isLoading) {
        setHeaderAction("Refrescar", !s.isLoading) { vm.refresh(context) }
    }
    // Primera carga
    LaunchedEffect(Unit) { vm.refresh(context) }
    // Limpia la acción al salir
    DisposableEffect(Unit) { onDispose { setHeaderAction(null, true, null) } }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(bottom = 12.dp)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState()), // <-- scroll vertical añadido
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (s.isLoading) { LinearProgressIndicator(Modifier.fillMaxWidth()) }

        // KPIs
        KpiRow(k = s.kpis, compact = compact)

        // Alertas
        AlertsSection(
            alerts = s.alerts,
            compact = compact,
            onAcknowledge = { vm.acknowledgeAlert(it) },
            onPrimary = { /* navega a candados/diagnóstico según CTA si quieres */ }
        )

        // Accesos rápidos
        QuickActionsRow(
            items = listOf(
                /*QuickAction(
                    icon = Icons.Outlined.Key,
                    label = "Crear passcode",
                    onClick = { /* navController.navigate("contraseñas/…") o selector de lock */ }
                ),
                QuickAction(
                    icon = Icons.Outlined.Refresh,
                    label = "Reset contraseñas",
                    onClick = { /* confirm + reset centro / selección múltiple */ }
                ),*/
                QuickAction(
                    icon = Icons.Outlined.Add,
                    label = "Añadir candado",
                    onClick = { /* nav a flujo de alta (BLE scan) */ },
                    enabled = false
                ),
                QuickAction(
                    icon = Icons.Outlined.CloudDownload,
                    label = "Exportar registros",
                    onClick = { /* export CSV */ },
                    enabled = false
                )
            ),
            compact = compact
        )

        // Actividad reciente
        ActivitySection(items = s.activity, compact = compact)
        Spacer(Modifier.height(8.dp))
    }
}

/* ------------------------------------------------------------------------------------------------
 * SECTIONS
 * ------------------------------------------------------------------------------------------------*/

@Composable
private fun KpiRow(k: DashboardKpis, compact: Boolean) {
    val scroll = rememberScrollState()
    Row(
        Modifier.fillMaxWidth().horizontalScroll(scroll),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        KpiCard(Icons.Outlined.Assessment, "Ocupación", "${k.occupancyPct}%", MaterialTheme.colorScheme.primary, compact)
        KpiCard(Icons.Outlined.History, "Reservas activas", "${k.activeBookings}", MaterialTheme.colorScheme.tertiary, compact)
        KpiCard(Icons.Outlined.Bolt, "Batería baja", "${k.lowBatteries}", MaterialTheme.colorScheme.error, compact)
        KpiCard(Icons.Outlined.BugReport, "Offline", "${k.offlineLocks}", MaterialTheme.colorScheme.error, compact)
        KpiCard(Icons.Outlined.AutoAwesome, "Contraseñas (24h)", "${k.contraseñas24h}", MaterialTheme.colorScheme.secondary, compact)
    }
}

@Composable
private fun KpiCard(
    icon: ImageVector,
    title: String,
    value: String,
    accent: Color,
    compact: Boolean
) {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            Modifier
                .padding(if (compact) 12.dp else 16.dp)
                .widthIn(min = if (compact) 140.dp else 180.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(if (compact) 36.dp else 42.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) { Icon(icon, contentDescription = null, tint = accent) }

            Spacer(Modifier.width(if (compact) 8.dp else 12.dp))

            Column {
                Text(
                    title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    value,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )
            }
        }
    }
}

@Composable
private fun AlertsSection(
    alerts: List<AlertItem>,
    compact: Boolean,
    onAcknowledge: (String) -> Unit,
    onPrimary: (AlertItem) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader("Alertas")
        if (alerts.isEmpty()) {
            OutlinedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                ListItem(
                    headlineContent = { Text("Sin alertas") },
                    supportingContent = { Text("¡Todo en orden! 🎉") }
                )
            }
        } else {
            alerts.forEach { a -> AlertRow(a, compact, onAcknowledge, onPrimary) }
        }
    }
}

@Composable
private fun AlertRow(
    item: AlertItem,
    compact: Boolean,
    onAcknowledge: (String) -> Unit,
    onPrimary: (AlertItem) -> Unit
) {
    val (bg, fg) = when (item.severity) {
        AlertSeverity.CRITICAL -> MaterialTheme.colorScheme.error.copy(alpha = 0.10f) to MaterialTheme.colorScheme.error
        AlertSeverity.WARNING  -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.10f) to MaterialTheme.colorScheme.tertiary
        AlertSeverity.INFO     -> MaterialTheme.colorScheme.primary.copy(alpha = 0.10f) to MaterialTheme.colorScheme.primary
    }

    Surface(shape = RoundedCornerShape(16.dp), color = bg) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(fg.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Outlined.Security, contentDescription = null, tint = fg) }

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
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = if (compact) 2 else 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (!compact) {
                    if (!item.ctaLabel.isNullOrBlank()) {
                        TextButton(onClick = { onPrimary(item) }) { Text(item.ctaLabel!!) }
                    }
                    TextButton(
                        onClick = { onAcknowledge(item.id) },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                    ) { Text("Ocultar") }
                }
            }

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
                        onClick = { onAcknowledge(item.id) },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                    ) { Text("Ocultar", maxLines = 1, softWrap = false) }
                }
            }
        }
    }
}

data class QuickAction(
    val icon: ImageVector,
    val label: String,
    val onClick: () -> Unit,
    val enabled: Boolean = true
)

@Composable
private fun QuickActionsRow(items: List<QuickAction>, compact: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader("Accesos rápidos")

        if (compact) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items.forEach { QuickActionButton(it, modifier = Modifier.fillMaxWidth()) }
            }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items.take(2).forEach { QuickActionButton(it, modifier = Modifier.weight(1f)) }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items.drop(2).take(2).forEach { QuickActionButton(it, modifier = Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun QuickActionButton(item: QuickAction, modifier: Modifier = Modifier) {
    FilledTonalButton(
        onClick = item.onClick,
        enabled = item.enabled,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(item.icon, contentDescription = item.label)
            Spacer(Modifier.width(10.dp))
            Text(item.label, style = MaterialTheme.typography.labelLarge, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
        }
    }
}


@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun ActivitySection(items: List<ActivityItem>, compact: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader("Actividad reciente")
        if (items.isEmpty()) {
            OutlinedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                ListItem(
                    headlineContent = { Text("Sin actividad") },
                    supportingContent = { Text("Aquí verás resets, altas/bajas de contraseñas e incidencias.") }
                )
            }
        } else items.forEach { ActivityRow(it, compact) }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun ActivityRow(i: ActivityItem, compact: Boolean) {
    OutlinedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        ListItem(
            leadingContent = {
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Outlined.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
            },
            headlineContent = {
                Text(i.title, style = MaterialTheme.typography.bodyLarge, maxLines = if (compact) 1 else 2, overflow = TextOverflow.Ellipsis)
            },
            supportingContent = {
                val whenTxt = formatInstant(i.whenTs)
                Text(
                    listOfNotNull(whenTxt, i.meta).joinToString(" • "),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        )
    }
}


@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
    )
}

/* ------------------------------------------------------------------------------------------------
 * UTIL
 * ------------------------------------------------------------------------------------------------*/

@RequiresApi(Build.VERSION_CODES.O)
private fun formatInstant(instant: Instant): String {
    val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault())
    return fmt.format(instant)
}