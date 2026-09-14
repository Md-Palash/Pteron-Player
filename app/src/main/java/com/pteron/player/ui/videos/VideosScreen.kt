package com.pteron.player.ui.videos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pteron.player.data.model.SortDirection
import com.pteron.player.data.model.SortOption
import com.pteron.player.data.model.VideoFilter
import com.pteron.player.data.model.ViewMode
import com.pteron.player.navigation.BottomNavDestination
import com.pteron.player.ui.common.EmptyLibraryState
import com.pteron.player.ui.common.ErrorState
import com.pteron.player.ui.common.LoadingState
import com.pteron.player.ui.common.NoSearchResultsState
import com.pteron.player.ui.common.PteronBottomNavBar
import com.pteron.player.ui.common.TwoLineTitle
import com.pteron.player.ui.folder.VideoGridTile
import com.pteron.player.ui.folder.VideoListRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideosScreen(
    viewModel: VideosViewModel,
    onOpenVideo: (videoId: Long, bucketId: String) -> Unit,
    onNavigate: (BottomNavDestination) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var sortMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { TwoLineTitle(subtitle = "PTERON PLAYER", title = "All Videos") },
                actions = {
                    androidx.compose.foundation.layout.Box {
                        IconButton(onClick = { sortMenuExpanded = true }) {
                            Icon(Icons.Filled.Sort, contentDescription = "Sort")
                        }
                        DropdownMenu(expanded = sortMenuExpanded, onDismissRequest = { sortMenuExpanded = false }) {
                            SortOption.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.label) },
                                    onClick = {
                                        val newDirection = if (uiState.appearance.sortOption == option) {
                                            if (uiState.appearance.sortDirection == SortDirection.ASCENDING) SortDirection.DESCENDING else SortDirection.ASCENDING
                                        } else SortDirection.DESCENDING
                                        viewModel.onSortSelected(option, newDirection)
                                        sortMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            )
        },
        bottomBar = { PteronBottomNavBar(current = BottomNavDestination.VIDEOS, onSelect = onNavigate) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(VideoFilter.entries) { filter ->
                    FilterChip(
                        selected = uiState.activeFilter == filter,
                        onClick = { viewModel.onFilterSelected(filter) },
                        label = { Text(filter.label) }
                    )
                }
                item {
                    IconButton(onClick = {
                        viewModel.onViewModeSelected(if (uiState.appearance.viewMode == ViewMode.GRID) ViewMode.LIST else ViewMode.GRID)
                    }) {
                        Icon(
                            imageVector = if (uiState.appearance.viewMode == ViewMode.GRID) Icons.Filled.ViewList else Icons.Filled.GridView,
                            contentDescription = "Toggle grid or list view"
                        )
                    }
                }
            }

            when {
                uiState.errorMessage != null -> ErrorState(uiState.errorMessage!!, onRetry = viewModel::refresh)
                uiState.isLoading && uiState.videos.isEmpty() -> LoadingState()
                uiState.videos.isEmpty() -> EmptyLibraryState(onRefresh = viewModel::refresh)
                uiState.visibleVideos.isEmpty() -> NoSearchResultsState()
                else -> when (uiState.appearance.viewMode) {
                    ViewMode.GRID -> LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(uiState.visibleVideos, key = { it.id }) { video ->
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
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(uiState.visibleVideos, key = { it.id }) { video ->
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
