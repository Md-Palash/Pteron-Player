package com.pteron.player.ui.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.PlaylistPlay
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.pteron.player.theme.LocalThemeColors
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pteron.player.navigation.BottomNavDestination

/**
 * The header every screen starts with: a tiny small-caps line (app name or "SETTINGS") above the
 * screen title. Fixed sizes, so every screen's header lines up the same way.
 */
@Composable
fun TwoLineTitle(subtitle: String, title: String) {
    Column {
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.2.sp,
            maxLines = 1
        )
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            maxLines = 1
        )
    }
}

/**
 * A section heading inside a screen ("Continue Watching", "Folders (12)"), aligned to the same
 * 16dp edge as the cards below it, with an optional [trailing] slot on the right.
 */
@Composable
fun SectionTitle(
    text: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = text, style = MaterialTheme.typography.titleMedium, maxLines = 1)
        trailing?.invoke()
    }
}

/** Default diameter of the round buttons in a screen's top-right row. Screens can pass a bigger one. */
val DefaultTopButtonSize = 40.dp

@Composable
private fun CircularButtonShell(
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = DefaultTopButtonSize,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .semantics {
                role = Role.Button
                this.contentDescription = contentDescription
            }
            .bouncyClickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surfaceContainer),
        contentAlignment = Alignment.Center,
        content = { content() }
    )
}

/**
 * A small round card holding one icon -- the "view toggle" / "sort" buttons that float in the
 * top-right of every screen, on top of the plain background rather than inside their own
 * app-bar surface.
 */
@Composable
fun CircularActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = DefaultTopButtonSize
) {
    CircularButtonShell(contentDescription, onClick, modifier, size) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(size * 0.5f)
        )
    }
}

/**
 * The dark-mode button: a moon while the theme is light (tap to go dark), a sun while it is dark
 * (tap to go light). The two icons cross-fade while turning and scaling, driven by one animated
 * value, so the switch is a smooth swap rather than a cut.
 */
@Composable
fun DarkModeButton(onClick: () -> Unit, modifier: Modifier = Modifier, size: Dp = DefaultTopButtonSize) {
    val isDark = LocalThemeColors.current.isDark
    val progress by animateFloatAsState(
        targetValue = if (isDark) 1f else 0f,
        animationSpec = tween(durationMillis = 380, easing = FastOutSlowInEasing),
        label = "darkModeIcon"
    )
    val tint = MaterialTheme.colorScheme.onSurface
    CircularButtonShell(
        contentDescription = if (isDark) "Switch to light mode" else "Switch to dark mode",
        onClick = onClick,
        modifier = modifier,
        size = size
    ) {
        val iconSize = size * 0.5f
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Outlined.DarkMode,
                contentDescription = null,
                tint = tint,
                modifier = Modifier
                    .size(iconSize)
                    .graphicsLayer {
                        alpha = 1f - progress
                        rotationZ = -90f * progress
                        scaleX = 1f - 0.5f * progress
                        scaleY = 1f - 0.5f * progress
                    }
            )
            Icon(
                imageVector = Icons.Outlined.LightMode,
                contentDescription = null,
                tint = tint,
                modifier = Modifier
                    .size(iconSize)
                    .graphicsLayer {
                        alpha = progress
                        rotationZ = 90f * (1f - progress)
                        scaleX = 0.5f + 0.5f * progress
                        scaleY = 0.5f + 0.5f * progress
                    }
            )
        }
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
            .padding(horizontal = 3.dp)
            .size(44.dp)
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
    BottomNavDestination.AUDIO -> if (selected) Icons.Filled.MusicNote else Icons.Outlined.MusicNote
    BottomNavDestination.PLAYLISTS -> if (selected) Icons.Filled.PlaylistPlay else Icons.Outlined.PlaylistPlay
    BottomNavDestination.SETTINGS -> if (selected) Icons.Filled.Settings else Icons.Outlined.Settings
}
