package com.tukandado.tukandadov2.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryFull
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp


@Composable
fun BatteryIndicator(percent: Int, modifier: Modifier = Modifier) {
    val color = when {
        percent >= 80 -> Color(0xFF4CAF50)
        percent >= 30 -> MaterialTheme.colorScheme.onSurfaceVariant
        else          -> MaterialTheme.colorScheme.error
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Icon(
            imageVector = Icons.Outlined.BatteryFull,
            contentDescription = "Batería",
            tint = color
        )
        Spacer(Modifier.width(4.dp))
        Text("$percent%", color = color, style = MaterialTheme.typography.labelLarge)
    }
}
