package com.pteron.player.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import kotlinx.coroutines.launch
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.shadow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.pteron.player.data.model.SortDirection
import com.pteron.player.data.model.SortOption
import com.pteron.player.data.model.VideoFilter

/**
 * Lays [pills] over the top of [content] with no background of their own, so whatever scrolls
 * underneath passes behind the floating buttons and disappears at the top edge of this area
 * (right under the screen header). [content] receives the height of the pill row, to use as the
 * top content padding of its list so the first item starts below the pills.
 */
@Composable
fun FloatingPills(
    modifier: Modifier = Modifier,
    pills: @Composable () -> Unit,
    content: @Composable (topPadding: Dp) -> Unit
) {
    val density = LocalDensity.current
    // A close first estimate, so the list does not jump when the real height is measured.
    var pillsHeight by remember { mutableStateOf(58.dp) }
    Box(modifier = modifier) {
        content(pillsHeight)
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .onSizeChanged { pillsHeight = with(density) { it.height.toDp() } }
        ) { pills() }
    }
}

// --- Filter cards -----------------------------------------------------------------------------

@Composable
fun FilterCardsRow(active: VideoFilter, onSelect: (VideoFilter) -> Unit, modifier: Modifier = Modifier) {
    val filters = VideoFilter.entries
    val labels = remember { filters.map { it.label } }
    SlidingPillRow(
        labels = labels,
        selectedIndex = filters.indexOf(active),
        onSelect = { onSelect(filters[it]) },
        modifier = modifier
    )
}

/**
 * A row of pill buttons with one accent pill that glides (with a tiny spring overshoot) from the
 * old selection to the new one, instead of the colour simply swapping. The pills' backgrounds and
 * the gliding pill are painted in one draw pass behind the labels, so nothing is recomposed while
 * it moves. Scrolls sideways if the pills do not fit, and keeps the selected one in view.
 */
@Composable
fun SlidingPillRow(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val card = scheme.surfaceContainer
    val accent = scheme.primary
    val density = LocalDensity.current
    val scroll = rememberScrollState()
    val bounds = remember(labels.size) {
        mutableStateListOf<Rect?>().apply { repeat(labels.size) { add(null) } }
    }
    val indicatorX = remember { Animatable(0f) }
    val indicatorW = remember { Animatable(0f) }
    var placed by remember { mutableStateOf(false) }
    var viewport by remember { mutableIntStateOf(0) }
    val startPad = with(density) { 16.dp.toPx() }
    val target = bounds.getOrNull(selectedIndex)

    LaunchedEffect(target) {
        val t = target ?: return@LaunchedEffect
        if (!placed) {
            // First measurement: appear in place instead of gliding in from the left edge.
            indicatorX.snapTo(t.left)
            indicatorW.snapTo(t.width)
            placed = true
        } else {
            val glide = spring<Float>(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow)
            launch { indicatorX.animateTo(t.left, glide) }
            launch { indicatorW.animateTo(t.width, glide) }
        }
    }
    LaunchedEffect(target, viewport) {
        val t = target ?: return@LaunchedEffect
        if (viewport == 0) return@LaunchedEffect
        val left = t.left + startPad
        val right = t.right + startPad
        if (left < scroll.value || right > scroll.value + viewport) {
            scroll.animateScrollTo(((left + right) / 2f - viewport / 2f).toInt().coerceIn(0, scroll.maxValue))
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { viewport = it.width }
            .horizontalScroll(scroll)
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .drawBehind {
                    bounds.forEach { b ->
                        if (b != null) {
                            drawRoundRect(
                                card,
                                topLeft = Offset(b.left, b.top),
                                size = Size(b.width, b.height),
                                cornerRadius = CornerRadius(b.height / 2f)
                            )
                        }
                    }
                    val ref = bounds.getOrNull(selectedIndex) ?: bounds.firstOrNull { it != null }
                    if (placed && ref != null) {
                        drawRoundRect(
                            accent,
                            topLeft = Offset(indicatorX.value, ref.top),
                            size = Size(indicatorW.value, ref.height),
                            cornerRadius = CornerRadius(ref.height / 2f)
                        )
                    }
                }
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                labels.forEachIndexed { index, label ->
                    PillLabel(
                        label = label,
                        selected = index == selectedIndex,
                        onClick = { onSelect(index) },
                        onBounds = { r -> if (bounds.getOrNull(index) != r && index < bounds.size) bounds[index] = r }
                    )
                }
            }
        }
    }
}

@Composable
private fun PillLabel(label: String, selected: Boolean, onClick: () -> Unit, onBounds: (Rect) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val color by animateColorAsState(
        targetValue = if (selected) scheme.onPrimary else scheme.onSurface,
        animationSpec = tween(200),
        label = "pillText"
    )
    Box(
        modifier = Modifier
            .onGloballyPositioned { onBounds(it.boundsInParent()) }
            .clip(RoundedCornerShape(percent = 50))
            .semantics { role = Role.Tab }
            .bouncyClickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 11.dp)
    ) {
        Text(label, style = MaterialTheme.typography.titleSmall, color = color)
    }
}

// --- Sort button (top-right, shared across every tab that has sortable content) ---------------

/**
 * The floating circular sort icon that lives in the top-right icon row, same as the view-mode and
 * dark-mode buttons. Tapping it opens the same rounded sort menu the old inline dropdown used,
 * just anchored to this button instead of a card in the content itself.
 */
@Composable
fun TopBarSortButton(
    selectedOption: SortOption,
    direction: SortDirection,
    onSelect: (SortOption, SortDirection) -> Unit,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = DefaultTopButtonSize
) {
    var expanded by remember { mutableStateOf(false) }
    val gapPx = with(LocalDensity.current) { 6.dp.roundToPx() }
    val positionProvider = remember(gapPx) { BelowAnchorEndPositionProvider(gapPx) }

    Box(modifier = modifier) {
        CircularActionButton(
            icon = Icons.Outlined.Sort,
            contentDescription = "Sort: ${selectedOption.label}",
            onClick = { expanded = true },
            size = size
        )

        if (expanded) {
            Popup(
                popupPositionProvider = positionProvider,
                onDismissRequest = { expanded = false },
                properties = PopupProperties(focusable = true)
            ) {
                SortMenu(
                    selectedOption = selectedOption,
                    direction = direction,
                    onPick = { option ->
                        val newDirection = when {
                            option == selectedOption ->
                                if (direction == SortDirection.ASCENDING) SortDirection.DESCENDING else SortDirection.ASCENDING
                            // Names read naturally A to Z; everything else starts with the biggest/newest.
                            option == SortOption.NAME -> SortDirection.ASCENDING
                            else -> SortDirection.DESCENDING
                        }
                        onSelect(option, newDirection)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun SortMenu(
    selectedOption: SortOption,
    direction: SortDirection,
    onPick: (SortOption) -> Unit
) {
    val visibleState = remember { MutableTransitionState(false).apply { targetState = true } }
    AnimatedVisibility(
        visibleState = visibleState,
        enter = fadeIn(tween(140)) + scaleIn(
            animationSpec = tween(180),
            initialScale = 0.9f,
            transformOrigin = TransformOrigin(1f, 0f)
        ),
        exit = fadeOut(tween(100))
    ) {
        // The outer padding leaves room for the shadow, which the popup window would clip.
        Box(modifier = Modifier.padding(8.dp)) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.background,
                shadowElevation = 10.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            ) {
                Column(
                    modifier = Modifier.width(196.dp).padding(10.dp),
                    // Just a hairline between the cards, per feedback -- they used to have real
                    // breathing room (8dp) between them.
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    SortOption.entries.forEach { option ->
                        SortOptionCard(
                            option = option,
                            selected = option == selectedOption,
                            direction = direction,
                            onClick = { onPick(option) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SortOptionCard(
    option: SortOption,
    selected: Boolean,
    direction: SortDirection,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val content = if (selected) scheme.onPrimary else scheme.onSurface
    PteronClickableCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(percent = 50),
        color = if (selected) scheme.primary else scheme.surfaceContainer,
        borderColor = if (selected) Color.Transparent else scheme.outlineVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(option.label, style = MaterialTheme.typography.labelLarge, color = content)
            if (selected) {
                Icon(
                    imageVector = directionIcon(direction),
                    contentDescription = directionLabel(direction),
                    tint = content,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

private fun directionIcon(direction: SortDirection) =
    if (direction == SortDirection.ASCENDING) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward

private fun directionLabel(direction: SortDirection) =
    if (direction == SortDirection.ASCENDING) "Ascending" else "Descending"

/** Puts the popup just below its anchor, right-aligned with it, flipping above if it wouldn't fit. */
private class BelowAnchorEndPositionProvider(private val gapPx: Int) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val maxX = (windowSize.width - popupContentSize.width).coerceAtLeast(0)
        val x = (anchorBounds.right - popupContentSize.width).coerceIn(0, maxX)
        val below = anchorBounds.bottom + gapPx
        val y = if (below + popupContentSize.height <= windowSize.height) {
            below
        } else {
            (anchorBounds.top - gapPx - popupContentSize.height).coerceAtLeast(0)
        }
        return IntOffset(x, y)
    }
}
