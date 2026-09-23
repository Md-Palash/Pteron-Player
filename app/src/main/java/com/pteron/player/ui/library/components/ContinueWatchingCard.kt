package com.pteron.player.ui.library.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.pteron.player.util.formatTimecode

/**
 * Square card (the one card shape in the app that isn't rectangular) -- the thumbnail fills the
 * whole square edge to edge, with a bottom scrim carrying the title, remaining time and a thin
 * resume-progress bar, plus a centered play affordance.
 */
@Composable
fun ContinueWatchingCard(video: VideoItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val remaining = (video.durationMs - video.lastPositionMs).coerceAtLeast(0)

    Box(
        modifier = modifier
            .width(180.dp)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .bouncyClickable(onClick = onClick)
    ) {
        ThumbnailImage(contentUri = video.contentUri, modifier = Modifier.fillMaxSize())

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(10.dp)
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = "Resume", tint = MaterialTheme.colorScheme.onPrimary)
            }
        }

        // Bottom scrim keeps the title/time readable over any thumbnail without a hard edge.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f))
                    )
                )
                .padding(top = 20.dp, start = 10.dp, end = 10.dp, bottom = 8.dp)
        ) {
            Text(
                text = video.displayName,
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${formatTimecode(remaining)} left",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.85f)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.3f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(video.progressFraction)
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}
