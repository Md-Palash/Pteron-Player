package com.pteron.player.ui.player.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.SubtitlesOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pteron.player.ui.player.TrackOption

private val speedOptions = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)

/**
 * A small rounded card that floats over the video next to the button that opened it (instead of a
 * full-width bottom sheet). Tapping anywhere outside closes it.
 */
@Composable
private fun BoxScope.PlayerPopup(
    visible: Boolean,
    alignment: Alignment,
    edgeInsets: WindowInsetsSides,
    startPadding: Dp,
    endPadding: Dp,
    topPadding: Dp,
    bottomPadding: Dp,
    width: Dp,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(110)),
        exit = fadeOut(tween(80)),
        modifier = Modifier.matchParentSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + edgeInsets))
                .padding(start = startPadding, end = endPadding, top = topPadding, bottom = bottomPadding)
        ) {
            Surface(
                modifier = Modifier.align(alignment).width(width),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                shadowElevation = 12.dp
            ) {
                Column(
                    modifier = Modifier
                        .heightIn(max = 260.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(6.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) { content() }
            }
        }
    }
}

/** Playback speed: a narrow card at the bottom left, just above the speed button. */
@Composable
fun BoxScope.SpeedPopup(visible: Boolean, currentSpeed: Float, onSelect: (Float) -> Unit, onDismiss: () -> Unit) {
    PlayerPopup(
        visible = visible,
        alignment = Alignment.BottomStart,
        edgeInsets = WindowInsetsSides.Bottom,
        startPadding = 12.dp, endPadding = 0.dp, topPadding = 0.dp, bottomPadding = 66.dp,
        width = 112.dp,
        onDismiss = onDismiss
    ) {
        speedOptions.forEach { speed ->
            PopupRow(label = speed.toString().removeSuffix(".0") + "x", selected = speed == currentSpeed) { onSelect(speed) }
        }
    }
}

/** Audio track or subtitles: a compact card at the top right, under the buttons that open it. */
@Composable
fun BoxScope.TrackPopup(
    visible: Boolean,
    tracks: List<TrackOption>,
    allowDisable: Boolean,
    onSelect: (TrackOption?) -> Unit,
    onDismiss: () -> Unit,
    onLoadExternal: (() -> Unit)? = null
) {
    PlayerPopup(
        visible = visible,
        alignment = Alignment.TopEnd,
        edgeInsets = WindowInsetsSides.Top,
        startPadding = 0.dp, endPadding = 12.dp, topPadding = 52.dp, bottomPadding = 0.dp,
        width = 210.dp,
        onDismiss = onDismiss
    ) {
        if (onLoadExternal != null) {
            PopupRow(label = "Load subtitle file...", selected = false, leadingIcon = Icons.Outlined.FileOpen, onClick = onLoadExternal)
        }
        if (allowDisable) {
            PopupRow(label = "Off", selected = tracks.none { it.isSelected }, leadingIcon = Icons.Outlined.SubtitlesOff) { onSelect(null) }
        }
        if (tracks.isEmpty()) {
            Text(
                "No tracks available.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp)
            )
        }
        tracks.forEach { track ->
            PopupRow(label = track.label, selected = track.isSelected) { onSelect(track) }
        }
    }
}

@Composable
private fun PopupRow(label: String, selected: Boolean, leadingIcon: ImageVector? = null, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val content = if (selected) scheme.onPrimary else scheme.onSurface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) scheme.primary else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (leadingIcon != null) Icon(leadingIcon, contentDescription = null, tint = content, modifier = Modifier.size(18.dp))
        Text(
            label,
            style = MaterialTheme.typography.titleSmall,
            color = content,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (selected) Icon(Icons.Filled.Check, contentDescription = "Selected", tint = content, modifier = Modifier.size(16.dp))
    }
}
