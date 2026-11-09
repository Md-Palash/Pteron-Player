package com.pteron.player.ui.folder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pteron.player.PteronApp
import com.pteron.player.data.model.VideoItem
import com.pteron.player.ui.common.PteronClickableCard

private data class FolderTarget(val path: String, val name: String, val count: Int)

/**
 * Pick where the selected videos go: an existing folder, or a new one (it is created when the
 * videos are moved in -- Android folders only exist while they hold a file).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoveToFolderSheet(videos: List<VideoItem>, onPick: (relativePath: String) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { (context.applicationContext as PteronApp).mediaStoreRepository }
    val all by produceState<List<VideoItem>?>(initialValue = null) {
        value = runCatching { repository.loadAllVideos() }.getOrDefault(emptyList())
    }
    val here = remember(videos) { videos.map { it.relativePath }.toSet() }
    val targets = remember(all) {
        all.orEmpty()
            .filter { it.relativePath.isNotBlank() }
            .groupBy { it.relativePath }
            .map { (path, items) ->
                FolderTarget(path, items.first().bucketName.ifBlank { path.trimEnd('/').substringAfterLast('/') }, items.size)
            }
            .filter { !(here.size == 1 && it.path == here.first()) }
            .sortedBy { it.name.lowercase() }
    }
    var showNew by remember { mutableStateOf(false) }
    val label = "${videos.size} video${if (videos.size == 1) "" else "s"}"

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Move $label to...",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
            )
            TargetRow(Icons.Outlined.CreateNewFolder, "New folder", null) { showNew = true }
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(targets, key = { it.path }) { target ->
                    TargetRow(Icons.Outlined.Folder, target.name, "${target.count} video${if (target.count == 1) "" else "s"}") {
                        onPick(target.path)
                    }
                }
            }
        }
    }

    if (showNew) {
        var name by remember { mutableStateOf("") }
        val clean = name.replace(Regex("[\\\\/:*?\"<>|]"), "").trim().take(50)
        AlertDialog(
            onDismissRequest = { showNew = false },
            title = { Text("New folder") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(50) },
                    singleLine = true,
                    placeholder = { Text("Folder name") }
                )
            },
            confirmButton = {
                TextButton(enabled = clean.isNotEmpty(), onClick = {
                    showNew = false
                    onPick("Movies/$clean/")
                }) { Text("Create and move") }
            },
            dismissButton = { TextButton(onClick = { showNew = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun TargetRow(icon: ImageVector, label: String, supporting: String?, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    PteronClickableCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, contentDescription = null, tint = scheme.primary, modifier = Modifier.size(22.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (supporting != null) {
                    Text(supporting, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                }
            }
        }
    }
}
