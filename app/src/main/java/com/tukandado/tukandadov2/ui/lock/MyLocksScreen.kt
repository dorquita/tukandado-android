package com.tukandado.tukandadov2.ui

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.tukandado.tukandadov2.ttlock.TTLockManager

data class UserLock(
    val name: String,
    val mac: String,
    val lockData: String
)

@Composable
fun MyLocksScreen(
    locks: List<UserLock>
) {
    val context = LocalContext.current
    val ttLockManager = remember { TTLockManager(context) }

    var isLoading by remember { mutableStateOf(false) }
    var selectedLock by remember { mutableStateOf<UserLock?>(null) }
    var resultMessage by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Tus taquillas", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn {
            items(locks) { lock ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable {
                            isLoading = true
                            selectedLock = lock

                            ttLockManager.controlLock(
                                lockDataJson = lock.lockData,
                                lockMac = lock.mac,
                                isOpen = true
                            )

                            // El resultado real se ve en Logcat, puedes extenderlo con callbacks si quieres
                            resultMessage = "Intentando abrir ${lock.name}..."
                            isLoading = false
                        }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(lock.name, style = MaterialTheme.typography.bodyLarge)
                        Text(lock.mac, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        resultMessage?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium)
        }

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
}