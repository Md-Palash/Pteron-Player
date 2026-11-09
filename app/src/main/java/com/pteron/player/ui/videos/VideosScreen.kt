package com.pteron.player.ui.videos

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.material.icons.outlined.Search
import androidx.compose.runtime.saveable.rememberSaveable
import com.pteron.player.ui.common.rememberMoveVideos
import com.pteron.player.ui.folder.MoveToFolderSheet
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.ViewList
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pteron.player.ui.common.SearchCard
import com.pteron.player.ui.common.SelectionBar
import com.pteron.player.ui.common.rememberDeleteVideos
import com.pteron.player.ui.common.shareVideos
import com.pteron.player.ui.common.toggled
import com.pteron.player.ui.playlists.AddVideosToPlaylistSheet
import com.pteron.player.data.model.VideoItem
import com.pteron.player.data.model.ViewMode
import com.pteron.player.ui.playlists.AddToPlaylistSheet
import com.pteron.player.ui.common.CircularActionButton
import com.pteron.player.ui.common.DarkModeButton
import com.pteron.player.ui.common.EmptyLibraryState
import com.pteron.player.ui.common.ErrorState
import com.pteron.player.ui.common.LoadingState
import com.pteron.player.ui.common.NoSearchResultsState
import com.pteron.player.ui.common.TopBarSortButton
import com.pteron.player.ui.common.TwoLineTitle
import com.pteron.player.ui.common.FilterCardsRow
import com.pteron.player.ui.common.FloatingPills
import com.pteron.player.ui.folder.VideoGridTile
import com.pteron.player.ui.folder.VideoListRow

private val BottomNavClearance = 108.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideosScreen(
    viewModel: VideosViewModel,
    onOpenVideo: (videoId: Long, bucketId: String) -> Unit,
    /** False while the screen is still sliding in: the big list waits for it. */
    settled: Boolean = true
) {
    val uiState by viewModel.uiState.collectAsState()
    // `visibleVideos` filters and sorts the whole list on every read, so compute it once per state.
    val visibleVideos = remember(uiState) { uiState.visibleVideos }

    // Multi-select: long press a video to start, tap to add or remove, back to cancel.
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    val selectionMode = selectedIds.isNotEmpty()
    val selectedVideos = remember(uiState.videos, selectedIds) { uiState.videos.filter { it.id in selectedIds } }
    var showMultiPlaylist by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val requestDelete = rememberDeleteVideos(onDeleted = { selectedIds = emptySet() })
    val requestMove = rememberMoveVideos(onMoved = { selectedIds = emptySet() })
    var showMoveSheet by remember { mutableStateOf(false) }
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    if (showMoveSheet) {
        MoveToFolderSheet(
            videos = selectedVideos,
            onPick = { path ->
                showMoveSheet = false
                requestMove?.invoke(selectedVideos, path)
            },
            onDismiss = { showMoveSheet = false }
        )
    }
    BackHandler(enabled = selectionMode) { selectedIds = emptySet() }
    if (showMultiPlaylist) {
        AddVideosToPlaylistSheet(
            videos = selectedVideos,
            onDone = { selectedIds = emptySet() },
            onDismiss = { showMultiPlaylist = false }
        )
    }

    // The video whose "Add to playlist" sheet is open, if any.
    var playlistTarget by remember { mutableStateOf<VideoItem?>(null) }
    playlistTarget?.let { target ->
        AddToPlaylistSheet(video = target, onDismiss = { playlistTarget = null })
    }

    Scaffold { insets ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(insets)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(Modifier.fillMaxSize()) {
                if (selectionMode) {
                    SelectionBar(
                        count = selectedIds.size,
                        allSelected = visibleVideos.isNotEmpty() && selectedIds.size == visibleVideos.size,
                        onClose = { selectedIds = emptySet() },
                        onSelectAll = { selectedIds = visibleVideos.map { it.id }.toSet() },
                        onAddToPlaylist = { showMultiPlaylist = true },
                        onMove = requestMove?.let { { showMoveSheet = true } },
                        onShare = { shareVideos(context, selectedVideos) },
                        onDelete = requestDelete?.let { delete -> { delete(selectedVideos) } }
                    )
                } else Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TwoLineTitle(subtitle = "PTERON PLAYER", title = "All Videos")
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CircularActionButton(
                        icon = Icons.Outlined.Search,
                        contentDescription = "Search",
                        onClick = {
                            searchOpen = !searchOpen
                            if (!searchOpen) viewModel.onSearchQueryChange("")
                        }
                    )
                    TopBarSortButton(
                            selectedOption = uiState.appearance.sortOption,
                            direction = uiState.appearance.sortDirection,
                            onSelect = viewModel::onSortSelected
                        )
                        CircularActionButton(
                            icon = if (uiState.appearance.viewMode == ViewMode.GRID) Icons.Outlined.ViewList else Icons.Outlined.GridView,
                            contentDescription = "Toggle grid or list view",
                            onClick = {
                                viewModel.onViewModeSelected(if (uiState.appearance.viewMode == ViewMode.GRID) ViewMode.LIST else ViewMode.GRID)
                            }
                        )
                        DarkModeButton(onClick = viewModel::toggleDarkMode)
                    }
                }

                
                AnimatedVisibility(
                    visible = searchOpen,
                    enter = fadeIn(tween(140)) + expandVertically(tween(160)),
                    exit = fadeOut(tween(100)) + shrinkVertically(tween(140))
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), contentAlignment = Alignment.Center) {
                        SearchCard(
                            value = uiState.searchQuery,
                            onValueChange = viewModel::onSearchQueryChange,
                            placeholder = "Search videos...",
                            autoFocus = true
                        )
                    }
                }
                FloatingPills(
 modifier = Modifier.fillMaxSize(),
 pills = { FilterCardsRow(active = uiState.activeFilter, onSelect = viewModel::onFilterSelected) }
 ) { topPadding ->
AnimatedVisibility(visible = settled, enter = fadeIn(tween(160)), modifier = Modifier.fillMaxSize()) {
when {
                    uiState.errorMessage != null -> ErrorState(uiState.errorMessage!!, onRetry = viewModel::refresh)
                    uiState.isLoading && uiState.videos.isEmpty() -> LoadingState()
                    uiState.videos.isEmpty() -> EmptyLibraryState(onRefresh = viewModel::refresh)
                    visibleVideos.isEmpty() -> NoSearchResultsState()
                    else -> when (uiState.appearance.viewMode) {
                        ViewMode.GRID -> LazyVerticalGrid(
                            columns = GridCells.Fixed(uiState.appearance.videoGridColumns),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = topPadding + 8.dp, bottom = BottomNavClearance),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(visibleVideos, key = { it.id }) { video ->
                                VideoGridTile(
                                    video = video,
                                    onClick = {
                                    if (selectionMode) {
                                        selectedIds = selectedIds.toggled(video.id)
                                    } else {
                                        onOpenVideo(video.id, video.bucketId)
                                    }
                                },
                                selected = video.id in selectedIds,
                                selectionMode = selectionMode,
                                onLongClick = { selectedIds = selectedIds + video.id },
                                    onToggleFavorite = { viewModel.toggleFavorite(video) },
                                    onToggleWatched = { viewModel.toggleWatched(video) },
                                    onClearProgress = { viewModel.clearProgress(video) },
                                onAddToPlaylist = { playlistTarget = video },
                                columns = uiState.appearance.videoGridColumns
                                )
                            }
                        }
                        ViewMode.LIST -> LazyColumn(
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = topPadding + 8.dp, bottom = BottomNavClearance),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(visibleVideos, key = { it.id }) { video ->
                                VideoListRow(
                                    video = video,
                                    onClick = {
                                    if (selectionMode) {
                                        selectedIds = selectedIds.toggled(video.id)
                                    } else {
                                        onOpenVideo(video.id, video.bucketId)
                                    }
                                },
                                selected = video.id in selectedIds,
                                selectionMode = selectionMode,
                                onLongClick = { selectedIds = selectedIds + video.id },
                                    onToggleFavorite = { viewModel.toggleFavorite(video) },
                                    onToggleWatched = { viewModel.toggleWatched(video) },
                                    onClearProgress = { viewModel.clearProgress(video) },
                                onAddToPlaylist = { playlistTarget = video }
                                )
                            }
                        }
                    }
                }
 
}
}
            }
        }
    }
}
