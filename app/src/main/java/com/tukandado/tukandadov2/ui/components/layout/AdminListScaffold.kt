package com.tukandado.tukandadov2.ui.components.layout

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Layout común para pantallas de admin tipo listado + botón de acción inferior.
 *
 * Uso:
 * AdminListScaffold(
 *   isEmpty = items.isEmpty(),
 *   actionIcon = Icons.Outlined.Add,
 *   actionText = "Enviar llave digital",
 *   onAction = { ... },
 *   emptyTitle = "Sin llaves",
 *   emptySubtitle = "Aún no has enviado ninguna llave digital."
 * ) {
 *   // Tu LazyColumn / contenido de lista
 * }
 */
@Composable
fun AdminListScaffold(
    isEmpty: Boolean,
    actionIcon: ImageVector,
    actionText: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    emptyIcon: ImageVector = Icons.Outlined.Inbox,
    emptyTitle: String = "Sin datos",
    emptySubtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Scaffold(
        modifier = modifier,
        /*bottomBar = {
            Surface(tonalElevation = 2.dp) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = onAction,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(actionIcon, contentDescription = actionText)
                        Spacer(Modifier.width(8.dp))
                        Text(actionText)
                    }
                }
            }
        }*/
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            if (isEmpty) {
                EmptyState(
                    icon = emptyIcon,
                    title = emptyTitle,
                    subtitle = emptySubtitle,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 80.dp) // deja aire sobre el botón inferior
                )
            } else {
                content()
                Spacer(Modifier.height(80.dp)) // evita solape con el botón en listas cortas
            }
        }
    }
}

@Composable
private fun EmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(56.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (!subtitle.isNullOrBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}