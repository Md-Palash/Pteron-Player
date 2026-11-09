package com.pteron.player.ui.common

import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import android.content.ContentValues
import android.widget.Toast
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.PlaylistAdd
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.Share
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pteron.player.data.model.VideoItem

/** Adds [id] to the set, or removes it when it is already inside. */
fun Set<Long>.toggled(id: Long): Set<Long> = if (id in this) this - id else this + id

/**
 * Replaces a screen's header while videos are selected. The tick on the right selects everything
 * when it is off and clears the selection (leaving selection mode) when it is on.
 */
@Composable
fun SelectionBar(
    count: Int,
    allSelected: Boolean,
    onClose: () -> Unit,
    onSelectAll: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onMove: (() -> Unit)?,
    onShare: () -> Unit,
    onDelete: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val size = 42.dp
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TwoLineTitle(subtitle = "SELECTED", title = "$count")
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
            SelectAllTick(allSelected, size) { if (allSelected) onClose() else onSelectAll() }
            CircularActionButton(Icons.Outlined.PlaylistAdd, "Add to playlist", onAddToPlaylist, size = size)
            if (onMove != null) CircularActionButton(Icons.Outlined.FolderOpen, "Move to folder", onMove, size = size)
            CircularActionButton(Icons.Outlined.Share, "Share", onShare, size = size)
            if (onDelete != null) CircularActionButton(Icons.Outlined.DeleteOutline, "Delete", onDelete, size = size)
        }
    }
}

/** A tick in a circle: filled while everything is selected, an empty ring otherwise. */
@Composable
private fun SelectAllTick(checked: Boolean, size: Dp, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .semantics {
                role = Role.Checkbox
                contentDescription = if (checked) "Clear selection" else "Select all"
            }
            .bouncyClickable(onClick = onClick)
            .background(if (checked) scheme.primary else scheme.surfaceContainer)
            .border(1.5.dp, if (checked) scheme.primary else scheme.outline, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = null,
            tint = if (checked) scheme.onPrimary else scheme.onSurfaceVariant,
            modifier = Modifier.size(size * 0.5f)
        )
    }
}

/** Opens the system share sheet for one or many videos. */
fun shareVideos(context: Context, videos: List<VideoItem>) {
    if (videos.isEmpty()) return
    runCatching {
        val uris = ArrayList(videos.map { Uri.parse(it.contentUri) })
        val send = if (uris.size == 1) {
            Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_STREAM, uris[0])
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
        }
        val clip = ClipData.newRawUri("Videos", uris.first())
        uris.drop(1).forEach { clip.addItem(ClipData.Item(it)) }
        send.type = "video/*"
        send.clipData = clip
        send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(Intent.createChooser(send, "Share").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

/**
 * Returns a function that asks Android to delete the given videos (the system shows its own
 * confirmation), or null below Android 11 where apps cannot delete other apps' media this way.
 * [onDeleted] runs only when the person confirmed.
 */
@Composable
fun rememberDeleteVideos(onDeleted: () -> Unit): ((List<VideoItem>) -> Unit)? {
    val context = LocalContext.current
    val onDeletedNow by rememberUpdatedState(onDeleted)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) onDeletedNow()
    }
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
    return remember(context, launcher) {
        { videos: List<VideoItem> ->
            if (videos.isNotEmpty()) {
                runCatching {
                    val request = MediaStore.createDeleteRequest(
                        context.contentResolver,
                        videos.map { Uri.parse(it.contentUri) }
                    )
                    launcher.launch(IntentSenderRequest.Builder(request.intentSender).build())
                }
            }
            Unit
        }
    }
}

/**
 * Returns a function that moves videos into the folder at [relativePath] (e.g. "Movies/Trips/",
 * created on the fly when it does not exist yet). Android asks once for permission to change the
 * files; this needs Android 11+, so below that it returns null.
 */
@Composable
fun rememberMoveVideos(onMoved: () -> Unit): ((List<VideoItem>, String) -> Unit)? {
    val context = LocalContext.current
    val onMovedNow by rememberUpdatedState(onMoved)
    val scope = rememberCoroutineScope()
    var pending by remember { mutableStateOf<Pair<List<VideoItem>, String>?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        val job = pending
        pending = null
        if (result.resultCode == Activity.RESULT_OK && job != null) {
            scope.launch {
                val moved = withContext(Dispatchers.IO) {
                    val values = ContentValues().apply { put(MediaStore.MediaColumns.RELATIVE_PATH, job.second) }
                    job.first.count { video ->
                        runCatching { context.contentResolver.update(Uri.parse(video.contentUri), values, null, null) }
                            .getOrDefault(0) > 0
                    }
                }
                Toast.makeText(context, "Moved $moved video${if (moved == 1) "" else "s"}", Toast.LENGTH_SHORT).show()
                onMovedNow()
            }
        }
    }
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
    return remember(context, launcher) {
        { videos: List<VideoItem>, destination: String ->
            if (videos.isNotEmpty()) {
                runCatching {
                    val request = MediaStore.createWriteRequest(
                        context.contentResolver,
                        videos.map { Uri.parse(it.contentUri) }
                    )
                    pending = videos to destination
                    launcher.launch(IntentSenderRequest.Builder(request.intentSender).build())
                }
            }
            Unit
        }
    }
}
