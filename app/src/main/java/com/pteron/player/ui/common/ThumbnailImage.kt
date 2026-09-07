package com.pteron.player.ui.common

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest

/**
 * Displays a cached MediaStore video thumbnail (see [com.pteron.player.data.media.VideoThumbnailFetcher]),
 * falling back to a placeholder glyph while loading or on failure -- never a blank tile.
 */
@Composable
fun ThumbnailImage(contentUri: String, modifier: Modifier = Modifier) {
    SubcomposeAsyncImage(
        model = ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
            .data(Uri.parse(contentUri))
            .crossfade(150)
            .build(),
        contentDescription = null,
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentScale = ContentScale.Crop
    ) {
        val state = painter.state
        when (state) {
            is coil.compose.AsyncImagePainter.State.Success -> SubcomposeAsyncImageContent()
            else -> androidx.compose.foundation.layout.Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Movie,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}
