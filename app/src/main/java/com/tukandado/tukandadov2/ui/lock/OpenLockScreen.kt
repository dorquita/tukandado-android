package com.tukandado.tukandadov2.ui.lock

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.tukandado.tukandadov2.ttlock.TTLockManager
import com.tukandado.tukandadov2.viewmodel.LockViewModel
import kotlinx.coroutines.launch

@Composable
fun OpenLockScreen(navController: NavController) {
    val context = LocalContext.current
    val lockViewModel: LockViewModel = viewModel()
    val locks by lockViewModel.locks.collectAsState()
    val error by lockViewModel.error.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val ttLockManager = remember { TTLockManager(context) }

    var selectedLockName by remember { mutableStateOf<String?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    // Cargar candados al inicio
    LaunchedEffect(Unit) {
        lockViewModel.getAllLocks(context)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            Text("Candados disponibles", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(16.dp))
        }

        items(locks) { lock ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable {
                        selectedLockName = lock.lockName
                        showDialog = true
                    }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Nombre: ${lock.lockName}")
                    Text("Alias: ${lock.lockAlias}")
                    Text("En uso: ${if (lock.isInUse) "Sí" else "No"}")
                }
            }
        }

        if (error != null) {
            item {
                Text("Error: $error", color = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (showDialog && selectedLockName != null) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("¿Abrir candado?") },
            text = { Text("¿Quieres abrir el candado \"$selectedLockName\"?") },
            confirmButton = {
                TextButton(onClick = {
                    showDialog = false
                    Toast.makeText(context, "🔓 Abriendo candado...", Toast.LENGTH_SHORT).show()

                    coroutineScope.launch {
                        val selectedLock = locks.firstOrNull { it.lockName == selectedLockName }

                        if (selectedLock != null) {
                            try {
                                ttLockManager.controlLock(
                                    lockDataJson = selectedLock.lockData,
                                    lockMac = selectedLock.lockMac,
                                    isOpen = true
                                )
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error al abrir el candado: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            Toast.makeText(context, "No se encontró el candado en la lista", Toast.LENGTH_SHORT).show()
                        }
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