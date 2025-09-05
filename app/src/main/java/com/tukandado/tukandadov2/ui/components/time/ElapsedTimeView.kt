import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import com.tukandado.tukandadov2.data.SessionManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.time.Duration
import java.time.Instant

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ElapsedTimeView(sessionManager: SessionManager) {
    val booking = sessionManager.getActiveBooking().collectAsState(initial = null).value
    val startTime = booking?.startTime // ISO 8601, p.ej. "2025-08-23T08:35:42.123Z"

    if (startTime != null) {
        ElapsedTimer(startTimeIso = startTime)
    } else {
        Text("—")
    }
}

@SuppressLint("DefaultLocale")
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ElapsedTimer(startTimeIso: String) {
    // Parse seguro del startTime
    val startInstant = remember(startTimeIso) {
        try { Instant.parse(startTimeIso) } catch (_: Exception) { null }
    }

    var elapsedSec by remember(startInstant) { mutableIntStateOf(0) }

    // Recalcular cada segundo desde el reloj real
    LaunchedEffect(startInstant) {
        if (startInstant == null) {
            elapsedSec = 0
            return@LaunchedEffect
        }
        while (isActive) {
            val duration = Duration.between(startInstant, Instant.now())
            elapsedSec = duration.seconds.coerceAtLeast(0).toInt()
            delay(1000L)
        }
    }

    val formatted = String.format(
        "%02d:%02d:%02d",
        elapsedSec / 3600,
        (elapsedSec % 3600) / 60,
        elapsedSec % 60
    )
    Text("Tiempo transcurrido: $formatted")
}