package com.pteron.player.ui.folder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.ViewList
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pteron.player.data.model.ViewMode
import com.pteron.player.ui.common.EmptyLibraryState
import com.pteron.player.ui.common.ErrorState
import com.pteron.player.ui.common.LoadingState
import com.pteron.player.ui.common.NoSearchResultsState
import com.pteron.player.ui.common.TwoLineTitle
import com.pteron.player.ui.common.VideoListHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderScreen(
    viewModel: FolderViewModel,
    onBack: () -> Unit,
    onOpenVideo: (videoId: Long, bucketId: String) -> Unit,
    onShufflePlay: (firstVideoId: Long, bucketId: String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    // `visibleVideos` filters and sorts the whole list on every read, so compute it once per state.
    val visibleVideos = remember(uiState) { uiState.visibleVideos }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { TwoLineTitle(subtitle = "PTERON PLAYER", title = uiState.folderName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.onViewModeSelected(if (uiState.appearance.viewMode == ViewMode.GRID) ViewMode.LIST else ViewMode.GRID) }) {
                        Icon(
                            imageVector = if (uiState.appearance.viewMode == ViewMode.GRID) Icons.Outlined.ViewList else Icons.Outlined.GridView,
                            contentDescription = "Toggle grid or list view"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (visibleVideos.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = {
                        val shuffled = viewModel.shufflePlayOrder()
                        shuffled.firstOrNull()?.let { onShufflePlay(it.id, it.bucketId) }
                    },
                    icon = { Icon(Icons.Outlined.Shuffle, contentDescription = null) },
                    text = { Text("Shuffle Play") }
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            VideoListHeader(
                activeFilter = uiState.activeFilter,
                onFilterSelected = viewModel::onFilterSelected,
                videoCount = visibleVideos.size,
                sortOption = uiState.appearance.sortOption,
                sortDirection = uiState.appearance.sortDirection,
                onSortSelected = viewModel::onSortSelected
            )

            when {
                uiState.errorMessage != null -> ErrorState(uiState.errorMessage!!, onRetry = {})
                uiState.isLoading && uiState.videos.isEmpty() -> LoadingState()
                uiState.videos.isEmpty() -> EmptyLibraryState(onRefresh = {})
                visibleVideos.isEmpty() -> NoSearchResultsState()
                else -> when (uiState.appearance.viewMode) {
                    ViewMode.GRID -> LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(visibleVideos, key = { it.id }) { video ->
                            VideoGridTile(
                                video = video,
                                onClick = { onOpenVideo(video.id, video.bucketId) },
                                onToggleFavorite = { viewModel.toggleFavorite(video) },
                                onToggleWatched = { viewModel.toggleWatched(video) },
                                onClearProgress = { viewModel.clearProgress(video) }
                            )
                        }
                    }
                    ViewMode.LIST -> LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(visibleVideos, key = { it.id }) { video ->
                            VideoListRow(
                                video = video,
                                onClick = { onOpenVideo(video.id, video.bucketId) },
                                onToggleFavorite = { viewModel.toggleFavorite(video) },
                                onToggleWatched = { viewModel.toggleWatched(video) },
                                onClearProgress = { viewModel.clearProgress(video) }
                            )
                        }
                    }
                }
            }
        }
    }
}
