package com.pteron.player.ui.audio

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.PlaylistRemove
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pteron.player.data.model.AudioItem
import com.pteron.player.playback.AudioPlayerController
import com.pteron.player.ui.common.CircularActionButton
import com.pteron.player.ui.common.FullScreenMessage
import com.pteron.player.ui.common.TwoLineTitle
import com.pteron.player.ui.common.bouncyClickable
import com.pteron.player.ui.playlists.ActionPill
import com.pteron.player.ui.playlists.ConfirmDeleteDialog
import com.pteron.player.ui.playlists.PlaylistNameDialog
import com.pteron.player.util.formatTimecode

/** All songs of one artist. */
@Composable
fun AudioArtistScreen(
    viewModel: AudioViewModel,
    controller: AudioPlayerController,
    artist: String,
    onBack: () -> Unit,
    onOpenNowPlaying: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val songs = remember(state.songs, artist) { state.songs.filter { it.artist == artist } }
    SongsPage(
        subtitle = "ARTIST",
        title = artist,
        songs = songs,
        emptyTitle = "No songs",
        emptyDescription = "There are no songs by this artist on the device.",
        viewModel = viewModel,
        controller = controller,
        playlists = state.playlists,
        onBack = onBack,
        onOpenNowPlaying = onOpenNowPlaying
    )
}

/** One audio playlist: play / shuffle, reorder, remove, rename and delete. */
@Composable
fun AudioPlaylistScreen(
    viewModel: AudioViewModel,
    controller: AudioPlayerController,
    playlistId: Long,
    onBack: () -> Unit,
    onOpenNowPlaying: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val playlist = state.playlists.firstOrNull { it.id == playlistId }
    val songs = remember(state.songs, playlist) {
        val byId = state.songs.associateBy { it.id }
        playlist?.videoIds?.mapNotNull { byId[it] }.orEmpty()
    }
    var menuOpen by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }

    SongsPage(
        subtitle = "PLAYLIST",
        title = playlist?.name.orEmpty(),
        songs = songs,
        emptyTitle = "This playlist is empty",
        emptyDescription = "Open the \u22EE menu on any song and choose Add to playlist.",
        viewModel = viewModel,
        controller = controller,
        playlists = state.playlists,
        onBack = onBack,
        onOpenNowPlaying = onOpenNowPlaying,
        headerAction = {
            Box {
                CircularActionButton(Icons.Outlined.MoreVert, "Playlist options", onClick = { menuOpen = true })
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                        onClick = { menuOpen = false; showRename = true }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete playlist") },
                        leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                        onClick = { menuOpen = false; showDelete = true }
                    )
                }
            }
        },
        rowActions = { song, index ->
            buildList {
                if (index > 0) add(SheetAction("Move up", Icons.Outlined.ArrowUpward) { viewModel.moveInPlaylist(playlistId, song.id, -1) })
                if (index < songs.lastIndex) add(SheetAction("Move down", Icons.Outlined.ArrowDownward) { viewModel.moveInPlaylist(playlistId, song.id, 1) })
                add(SheetAction("Remove from playlist", Icons.Outlined.PlaylistRemove) { viewModel.removeFromPlaylist(playlistId, song.id) })
            }
        }
    )

    if (showRename && playlist != null) {
        PlaylistNameDialog(
            title = "Rename playlist",
            confirmLabel = "Save",
            initialName = playlist.name,
            onConfirm = {
                viewModel.renamePlaylist(playlistId, it)
                showRename = false
            },
            onDismiss = { showRename = false }
        )
    }
    if (showDelete && playlist != null) {
        ConfirmDeleteDialog(
            title = "Delete playlist?",
            message = "\"${playlist.name}\" will be deleted. Your songs are not affected.",
            onConfirm = {
                showDelete = false
                viewModel.deletePlaylist(playlistId)
                onBack()
            },
            onDismiss = { showDelete = false }
        )
    }
}

/** The shared page: header, Play / Shuffle pills and the songs as list rows. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SongsPage(
    subtitle: String,
    title: String,
    songs: List<AudioItem>,
    emptyTitle: String,
    emptyDescription: String,
    viewModel: AudioViewModel,
    controller: AudioPlayerController,
    playlists: List<com.pteron.player.data.model.Playlist>,
    onBack: () -> Unit,
    onOpenNowPlaying: () -> Unit,
    headerAction: (@Composable () -> Unit)? = null,
    rowActions: (AudioItem, Int) -> List<SheetAction> = { _, _ -> emptyList() }
) {
    val player by controller.state.collectAsState()
    val prefs by viewModel.musicPrefs.collectAsState()
    val menu = remember { SongMenuState() }
    // The row index of the song whose menu is open, so reorder actions know their position.
    var menuIndex by remember { mutableStateOf(0) }

    fun play(index: Int, shuffle: Boolean) {
        controller.playQueue(songs, index, shuffle)
        if (prefs.openNowPlayingOnPlay) onOpenNowPlaying()
    }

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
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.padding(end = 8.dp).bouncyClickable(onClick = onBack)) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                        }
                        TwoLineTitle(subtitle = subtitle, title = title)
                    }
                    headerAction?.invoke()
                }

                if (songs.isEmpty()) {
                    FullScreenMessage(
                        icon = Icons.Outlined.LibraryMusic,
                        title = emptyTitle,
                        description = emptyDescription,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ActionPill(
                            label = "Play all",
                            icon = Icons.Outlined.PlayArrow,
                            filled = true,
                            onClick = { play(0, false) },
                            modifier = Modifier.weight(1f)
                        )
                        ActionPill(
                            label = "Shuffle",
                            icon = Icons.Outlined.Shuffle,
                            filled = false,
                            onClick = { play(songs.indices.random(), true) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Text(
                        text = "${songs.size} song${if (songs.size == 1) "" else "s"} • ${formatTimecode(songs.sumOf { it.durationMs })}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                    )
                    LazyColumn(
                        contentPadding = PaddingValues(
                            start = 16.dp, end = 16.dp, top = 0.dp,
                            bottom = if (player.current != null) 96.dp else 24.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
                            SongListRow(
                                song = song,
                                isCurrent = player.current?.id == song.id,
                                onClick = { play(index, false) },
                                onMenu = {
                                    menuIndex = index
                                    menu.options = song
                                }
                            )
                        }
                    }
                }
            }

            MiniPlayer(
                controller = controller,
                onOpen = onOpenNowPlaying,
                modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 16.dp, vertical = 16.dp)
            )
        }
    }

    SongMenuHost(
        menu = menu,
        viewModel = viewModel,
        controller = controller,
        playlists = playlists,
        extra = { song -> rowActions(song, menuIndex) }
    )
}
