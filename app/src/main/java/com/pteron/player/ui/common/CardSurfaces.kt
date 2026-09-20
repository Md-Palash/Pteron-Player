package com.pteron.player.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * A rounded, tappable card in the theme's medium shade with the app's press-bounce.
 *
 * The clip sits outside [bouncyClickable] so the press ripple follows the rounded corners, and
 * the background and border sit inside it so the whole card scales together on press.
 */
@Composable
fun PteronClickableCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    color: Color = MaterialTheme.colorScheme.surfaceContainer,
    borderColor: Color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(shape)
            .semantics { role = Role.Button }
            .bouncyClickable(onClick = onClick)
            .background(color, shape)
            .border(1.dp, borderColor, shape),
        content = content
    )
}
