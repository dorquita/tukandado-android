import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Nfc
import androidx.compose.material.icons.outlined.Password
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tukandado.tukandadov2.ui.components.AdminActionButton


@Composable
fun ClientActionsGrid(
    onPasswords: () -> Unit,
    onRF: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Separador con margen razonable
        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 16.dp),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AdminActionButton(
                icon = Icons.Outlined.Password,
                label = "Contraseñas",
                shortLabel = "Códigos",      // se usa solo si hay zoom alto (si aplicaste mi versión)
                onClick = onPasswords,
                enabled = true,
                modifier = Modifier.weight(1f) // ⬅️ asegura que entra
            )

            AdminActionButton(
                icon = Icons.Outlined.Nfc,
                label = "Tarjetas RF",
                shortLabel = "Tarj. RF",
                onClick = onRF,
                enabled = false,              // grisado pero visible
                modifier = Modifier.weight(1f)
            )
        }
    }
}