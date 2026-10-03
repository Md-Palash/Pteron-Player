package com.pteron.player.ui.playlists

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.PlaylistPlay
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pteron.player.PteronApp
import com.pteron.player.data.model.VideoItem
import com.pteron.player.ui.common.PteronClickableCard
import kotlinx.coroutines.launch

/**
 * The "Add to playlist" sheet opened from a video's menu. Tapping a playlist adds the video to it
 * (or removes it again if it is already inside); "New playlist" creates one that starts with this
 * video. It talks to the repository directly, so any screen can show it without extra plumbing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToPlaylistSheet(video: VideoItem, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { (context.applicationContext as PteronApp).playlistRepository }
    val playlists by repository.playlists.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var showCreate by remember { mutableStateOf(false) }

    fun toast(message: String) = Toast.makeText(context, message, Toast.LENGTH_SHORT).show()

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Add to playlist",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
            )
            PickRow(
                icon = Icons.Outlined.Add,
                label = "New playlist",
                supporting = null,
                selected = false,
                onClick = { showCreate = true }
            )
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(playlists, key = { it.id }) { playlist ->
                    val contains = video.id in playlist.videoIds
                    PickRow(
                        icon = Icons.Outlined.PlaylistPlay,
                        label = playlist.name,
                        supporting = "${playlist.videoIds.size} video${if (playlist.videoIds.size == 1) "" else "s"}",
                        selected = contains,
                        onClick = {
                            scope.launch {
                                if (contains) {
                                    repository.removeVideo(playlist.id, video.id)
                                    toast("Removed from ${playlist.name}")
                                } else {
                                    repository.addVideo(playlist.id, video.id)
                                    toast("Added to ${playlist.name}")
                                }
                                onDismiss()
                            }
                        }
                    )
                }
            }
        }
    }

    if (showCreate) {
        PlaylistNameDialog(
            title = "New playlist",
            confirmLabel = "Create",
            onConfirm = { name ->
                showCreate = false
                scope.launch {
                    repository.create(name, firstVideoId = video.id)
                    toast("Added to ${name.trim()}")
                    onDismiss()
                }
            },
            onDismiss = { showCreate = false }
        )
    }
}

/** Same card language as the other selector sheets: medium shade normally, accent when selected. */
@Composable
private fun PickRow(
    icon: ImageVector,
    label: String,
    supporting: String?,
    selected: Boolean,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val content = if (selected) scheme.onPrimary else scheme.onSurface
    PteronClickableCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) scheme.primary else scheme.surfaceContainer,
        borderColor = if (selected) Color.Transparent else scheme.outlineVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(22.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    color = content,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (supporting != null) {
                    Text(supporting, style = MaterialTheme.typography.bodySmall, color = content.copy(alpha = 0.75f))
                }
            }
            if (selected) {
                Box(
                    modifier = Modifier.size(20.dp).clip(CircleShape).background(content.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Check, contentDescription = "In this playlist", tint = content, modifier = Modifier.size(13.dp))
                }
            }
        }
    }
}
