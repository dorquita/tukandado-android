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

/**
 * Pantalla de creación de passcode usando el botón de acción global del HeaderBar.
 * Recibe setHeaderAction para configurar el botón "Guardar" del AppBar superior.
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
    // 👇 función que te expone MainScreen para configurar el botón del HeaderBar
    setHeaderAction: (label: String?, enabled: Boolean, onClick: (() -> Unit)?) -> Unit
) {
    val vm: PasscodeViewModel = viewModel()
    val context = LocalContext.current

    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("timebound") } // permanent | timebound | one_time
    var validFrom by remember { mutableStateOf<Instant?>(null) }
    var validTo by remember { mutableStateOf<Instant?>(null) }
    var isCustom by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    val TAG = "PasscodeDatePick"

    LaunchedEffect(Unit) {
        Log.d(TAG, "init validFrom=$validFrom validTo=$validTo")
    }
    LaunchedEffect(validFrom) {
        Log.d(TAG, "RECOMPOSE → validFrom cambió a: $validFrom")
    }
    LaunchedEffect(validTo) {
        Log.d(TAG, "RECOMPOSE → validTo cambió a: $validTo")
    }


    // Reglas de habilitación del guardado
    val canSave by derivedStateOf {
        code.length in 4..10 && (type != "timebound" || (validFrom != null && validTo != null))
    }

    // Acción de guardado (misma lógica que ya tenías)

    val onSave: () -> Unit = save@{
        if (!canSave || loading) return@save
        scope.launch {
            loading = true
            error = null
            try {
                val result = vm.createPasscode(
                    context = context,
                    request = CreatePasscodeRequest(
                        lockId = lockId,
                        code = code,
                        type = type,
                        validFrom = validFrom?.toString(),
                        validTo = validTo?.toString(),
                        name = name.ifBlank { null },
                        isCustom = isCustom,
                        lockData = lockData,
                        lockMac = lockMac
                    ),
                    refreshAfterCreate = true
                )

                if (result.isSuccess) {
                    Toast.makeText(context, "Código creado correctamente", Toast.LENGTH_SHORT).show()
                    navController.popBackStack() // vuelve al listado
                } else {
                    val msg = result.exceptionOrNull()?.message ?: "Error creando el código"
                    error = msg
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                }
            } finally {
                loading = false
            }
        }
    }

    // 👇 Conecta el botón del HeaderBar a esta pantalla
    LaunchedEffect(code, type, validFrom, validTo) {
        setHeaderAction("Guardar", canSave, onSave)
    }
    // Limpia el botón cuando sales de la pantalla
    DisposableEffect(Unit) {
        onDispose { setHeaderAction(null, true, null) }
    }

    // Contenido de la pantalla (sin TopAppBar local)
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

        // Tipo: segmented
        SegmentedButtons(
            options = listOf(
                "permanent" to "Permanente",
                "timebound" to "Temporal",
                "one_time" to "Una vez"
            ),
            selected = type,
            onSelect = { type = it }
        )

        OutlinedTextField(
            value = code,
            onValueChange = { if (it.length <= 10 && it.all { c -> c.isDigit() }) code = it },
            label = { Text("PIN (4–10 dígitos)") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true
        )

        // Vigencia solo si no es permanente
        if (type != "permanent") {
            DateTimeRow(
                label = "Inicio",
                instant = validFrom,
                onPick = {
                    Log.d(TAG, "onPick Inicio (actual=$validFrom)")
                    scope.launch {
                        val picked = pickDateTime(context, validFrom)
                        Log.d(TAG, "onPick Inicio → picked=$picked")
                        if (picked != null) validFrom = picked
                    }
                }
            )
            DateTimeRow(
                label = "Fin",
                instant = validTo,
                onPick = {
                    Log.d(TAG, "onPick Fin (actual=$validTo)")
                    scope.launch {
                        val picked = pickDateTime(context, validTo)
                        Log.d(TAG, "onPick Fin → picked=$picked")
                        if (picked != null) validTo = picked
                    }
                }
            )

        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FilterChip(
                selected = isCustom,
                onClick = { isCustom = !isCustom },
                label = { Text("Personalizado") }
            )
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
    onSelect: (String) -> Unit
) {
    SingleChoiceSegmentedButtonRow {
        options.forEachIndexed { idx, (value, label) ->
            SegmentedButton(
                selected = selected == value,
                onClick = { onSelect(value) },
                shape = SegmentedButtonDefaults.itemShape(index = idx, count = options.size)
            ) { Text(label) }
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
                    // Aquí SÍ usamos onDismiss (si el usuario cierra sin elegir hora).
                    setOnDismissListener { if (!resumed) safeResume(null) }
                    show()
                }
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).apply {
            setOnCancelListener { safeResume(null) }
            // ⚠️ IMPORTANTE: NO pongas onDismiss aquí,
            // se dispara también cuando confirmas y cerraría con null.
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