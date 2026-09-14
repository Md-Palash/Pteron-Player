package com.pteron.player.ui.player.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/** Brightness HUD: icon, fill bar, and a percentage -- shown only while the user is
 *  actively dragging on the left half of the screen. */
@Composable
fun BrightnessHud(level: Float, visible: Boolean, modifier: Modifier = Modifier) {
    GestureHud(
        visible = visible,
        icon = Icons.Filled.BrightnessMedium,
        iconTint = MaterialTheme.colorScheme.tertiary,
        fillColor = MaterialTheme.colorScheme.tertiary,
        fraction = level,
        label = "${(level * 100).roundToInt()}%",
        modifier = modifier
    )
}

/** Volume HUD: icon, fill bar, and "current/max" -- matches the on-device volume steps
 *  rather than a percentage, since that's what the user is actually changing. */
@Composable
fun VolumeHud(fraction: Float, current: Int, max: Int, visible: Boolean, modifier: Modifier = Modifier) {
    GestureHud(
        visible = visible,
        icon = if (current == 0) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
        iconTint = MaterialTheme.colorScheme.primary,
        fillColor = MaterialTheme.colorScheme.primary,
        fraction = fraction,
        label = "$current/$max",
        modifier = modifier
    )
}

@Composable
private fun GestureHud(
    visible: Boolean,
    icon: ImageVector,
    iconTint: Color,
    fillColor: Color,
    fraction: Float,
    label: String,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut(), modifier = modifier) {
        Column(
            modifier = Modifier
                .width(44.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.height(20.dp))
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(96.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.25f)),
                contentAlignment = Alignment.BottomCenter
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(fraction.coerceIn(0f, 1f))
                        .clip(RoundedCornerShape(50))
                        .background(fillColor)
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White)
        }
    }
}
