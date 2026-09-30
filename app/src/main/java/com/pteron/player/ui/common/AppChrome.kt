package com.pteron.player.ui.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.PlaylistPlay
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pteron.player.navigation.BottomNavDestination

/** The small caps app name over the larger screen title, matching the Stitch header pattern. */
@Composable
fun TwoLineTitle(subtitle: String, title: String) {
    Column {
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            maxLines = 1
        )
    }
}

/**
 * A small round card holding one icon -- the "view toggle" / "dark mode" / "sort" buttons that
 * float in the top-right of every screen, on top of the plain background rather than inside
 * their own app-bar surface.
 */
@Composable
fun CircularActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .semantics { role = Role.Button }
            .bouncyClickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surfaceContainer),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * The persistent bottom navigation, redrawn as a floating pill: inset from the screen edges
 * (never full-width) with both ends fully rounded, sitting on top of the screen's own plain
 * background instead of a bar with its own surface splitting the screen into zones. Icon-only --
 * a capsule this short has no room for a second line of labels without losing the pill shape.
 */
@Composable
fun PteronBottomNavBar(
    current: BottomNavDestination,
    onSelect: (BottomNavDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 10.dp, vertical = 10.dp)
    ) {
        BottomNavDestination.entries.forEach { destination ->
            val selected = destination == current
            NavPillItem(
                icon = iconFor(destination, selected),
                label = destination.label,
                selected = selected,
                onClick = { onSelect(destination) }
            )
        }
    }
}

@Composable
private fun NavPillItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    val background by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent,
        animationSpec = tween(180),
        label = "navPillBackground"
    )
    val tint by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(180),
        label = "navPillTint"
    )
    Box(
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .size(48.dp)
            .clip(CircleShape)
            .semantics { role = Role.Button }
            .bouncyClickable(onClick = onClick)
            .background(background),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(22.dp))
    }
}

private fun iconFor(destination: BottomNavDestination, selected: Boolean): ImageVector = when (destination) {
    BottomNavDestination.LIBRARY -> if (selected) Icons.Filled.Folder else Icons.Outlined.Folder
    BottomNavDestination.VIDEOS -> if (selected) Icons.Filled.VideoLibrary else Icons.Outlined.VideoLibrary
    BottomNavDestination.PLAYLISTS -> if (selected) Icons.Filled.PlaylistPlay else Icons.Outlined.PlaylistPlay
    BottomNavDestination.SETTINGS -> if (selected) Icons.Filled.Settings else Icons.Outlined.Settings
}
