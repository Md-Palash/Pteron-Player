package com.pteron.player.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
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
 * The controls above a list of videos: the filter cards (All, Unwatched, ...) and, underneath, the
 * video count with the sort dropdown. Shared by the Videos and Folder screens.
 */
@Composable
fun VideoListHeader(
    activeFilter: VideoFilter,
    onFilterSelected: (VideoFilter) -> Unit,
    videoCount: Int,
    sortOption: SortOption,
    sortDirection: SortDirection,
    onSortSelected: (SortOption, SortDirection) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(top = 8.dp, bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        FilterCardsRow(active = activeFilter, onSelect = onFilterSelected)
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (videoCount == 1) "1 video" else "$videoCount videos",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            SortDropdown(selectedOption = sortOption, direction = sortDirection, onSelect = onSortSelected)
        }
    }
}

// --- Filter cards -----------------------------------------------------------------------------

@Composable
fun FilterCardsRow(active: VideoFilter, onSelect: (VideoFilter) -> Unit, modifier: Modifier = Modifier) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(VideoFilter.entries, key = { it.name }) { filter ->
            FilterCard(label = filter.label, selected = active == filter, onClick = { onSelect(filter) })
        }
    }
}

/** A rounded card: medium shade normally, dark (accent) shade when selected. */
@Composable
private fun FilterCard(label: String, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val container by animateColorAsState(
        targetValue = if (selected) scheme.primary else scheme.surfaceContainer,
        animationSpec = tween(180),
        label = "filterCardColor"
    )
    val content by animateColorAsState(
        targetValue = if (selected) scheme.onPrimary else scheme.onSurface,
        animationSpec = tween(180),
        label = "filterCardContent"
    )
    PteronClickableCard(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = container,
        borderColor = if (selected) Color.Transparent else scheme.outlineVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedVisibility(
                visible = selected,
                enter = fadeIn(tween(150)) + expandHorizontally(tween(150)),
                exit = fadeOut(tween(120)) + shrinkHorizontally(tween(120))
            ) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = content,
                    modifier = Modifier.padding(end = 6.dp).size(16.dp)
                )
            }
            Text(label, style = MaterialTheme.typography.labelLarge, color = content)
        }
    }
}

// --- Sort dropdown ----------------------------------------------------------------------------

/**
 * A card showing the current sort; tapping it opens a floating menu where every option is its own
 * rounded card. Picking the option that is already active flips its direction.
 */
@Composable
fun SortDropdown(
    selectedOption: SortOption,
    direction: SortDirection,
    onSelect: (SortOption, SortDirection) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(200),
        label = "sortArrow"
    )
    val gapPx = with(LocalDensity.current) { 6.dp.roundToPx() }
    val positionProvider = remember(gapPx) { BelowAnchorEndPositionProvider(gapPx) }

    Box(modifier = modifier) {
        PteronClickableCard(onClick = { expanded = true }) {
            Row(
                modifier = Modifier.padding(start = 12.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.Sort,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(selectedOption.label, style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = directionIcon(direction),
                    contentDescription = directionLabel(direction),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
                Icon(
                    Icons.Filled.ArrowDropDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp).rotate(arrowRotation)
                )
            }
        }

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
                    modifier = Modifier.width(216.dp).padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
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
        shape = RoundedCornerShape(14.dp),
        color = if (selected) scheme.primary else scheme.surfaceContainer,
        borderColor = if (selected) Color.Transparent else scheme.outlineVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
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
