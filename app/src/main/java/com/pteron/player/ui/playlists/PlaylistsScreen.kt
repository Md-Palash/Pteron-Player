package com.pteron.player.ui.playlists

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.PlaylistPlay
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pteron.player.navigation.BottomNavDestination
import com.pteron.player.ui.common.CircularActionButton
import com.pteron.player.ui.common.FullScreenMessage
import com.pteron.player.ui.common.PteronBottomNavBar
import com.pteron.player.ui.common.TwoLineTitle

/**
 * Playlists are not implemented yet -- this is an honest placeholder, not a
 * feature pretending to work. Building real playlists means a data model,
 * create/rename/reorder/delete UI, and a picker for adding videos to one;
 * that's a real feature to build deliberately, not squeeze in as a side
 * effect of a visual pass.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistsScreen(onNavigate: (BottomNavDestination) -> Unit, onToggleDarkMode: () -> Unit) {
    Scaffold { insets ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(insets)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TwoLineTitle(subtitle = "PTERON PLAYER", title = "Playlists")
                    CircularActionButton(
                        icon = Icons.Outlined.DarkMode,
                        contentDescription = "Toggle dark mode",
                        onClick = onToggleDarkMode
                    )
                }
                FullScreenMessage(
                    icon = Icons.Outlined.PlaylistPlay,
                    title = "Playlists are coming soon",
                    description = "Creating and managing custom playlists isn't built yet. For now, use Favorites (the heart icon on any video) to keep track of what you want to watch.",
                    modifier = Modifier.fillMaxSize()
                )
            }

            PteronBottomNavBar(
                current = BottomNavDestination.PLAYLISTS,
                onSelect = onNavigate,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 40.dp, vertical = 18.dp)
            )
        }
    }
}
