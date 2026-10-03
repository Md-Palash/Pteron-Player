package com.pteron.player.ui.playlists

import android.content.ContentUris
import android.provider.MediaStore
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PlaylistPlay
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pteron.player.data.model.Playlist
import com.pteron.player.navigation.BottomNavDestination
import com.pteron.player.ui.common.CircularActionButton
import com.pteron.player.ui.common.DarkModeButton
import com.pteron.player.ui.common.FullScreenMessage
import com.pteron.player.ui.common.LoadingState
import com.pteron.player.ui.common.PteronBottomNavBar
import com.pteron.player.ui.common.PteronClickableCard
import com.pteron.player.ui.common.ThumbnailImage
import com.pteron.player.ui.common.TwoLineTitle

/** Extra bottom padding so scrollable content never sits behind the floating nav pill. */
private val BottomNavClearance = 108.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistsScreen(
    viewModel: PlaylistsViewModel,
    onOpenPlaylist: (Long) -> Unit,
    onNavigate: (BottomNavDestination) -> Unit,
    onToggleDarkMode: () -> Unit
) {
    val playlists by viewModel.playlists.collectAsState()
    var showCreate by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<Playlist?>(null) }
    var deleteTarget by remember { mutableStateOf<Playlist?>(null) }

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
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CircularActionButton(
                            icon = Icons.Outlined.Add,
                            contentDescription = "New playlist",
                            onClick = { showCreate = true }
                        )
                        DarkModeButton(onClick = onToggleDarkMode)
                    }
                }

                val list = playlists
                when {
                    list == null -> LoadingState()
                    list.isEmpty() -> FullScreenMessage(
                        icon = Icons.Outlined.PlaylistPlay,
                        title = "No playlists yet",
                        description = "Create a playlist, then add videos from the ⋮ menu on any video.",
                        actionLabel = "New playlist",
                        onAction = { showCreate = true },
                        modifier = Modifier.fillMaxSize()
                    )
                    else -> LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = BottomNavClearance),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(list, key = { it.id }) { playlist ->
                            PlaylistCard(
                                playlist = playlist,
                                onClick = { onOpenPlaylist(playlist.id) },
                                onRename = { renameTarget = playlist },
                                onDelete = { deleteTarget = playlist }
                            )
                        }
                    }
                }
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

    if (showCreate) {
        PlaylistNameDialog(
            title = "New playlist",
            confirmLabel = "Create",
            onConfirm = {
                viewModel.create(it)
                showCreate = false
            },
            onDismiss = { showCreate = false }
        )
    }
    renameTarget?.let { target ->
        PlaylistNameDialog(
            title = "Rename playlist",
            confirmLabel = "Save",
            initialName = target.name,
            onConfirm = {
                viewModel.rename(target.id, it)
                renameTarget = null
            },
            onDismiss = { renameTarget = null }
        )
    }
    deleteTarget?.let { target ->
        ConfirmDeleteDialog(
            title = "Delete playlist?",
            message = "\"${target.name}\" will be deleted. Your videos are not affected.",
            onConfirm = {
                viewModel.delete(target.id)
                deleteTarget = null
            },
            onDismiss = { deleteTarget = null }
        )
    }
}

@Composable
private fun PlaylistCard(
    playlist: Playlist,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    PteronClickableCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PlaylistCover(firstVideoId = playlist.videoIds.firstOrNull())
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    playlist.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val count = playlist.videoIds.size
                Text(
                    "$count video${if (count == 1) "" else "s"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = "More options")
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            onRename()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

/** The first video's thumbnail, or an accent tile with a playlist glyph while the playlist is empty. */
@Composable
private fun PlaylistCover(firstVideoId: Long?) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = Modifier.width(96.dp).height(60.dp).clip(shape),
        contentAlignment = Alignment.Center
    ) {
        if (firstVideoId != null) {
            ThumbnailImage(
                contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, firstVideoId).toString(),
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.PlaylistPlay,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}
