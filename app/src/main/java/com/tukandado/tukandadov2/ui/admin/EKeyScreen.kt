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
import com.tukandado.tukandadov2.ui.components.layout.AdminListScaffold
import com.tukandado.tukandadov2.viewmodel.EkeyViewModel

@Composable
fun EKeyScreen(
    lockId: String,
    onBack: () -> Unit,
    navController: NavController
) {
    Log.d("EKeyScreen", "🧭 Composable creado con lockId=$lockId")

    val context = LocalContext.current
    val ekeyViewModel: EkeyViewModel = viewModel()

    val ekeys by ekeyViewModel.ekeys.collectAsState()
    val error by ekeyViewModel.error.collectAsState()

    LaunchedEffect(lockId) {
        Log.d("EKeyScreen", "🚀 LaunchedEffect -> getLockEkeys($lockId)")
        ekeyViewModel.getLockEkeys(context, lockId)
    }

    AdminListScaffold(
        isEmpty = ekeys.isEmpty(),
        actionIcon = Icons.Outlined.Add,
        actionText = "Enviar llave digital",
        onAction = {
            Log.d("EKeyScreen", "➕ Click en acción inferior (Enviar llave digital)")
            // navController.navigate("ekey/create/$lockId")
        },
        modifier = Modifier.fillMaxSize(),
        emptyIcon = Icons.Outlined.Inbox,
        emptyTitle = "Sin llaves",
        emptySubtitle = "Aún no has enviado ninguna llave digital."
    ) {
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
            items(ekeys) { ekey ->
                ListItem(
                    headlineContent = { Text(ekey.eKeyName ?: "Llave sin nombre") },
                    supportingContent = { Text(ekey.eKeyName ?: "") }
                )
                Divider()
            }
        }

        // Log de tamaño de lista
        LaunchedEffect(ekeys) {
            Log.d("EKeyScreen", "📦 ekeys size = ${ekeys.size}")
        }
        LaunchedEffect(error) {
            if (!error.isNullOrBlank()) Log.e("EKeyScreen", "❌ error = $error")
        }
    }
}