import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.min
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp

@Composable
fun GatewayChip(
    selected: Boolean,
    onClick: () -> Unit,
) {
    val d = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(density = d.density, fontScale = min(d.fontScale, 1.15f))
    ) {
        FilterChip(
            selected = selected,
            onClick = onClick,
            label = {
                Text(
                    "Gateway",
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            },
            leadingIcon = { Icon(Icons.Outlined.Hub, contentDescription = null) },
            modifier = Modifier
                .heightIn(min = 40.dp)
        )
    }
}

@Composable
fun CompactChip(content: @Composable () -> Unit) {
    val d = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(d.density, fontScale = min(d.fontScale, 1.15f))
    ) { content() }
}