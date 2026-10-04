package com.pteron.player.ui.folder

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PlaylistAdd
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pteron.player.data.model.VideoItem
import com.pteron.player.ui.common.ThumbnailImage
import com.pteron.player.ui.common.bouncyClickable
import com.pteron.player.util.formatFileSize
import com.pteron.player.util.formatPercent
import com.pteron.player.util.formatTimecode

/**
 * Every video is a rectangular card, corners rounded a bit (lighter than the Settings cards, so
 * the two read as different tiers). The thumbnail sits flush against the card's own edges and
 * shares its corner radius on that side, so it reads as part of the card rather than a separate
 * image dropped on top of it -- only the text underneath/beside it gets its own inset padding.
 */
private const val CardRadiusDp = 18
private val VideoCardShape = RoundedCornerShape(CardRadiusDp.dp)

/** List rows: the thumbnail is inset from the card edge by this much, with its own rounded corners. */
private val ListThumbInset = 8.dp
private val ListThumbShape = RoundedCornerShape(12.dp)

/** Grid tiles: the thumbnail fills the whole card; width / height of the tile. */
private const val GridTileAspect = 1.45f

/** Card chrome shared by the list row and the grid tile: rounded, medium shade, soft outline. */
@Composable
private fun Modifier.videoCard(onClick: () -> Unit): Modifier = this
    .clip(VideoCardShape)
    .bouncyClickable(onClick = onClick)
    .background(MaterialTheme.colorScheme.surfaceContainer, VideoCardShape)
    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), VideoCardShape)

@Composable
fun VideoListRow(
    video: VideoItem,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleWatched: () -> Unit,
    onClearProgress: () -> Unit,
    onAddToPlaylist: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(104.dp)
            .videoCard(onClick)
            .padding(ListThumbInset)
    ) {
        Box(
            modifier = Modifier
                .width(132.dp)
                .fillMaxHeight()
                .clip(ListThumbShape)
        ) {
            ThumbnailImage(contentUri = video.contentUri, modifier = Modifier.fillMaxSize())
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text(formatTimecode(video.durationMs), style = MaterialTheme.typography.labelSmall, color = Color.White)
            }
            if (video.isWatched) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                        .size(20.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.tertiaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(13.dp)
                    )
                }
            } else if (video.lastPositionMs == 0L) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .padding(horizontal = 6.dp, vertical = 1.dp)
                ) {
                    Text("NEW", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
        }

        Column(modifier = Modifier.weight(1f).padding(start = 12.dp, end = 4.dp, top = 2.dp, bottom = 2.dp)) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = video.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onToggleFavorite, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = if (video.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (video.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
                VideoOverflowMenu(
                    video = video,
                    onToggleWatched = onToggleWatched,
                    onClearProgress = onClearProgress,
                    onAddToPlaylist = onAddToPlaylist
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (video.isFourK) MetaBadge("4K")
                video.mimeType.substringAfter('/').uppercase().takeIf { it.isNotBlank() }?.let { MetaBadge(it) }
            }
            Text(
                text = formatFileSize(video.sizeBytes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            VideoStatusRow(video)
        }
    }
}

@Composable
private fun VideoStatusRow(video: VideoItem) {
    when {
        video.isWatched -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(14.dp))
            Text("Watched", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        video.lastPositionMs > 0L -> Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    Icon(Icons.Outlined.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(13.dp))
                    Text(
                        "Resume ${formatTimecode(video.lastPositionMs)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(formatPercent(video.progressFraction), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            androidx.compose.foundation.layout.Spacer(Modifier.height(3.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                Box(Modifier.fillMaxWidth(video.progressFraction).fillMaxSize().clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.primary))
            }
        }
        else -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(modifier = Modifier.size(6.dp).clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.primary))
            Text("Unwatched", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun VideoGridTile(
    video: VideoItem,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleWatched: () -> Unit,
    onClearProgress: () -> Unit,
    onAddToPlaylist: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // The thumbnail fills the whole card; the title and details sit on a bottom scrim above it.
    Box(modifier = modifier.aspectRatio(GridTileAspect).videoCard(onClick)) {
        ThumbnailImage(contentUri = video.contentUri, modifier = Modifier.fillMaxSize())

        // Bottom scrim: keeps the text readable on any thumbnail without a hard edge.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.78f))))
                .padding(start = 10.dp, end = 10.dp, top = 26.dp, bottom = 9.dp)
        ) {
            Text(
                video.displayName,
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = if (video.lastPositionMs > 0L && !video.isWatched) {
                        "Resume ${formatTimecode(video.lastPositionMs)} • ${formatPercent(video.progressFraction)}"
                    } else {
                        formatFileSize(video.sizeBytes)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.55f))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(formatTimecode(video.durationMs), style = MaterialTheme.typography.labelSmall, color = Color.White)
                }
            }
        }

        if (video.progressFraction in 0.01f..0.98f && !video.isWatched) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .align(Alignment.BottomCenter)
                    .background(Color.White.copy(alpha = 0.3f))
            ) {
                Box(Modifier.fillMaxWidth(video.progressFraction).fillMaxSize().background(MaterialTheme.colorScheme.primary))
            }
        }

        if (video.isWatched) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .size(18.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.tertiaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.size(12.dp))
            }
        } else if (video.lastPositionMs == 0L) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(horizontal = 6.dp, vertical = 1.dp)
            ) {
                Text("NEW", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }
        Row(
            modifier = Modifier.align(Alignment.TopEnd).padding(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onToggleFavorite, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = if (video.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (video.isFavorite) MaterialTheme.colorScheme.primary else Color.White,
                    modifier = Modifier.size(15.dp)
                )
            }
            VideoOverflowMenu(
                video = video,
                onToggleWatched = onToggleWatched,
                onClearProgress = onClearProgress,
                onAddToPlaylist = onAddToPlaylist,
                tint = Color.White
            )
        }
    }
}

/** Shared "..." menu for watched/progress actions, used by both the list row and grid tile
 *  (favorite has its own dedicated quick-toggle button in both, since it's the most common action). */
@Composable
private fun VideoOverflowMenu(
    video: VideoItem,
    onToggleWatched: () -> Unit,
    onClearProgress: () -> Unit,
    onAddToPlaylist: (() -> Unit)? = null,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }, modifier = Modifier.size(28.dp)) {
            Icon(
                imageVector = Icons.Outlined.MoreVert,
                contentDescription = "More options",
                tint = tint,
                modifier = Modifier.size(16.dp)
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (onAddToPlaylist != null) {
                DropdownMenuItem(
                    text = { Text("Add to playlist") },
                    leadingIcon = { Icon(Icons.Outlined.PlaylistAdd, contentDescription = null) },
                    onClick = {
                        onAddToPlaylist()
                        expanded = false
                    }
                )
            }
            DropdownMenuItem(
                text = { Text(if (video.isWatched) "Mark as unwatched" else "Mark as watched") },
                leadingIcon = {
                    Icon(
                        if (video.isWatched) Icons.Outlined.VisibilityOff else Icons.Filled.Check,
                        contentDescription = null
                    )
                },
                onClick = {
                    onToggleWatched()
                    expanded = false
                }
            )
            if (video.lastPositionMs > 0L) {
                DropdownMenuItem(
                    text = { Text("Clear progress") },
                    leadingIcon = { Icon(Icons.Outlined.RemoveCircleOutline, contentDescription = null) },
                    onClick = {
                        onClearProgress()
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun MetaBadge(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
            .padding(horizontal = 4.dp, vertical = 1.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
    }
}
