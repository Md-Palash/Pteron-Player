package com.pteron.player.ui.playlists

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.PlaylistRemove
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pteron.player.data.model.VideoItem
import com.pteron.player.ui.common.CircularActionButton
import com.pteron.player.ui.common.FullScreenMessage
import com.pteron.player.ui.common.LoadingState
import com.pteron.player.ui.common.PteronClickableCard
import com.pteron.player.ui.common.ThumbnailImage
import com.pteron.player.ui.common.TwoLineTitle
import com.pteron.player.ui.common.bouncyClickable
import com.pteron.player.ui.folder.VideoListRow
import com.pteron.player.util.formatFileSize
import com.pteron.player.util.formatTimecode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    viewModel: PlaylistDetailViewModel,
    onBack: () -> Unit,
    /** Start playback at [video] with the playlist as the queue. */
    onPlay: (video: VideoItem, shuffle: Boolean) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var menuOpen by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }

    // Deleting the playlist (from here or elsewhere) closes this page.
    LaunchedEffect(state.notFound) { if (state.notFound) onBack() }

    Scaffold { insets ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(insets)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.padding(end = 8.dp).bouncyClickable(onClick = onBack)) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                    TwoLineTitle(subtitle = "PLAYLIST", title = state.name)
                }
                Box {
                    CircularActionButton(
                        icon = Icons.Outlined.MoreVert,
                        contentDescription = "Playlist options",
                        onClick = { menuOpen = true }
                    )
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Rename") },
                            leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                            onClick = {
                                menuOpen = false
                                showRename = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete playlist") },
                            leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                            onClick = {
                                menuOpen = false
                                showDelete = true
                            }
                        )
                    }
                }
            }

            when {
                state.isLoading -> LoadingState()
                state.videos.isEmpty() -> FullScreenMessage(
                    icon = Icons.Outlined.PlaylistRemove,
                    title = "This playlist is empty",
                    description = "Open the ⋮ menu on any video and choose Add to playlist.",
                    modifier = Modifier.fillMaxSize()
                )
                else -> {
                    val videos = state.videos
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ActionPill(
                            label = "Play all",
                            icon = Icons.Outlined.PlayArrow,
                            filled = true,
                            onClick = { onPlay(videos.first(), false) },
                            modifier = Modifier.weight(1f)
                        )
                        ActionPill(
                            label = "Shuffle",
                            icon = Icons.Outlined.Shuffle,
                            filled = false,
                            onClick = { onPlay(videos.random(), true) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Text(
                        text = "${videos.size} video${if (videos.size == 1) "" else "s"} • ${formatTimecode(videos.sumOf { it.durationMs })}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                    )
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(videos, key = { _, video -> video.id }) { index, video ->
                            // The same list row as the Videos tab; the playlist actions join its "..." menu.
                            VideoListRow(
                                video = video,
                                onClick = { onPlay(video, false) },
                                onToggleFavorite = { viewModel.toggleFavorite(video) },
                                onToggleWatched = { viewModel.toggleWatched(video) },
                                onClearProgress = { viewModel.clearProgress(video) },
                                extraMenuItems = { dismiss ->
                                    if (index > 0) {
                                        DropdownMenuItem(
                                            text = { Text("Move up") },
                                            leadingIcon = { Icon(Icons.Outlined.ArrowUpward, contentDescription = null) },
                                            onClick = { dismiss(); viewModel.move(video, -1) }
                                        )
                                    }
                                    if (index < videos.lastIndex) {
                                        DropdownMenuItem(
                                            text = { Text("Move down") },
                                            leadingIcon = { Icon(Icons.Outlined.ArrowDownward, contentDescription = null) },
                                            onClick = { dismiss(); viewModel.move(video, 1) }
                                        )
                                    }
                                    DropdownMenuItem(
                                        text = { Text("Remove from playlist") },
                                        leadingIcon = { Icon(Icons.Outlined.PlaylistRemove, contentDescription = null) },
                                        onClick = { dismiss(); viewModel.remove(video) }
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showRename) {
        PlaylistNameDialog(
            title = "Rename playlist",
            confirmLabel = "Save",
            initialName = state.name,
            onConfirm = {
                viewModel.rename(it)
                showRename = false
            },
            onDismiss = { showRename = false }
        )
    }
    if (showDelete) {
        ConfirmDeleteDialog(
            title = "Delete playlist?",
            message = "\"${state.name}\" will be deleted. Your videos are not affected.",
            onConfirm = {
                showDelete = false
                viewModel.delete()
            },
            onDismiss = { showDelete = false }
        )
    }
}

/** A pill button: accent-filled for the main action, card shade for the secondary one. */
@Composable
fun ActionPill(
    label: String,
    icon: ImageVector,
    filled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val content = if (filled) scheme.onPrimary else scheme.onSurface
    PteronClickableCard(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(percent = 50),
        color = if (filled) scheme.primary else scheme.surfaceContainer,
        borderColor = if (filled) Color.Transparent else scheme.outlineVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, color = content)
        }
    }
}

