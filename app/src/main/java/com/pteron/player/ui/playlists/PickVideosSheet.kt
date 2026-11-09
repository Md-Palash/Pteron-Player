package com.pteron.player.ui.playlists

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pteron.player.PteronApp
import com.pteron.player.data.model.VideoItem
import com.pteron.player.ui.common.LoadingState
import com.pteron.player.ui.common.PteronClickableCard
import com.pteron.player.ui.common.SearchCard
import com.pteron.player.ui.common.ThumbnailImage
import com.pteron.player.util.formatFileSize
import com.pteron.player.util.formatTimecode

/** Pick any number of videos from the library to put into a playlist (videos already in it are left out). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickVideosSheet(existing: Set<Long>, onAdd: (List<Long>) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { (context.applicationContext as PteronApp).mediaStoreRepository }
    val all by produceState<List<VideoItem>?>(initialValue = null) {
        value = runCatching { repository.loadAllVideos() }.getOrDefault(emptyList())
    }
    var query by remember { mutableStateOf("") }
    var picked by remember { mutableStateOf(setOf<Long>()) }
    val shown = remember(all, query, existing) {
        all.orEmpty().filter { it.id !in existing && (query.isBlank() || it.displayName.contains(query, ignoreCase = true)) }
    }
    val scheme = MaterialTheme.colorScheme

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Add videos", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                TextButton(
                    enabled = picked.isNotEmpty(),
                    onClick = {
                        onAdd(picked.toList())
                        onDismiss()
                    }
                ) { Text(if (picked.isEmpty()) "Add" else "Add (${picked.size})") }
            }
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                SearchCard(value = query, onValueChange = { query = it }, placeholder = "Search videos...")
            }
            if (all == null) {
                Box(modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp), contentAlignment = Alignment.Center) { LoadingState() }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 460.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(shown, key = { it.id }) { video ->
                        val selected = video.id in picked
                        PteronClickableCard(
                            onClick = { picked = if (selected) picked - video.id else picked + video.id },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = if (selected) scheme.primary.copy(alpha = 0.16f) else scheme.surfaceContainer
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                ThumbnailImage(
                                    contentUri = video.contentUri,
                                    modifier = Modifier.width(76.dp).size(width = 76.dp, height = 48.dp).clip(RoundedCornerShape(10.dp))
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(video.displayName, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        "${formatTimecode(video.durationMs)} \u2022 ${formatFileSize(video.sizeBytes)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = scheme.onSurfaceVariant
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(if (selected) scheme.primary else scheme.surfaceContainerHighest),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (selected) Icon(Icons.Filled.Check, contentDescription = "Selected", tint = scheme.onPrimary, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
