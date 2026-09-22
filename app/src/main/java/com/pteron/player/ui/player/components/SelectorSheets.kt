package com.pteron.player.ui.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.SubtitlesOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pteron.player.ui.common.PteronClickableCard
import com.pteron.player.ui.player.TrackOption

private val speedOptions = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeedSelectorSheet(currentSpeed: Float, onSelect: (Float) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SheetTitle("Playback speed")
            speedOptions.forEach { speed ->
                SelectorRow(
                    label = "${speed}x",
                    selected = speed == currentSpeed,
                    onClick = { onSelect(speed) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackSelectorSheet(
    title: String,
    tracks: List<TrackOption>,
    allowDisable: Boolean,
    onSelect: (TrackOption?) -> Unit,
    onDismiss: () -> Unit,
    onLoadExternal: (() -> Unit)? = null
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SheetTitle(title)
            if (onLoadExternal != null) {
                SelectorRow(
                    label = "Load subtitle file...",
                    supportingLabel = ".srt, .vtt, or .ass from your device",
                    leadingIcon = Icons.Outlined.FileOpen,
                    selected = false,
                    onClick = onLoadExternal
                )
            }
            if (tracks.isEmpty()) {
                Text(
                    "No tracks available for this video.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 12.dp)
                )
                return@Column
            }
            if (allowDisable) {
                SelectorRow(
                    label = "Off",
                    leadingIcon = Icons.Outlined.SubtitlesOff,
                    selected = tracks.none { it.isSelected },
                    onClick = { onSelect(null) }
                )
            }
            // A plain Column would work for the handful of tracks a video normally has, but a
            // LazyColumn costs nothing extra and stays correct if a file ever has many more.
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(tracks, key = { "${it.groupIndex}-${it.trackIndex}" }) { track ->
                    SelectorRow(
                        label = track.label,
                        selected = track.isSelected,
                        onClick = { onSelect(track) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SheetTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
    )
}

/**
 * A rounded card row shared by every selector sheet: the theme's medium shade normally, the dark
 * (accent) shade when selected -- the same language as the sort-menu cards elsewhere in the app,
 * so audio/subtitle/speed pickers read as the same control instead of a different, older style.
 */
@Composable
private fun SelectorRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    supportingLabel: String? = null,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    val scheme = MaterialTheme.colorScheme
    val content = if (selected) scheme.onPrimary else scheme.onSurface
    PteronClickableCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) scheme.primary else scheme.surfaceContainer,
        borderColor = if (selected) Color.Transparent else scheme.outlineVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (leadingIcon != null) {
                Icon(leadingIcon, contentDescription = null, tint = content, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    color = content,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (supportingLabel != null) {
                    Text(
                        supportingLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = content.copy(alpha = 0.75f)
                    )
                }
            }
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(content.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = content, modifier = Modifier.size(13.dp))
                }
            }
        }
    }
}
