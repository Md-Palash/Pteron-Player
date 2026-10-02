package com.pteron.player.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

private val SetOuterRadius = 28.dp
private val SetInnerRadius = 4.dp

/** The hairline between two cards of the same set. */
private val SetGap = 2.dp

/**
 * The shape of card [index] in a set of [count] cards: the first card is rounded on its top
 * side only, the last on its bottom side only, every card in between is (almost) rectangular.
 * A set with a single card is rounded all around.
 */
fun setItemShape(index: Int, count: Int): Shape = when {
    count <= 1 -> RoundedCornerShape(SetOuterRadius)
    index == 0 -> RoundedCornerShape(SetOuterRadius, SetOuterRadius, SetInnerRadius, SetInnerRadius)
    index == count - 1 -> RoundedCornerShape(SetInnerRadius, SetInnerRadius, SetOuterRadius, SetOuterRadius)
    else -> RoundedCornerShape(SetInnerRadius)
}

/**
 * A "set": [count] cards stacked with a hairline gap, shaped by [setItemShape] so they read as one
 * rounded block made of separate cards. Any number of cards works (3, 4, 5, 6 ...). When the
 * Settings list grows, split it into several sets and place them one under another.
 */
@Composable
fun SettingsSet(
    count: Int,
    modifier: Modifier = Modifier,
    item: @Composable (index: Int, shape: Shape) -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SetGap)
    ) {
        repeat(count) { index -> item(index, setItemShape(index, count)) }
    }
}

/** A card of a set in the theme's card shade (or [color]); tappable when [onClick] is given. */
@Composable
fun SettingsSetCard(
    shape: Shape,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    color: Color = MaterialTheme.colorScheme.surfaceContainer,
    content: @Composable BoxScope.() -> Unit
) {
    val base = modifier
        .fillMaxWidth()
        .clip(shape)
        .let { if (onClick != null) it.semantics { role = Role.Button }.bouncyClickable(pressedScale = 0.985f, onClick = onClick) else it }
        .background(color, shape)
    Box(modifier = base, content = content)
}

/** The standard settings row: round icon bubble, title, one-line description, chevron. */
@Composable
fun SettingsSetRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    shape: Shape,
    onClick: () -> Unit
) {
    SettingsSetCard(shape = shape, onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
