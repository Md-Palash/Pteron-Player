package com.pteron.player.ui.common

import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
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

@Composable
fun PteronBottomNavBar(current: BottomNavDestination, onSelect: (BottomNavDestination) -> Unit) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
        BottomNavDestination.entries.forEach { destination ->
            val selected = destination == current
            NavigationBarItem(
                selected = selected,
                onClick = { onSelect(destination) },
                icon = {
                    Icon(
                        imageVector = iconFor(destination, selected),
                        contentDescription = destination.label
                    )
                },
                label = { Text(destination.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

private fun iconFor(destination: BottomNavDestination, selected: Boolean): ImageVector = when (destination) {
    BottomNavDestination.LIBRARY -> if (selected) Icons.Filled.Folder else Icons.Outlined.Folder
    BottomNavDestination.VIDEOS -> if (selected) Icons.Filled.VideoLibrary else Icons.Outlined.VideoLibrary
    BottomNavDestination.PLAYLISTS -> if (selected) Icons.Filled.PlaylistPlay else Icons.Outlined.PlaylistPlay
    BottomNavDestination.SETTINGS -> if (selected) Icons.Filled.Settings else Icons.Outlined.Settings
}
