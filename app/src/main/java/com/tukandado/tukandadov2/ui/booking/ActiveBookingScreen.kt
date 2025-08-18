package com.tukandado.tukandadov2.ui.booking

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tukandado.tukandadov2.ttlock.TTLockManager
import com.tukandado.tukandadov2.viewmodel.BookingViewModel
import com.tukandado.tukandadov2.ui.components.layout.LockActionLayout
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("DefaultLocale")
@Composable
fun ActiveBookingScreen(
    lockName: String,
    lockAlias: String,
    lockData: String,
    lockMac: String,
    onRelease: () -> Unit
) {
    val context = LocalContext.current
    val ttLockManager = remember { TTLockManager(context) }
    val bookingViewModel: BookingViewModel = viewModel()
    val scope = rememberCoroutineScope()

    var elapsedTime by remember { mutableIntStateOf(0) }
    var isOpening by remember { mutableStateOf(false) }
    var isLockerOpen by remember { mutableStateOf(false) }

    // Cronómetro simple
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            elapsedTime++
        }
    }
    val formattedTime = String.format(
        "%02d:%02d:%02d",
        elapsedTime / 3600,
        (elapsedTime % 3600) / 60,
        elapsedTime % 60
    )

    LockActionLayout(
        lockName = lockName,
        lockAlias = lockAlias,
        isOpening = isOpening,
        isLockerOpen = isLockerOpen,
        onOpen = {
            // Llamamos a la función suspend dentro de una coroutine
            scope.launch {
                isOpening = true
                isLockerOpen = false

                val result = try {
                    // Tu TTLockManager.controlLock es suspend y devuelve Result<Unit>
                    ttLockManager.controlLock(
                        lockDataJson = lockData,
                        lockMac = lockMac,
                        isOpen = true
                    )
                } catch (e: Exception) {
                    Result.failure(e)
                }

                isOpening = false

                result.fold(
                    onSuccess = {
                        isLockerOpen = true
                        Toast.makeText(context, "Candado abierto", Toast.LENGTH_SHORT).show()
                        // Mostrar estado "abierto" un rato y apagar
                        launch {
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
            Text(formattedTime, style = MaterialTheme.typography.headlineMedium)
        },
        bottomActions = {
            Button(
                onClick = {
                    bookingViewModel.endBooking(context)
                    onRelease()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Cerrar y liberar taquilla")
            }
            Spacer(Modifier.height(12.dp))
        }
    )
}