package com.tukandado.tukandadov2.ui.components.layout

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import com.tukandado.tukandadov2.ttlock.TTLockManager
import com.tukandado.tukandadov2.ui.components.AdminActionsGrid
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.material3.TextButton


@Composable
fun AdminLockControlScreen(
    lockName: String,
    lockAlias: String,
    lockData: String,
    lockMac: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val ttLockManager = remember { TTLockManager(context) }
    val scope = rememberCoroutineScope()

    var isOpening by remember { mutableStateOf(false) }
    var isLockerOpen by remember { mutableStateOf(false) }

    LockActionLayout(
        lockName = lockName,
        lockAlias = lockAlias,
        isOpening = isOpening,
        isLockerOpen = isLockerOpen,
        onOpen = {
            isOpening = true
            try {
                ttLockManager.controlLock(lockData, lockMac, true)
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
            scope.launch {
                delay(2000L)
                isOpening = false
                isLockerOpen = true
                delay(3000L)
                isLockerOpen = false
            }
        },
        topInfo = {

        },
        bottomActions = {
            AdminActionsGrid(
                onEKeys = { /* TODO: nav a pantalla eKeys */ },
                onPasswords = { /* TODO: nav a passcodes */ },
                onRF = { /* TODO: nav a tarjetas RF */ },
                onLogs = { /* TODO: nav a registros */ },
                onConfig = { /* TODO: nav a configuración avanzada */ }
            )
            Spacer(Modifier.height(12.dp))
        }
    )
}