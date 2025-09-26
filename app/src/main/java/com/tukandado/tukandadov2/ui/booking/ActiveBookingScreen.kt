package com.tukandado.tukandadov2.ui.booking

import ClientActionsGrid
import ElapsedTimeView
import android.annotation.SuppressLint
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tukandado.tukandadov2.data.SessionManager
import com.tukandado.tukandadov2.ttlock.TTLockManager
import com.tukandado.tukandadov2.viewmodel.BookingViewModel
import com.tukandado.tukandadov2.ui.components.layout.LockActionLayout
import com.tukandado.tukandadov2.viewmodel.PasscodeViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive


data class BookingPasscodeDto(
    val _id: String,
    val code: String,
    val name: String? = null,
    val type: String? = null
)

/**
 * Borra secuencialmente passcodes por BLE.
 *
 * @param deleteOne   función que borra 1 passcode (devuelve Result<Unit>)
 * @param passcodes   lista a borrar en orden
 * @param onProgress  callback de progreso (hechos, total, ok, code)
 * @param onBackendMarkDeleted callback que marca en backend el passcode como "deleted" si el borrado BLE fue OK
 * @param interOpDelayMs pequeño delay entre operaciones BLE para estabilizar
 */
suspend fun removeBookingPasscodesSequential(
    deleteOne: suspend (code: String) -> Result<Unit>,
    passcodes: List<BookingPasscodeDto>,
    onProgress: (done: Int, total: Int, ok: Boolean, code: String) -> Unit = { _, _, _, _ -> },
    onBackendMarkDeleted: suspend (id: String) -> Unit = {},
    interOpDelayMs: Long = 250L
): Result<Unit> {
    val total = passcodes.size
    var done = 0

    for (p in passcodes) {
        if (!currentCoroutineContext().isActive) {
            return Result.failure(Exception("Operación cancelada"))
        }

        val r = deleteOne(p.code)
        val ok = r.isSuccess

        if (ok) {
            // Notificamos al backend (best-effort)
            runCatching { onBackendMarkDeleted(p._id) }
        }

        done++
        onProgress(done, total, ok, p.code)

        // Si quieres abortar al primer fallo, descomenta:
        if (!ok) return r

        delay(interOpDelayMs)
    }
    return Result.success(Unit)
}

@RequiresApi(Build.VERSION_CODES.O)
@SuppressLint("DefaultLocale")
@Composable
fun ActiveBookingScreen(
    lockName: String,
    lockAlias: String,
    lockData: String,
    lockMac: String,
    onRelease: () -> Unit,
    // 👇 Opcionales para navegar a pantallas de acciones del cliente
    onOpenPasscodes: () -> Unit = {},
    onOpenRF: () -> Unit = {}
) {
    val context = LocalContext.current
    val ttLockManager = remember { TTLockManager(context) }
    val bookingViewModel: BookingViewModel = viewModel()
    val scope = rememberCoroutineScope()

    val sessionManager = remember { SessionManager(context) }

    var isOpening by remember { mutableStateOf(false) }
    var isLockerOpen by remember { mutableStateOf(false) }

    val passcodeVm: PasscodeViewModel = viewModel()

    LockActionLayout(
        lockName = lockName,
        lockAlias = lockAlias,
        isOpening = isOpening,
        isLockerOpen = isLockerOpen,
        onOpen = {
            scope.launch {
                isOpening = true
                isLockerOpen = false

                val result = runCatching {
                    ttLockManager.controlLock(
                        lockDataJson = lockData,
                        lockMac = lockMac,
                        isOpen = true
                    )
                }.getOrElse { Result.failure(it) }

                isOpening = false

                result.fold(
                    onSuccess = {
                        isLockerOpen = true
                        Toast.makeText(context, "Candado abierto", Toast.LENGTH_SHORT).show()
                        scope.launch {
                            delay(2500)
                            isLockerOpen = false
                        }
                    },
                    onFailure = { e ->
                        isLockerOpen = false
                        Toast.makeText(
                            context,
                            e.message ?: "No se pudo abrir el candado",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                )
            }
        },
        topInfo = {
            // Encabezado de reserva activa
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ElapsedTimeView(sessionManager)
            }
        },
        bottomActions = {
            // Botón principal para finalizar
            Button(
                onClick = {
                    scope.launch {
                        val ok = bookingViewModel.endBooking(context)  // <-- debe limpiar activeBooking en DataStore
                        if (ok) {
                            onRelease()
                        } else {
                            Toast.makeText(context, "No se pudo finalizar la reserva", Toast.LENGTH_LONG).show()
                        }
                    }
                    scope.launch {
                        // 1) Obtener bookingId con nulos controlados (como ya hiciste)
                        val booking = sessionManager.getActiveBooking().first()
                        val bookingId = booking?._id
                        if (bookingId.isNullOrBlank()) {
                            Toast.makeText(context, "No hay reserva activa", Toast.LENGTH_SHORT).show()
                            return@launch
                        }

                        // 2) Traer los passcodes del backend
                        val listResult = passcodeVm.getBookingPasscodes(
                            context = context,
                            bookingId = bookingId,
                            includeDeleted = false
                        )
                        val passcodes = listResult.getOrElse {
                            Toast.makeText(context, it.message ?: "Error obteniendo passcodes", Toast.LENGTH_LONG).show()
                            return@launch
                        }
                        if (passcodes.isEmpty()) {
                            Toast.makeText(context, "No hay códigos para esta reserva", Toast.LENGTH_SHORT).show()
                            return@launch
                        }

                        // 3) Borrar uno a uno con TTLock
                        //    Puedes usar tu TTLockManager propio si ya tiene delete; si no, usa deletePasscodeSuspendTTLock
                        var lastMsg = ""
                        val result = removeBookingPasscodesSequential(
                            deleteOne = { code ->
                                // Si tienes un wrapper propio:
                                // ttLockManager.deletePasscodeSuspend(code, lockData, lockMac)

                                // Con el helper genérico:
                                TTLockManager(context).deletePasscodeSuspend(
                                    passcode = code,
                                    lockDataJson = lockData,
                                    lockMac = lockMac
                                )
                            },
                            passcodes = passcodes.map { BookingPasscodeDto(
                                it._id,
                                it.code.toString(), it.name, it.type) },
                            onProgress = { done, total, ok, code ->
                                lastMsg = if (ok) "Eliminado $done/$total (PIN $code)" else "Fallo en PIN $code"
                            },
                            onBackendMarkDeleted = { passcodeId ->
                                // PATCH /passcodes/:id { status: 'deleted' } (si lo implementaste)
                                //runCatching { api.markPasscodeDeleted(passcodeId) }
                                Log.d("Se ha eliminado el pass:", passcodeId)
                            }
                        )

                        if (result.isSuccess) {
                            Toast.makeText(context, "Passcodes eliminados correctamente", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, result.exceptionOrNull()?.message ?: "Error eliminando passcodes", Toast.LENGTH_LONG).show()
                            // Opcional: mostrar botón "Reintentar" o "Resetear teclado" como fallback
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cerrar y liberar taquilla")
            }
            Spacer(Modifier.height(12.dp))

            // Tarjeta de información útil durante la reserva
            /*OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
            ) {
                ListItem(
                    headlineContent = { Text("Consejos durante tu reserva") },
                    supportingContent = {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("• Asegúrate de cerrar bien antes de salir.")
                            Text("• Puedes crear tarjetas RF y PIN durante la reserva.")
                        }
                    }
                )
            }*/
            Spacer(Modifier.height(8.dp))

            // Acciones rápidas del cliente: Contraseñas y Tarjetas RF
            ClientActionsGrid(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                onPasswords = onOpenPasscodes,
                onRF = onOpenRF
            )

            Spacer(Modifier.height(12.dp))
        },
        isAdmin = false
    )
}