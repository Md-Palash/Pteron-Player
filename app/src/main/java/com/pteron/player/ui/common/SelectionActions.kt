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
import androidx.compose.material.icons.outlined.Close
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

/** Replaces a screen's header while videos are selected: close, count, and the batch actions. */
@Composable
fun SelectionBar(
    count: Int,
    onClose: () -> Unit,
    onSelectAll: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onShare: () -> Unit,
    onDelete: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val size = 36.dp
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CircularActionButton(Icons.Outlined.Close, "Cancel selection", onClose, size = size)
            TwoLineTitle(subtitle = "SELECTED", title = "$count video${if (count == 1) "" else "s"}")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            CircularActionButton(Icons.Outlined.SelectAll, "Select all", onSelectAll, size = size)
            CircularActionButton(Icons.Outlined.PlaylistAdd, "Add to playlist", onAddToPlaylist, size = size)
            CircularActionButton(Icons.Outlined.Share, "Share", onShare, size = size)
            if (onDelete != null) CircularActionButton(Icons.Outlined.DeleteOutline, "Delete", onDelete, size = size)
        }
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
