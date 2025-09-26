package com.tukandado.tukandadov2.ui.admin

import CompactChip
import android.annotation.SuppressLint
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.tukandado.tukandadov2.api.ActiveBooking
import com.tukandado.tukandadov2.api.CreatePasscodeRequest
import com.tukandado.tukandadov2.api.PasscodeDto
import com.tukandado.tukandadov2.data.SessionManager
import com.tukandado.tukandadov2.ui.components.StatusChip
import com.tukandado.tukandadov2.ui.components.StatusChoiceChip
import com.tukandado.tukandadov2.ui.components.TypeChip
import com.tukandado.tukandadov2.ui.components.UsageChip
import com.tukandado.tukandadov2.ui.components.ValidityLine
import com.tukandado.tukandadov2.ui.components.layout.AdminListScaffold
import com.tukandado.tukandadov2.viewmodel.PasscodeViewModel
import kotlinx.coroutines.launch

@SuppressLint("UnrememberedMutableState")
@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasscodeListScreen(
    lockId: String,
    lockData: String,
    lockMac: String,
    navController: NavController,
    setHeaderAction: (label: String?, enabled: Boolean, onClick: (() -> Unit)?) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val vm: PasscodeViewModel = viewModel()

    val passcodes by vm.passcodes.collectAsState()
    val error by vm.error.collectAsState()
    val loading by vm.loading.collectAsState()
    val page by vm.page.collectAsState()
    val pages by vm.pages.collectAsState()
    val total by vm.total.collectAsState()

    val sessionManager = remember { SessionManager(context) }

    val clubId: String? by sessionManager
        .getClub()
        .collectAsState(initial = null)

    var q by remember { mutableStateOf("") }
    var showOnlyActive by remember { mutableStateOf(false) }
    var statusFilter by remember { mutableStateOf<String?>(null) } // "active","expired","revoked","failed",null

    // Reglas de habilitación del guardado

    // Observa el estado global para deshabilitar mientras carga
    val state by vm.state.collectAsState()

    // Habilitado si tenemos lockData/MAC y no está cargando
    val canReset by remember(lockData, lockMac, state.isLoading) {
        derivedStateOf {
            lockData.isNotBlank() &&
                    lockMac.isNotBlank() &&
                    !state.isLoading
        }
    }

    val scope = rememberCoroutineScope()

// Acción de reset global (sin code, sin fechas)
    val onReset: () -> Unit = onReset@{
        if (!canReset) return@onReset
        scope.launch {
            val result = vm.resetPasscodes(
                context = context,
                lockDataJson = lockData,
                lockIdForBackend = lockId,
                lockMac = lockMac
            )
            if (result.isSuccess) {
                Toast.makeText(context, "Passcodes reiniciados correctamente", Toast.LENGTH_SHORT).show()
                // Si quieres recargar o navegar, hazlo aquí.
                navController.popBackStack()
            } else {
                val msg = result.exceptionOrNull()?.message ?: "Error reiniciando passcodes"
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            }
        }
    }

    // Coloca/actualiza la acción del header
    LaunchedEffect(canReset, onReset) {
        setHeaderAction("Reiniciar", canReset, onReset)
    }

    // 🚀 Llamada inicial: replica exactamente la que te funciona
    LaunchedEffect(lockId) {
        Log.d("PasscodeListScreen", "➡️ listPasscodes init lockId=$lockId")

        val activeOnlyParam = if (statusFilter == "active") true else null

        vm.listPasscodes(
            context = context,
            page = 1,
            limit = 20,
            lockId = lockId,
            clubId = clubId,
            activeOnly = activeOnlyParam, // 👈 importante: no fuerces false
            status = null,
            q = null
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    val dataEnc = Uri.encode(lockData)
                    val macEnc  = Uri.encode(lockMac)
                    navController.navigate("passcodes/create/$lockId/$dataEnc/$macEnc")
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "Crear")
            }
        }
    ) { padding ->

        Column(Modifier.fillMaxSize().padding(padding)) {

            // 🔎 Barra de filtros
            FilterBar(
                q = q,
                onQuery = {
                    q = it
                    vm.listPasscodes(
                        context,
                        page = 1,
                        limit = 20,
                        lockId = lockId,
                        clubId = clubId,
                        q = q.ifBlank { null },
                        activeOnly = if (showOnlyActive) true else null, // si es false, mejor omitir
                        status = statusFilter
                    )
                },
                showOnlyActive = showOnlyActive,
                onToggleActive = {
                    showOnlyActive = it
                    vm.listPasscodes(
                        context,
                        page = 1,
                        limit = 20,
                        lockId = lockId,
                        clubId = clubId,
                        q = q.ifBlank { null },
                        activeOnly = if (showOnlyActive) true else null,
                        status = statusFilter
                    )
                },
                statusFilter = statusFilter,
                onStatusChange = {
                    statusFilter = it
                    vm.listPasscodes(
                        context,
                        page = 1,
                        limit = 20,
                        lockId = lockId,
                        clubId = clubId,
                        q = q.ifBlank { null },
                        activeOnly = if (showOnlyActive) true else null,
                        status = statusFilter
                    )
                }
            )

            if (loading) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }

            AdminListScaffold(
                isEmpty = !loading && passcodes.isEmpty(), // 👈 evita "vacío" durante carga
                actionIcon = Icons.Outlined.Add,
                actionText = "Crear passcode",
                onAction = { navController.navigate("passcodes/create/$lockId") },
                modifier = Modifier.weight(1f),
                emptyIcon = Icons.Outlined.Inbox,
                emptyTitle = "Sin passcodes",
                emptySubtitle = "Crea el primero para compartir acceso."
            ) {
                if (!error.isNullOrBlank()) {
                    Text(
                        text = error ?: "",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(12.dp)
                    )
                    Divider()
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                    contentPadding = PaddingValues(bottom = 96.dp)
                ) {
                    items(
                        items = passcodes,
                        key = { it._id ?: it.hashCode() } // 👈 clave estable
                    ) { pc ->
                        val lockDataEncoded = Uri.encode(lockData)
                        val lockMacEncoded = Uri.encode(lockMac)

                        // 🔎 Logs de depuración
                        Log.d("PasscodesList", "Rendering PasscodeRow for id=${pc._id}")
                        Log.d("PasscodesList", "Raw lockData=$lockData")
                        Log.d("PasscodesList", "Raw lockMac=$lockMac")
                        Log.d("PasscodesList", "Encoded lockData=$lockDataEncoded")
                        Log.d("PasscodesList", "Encoded lockMac=$lockMacEncoded")

                        PasscodeRow(
                            pc = pc,
                            onClick = {
                                val route = "passcodes/${pc._id}/detail/$lockId/$lockDataEncoded/$lockMacEncoded/"
                                Log.d("PasscodesList", "➡️ Navigating to $route")
                                try {
                                    navController.navigate(route)
                                    Log.d("PasscodesList", "✅ Navigation success")
                                } catch (e: Exception) {
                                    Log.e("PasscodesList", "💥 Navigation failed", e)
                                }
                            }
                        )
                        Divider()
                    }

                    item {
                        if (pages > 1) {
                            Row(
                                Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    enabled = page > 1,
                                    onClick = {
                                        vm.prevPage(
                                            context,
                                            lockId = lockId,
                                            q = q.ifBlank { null },
                                            activeOnly = if (showOnlyActive) true else null,
                                            status = statusFilter
                                        )
                                    }
                                ) { Text("Anterior") }

                                Text("Página $page / $pages  ·  $total en total")

                                TextButton(
                                    enabled = page < pages,
                                    onClick = {
                                        vm.nextPage(
                                            context,
                                            lockId = lockId,
                                            q = q.ifBlank { null },
                                            activeOnly = if (showOnlyActive) true else null,
                                            status = statusFilter
                                        )
                                    }
                                ) { Text("Siguiente") }
                            }
                        }
                    }
                }
            }

            // 🔭 Logs útiles
            LaunchedEffect(passcodes) {
                Log.d("PasscodeListScreen", "📦 passcodes size = ${passcodes.size}")
            }
            LaunchedEffect(error) {
                if (!error.isNullOrBlank()) Log.e("PasscodeListScreen", "❌ error = $error")
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun PasscodeRow(pc: PasscodeDto, onClick: () -> Unit) {
    ListItem(
        headlineContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    pc.name ?: "Código sin nombre",
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.width(8.dp))
                // Si StatusChip es estrecho, protégelo con CompactChip
                CompactChip { StatusChip(pc.status) }
            }
        },
        supportingContent = {
            Column {
                // Chips en fila con scroll para que no salten de línea
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    CompactChip { TypeChip(pc.type) }
                    Spacer(Modifier.width(8.dp))
                    CompactChip { UsageChip(pc.usage?.count ?: 0) }
                }
                Spacer(Modifier.height(4.dp))
                ValidityLine(validFrom = pc.validFrom, validTo = pc.validTo)
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    )
}

@Composable
private fun FilterBar(
    q: String,
    onQuery: (String) -> Unit,
    showOnlyActive: Boolean,
    onToggleActive: (Boolean) -> Unit,
    statusFilter: String?,
    onStatusChange: (String?) -> Unit
) {
    val fontScale = LocalConfiguration.current.fontScale
    val compact = fontScale >= 1.3f

    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        OutlinedTextField(
            value = q,
            onValueChange = onQuery,
            label = { Text("Buscar por nombre", maxLines = 1) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)            // alto estable
                .padding(horizontal = 12.dp, vertical = 12.dp)
        )

        // Fila de chips con scroll horizontal (no se apilan mal)
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val labelTodos     = "Todos"
            val labelActivos   = "Activos"
            val labelExpirados = if (compact) "Expir." else "Expirados"
            val labelRevocados = if (compact) "Revoc." else "Revocados"

            Spacer(Modifier.width(0.dp)) // truco: primer chip pegado al borde

            CompactChip {
                FilterChip(
                    selected = statusFilter == null,
                    onClick = { onStatusChange(null) },
                    label = { Text(labelTodos, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) }
                )
            }
            Spacer(Modifier.width(8.dp))

            CompactChip {
                FilterChip(
                    selected = statusFilter == "active",
                    onClick = { onStatusChange("active") },
                    label = { Text(labelActivos, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) }
                )
            }
            Spacer(Modifier.width(8.dp))

            CompactChip {
                FilterChip(
                    selected = statusFilter == "expired",
                    onClick = { onStatusChange("expired") },
                    label = { Text(labelExpirados, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) }
                )
            }
            Spacer(Modifier.width(8.dp))

            CompactChip {
                FilterChip(
                    selected = statusFilter == "revoked",
                    onClick = { onStatusChange("revoked") },
                    label = { Text(labelRevocados, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) }
                )
            }
        }

        Divider()
    }
}
