package com.pteron.player.ui.playlists

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlaylistPlay
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pteron.player.navigation.BottomNavDestination
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
fun PlaylistsScreen(onNavigate: (BottomNavDestination) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(title = { TwoLineTitle(subtitle = "PTERON PLAYER", title = "Playlists") })
        },
        bottomBar = { PteronBottomNavBar(current = BottomNavDestination.PLAYLISTS, onSelect = onNavigate) }
    ) { padding ->
        FullScreenMessage(
            icon = Icons.Outlined.PlaylistPlay,
            title = "Playlists are coming soon",
            description = "Creating and managing custom playlists isn't built yet. For now, use Favorites (the heart icon on any video) to keep track of what you want to watch.",
            modifier = Modifier.padding(padding)
        )
    }
}
