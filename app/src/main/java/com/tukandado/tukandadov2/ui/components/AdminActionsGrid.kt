package com.tukandado.tukandadov2.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AdminActionsGrid(
    onEKeys: () -> Unit,
    onPasswords: () -> Unit,
    onRF: () -> Unit,
    onLogs: () -> Unit,
    onConfig: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 40.dp),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )

        // Fila superior (3 botones)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            //AdminActionButton(Icons.Outlined.VpnKey, "Llaves digitales", onEKeys)
            AdminActionButton(Icons.Outlined.Password, "Contraseñas", onPasswords, enabled = true)
            AdminActionButton(Icons.Outlined.Nfc, "Tarjetas RF", onRF)
        }

        Spacer(Modifier.height(16.dp))

        // Fila inferior (2 botones centrados)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            AdminActionButton(Icons.Outlined.History, "Registros", onLogs)
            AdminActionButton(Icons.Outlined.Settings, "Configuración", onConfig)
        }
    }
}