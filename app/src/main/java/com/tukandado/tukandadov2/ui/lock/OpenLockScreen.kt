package com.tukandado.tukandadov2.ui.lock

import android.graphics.drawable.shapes.Shape
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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.tv.material3.CardShape
import com.tukandado.tukandadov2.api.LockResponse
import com.tukandado.tukandadov2.data.SessionManager
import com.tukandado.tukandadov2.viewmodel.BookingViewModel
import kotlinx.coroutines.flow.first

private val CardShape: RoundedCornerShape = RoundedCornerShape(16.dp)

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

    val role by SessionManager(context).getRole().collectAsState(initial = "client")
    Log.d("El role es;", role.toString())
    val isAdmin = role == "admin" || role == "superadmin"

    LaunchedEffect(Unit) {
        lockViewModel.getAllLocks(context)
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Spacer(Modifier.height(8.dp))

        // Grid de candados
        /*LazyVerticalGrid(
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
                            if (isAdmin) {
                                val name = Uri.encode(lock.lockName)
                                val alias = Uri.encode(lock.lockAlias)
                                val data = Uri.encode(lock.lockData)
                                val mac  = Uri.encode(lock.lockMac)
                                val lockId  = Uri.encode(lock.lockId.toString())
                                navController.navigate("adminLock/$name/$alias/$data/$mac/$lockId")
                            }
                            else {
                                selectedLockName = lock.lockAlias
                                selectedLock = lock
                                showDialog = true
                            }
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

        */
        LockListScreen(
            locks = locks,
            isAdmin = isAdmin,
            navController = navController,
            onTapNonAdmin = { lock ->
                // abre diálogo para el usuario no admin
            }
        )

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


@Composable
fun LockListScreen(
    locks: List<LockResponse>,
    isAdmin: Boolean,
    navController: NavController?,
    modifier: Modifier = Modifier,
    onTapNonAdmin: (LockResponse) -> Unit = {}
) {
    var search by remember { mutableStateOf("") }

    Column(
        modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Buscador
        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            placeholder = { Text("Buscar candado…") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))

        // Lista
        val filtered = remember(search, locks) {
            if (search.isBlank()) locks
            else locks.filter { l ->
                val q = search.trim().lowercase()
                l.lockAlias.lowercase().contains(q) || l.lockName.lowercase().contains(q)
            }
        }

        if (filtered.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Sin resultados")
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(filtered.size) { index ->
                    val lock = filtered[index]
                    LockRow(
                        lock = lock,
                        onClick = {
                            Log.d("isadmin", isAdmin.toString())
                            if (isAdmin) {
                                navController?.navigate(
                                    "adminLock/" +
                                            Uri.encode(lock.lockName) + "/" +
                                            Uri.encode(lock.lockAlias) + "/" +
                                            Uri.encode(lock.lockData) + "/" +
                                            Uri.encode(lock.lockMac) + "/" +
                                            Uri.encode(lock._id)
                                )
                            } else {
                                navController?.navigate(
                                    "activeReservation/" +
                                            Uri.encode(lock.lockName) + "/" +
                                            Uri.encode(lock.lockAlias) + "/" +
                                            Uri.encode(lock.lockData) + "/" +
                                            Uri.encode(lock.lockMac))
                                //onTapNonAdmin(lock)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun UsagePill(isInUse: Boolean) {
    val bg = if (isInUse) Color(0xFFFDECEC) else Color(0xFFE8F5E9)
    val fg = if (isInUse) Color(0xFFB00020) else Color(0xFF1B5E20)
    val text = if (isInUse) "Ocupado" else "Libre"

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text, color = fg, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun LockRow(lock: LockResponse, onClick: () -> Unit) {
    ElevatedCard(
        shape = CardShape,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 88.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar con inicial
            val title = lock.lockAlias.takeIf { it.isNotEmpty() } ?: lock.lockName
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title.firstOrNull()?.uppercase() ?: "?",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.width(12.dp))

            // Texto
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AssistChip(
                        onClick = {},
                        label = { Text("Remota") },
                        enabled = false
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Permanente/Administración",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Estado uso + batería
            Column(horizontalAlignment = Alignment.End) {
                UsagePill(isInUse = lock.isInUse)
                Spacer(Modifier.height(6.dp))
                BatteryBadge(percent = 80)
            }
        }
    }
}

@Composable
private fun BatteryBadge(percent: Int?) {
    val p = (percent ?: 0).coerceIn(0, 100)

    // ✅ Evitar desestructuración con when
    val icon: ImageVector
    val tint: Color

    when {
        percent == null -> {
            icon = Icons.Default.BatteryStd
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        }
        p <= 10 -> {
            icon = Icons.Default.BatteryStd
            tint = Color(0xFFB00020)
        }
        p <= 30 -> {
            icon = Icons.Default.BatteryChargingFull
            tint = Color(0xFFFF6D00)
        }
        else -> {
            icon = Icons.Default.BatteryFull
            tint = Color(0xFF1B5E20)
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(4.dp))
        Text("$p%", style = MaterialTheme.typography.bodySmall, color = tint)
    }
}