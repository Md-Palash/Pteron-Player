package com.pteron.player.ui.folder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pteron.player.data.model.SortDirection
import com.pteron.player.data.model.SortOption
import com.pteron.player.data.model.VideoFilter
import com.pteron.player.data.model.ViewMode
import com.pteron.player.ui.common.EmptyLibraryState
import com.pteron.player.ui.common.ErrorState
import com.pteron.player.ui.common.LoadingState
import com.pteron.player.ui.common.NoSearchResultsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderScreen(
    viewModel: FolderViewModel,
    onBack: () -> Unit,
    onOpenVideo: (videoId: Long, bucketId: String) -> Unit,
    onShufflePlay: (firstVideoId: Long, bucketId: String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var sortMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.folderName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    IconButton(onClick = { viewModel.onViewModeSelected(if (uiState.appearance.viewMode == ViewMode.GRID) ViewMode.LIST else ViewMode.GRID) }) {
                        Icon(
                            imageVector = if (uiState.appearance.viewMode == ViewMode.GRID) Icons.Filled.ViewList else Icons.Filled.GridView,
                            contentDescription = "Toggle grid or list view"
                        )
                    }
                    Box {
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
        floatingActionButton = {
            if (uiState.visibleVideos.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = {
                        val shuffled = viewModel.shufflePlayOrder()
                        shuffled.firstOrNull()?.let { onShufflePlay(it.id, it.bucketId) }
                    },
                    icon = { Icon(Icons.Filled.Shuffle, contentDescription = null) },
                    text = { Text("Shuffle Play") }
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            FilterChipsRow(
                active = uiState.activeFilter,
                onSelect = viewModel::onFilterSelected
            )

            when {
                uiState.errorMessage != null -> ErrorState(uiState.errorMessage!!, onRetry = {})
                uiState.isLoading && uiState.videos.isEmpty() -> LoadingState()
                uiState.videos.isEmpty() -> EmptyLibraryState(onRefresh = {})
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
                            VideoGridTile(video = video, onClick = { onOpenVideo(video.id, video.bucketId) })
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
                                onToggleFavorite = { viewModel.toggleFavorite(video) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChipsRow(active: VideoFilter, onSelect: (VideoFilter) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(VideoFilter.entries) { filter ->
            FilterChip(
                selected = active == filter,
                onClick = { onSelect(filter) },
                label = { Text(filter.label) }
            )
        }
    }
}
