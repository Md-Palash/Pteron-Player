package com.pteron.player.ui.common

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest

/**
 * Displays a cached MediaStore video thumbnail (see [com.pteron.player.data.media.VideoThumbnailFetcher]).
 *
 * A placeholder glyph sits behind the image, so a tile is never blank while loading or on failure;
 * once the picture arrives it fades in over the glyph. Two things keep lists smooth:
 *  - the request is remembered per Uri instead of being rebuilt on every recomposition, and
 *  - plain [AsyncImage] is used rather than SubcomposeAsyncImage, whose sub-composition per item is
 *    noticeably more expensive while a list is flinging.
 */
@Composable
fun ThumbnailImage(contentUri: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val request = remember(contentUri) {
        ImageRequest.Builder(context)
            .data(Uri.parse(contentUri))
            .crossfade(150)
            .build()
    }
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Movie,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline
        )
        AsyncImage(
            model = request,
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop
        )
    }
}
