package com.tukandado.tukandadov2.ui.components

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable fun StatusChip(status: String) {
    val color = when (status) {
        "active" -> MaterialTheme.colorScheme.primary
        "pending_sync" -> MaterialTheme.colorScheme.tertiary
        "expired" -> MaterialTheme.colorScheme.outline
        "revoked" -> MaterialTheme.colorScheme.outlineVariant
        "failed" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.secondary
    }
    AssistChip(onClick = {}, label = { Text(status) }, colors = AssistChipDefaults.assistChipColors(labelColor = color))
}

@Composable fun TypeChip(type: String) {
    AssistChip(onClick = {}, label = { Text(type) })
}

@Composable fun UsageChip(count: Int) {
    AssistChip(onClick = {}, label = { Text("Usos: $count") })
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ValidityLine(validFrom: String?, validTo: String?, type: String? = null) {
    Text(formatValidity(validFrom, validTo, type))
}

@Composable
fun InfoRow(label: String, value: String) {
    Row {
        Text("$label: ")
        Spacer(Modifier.width(6.dp))
        Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun FilterChipX(selected: Boolean, label: String, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) }
    )
}

@Composable
fun StatusChoiceChip(
    current: String?,
    value: String?,
    label: String,
    onSelect: (String?) -> Unit
) {
    FilterChip(
        selected = current == value,
        onClick = { onSelect(value) },
        label = { Text(label) }
    )
}

@RequiresApi(Build.VERSION_CODES.O)
private val zone = ZoneId.systemDefault()
@RequiresApi(Build.VERSION_CODES.O)
private val fmt = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale.getDefault())

@RequiresApi(Build.VERSION_CODES.O)
private fun parseIsoOrNull(s: String?): Instant? = try {
    if (s.isNullOrBlank()) null else Instant.parse(s)
} catch (_: Throwable) { null }

@RequiresApi(Build.VERSION_CODES.O)
private fun isEpochZero(i: Instant?) = i?.epochSecond == 0L

@RequiresApi(Build.VERSION_CODES.O)
fun formatIsoToUser(s: String?): String {
    val i = parseIsoOrNull(s) ?: return "—"
    return fmt.format(i.atZone(zone))
}

@RequiresApi(Build.VERSION_CODES.O)
fun formatValidity(validFrom: String?, validTo: String?, type: String?): String {
    val startI = parseIsoOrNull(validFrom)
    val endI = parseIsoOrNull(validTo)

    val startLabel = when {
        isEpochZero(startI) -> "Desde siempre"
        startI != null -> formatIsoToUser(validFrom)
        else -> "—"
    }

    // Permanente si no hay fin o el tipo lo indica
    if (endI == null || type == "permanent") {
        return "Vigencia: $startLabel → Permanente"
    }

    val endLabel = formatIsoToUser(validTo)
    return "Vigencia: $startLabel → $endLabel"
}
