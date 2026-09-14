package com.pteron.player.ui.library.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FlightTakeoff
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pteron.player.data.model.VideoFolder
import com.pteron.player.ui.common.bouncyClickable
import com.pteron.player.util.formatFileSize

/**
 * Folder tile styled after the Stitch "tactile 2D folder" design: a solid
 * wood-tone card with a small tab peeking out from behind the top edge, like
 * a real manila folder. The tab and body share [folderTone] exactly, so the
 * illusion holds regardless of which tone the user picks in Settings.
 */
@Composable
fun FolderCard(
    folder: VideoFolder,
    folderTone: Color,
    showVideoCount: Boolean,
    showFolderSize: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.aspectRatio(1.5f)) {
        // Folder tab, peeking out above the card's top-left corner.
        Box(
            modifier = Modifier
                .padding(start = 18.dp)
                .offset(y = (-6).dp)
                .width(44.dp)
                .height(12.dp)
                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                .background(folderTone)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.5f)
                .clip(RoundedCornerShape(18.dp))
                .background(folderTone)
                .bouncyClickable(onClick = onClick)
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = folderIconFor(folder.name),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(19.dp)
                    )
                }
                if (showVideoCount) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color.Black.copy(alpha = 0.22f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "${folder.videoCount} items",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }
            }

            Column {
                Text(
                    text = folder.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val subtitle = buildString {
                    append("${folder.videoCount} video${if (folder.videoCount == 1) "" else "s"}")
                    if (showFolderSize) append(" • ${formatFileSize(folder.totalSizeBytes)}")
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.82f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun FolderListRow(
    folder: VideoFolder,
    folderTone: Color,
    showVideoCount: Boolean,
    showFolderSize: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .bouncyClickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(folderTone),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = folderIconFor(folder.name),
                contentDescription = null,
                tint = Color.White
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = folder.name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val subtitle = buildString {
                if (showVideoCount) append("${folder.videoCount} videos")
                if (showFolderSize) {
                    if (isNotEmpty()) append(" • ")
                    append(formatFileSize(folder.totalSizeBytes))
                }
            }
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun folderIconFor(name: String): ImageVector = when {
    name.contains("camera", true) || name.contains("dcim", true) -> Icons.Outlined.PhotoCamera
    name.contains("movie", true) || name.contains("cinema", true) || name.contains("film", true) -> Icons.Outlined.Movie
    name.contains("download", true) -> Icons.Outlined.FileDownload
    name.contains("document", true) || name.contains("doc", true) -> Icons.Outlined.Explore
    name.contains("tutorial", true) || name.contains("tech", true) || name.contains("code", true) -> Icons.Outlined.Terminal
    name.contains("drone", true) -> Icons.Outlined.FlightTakeoff
    else -> Icons.Outlined.Folder
}
