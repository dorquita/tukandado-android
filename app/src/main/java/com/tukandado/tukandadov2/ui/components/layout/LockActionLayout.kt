package com.tukandado.tukandadov2.ui.components.layout

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.tukandado.tukandadov2.R
import com.tukandado.tukandadov2.ui.components.BatteryIndicator

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LockActionLayout(
    lockName: String,
    lockAlias: String,
    isOpening: Boolean,
    isLockerOpen: Boolean,
    onOpen: () -> Unit,
    topInfo: @Composable () -> Unit = {},
    bottomActions: @Composable ColumnScope.() -> Unit = {},
    // 👇 Nuevo: controlar si mostramos batería
    isAdmin: Boolean = false
) {
    val haptics = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 32.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.fillMaxWidth()) {
            Text(
                text = lockAlias,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.align(Alignment.Center)
            )
            // 👇 Sólo admins ven el indicador de batería
            if (isAdmin) {
                BatteryIndicator(
                    percent = 100, // si tienes el dato real, pásalo aquí
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 4.dp)
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        topInfo()
        Spacer(Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .combinedClickable(
                    enabled = !isOpening,
                    onClick = { onOpen() },
                    onLongClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onOpen()
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.tukandado_open),
                contentDescription = "Tukandado",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            if (isOpening) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    strokeWidth = 4.dp
                )
            }
        }

        Text("Pulsa para abrir", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        Spacer(Modifier.height(24.dp))
        bottomActions()
    }
}