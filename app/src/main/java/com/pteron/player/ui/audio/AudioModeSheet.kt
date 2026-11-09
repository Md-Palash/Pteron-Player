package com.pteron.player.ui.audio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pteron.player.data.prefs.AudioMode
import com.pteron.player.ui.common.PteronClickableCard

/** The ready-made sound profiles of the music player. "Normal" leaves the sound untouched. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioModeSheet(selected: AudioMode, onSelect: (AudioMode) -> Unit, onDismiss: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Audio mode", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp))
            AudioMode.musicModes.forEach { mode ->
                val isSelected = mode == selected
                val content = if (isSelected) scheme.onPrimary else scheme.onSurface
                PteronClickableCard(
                    onClick = { onSelect(mode) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) scheme.primary else scheme.surfaceContainer,
                    borderColor = if (isSelected) Color.Transparent else scheme.outlineVariant.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(mode.displayName, style = MaterialTheme.typography.titleSmall, color = content, modifier = Modifier.weight(1f))
                        if (isSelected) Icon(Icons.Filled.Check, contentDescription = "Selected", tint = content, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}
