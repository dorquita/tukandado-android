package com.tukandado.tukandadov2.ui.booking

import android.annotation.SuppressLint
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tukandado.tukandadov2.R
import com.tukandado.tukandadov2.ttlock.TTLockManager
import com.tukandado.tukandadov2.viewmodel.BookingViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.tukandado.tukandadov2.ui.components.layout.LockActionLayout

@SuppressLint("DefaultLocale")
@Composable
fun ActiveBookingScreen(
    lockName: String,
    lockAlias: String,
    lockData: String,
    lockMac: String,
    onRelease: () -> Unit
) {
    val context = LocalContext.current
    val ttLockManager = remember { TTLockManager(context) }
    val bookingViewModel: BookingViewModel = viewModel()
    val scope = rememberCoroutineScope()

    var elapsedTime by remember { mutableIntStateOf(0) }
    var isOpening by remember { mutableStateOf(false) }
    var isLockerOpen by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            elapsedTime++
        }
    }
    val formattedTime = String.format("%02d:%02d:%02d", elapsedTime / 3600, (elapsedTime % 3600) / 60, elapsedTime % 60)

    LockActionLayout(
        lockName = lockName,
        lockAlias = lockAlias,
        isOpening = isOpening,
        isLockerOpen = isLockerOpen,
        onOpen = {
            isOpening = true
            try {
                ttLockManager.controlLock(lockData, lockMac, true)
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
            scope.launch {
                delay(3000L)
                isOpening = false
                isLockerOpen = true
                delay(5000L)
                isLockerOpen = false
            }
        },
        topInfo = {
            Text(formattedTime, style = MaterialTheme.typography.headlineMedium)
        },
        bottomActions = {
            Button(
                onClick = {
                    bookingViewModel.endBooking(context)
                    onRelease()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Cerrar y liberar taquilla")
            }
        }
    )
}
