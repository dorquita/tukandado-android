package com.tukandado.tukandadov2.ui.lock

import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.tukandado.tukandadov2.viewmodel.LockViewModel
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.Alignment
import com.tukandado.tukandadov2.api.LockResponse
import com.tukandado.tukandadov2.viewmodel.BookingViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OpenLockScreen(navController: NavController) {
    val context = LocalContext.current
    val lockViewModel: LockViewModel = viewModel()
    val bookingViewModel: BookingViewModel = viewModel()
    val locks by lockViewModel.locks.collectAsState()
    val error by lockViewModel.error.collectAsState()
    //val coroutineScope = rememberCoroutineScope()

    var selectedLockName by remember { mutableStateOf<String?>(null) }
    var selectedLock by remember { mutableStateOf<LockResponse?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        lockViewModel.getAllLocks(context)
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Zona Hombres A", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))

        // Grid de candados
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(locks) { lock ->
                Card(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clickable {
                            selectedLockName = lock.lockAlias
                            selectedLock = lock
                            showDialog = true
                        }
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = lock.lockAlias.takeIf { it.isNotEmpty() } ?: lock.lockName,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(Modifier.height(4.dp))
                            Image(
                                painter = painterResource(
                                    id = if (lock.isInUse) com.tukandado.tukandadov2.R.drawable.led_red else com.tukandado.tukandadov2.R.drawable.led_green
                                ),
                                contentDescription = if (lock.isInUse) "Ocupado" else "Libre"
                            )
                        }
                    }
                }
            }
        }

        if (error != null) {
            Text("Error: $error", color = MaterialTheme.colorScheme.error)
        }
    }

    if (showDialog && selectedLockName != null) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Reserva") },
            text = { Text("¿Quieres iniciar la reserva \"$selectedLockName\"?") },
            confirmButton = {
                TextButton(onClick = {
                    showDialog = false
                    Log.d("DEBUG RESERVA: Lock seleccionado es??", selectedLock.toString())

                    Log.d("DEBUG RESERVA: Lock seleccionado es??", selectedLockName + "")
                    Log.d("DEBUG RESERVA: LockCompleto seleccionado es??", selectedLock.toString())

                    val encodedName = Uri.encode(selectedLock?.lockName)
                    val encodedAlias = Uri.encode(selectedLock?.lockAlias)
                    val encodedData = Uri.encode(selectedLock?.lockData)
                    val encodedMac = Uri.encode(selectedLock?.lockMac)

                    selectedLock?.let {
                        Log.d("","")
                        bookingViewModel.startBooking(context, selectedLock?._id!!)
                        navController.navigate(
                            "activeReservation/${encodedName}/${encodedAlias}/${encodedData}/${encodedMac}"
                        )
                    }
                }) {
                    Text("Sí")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}