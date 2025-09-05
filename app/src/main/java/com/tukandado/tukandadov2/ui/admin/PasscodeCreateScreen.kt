package com.tukandado.tukandadov2.ui.admin

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.tukandado.tukandadov2.api.CreatePasscodeRequest
import com.tukandado.tukandadov2.viewmodel.PasscodeViewModel
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import androidx.compose.ui.draw.alpha
import com.tukandado.tukandadov2.data.SessionManager
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import com.tukandado.tukandadov2.ttlock.TTLockManager

/**
 * Pantalla de creación de passcode.
 * - Admin: igual que antes (tipo, fechas, personalizado).
 * - Cliente: sin tipo ni personalizado. Crea siempre "permanent" y se mostrará
 *            un aviso de que el código se borrará al finalizar la reserva.
 */
@SuppressLint("UnrememberedMutableState")
@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasscodeCreateScreen(
    lockId: String,
    lockData: String,
    lockMac: String,
    navController: NavController,
    onBack: () -> Unit,
    setHeaderAction: (label: String?, enabled: Boolean, onClick: (() -> Unit)?) -> Unit
) {
    val vm: PasscodeViewModel = viewModel()
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val role by sessionManager.getRole().collectAsState(initial = "client")
    val isAdmin = role == "admin" || role == "superadmin"
    val isClient = !isAdmin

    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }

    // Admin-only state
    var type by remember { mutableStateOf("timebound") } // permanent | timebound | one_time
    var validFrom by remember { mutableStateOf<Instant?>(null) }
    var validTo by remember { mutableStateOf<Instant?>(null) }
    var isCustom by remember { mutableStateOf(false) }

    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    val TAG = "PasscodeCreate"

    val clientPasscodeVm: PasscodeViewModel = viewModel()

    // Reglas de habilitación del guardado
    val canSave by derivedStateOf {
        if (isClient) {
            // Cliente: solo necesita PIN válido
            code.length in 4..10
        } else {
            // Admin: igual que antes
            code.length in 4..10 && (type != "timebound" || (validFrom != null && validTo != null))
        }
    }

    val onSave: () -> Unit = save@{
        if (!canSave || loading) return@save
        scope.launch {
            loading = true
            error = null
            try {
                if (isClient) {
                    val result = vm.createPasscodeClient(
                        context = context,
                        request = CreatePasscodeRequest(
                            lockId   = lockId,
                            code     = code,
                            type     = "booking",          // para cliente lo tratamos como permanente
                            validFrom= null,
                            validTo  = null,
                            name     = name.ifBlank { null },
                            isCustom = false,
                            lockData = lockData,
                            lockMac  = lockMac
                        ),
                        clientPasscodeVm = clientPasscodeVm,
                        refreshAfterCreate = true
                    )

                    if (result.isSuccess) {
                        Toast.makeText(context, "Código creado correctamente", Toast.LENGTH_SHORT).show()
                        navController.popBackStack()
                    } else {
                        val msg = result.exceptionOrNull()?.message ?: "Error creando el código"
                        error = msg
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    }
                } else {
                    // Admin: lo que ya hacías
                    val finalType   = type
                    val finalFrom   = validFrom?.toString()
                    val finalTo     = validTo?.toString()
                    val finalCustom = isCustom

                    val result = vm.createPasscode(
                        context = context,
                        request = CreatePasscodeRequest(
                            lockId = lockId,
                            code = code,
                            type = finalType,
                            validFrom = finalFrom,
                            validTo = finalTo,
                            name = name.ifBlank { null },
                            isCustom = finalCustom,
                            lockData = lockData,
                            lockMac = lockMac
                        ),
                        refreshAfterCreate = true
                    )

                    if (result.isSuccess) {
                        Toast.makeText(context, "Código creado correctamente", Toast.LENGTH_SHORT).show()
                        navController.popBackStack()
                    } else {
                        val msg = result.exceptionOrNull()?.message ?: "Error creando el código"
                        error = msg
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    }
                }
            } finally {
                loading = false
            }
        }
    }

    // Conecta el botón del HeaderBar
    LaunchedEffect(code, type, validFrom, validTo, isClient) {
        setHeaderAction("Guardar", canSave && !loading, onSave)
    }
    DisposableEffect(Unit) {
        onDispose { setHeaderAction(null, true, null) }
    }

    // Contenido
    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Nombre (opcional)") },
            modifier = Modifier.fillMaxWidth()
        )

        // ---------- UI específica por rol ----------
        if (isAdmin) {
            // Admin: selector de tipo
            SegmentedButtons(
                options = listOf(
                    "permanent" to "Permanente",
                    "timebound" to "Temporal",
                    "one_time"  to "Una vez"
                ),
                selected = type,
                onSelect = { type = it },
                disabled = setOf("one_time")
            )
        } else {
            // Cliente: aviso de comportamiento
            OutlinedCard(Modifier.fillMaxWidth()) {
                ListItem(
                    headlineContent = { Text("Código temporal de reserva") },
                    supportingContent = {
                        Text(
                            "Tu PIN funcionará mientras tu reserva esté activa. " +
                                    "Al finalizar la reserva, el código se eliminará automáticamente."
                        )
                    }
                )
            }
        }

        OutlinedTextField(
            value = code,
            onValueChange = { if (it.length <= 10 && it.all(Char::isDigit)) code = it },
            label = { Text("PIN (4–10 dígitos)") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true
        )

        // Vigencia solo si no es permanente (Admin)
        if (isAdmin && type != "permanent") {
            DateTimeRow(
                label = "Inicio",
                instant = validFrom,
                onPick = {
                    scope.launch {
                        val picked = pickDateTime(context, validFrom)
                        if (picked != null) validFrom = picked
                    }
                }
            )
            DateTimeRow(
                label = "Fin",
                instant = validTo,
                onPick = {
                    scope.launch {
                        val picked = pickDateTime(context, validTo)
                        if (picked != null) validTo = picked
                    }
                }
            )
        }

        // Chip “Personalizado” solo Admin
        if (isAdmin) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilterChip(
                    selected = isCustom,
                    onClick = { isCustom = !isCustom },
                    label = { Text("Personalizado") }
                )
            }
        }

        if (error != null) {
            Text(error!!, color = MaterialTheme.colorScheme.error)
        }
        if (loading) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }
    }
}

/* ---------- UI helpers ---------- */

@Composable
private fun SegmentedButtons(
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
    disabled: Set<String> = emptySet()
) {
    SingleChoiceSegmentedButtonRow {
        options.forEachIndexed { idx, (value, label) ->
            val isDisabled = value in disabled
            val buttonModifier = if (isDisabled) Modifier.alpha(0.5f) else Modifier
            val labelColor = if (isDisabled)
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            else
                MaterialTheme.colorScheme.onSurface

            SegmentedButton(
                selected = selected == value,
                onClick = { if (!isDisabled) onSelect(value) },
                enabled = !isDisabled,
                modifier = buttonModifier,
                shape = SegmentedButtonDefaults.itemShape(index = idx, count = options.size)
            ) {
                Text(label, color = labelColor)
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun DateTimeRow(
    label: String,
    instant: Instant?,
    onPick: () -> Unit
) {
    val text = instant?.let { formatInstant(it) } ?: "Seleccionar…"
    OutlinedCard(onClick = onPick, modifier = Modifier.fillMaxWidth()) {
        ListItem(
            headlineContent = { Text(label) },
            supportingContent = { Text(text) }
        )
    }
}

/* ---------- Date helpers (API 26+) ---------- */

@RequiresApi(Build.VERSION_CODES.O)
private suspend fun pickDateTime(context: Context, current: Instant?): Instant? =
    suspendCancellableCoroutine { cont ->
        val cal = Calendar.getInstance().apply {
            timeInMillis = current?.toEpochMilli() ?: System.currentTimeMillis()
        }
        var resumed = false
        var timeDialog: TimePickerDialog? = null

        fun safeResume(value: Instant?) {
            if (!resumed && !cont.isCompleted) {
                resumed = true
                cont.resume(value)
            }
        }

        val dateDialog = DatePickerDialog(
            context,
            { _, y, m, d ->
                cal.set(Calendar.YEAR, y)
                cal.set(Calendar.MONTH, m)
                cal.set(Calendar.DAY_OF_MONTH, d)

                timeDialog = TimePickerDialog(
                    context,
                    { _, h, min ->
                        cal.set(Calendar.HOUR_OF_DAY, h)
                        cal.set(Calendar.MINUTE, min)
                        cal.set(Calendar.SECOND, 0)
                        cal.set(Calendar.MILLISECOND, 0)
                        safeResume(Instant.ofEpochMilli(cal.timeInMillis))
                    },
                    cal.get(Calendar.HOUR_OF_DAY),
                    cal.get(Calendar.MINUTE),
                    true
                ).apply {
                    setOnCancelListener { safeResume(null) }
                    setOnDismissListener { if (!resumed) safeResume(null) }
                    show()
                }
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).apply {
            setOnCancelListener { safeResume(null) }
            show()
        }

        cont.invokeOnCancellation {
            try { dateDialog.dismiss() } catch (_: Throwable) {}
            try { timeDialog?.dismiss() } catch (_: Throwable) {}
        }
    }

@RequiresApi(Build.VERSION_CODES.O)
private fun formatInstant(i: Instant): String =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.getDefault())
        .withZone(ZoneId.systemDefault())
        .format(i)