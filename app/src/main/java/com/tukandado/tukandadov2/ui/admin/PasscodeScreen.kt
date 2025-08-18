package com.tukandado.tukandadov2.ui.admin

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.tukandado.tukandadov2.api.PasscodeDto
import com.tukandado.tukandadov2.ui.components.layout.AdminListScaffold
import com.tukandado.tukandadov2.viewmodel.LoginViewModel
import com.tukandado.tukandadov2.viewmodel.PasscodeViewModel

@Composable
fun PasscodeScreen(
    lockId: String,
    onBack: () -> Unit,
    navController: NavController
) {
    Log.d("PasscodeScreen", "🧭 Composable creado con lockId=$lockId")

    val context = LocalContext.current
    val vm: PasscodeViewModel = viewModel()

    val passcodes by vm.passcodes.collectAsState()
    val error by vm.error.collectAsState()
    val loading by vm.loading.collectAsState()
    val page by vm.page.collectAsState()
    val pages by vm.pages.collectAsState()
    val total by vm.total.collectAsState()

    LaunchedEffect(lockId) {
        Log.d("PasscodeScreen", "🚀 LaunchedEffect -> listPasscodes(lockId=$lockId)")
        vm.listPasscodes(
            context = context,
            page = 1,
            limit = 20,
            lockId = lockId,
            clubId = "67ceeb75a15ec32e57052c1f",
            activeOnly = null // pon true si quieres solo activos
        )
    }

    AdminListScaffold(
        isEmpty = passcodes.isEmpty(),
        actionIcon = Icons.Outlined.Add,
        actionText = "Crear passcode",
        onAction = {
            Log.d("PasscodeScreen", "➕ Click en acción (Crear passcode)")
            // navController.navigate("passcode/create/$lockId")
        },
        modifier = Modifier.fillMaxSize(),
        emptyIcon = Icons.Outlined.Inbox,
        emptyTitle = "Sin passcodes",
        emptySubtitle = "Todavía no has creado ningún passcode."
    ) {
        if (loading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
        }

        if (!error.isNullOrBlank()) {
            Text(
                text = error ?: "",
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.error
            )
            Divider()
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(0.dp),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            items(passcodes) { pc ->
                PasscodeListItem(pc)
                Divider()
            }

            // Control simple de paginación (opcional)
            item {
                if (pages > 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(
                            enabled = page > 1,
                            onClick = { vm.prevPage(context, lockId = lockId) }
                        ) { Text("Anterior") }

                        Text("Página $page de $pages  ·  Total $total")

                        TextButton(
                            enabled = page < pages,
                            onClick = { vm.nextPage(context, lockId = lockId) }
                        ) { Text("Siguiente") }
                    }
                }
            }
        }

        // Logs útiles
        LaunchedEffect(passcodes) {
            Log.d("PasscodeScreen", "📦 passcodes size = ${passcodes.size}")
        }
        LaunchedEffect(error) {
            if (!error.isNullOrBlank()) Log.e("PasscodeScreen", "❌ error = $error")
        }
    }
}

@Composable
private fun PasscodeListItem(pc: PasscodeDto) {
    val title = pc.name ?: "Código sin nombre"
    val subtitle = buildString {
        append(pc.type)
        append(" · ")
        append(pc.status)
        val vf = pc.validFrom
        val vt = pc.validTo
        if (vf != null || vt != null) {
            append(" · Vigencia: ")
            append(vf ?: "—")
            append(" → ")
            append(vt ?: "—")
        }
        val uses = pc.usage?.count ?: 0
        append(" · Usos: $uses")
    }

    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) }
    )
}