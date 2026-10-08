package com.pteron.player.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pteron.player.R
import androidx.compose.material3.MaterialTheme

/**
 * The Pteron player logo (the rounded tile with the feather and play mark).
 *
 * It is a vector drawable, so it is drawn crisp at any size and costs no bitmap memory -- the old
 * version rasterized the launcher icon into a bitmap on every size change.
 */
@Composable
fun AppIcon(modifier: Modifier = Modifier, size: Dp = 36.dp) {
    Image(
        painter = painterResource(R.drawable.ic_logo_player),
        contentDescription = "Pteron Player",
        modifier = modifier.size(size)
    )
}

/**
 * The "Pteron" wordmark. It is two vector layers on the same canvas: the letters (tinted with the
 * theme's text color, so they stay readable on dark themes) and the emerald "o" ring with the
 * feather quill (always in its own colors).
 */
@Composable
fun PteronWordmark(modifier: Modifier = Modifier, height: Dp = 40.dp) {
    Box(modifier = modifier.height(height).aspectRatio(WordmarkAspect)) {
        Image(
            painter = painterResource(R.drawable.ic_wordmark_text),
            contentDescription = "Pteron",
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Fit,
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground)
        )
        Image(
            painter = painterResource(R.drawable.ic_wordmark_mark),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Fit
        )
    }
}

/**
 * The logo tile and the wordmark as one lockup. The wordmark drawable is 156 units tall but its
 * letters only occupy the lower part (about y 85..149, centre at ~75% of the height), the rest is
 * the feather rising above them. The wordmark is therefore lifted by 25% of its height so the
 * centre of the letters lines up with the centre of the logo tile.
 */
@Composable
fun PteronBrand(modifier: Modifier = Modifier) {
    val logoSize = 44.dp
    val wordmarkHeight = 40.dp
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AppIcon(size = logoSize)
        PteronWordmark(modifier = Modifier.offset(y = -(wordmarkHeight * 0.25f)), height = wordmarkHeight)
    }
}

/** Width / height of the wordmark drawables' viewport (320 x 156). */
private const val WordmarkAspect = 320f / 156f
