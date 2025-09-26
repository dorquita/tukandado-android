package com.tukandado.tukandadov2.ui.components

import android.annotation.SuppressLint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// Layout & spacing
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape

// Material 3
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha

import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.unit.Density
import kotlin.math.min


@Composable
fun AdminActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = false,
    shortLabel: String? = null,
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier
) {
    val fontScale = LocalConfiguration.current.fontScale
    val compact = fontScale >= 1.3f

    val colWidth   = if (compact) 112.dp else 96.dp
    val circleSize = if (compact) 72.dp else 64.dp
    val iconSize   = if (compact) 30.dp else 28.dp
    val disabledAlpha = 0.45f

    Column(
        modifier = modifier.width(colWidth),           // ⬅️ el modifier del padre SOLO aquí
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier                         // ⬅️ tamaño fijo → no se estira a lo alto
                .size(circleSize)
                .then(if (!enabled) Modifier.alpha(disabledAlpha) else Modifier),
            enabled = enabled,
            onClick = onClick,
            shape = CircleShape,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            color = MaterialTheme.colorScheme.surface
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),      // ⬅️ ocupa el círculo
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(iconSize)
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        // Protege el texto con fontScale local y elipsis
        val d = LocalDensity.current
        CompositionLocalProvider(
            LocalDensity provides Density(d.density, fontScale = min(d.fontScale, 1.15f))
        ) {
            Text(
                text = if (compact) (shortLabel ?: label) else label,
                style = MaterialTheme.typography.labelMedium.copy(
                    lineBreak = LineBreak.Heading,
                    hyphens = Hyphens.None
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (compact) 1 else 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}