package com.pteron.player.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.pteron.player.R

/**
 * The app's launcher icon, cropped to a circle.
 *
 * `painterResource` can't load adaptive-icon XML, so the launcher drawable is rendered once, at
 * exactly the size it is shown, into a small bitmap that is then remembered.
 */
@Composable
fun AppIcon(modifier: Modifier = Modifier, size: Dp = 36.dp) {
    val context = LocalContext.current
    val sizePx = with(LocalDensity.current) { size.roundToPx() }
    val bitmap = remember(sizePx) {
        ContextCompat.getDrawable(context, R.mipmap.ic_launcher)
            ?.toBitmap(sizePx, sizePx)
            ?.asImageBitmap()
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = "Pteron Player",
            modifier = modifier.size(size).clip(CircleShape)
        )
    }
}
