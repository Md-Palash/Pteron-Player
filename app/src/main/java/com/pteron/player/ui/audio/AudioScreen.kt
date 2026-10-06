package com.pteron.player.ui.audio

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PlaylistPlay
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.pteron.player.data.model.AudioItem
import com.pteron.player.navigation.BottomNavDestination
import com.pteron.player.playback.AudioPlayerController
import com.pteron.player.ui.common.CircularActionButton
import com.pteron.player.ui.common.DarkModeButton
import com.pteron.player.ui.common.ErrorState
import com.pteron.player.ui.common.FullScreenMessage
import com.pteron.player.ui.common.LoadingState
import com.pteron.player.ui.common.TwoLineTitle
import com.pteron.player.ui.common.audioLibraryPermission
import com.pteron.player.ui.library.PermissionUiState
import com.pteron.player.ui.playlists.PlaylistNameDialog
import com.pteron.player.util.TabReselect

private val TabLabels = listOf("Home", "Library", "Playlist", "Artist", "Favorite")
private const val TAB_HOME = 0
private const val TAB_LIBRARY = 1
private const val TAB_PLAYLIST = 2
private const val TAB_ARTIST = 3
private const val TAB_FAVORITE = 4

/** Room under scrolling content for the floating nav pill (and, when a song is loaded, the mini player). */
private val TopButtonSize = 48.dp
private val NavClearance = 108.dp
private val NavAndMiniClearance = 184.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioScreen(
    viewModel: AudioViewModel,
    controller: AudioPlayerController,
    onOpenArtist: (String) -> Unit,
    onOpenPlaylist: (Long) -> Unit,
    onOpenNowPlaying: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val prefs by viewModel.musicPrefs.collectAsState()
    val player by controller.state.collectAsState()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val activity = context as? android.app.Activity
        val canAskAgain = granted || activity == null ||
            androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(activity, audioLibraryPermission)
        viewModel.onPermissionResult(granted, canAskAgain)
    }
    val alreadyGranted = ContextCompat.checkSelfPermission(context, audioLibraryPermission) ==
        android.content.pm.PackageManager.PERMISSION_GRANTED
    LaunchedEffect(Unit) {
        if (alreadyGranted) viewModel.onPermissionResult(true, true) else permissionLauncher.launch(audioLibraryPermission)
    }

    var tab by rememberSaveable { mutableStateOf(TAB_HOME) }
    var showCreate by remember { mutableStateOf(false) }
    val menu = remember { SongMenuState() }

    // Tapping the Audio icon while already on this tab goes back to Home.
    LaunchedEffect(Unit) {
        TabReselect.events.collect { if (it == BottomNavDestination.AUDIO) tab = TAB_HOME }
    }

    fun play(songs: List<AudioItem>, index: Int, shuffle: Boolean = false) {
        controller.playQueue(songs, index, shuffle)
        if (prefs.openNowPlayingOnPlay) onOpenNowPlaying()
    }

    val bottomClearance = if (player.current != null) NavAndMiniClearance else NavClearance

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
                    TwoLineTitle(subtitle = "PTERON PLAYER", title = "Music")
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (tab == TAB_PLAYLIST) {
                            CircularActionButton(Icons.Outlined.Add, "New playlist", onClick = { showCreate = true }, size = TopButtonSize)
                        }
                        DarkModeButton(onClick = viewModel::toggleDarkMode, size = TopButtonSize)
                    }
                }

                when {
                    state.permission == PermissionUiState.DENIED -> FullScreenMessage(
                        icon = Icons.Outlined.LibraryMusic,
                        title = "Access your music",
                        description = "Pteron Player needs permission to see the audio files already stored on your device. Nothing leaves your phone.",
                        actionLabel = "Grant access",
                        onAction = { permissionLauncher.launch(audioLibraryPermission) }
                    )
                    state.permission == PermissionUiState.PERMANENTLY_DENIED -> FullScreenMessage(
                        icon = Icons.Outlined.LibraryMusic,
                        title = "Access your music",
                        description = "Audio access was denied. Enable it from system Settings to browse your music.",
                        actionLabel = "Open settings",
                        onAction = {
                            context.startActivity(
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                            )
                        }
                    )
                    state.errorMessage != null -> ErrorState(state.errorMessage!!, onRetry = viewModel::refresh)
                    state.isLoading && state.songs.isEmpty() -> LoadingState()
                    state.permission == PermissionUiState.GRANTED && state.songs.isEmpty() -> FullScreenMessage(
                        icon = Icons.Outlined.LibraryMusic,
                        title = "No music found",
                        description = "We couldn't find any songs on this device. Add some, then refresh.",
                        actionLabel = "Refresh",
                        onAction = viewModel::refresh
                    )
                    else -> {
                        AudioTabRow(TabLabels, tab) { tab = it }
                        Spacer(Modifier.height(12.dp))
                        Crossfade(targetState = tab, modifier = Modifier.fillMaxSize(), animationSpec = tween(200), label = "audioTabContent") { currentTab ->
                        when (currentTab) {
                            TAB_HOME -> AudioHome(
                                state = state,
                                bottomPadding = bottomClearance,
                                onPlay = { songs, index -> play(songs, index) },
                                onMenu = { menu.options = it },
                                onOpenArtist = onOpenArtist,
                                onOpenPlaylist = onOpenPlaylist,
                                onSeeAll = { tab = it }
                            )
                            TAB_LIBRARY -> SongGrid(
                                songs = state.songs,
                                emptyText = "No songs found.",
                                bottomPadding = bottomClearance,
                                onPlay = { songs, index -> play(songs, index) },
                                onMenu = { menu.options = it }
                            )
                            TAB_FAVORITE -> SongGrid(
                                songs = state.favorites,
                                emptyText = "Songs you mark as favorite will show up here.",
                                bottomPadding = bottomClearance,
                                onPlay = { songs, index -> play(songs, index) },
                                onMenu = { menu.options = it }
                            )
                            TAB_PLAYLIST -> PlaylistGrid(state, bottomClearance, onOpenPlaylist)
                            TAB_ARTIST -> ArtistGrid(state, bottomClearance, onOpenArtist)
                        }
                        }
                    }
                }
            }

            MiniPlayer(
                controller = controller,
                onOpen = onOpenNowPlaying,
                modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 8.dp).padding(bottom = 92.dp)
            )
        }
    }

    SongMenuHost(menu = menu, viewModel = viewModel, controller = controller, playlists = state.playlists)

    if (showCreate) {
        PlaylistNameDialog(
            title = "New playlist",
            confirmLabel = "Create",
            onConfirm = {
                viewModel.createPlaylist(it)
                showCreate = false
            },
            onDismiss = { showCreate = false }
        )
    }
}

// --- Home: one line of cards per section -----------------------------------------------------------

@Composable
private fun AudioHome(
    state: AudioUiState,
    bottomPadding: androidx.compose.ui.unit.Dp,
    onPlay: (List<AudioItem>, Int) -> Unit,
    onMenu: (AudioItem) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenPlaylist: (Long) -> Unit,
    onSeeAll: (Int) -> Unit
) {
    val byId = remember(state.songs) { state.songs.associateBy { it.id } }
    LazyColumn(
        contentPadding = PaddingValues(top = 4.dp, bottom = bottomPadding),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item(key = "recent") {
            HomeSection("Recently played") {
                SongCardRow(state.recents, "Songs you play will show up here.", onPlay, onMenu)
            }
        }
        item(key = "songs") {
            HomeSection("All songs", onSeeAll = { onSeeAll(TAB_LIBRARY) }) {
                SongCardRow(state.songs, "No songs found.", onPlay, onMenu)
            }
        }
        item(key = "playlists") {
            HomeSection("Playlists", onSeeAll = { onSeeAll(TAB_PLAYLIST) }) {
                if (state.playlists.isEmpty()) {
                    EmptyHint("Create a playlist from the Playlist tab.")
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.playlists, key = { it.id }) { playlist ->
                            val count = playlist.videoIds.size
                            CollectionCard(
                                title = playlist.name,
                                subtitle = "$count song${if (count == 1) "" else "s"}",
                                coverUri = playlist.videoIds.firstNotNullOfOrNull { byId[it] }?.contentUri,
                                icon = Icons.Outlined.PlaylistPlay,
                                onClick = { onOpenPlaylist(playlist.id) },
                                modifier = Modifier.width132()
                            )
                        }
                    }
                }
            }
        }
        item(key = "artists") {
            HomeSection("Artists", onSeeAll = { onSeeAll(TAB_ARTIST) }) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.artists.take(30), key = { it.name }) { artist ->
                        CollectionCard(
                            title = artist.name,
                            subtitle = "${artist.songCount} song${if (artist.songCount == 1) "" else "s"}",
                            coverUri = artist.coverContentUri,
                            icon = Icons.Outlined.Person,
                            onClick = { onOpenArtist(artist.name) },
                            modifier = Modifier.width132()
                        )
                    }
                }
            }
        }
        item(key = "favorites") {
            HomeSection("Favorites", onSeeAll = { onSeeAll(TAB_FAVORITE) }) {
                SongCardRow(state.favorites, "Songs you mark as favorite will show up here.", onPlay, onMenu)
            }
        }
    }
}

private fun Modifier.width132() = this.then(Modifier.width(132.dp))

@Composable
private fun HomeSection(title: String, onSeeAll: (() -> Unit)? = null, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AudioSectionHeader(title, onSeeAll = onSeeAll)
        content()
    }
}

// --- Full-page grids (three columns of square cards) -------------------------------------------------

@Composable
private fun SongGrid(
    songs: List<AudioItem>,
    emptyText: String,
    bottomPadding: androidx.compose.ui.unit.Dp,
    onPlay: (List<AudioItem>, Int) -> Unit,
    onMenu: (AudioItem) -> Unit
) {
    if (songs.isEmpty()) {
        EmptyHint(emptyText)
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = bottomPadding),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(songs.size, key = { songs[it].id }) { index ->
            val song = songs[index]
            SongCard(song = song, onClick = { onPlay(songs, index) }, onMenu = { onMenu(song) })
        }
    }
}

@Composable
private fun PlaylistGrid(state: AudioUiState, bottomPadding: androidx.compose.ui.unit.Dp, onOpenPlaylist: (Long) -> Unit) {
    if (state.playlists.isEmpty()) {
        EmptyHint("No playlists yet. Tap + to create one, then add songs from the \u22EE menu on any song.")
        return
    }
    val byId = remember(state.songs) { state.songs.associateBy { it.id } }
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = bottomPadding),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(state.playlists, key = { it.id }) { playlist ->
            val count = playlist.videoIds.size
            CollectionCard(
                title = playlist.name,
                subtitle = "$count song${if (count == 1) "" else "s"}",
                coverUri = playlist.videoIds.firstNotNullOfOrNull { byId[it] }?.contentUri,
                icon = Icons.Outlined.PlaylistPlay,
                onClick = { onOpenPlaylist(playlist.id) }
            )
        }
    }
}

@Composable
private fun ArtistGrid(state: AudioUiState, bottomPadding: androidx.compose.ui.unit.Dp, onOpenArtist: (String) -> Unit) {
    if (state.artists.isEmpty()) {
        EmptyHint("No artists found.")
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = bottomPadding),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(state.artists, key = { it.name }) { artist ->
            CollectionCard(
                title = artist.name,
                subtitle = "${artist.songCount} song${if (artist.songCount == 1) "" else "s"}",
                coverUri = artist.coverContentUri,
                icon = Icons.Outlined.Person,
                onClick = { onOpenArtist(artist.name) }
            )
        }
    }
}
