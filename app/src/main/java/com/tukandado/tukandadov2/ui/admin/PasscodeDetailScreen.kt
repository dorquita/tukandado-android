package com.tukandado.tukandadov2.ui.admin

import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.tukandado.tukandadov2.api.PasscodeDto
import com.tukandado.tukandadov2.ui.components.InfoRow
import com.tukandado.tukandadov2.ui.components.StatusChip
import com.tukandado.tukandadov2.ui.components.TypeChip
import com.tukandado.tukandadov2.ui.components.UsageChip
import com.tukandado.tukandadov2.ui.components.ValidityLine
import com.tukandado.tukandadov2.viewmodel.PasscodeUiModal
import com.tukandado.tukandadov2.viewmodel.PasscodeViewModel
import android.widget.Toast
import kotlinx.coroutines.launch



@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasscodeDetailScreen(
    passcodeId: String,
    navController: NavController,
    onBack: () -> Unit,
    lockId: String,
    lockDataJson: String? = null,   // opcional: pásalo si lo tienes
    lockMac: String? = null         // opcional: pásalo si lo tienes
) {
    val scope = rememberCoroutineScope()
    val vm: PasscodeViewModel = viewModel()
    val context = LocalContext.current

    // estado VM (para modales y loading)
    val state by vm.state.collectAsState()
    val selected by vm.selectedPasscode.collectAsState()

    // confirmaciones locales
    var confirmRevoke by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    // carga inicial del detalle
    LaunchedEffect(passcodeId) { vm.getPasscodeById(context, passcodeId) }

    Scaffold { padding ->
        val doc = selected
        if (doc == null) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.Center
            ) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
            return@Scaffold
        }

        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row {
                Text(
                    doc.name ?: "Código sin nombre",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(8.dp))
                StatusChip(doc.status)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TypeChip(doc.type)
                UsageChip(doc.usage?.count ?: 0)
            }

            SectionCard("Vigencia") {
                ValidityLine(doc.validFrom, doc.validTo)
            }

            SectionCard("Información") {
                InfoRow("Tipo", doc.type)
                InfoRow("Estado", doc.status)
                InfoRow("Usos", (doc.usage?.count ?: 0).toString())
                InfoRow("Último uso", doc.usage?.lastUsedAt ?: "—")
                InfoRow("KeyboardPwdId", doc.external?.keyboardPwdId?.toString() ?: "—")
                InfoRow("Lock externo", doc.external?.lockId?.toString() ?: "—")
                InfoRow("Creado", doc.createdAt ?: "—")
                InfoRow("Actualizado", doc.updatedAt ?: "—")
            }

            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Ver registros → navega a una pantalla de solo lectura
                OutlinedButton(
                    onClick = {
                        val lockIdExt = doc.external?.lockId?.toString()
                        if (lockIdExt != null) {
                            // ajusta la ruta a tu grafo
                            navController.navigate("lockLogs/$lockIdExt?passcodeId=${doc._id}")
                        }
                    },
                    //enabled = doc.external?.lockId != null
                    enabled = false
                ) { Text("Ver registros") }

                OutlinedButton(
                    onClick = { confirmRevoke = true },
                    enabled = !state.isLoading && (selected?.status != "revoked"),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Revocar") }

                TextButton(
                    onClick = { confirmDelete = true },
                    enabled = !state.isLoading,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Eliminar") }
            }
        }
    }

    // modal de confirmación: REVOCAR
    if (confirmRevoke) {
        AlertDialog(
            onDismissRequest = { confirmRevoke = false },
            title = { Text("Revocar contraseña") },
            text = { Text("Esta contraseña dejará de funcionar inmediatamente. ¿Quieres continuar?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmRevoke = false
                        val doc = vm.selectedPasscode.value ?: return@TextButton
                        scope.launch {
                            val result = vm.revokePasscode(
                                context = context,
                                passcodeId = doc._id,
                                originalCode = null,     // pásalo si lo tienes (BLE más fiable)
                                lockDataJson = lockDataJson,
                                lockMac = lockMac
                            )
                            if (result.isSuccess) {
                                Toast.makeText(context, "Contraseña revocada", Toast.LENGTH_SHORT).show()
                                // opcional: navController.popBackStack()
                            } else {
                                val msg = result.exceptionOrNull()?.message ?: "Error revocando la contraseña"
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                ) { Text("Revocar") }
            },
            dismissButton = {
                TextButton(onClick = { confirmRevoke = false }) { Text("Cancelar") }
            }
        )
    }

    // modal de confirmación: ELIMINAR (hace revoke antes de borrar en backend)
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Eliminar contraseña") },
            text = { Text("Se revocará y eliminará el registro de esta contraseña. ¿Seguro que quieres continuar?") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    val doc = vm.selectedPasscode.value ?: return@TextButton
                    scope.launch {
                        val result = vm.deletePasscode(
                            context = context,
                            passcodeId = doc._id,
                            originalCode = null,        // si lo tienes, pásalo (mejor para BLE)
                            lockDataJson = lockDataJson,
                            lockMac = lockMac
                        )

                        if (result.isSuccess) {
                            Log.d("DeleteFlow", "✅ deletePasscode devolvió success")

                            // debug de valores
                            Log.d("DeleteFlow", "lockDataJson=$lockDataJson")
                            Log.d("DeleteFlow", "lockMac=$lockMac")

                            val lockDataEncoded = Uri.encode(lockDataJson)
                            val lockMacEncoded = Uri.encode(lockMac)

                            Log.d("DeleteFlow", "Encoded lockData=$lockDataEncoded")
                            Log.d("DeleteFlow", "Encoded lockMac=$lockMacEncoded")

                            Toast.makeText(context, "Contraseña eliminada", Toast.LENGTH_SHORT).show()

                            Log.d("DeleteFlow", "Antes de navegar a passcodes/$lockId/$lockDataEncoded/$lockMacEncoded")

                            try {
                                navController.navigate("passcodes/$lockId/$lockDataEncoded/$lockMacEncoded") {
                                    launchSingleTop = true
                                    popUpTo("passcodes/$lockId/$lockDataEncoded/$lockMacEncoded") { inclusive = true }
                                }
                                Log.d("DeleteFlow", "🚀 Navegación ejecutada correctamente")
                            } catch (e: Exception) {
                                Log.e("DeleteFlow", "💥 Error en navigate", e)
                            }
                        } else {
                            val msg = result.exceptionOrNull()?.message ?: "Error eliminando contraseña"
                            Log.e("DeleteFlow", "❌ deletePasscode falló: $msg", result.exceptionOrNull())
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }

                    }
                }) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancelar") }
            }
        )
    }

    // modales globales del VM (éxito / error)
    when (val modal = state.modal) {
        is PasscodeUiModal.Success -> {
            AlertDialog(
                onDismissRequest = { vm.dismissModal() },
                title = { Text("Éxito") },
                text = { Text(modal.message) },
                confirmButton = { TextButton(onClick = { vm.dismissModal() }) { Text("Aceptar") } }
            )
        }
        is PasscodeUiModal.Error -> {
            AlertDialog(
                onDismissRequest = { vm.dismissModal() },
                title = { Text("Error") },
                text = { Text(modal.message) },
                confirmButton = { TextButton(onClick = { vm.dismissModal() }) { Text("Cerrar") } }
            )
        }
        PasscodeUiModal.None -> Unit
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}