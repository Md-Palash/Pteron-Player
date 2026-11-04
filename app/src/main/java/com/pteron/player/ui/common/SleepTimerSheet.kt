package com.pteron.player.ui.common

import android.os.SystemClock
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pteron.player.util.SleepTimer
import kotlinx.coroutines.delay
import kotlin.math.ceil

private val timerChoices = listOf(10, 15, 30, 45, 60, 90)

/** Bottom sheet shared by the video player and Now Playing: pick when playback should stop. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepTimerSheet(onDismiss: () -> Unit) {
    val endsAt by SleepTimer.endsAt.collectAsState()
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    // Only wakes up while a timer is set, and only twice a minute.
    LaunchedEffect(endsAt) {
        while (endsAt != null) {
            now = SystemClock.elapsedRealtime()
            delay(15_000L)
        }
    }
    val scheme = MaterialTheme.colorScheme

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Sleep timer",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
            )
            val end = endsAt
            if (end != null) {
                val minutes = ceil((end - now) / 60_000.0).toInt().coerceAtLeast(1)
                Text(
                    "Playback stops in about $minutes min",
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.primary,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
            timerChoices.forEach { minutes ->
                TimerRow(label = "$minutes minutes", selected = false) {
                    SleepTimer.start(minutes)
                    onDismiss()
                }
            }
            if (end != null) {
                TimerRow(label = "Turn off timer", selected = true) {
                    SleepTimer.cancel()
                    onDismiss()
                }
            }
        }
    }
}

@Composable
private fun TimerRow(label: String, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val content = if (selected) scheme.onPrimary else scheme.onSurface
    PteronClickableCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) scheme.primary else scheme.surfaceContainer,
        borderColor = if (selected) Color.Transparent else scheme.outlineVariant.copy(alpha = 0.5f)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.titleSmall,
            color = content,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
        )
    }
}
