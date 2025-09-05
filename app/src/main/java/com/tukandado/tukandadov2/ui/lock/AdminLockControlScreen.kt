package com.tukandado.tukandadov2.ui.components.layout

import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.tukandado.tukandadov2.ttlock.TTLockManager
import com.tukandado.tukandadov2.ui.components.AdminActionsGrid
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@Composable
fun AdminLockControlScreen(
    lockName: String,
    lockAlias: String,
    lockData: String,
    lockMac: String,
    lockId: String,
    onBack: () -> Unit,
    navController: NavController
) {
    val context = LocalContext.current
    val ttLockManager = remember { TTLockManager(context) }
    val scope = rememberCoroutineScope()

    var isOpening by remember { mutableStateOf(false) }
    var isLockerOpen by remember { mutableStateOf(false) }

    // Modal de feedback
    var dialogMsg by remember { mutableStateOf<String?>(null) }
    var dialogIsError by remember { mutableStateOf(false) }

    // Dialog
    if (dialogMsg != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { dialogMsg = null },
            title = { androidx.compose.material3.Text(if (dialogIsError) "Error" else "Éxito") },
            text = { androidx.compose.material3.Text(dialogMsg!!) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { dialogMsg = null }) {
                    androidx.compose.material3.Text("OK")
                }
            }
        )
    }

    LockActionLayout(
        lockName = lockName,
        lockAlias = lockAlias,
        isOpening = isOpening,
        isLockerOpen = isLockerOpen,
        onOpen = {
            scope.launch {
                isOpening = true
                isLockerOpen = false

                val result = ttLockManager.controlLock(
                    lockDataJson = lockData,
                    lockMac = lockMac,
                    isOpen = true
                )
                isOpening = false

                result.fold(
                    onSuccess = {
                        isLockerOpen = true
                        dialogIsError = false
                        dialogMsg = "Candado abierto correctamente"
                        // Oculta el icono de “abierto” tras un rato
                        launch {
                            kotlinx.coroutines.delay(2500)
                            isLockerOpen = false
                        }
                    },
                    onFailure = { e ->
                        isLockerOpen = false
                        dialogIsError = true
                        dialogMsg = e.message ?: "No se pudo abrir el candado"
                    }
                )
            }
        },
        topInfo = { /* opcional */ },
        bottomActions = {
            AdminActionsGrid(
                onEKeys = { navController.navigate("ekeys/$lockId") },
                onPasswords = {
                    val dataEnc = Uri.encode(lockData)
                    val macEnc = Uri.encode(lockMac)
                    navController.navigate("passcodes/$lockId/$dataEnc/$macEnc")
                },
                onRF = { navController.navigate("rfid") },
                onLogs = { navController.navigate("registers") },
                onConfig = { navController.navigate("configuration") }
            )
            Spacer(Modifier.height(12.dp))
        },
        isAdmin = true
    )
}