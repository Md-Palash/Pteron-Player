package com.pteron.player.ui.screens.home

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color

/**
 * A deliberately simple, flat folder silhouette: a small tab behind a
 * rounded body, both filled with the folder's chosen color. No
 * gradients, shadows, or illustrated detail — the shape alone should
 * read as "this holds your videos".
 */
@Composable
fun FolderIcon(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val tabHeight = size.height * 0.16f
        val tabWidth = size.width * 0.5f
        val tabCorner = CornerRadius(size.width * 0.06f, size.width * 0.06f)
        val bodyCorner = CornerRadius(size.width * 0.10f, size.width * 0.10f)

        drawRoundRect(
            color = color,
            topLeft = Offset.Zero,
            size = Size(tabWidth, tabHeight + tabCorner.x),
            cornerRadius = tabCorner
        )

        drawRoundRect(
            color = color,
            topLeft = Offset(0f, tabHeight),
            size = Size(size.width, size.height - tabHeight),
            cornerRadius = bodyCorner
        )
    }
}
