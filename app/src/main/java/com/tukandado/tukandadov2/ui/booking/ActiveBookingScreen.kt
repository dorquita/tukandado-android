package com.tukandado.tukandadov2.ui.booking

import android.annotation.SuppressLint
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tukandado.tukandadov2.R
import com.tukandado.tukandadov2.ttlock.TTLockManager
import com.tukandado.tukandadov2.viewmodel.BookingViewModel
import kotlinx.coroutines.delay

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


    // Timer
    var elapsedTime by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            Log.d("DEBUG RESERVA: Entro en la pagina de reserva","Si entro")
            delay(1000L)
            elapsedTime++
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Taquilla $lockAlias", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = String.format("%02d:%02d:%02d", elapsedTime / 3600, (elapsedTime % 3600) / 60, elapsedTime % 60),
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        Image(
            painter = painterResource(id = R.drawable.tukandado_open),
            contentDescription = "Tucán",
            modifier = Modifier.size(120.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))
        Text("Pulsa para abrir")

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = {
            try {
                ttLockManager.controlLock(
                    lockDataJson = lockData,
                    lockMac = lockMac,
                    isOpen = true
                )
            } catch (e: Exception) {
                Toast.makeText(context, "Error al abrir: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }) {
            Text("Taquilla abierta")
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                bookingViewModel.endBooking(context)
                onRelease()
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("Cerrar y liberar taquilla")
        }
    }
}
