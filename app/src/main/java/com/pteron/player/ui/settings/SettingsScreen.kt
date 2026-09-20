package com.pteron.player.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.pteron.player.data.model.AspectRatioMode
import com.pteron.player.data.prefs.AppTheme
import com.pteron.player.data.prefs.AppearanceState
import com.pteron.player.data.prefs.OrientationLock
import com.pteron.player.data.prefs.PlaybackPrefsState
import com.pteron.player.navigation.BottomNavDestination
import com.pteron.player.theme.colors
import com.pteron.player.theme.readableOn
import com.pteron.player.ui.common.PteronBottomNavBar
import com.pteron.player.ui.common.TwoLineTitle
import com.pteron.player.ui.common.bouncyClickable
import kotlin.math.roundToInt

/**
 * The layers of the Settings screen. The first layer shows only these as header cards; opening
 * one swaps in the settings that belong to it.
 */
private enum class SettingsSection(val title: String, val subtitle: String, val icon: ImageVector) {
    APPEARANCE("Theme", "Pick a ready-made color theme", Icons.Outlined.Palette),
    LIBRARY("Library & folders", "Folder tile badges", Icons.Outlined.FolderOpen),
    PLAYER("Player controls", "Gestures, seeking and control style", Icons.Outlined.TouchApp),
    PLAYBACK("Playback", "Resume, auto-play and screen behavior", Icons.Outlined.PlayCircle),
    AUDIO_SUBTITLES("Audio & subtitles", "Volume boost and subtitle size", Icons.Outlined.GraphicEq),
    DATA("Data & reset", "Watch history and default settings", Icons.Outlined.Storage)
}

private val lightThemes = AppTheme.entries.filter { !it.isDark }
private val darkThemes = AppTheme.entries.filter { it.isDark }

/** Slide-and-fade between the header list and a section: forward when opening, reversed on back. */
private fun settingsTransition(opening: Boolean): ContentTransform {
    val slide = tween<IntOffset>(durationMillis = 320, easing = FastOutSlowInEasing)
    return if (opening) {
        (slideInHorizontally(slide) { it / 4 } + fadeIn(tween(260))) togetherWith
            (slideOutHorizontally(slide) { -it / 6 } + fadeOut(tween(160)))
    } else {
        (slideInHorizontally(slide) { -it / 6 } + fadeIn(tween(260))) togetherWith
            (slideOutHorizontally(slide) { it / 4 } + fadeOut(tween(160)))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onNavigate: (BottomNavDestination) -> Unit) {
    val appearance by viewModel.appearance.collectAsState()
    val playbackPrefs by viewModel.playbackPrefs.collectAsState()

    // Survives rotation and returning from another tab, so the person stays where they were.
    var openSectionName by rememberSaveable { mutableStateOf<String?>(null) }
    val openSection = openSectionName?.let { name -> SettingsSection.entries.firstOrNull { it.name == name } }

    BackHandler(enabled = openSection != null) { openSectionName = null }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    AnimatedVisibility(
                        visible = openSection != null,
                        enter = fadeIn() + expandHorizontally(),
                        exit = fadeOut() + shrinkHorizontally()
                    ) {
                        IconButton(onClick = { openSectionName = null }) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back to settings")
                        }
                    }
                },
                title = {
                    AnimatedContent(
                        targetState = openSection,
                        transitionSpec = { fadeIn(tween(220, delayMillis = 60)) togetherWith fadeOut(tween(120)) },
                        label = "settingsTitle"
                    ) { section ->
                        TwoLineTitle(
                            subtitle = if (section == null) "PTERON PLAYER" else "SETTINGS",
                            title = section?.title ?: "Settings"
                        )
                    }
                }
            )
        },
        bottomBar = {
            PteronBottomNavBar(
                current = BottomNavDestination.SETTINGS,
                // Tapping the tab you're already on steps back out to the header list.
                onSelect = { destination ->
                    if (destination == BottomNavDestination.SETTINGS) openSectionName = null else onNavigate(destination)
                }
            )
        }
    ) { padding ->
        // Only the layer being shown is composed, so an unopened section costs nothing.
        AnimatedContent(
            targetState = openSection,
            modifier = Modifier.fillMaxSize().padding(padding),
            transitionSpec = { settingsTransition(opening = targetState != null) },
            label = "settingsLayer"
        ) { section ->
            if (section == null) {
                SettingsHome(onOpen = { openSectionName = it.name })
            } else {
                SettingsSectionContent(section, appearance, playbackPrefs, viewModel)
            }
        }
    }
}

// --- Layer 1: header cards ------------------------------------------------------------------

@Composable
private fun SettingsPage(spacing: Dp = 16.dp, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(spacing),
        content = content
    )
}

@Composable
private fun SettingsHome(onOpen: (SettingsSection) -> Unit) {
    SettingsPage(spacing = 12.dp) {
        SettingsSection.entries.forEach { section ->
            SectionHeaderCard(section = section, onClick = { onOpen(section) })
        }
    }
}

/** A card in the theme's medium shade; every card in Settings goes through here. */
@Composable
private fun SettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        content = content
    )
}

@Composable
private fun SectionHeaderCard(section: SettingsSection, onClick: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Card(
        // clip() sits outside bouncyClickable so the press ripple follows the rounded corners.
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .semantics { role = Role.Button }
            .bouncyClickable(onClick = onClick),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = section.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(section.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    section.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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

// --- Layer 2: the settings under one header ---------------------------------------------------

@Composable
private fun SettingsSectionContent(
    section: SettingsSection,
    appearance: AppearanceState,
    playbackPrefs: PlaybackPrefsState,
    viewModel: SettingsViewModel
) {
    SettingsPage {
        when (section) {
            SettingsSection.APPEARANCE -> {
                LabeledGroup("Light themes") { ThemeGrid(lightThemes, appearance.theme, viewModel::setTheme) }
                LabeledGroup("Dark themes") { ThemeGrid(darkThemes, appearance.theme, viewModel::setTheme) }
            }
            SettingsSection.LIBRARY -> {
                FolderAppearanceCard(appearance, viewModel)
            }
            SettingsSection.PLAYER -> {
                GestureSensitivityCard(appearance, viewModel)
                LabeledGroup("Seeking") { SeekDurationCard(playbackPrefs, viewModel) }
                LabeledGroup("Controls") { ControlAutoHideCard(playbackPrefs, viewModel) }
            }
            SettingsSection.PLAYBACK -> {
                PlaybackBehaviorCard(playbackPrefs, viewModel)
                LabeledGroup("Screen") { ScreenOptionsCard(playbackPrefs, viewModel) }
            }
            SettingsSection.AUDIO_SUBTITLES -> {
                LabeledGroup("Audio") { AudioBoostCard(playbackPrefs, viewModel) }
                LabeledGroup("Subtitles") { SubtitleSizeCard(playbackPrefs, viewModel) }
            }
            SettingsSection.DATA -> {
                DataCards(viewModel)
            }
        }
    }
}

@Composable
private fun LabeledGroup(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp)
        )
        content()
    }
}

// --- Theme picker -----------------------------------------------------------------------------

@Composable
private fun ThemeGrid(themes: List<AppTheme>, selected: AppTheme, onSelect: (AppTheme) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        themes.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                pair.forEach { theme ->
                    ThemePreviewCard(
                        theme = theme,
                        selected = theme == selected,
                        onClick = { onSelect(theme) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/**
 * A tiny mock of the app painted in the theme's own three shades, so the choice is visible before
 * tapping: background, top and bottom bars and a card in the medium shade, folders and a switch in
 * the dark (accent) shade.
 */
@Composable
private fun ThemePreviewCard(
    theme: AppTheme,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = remember(theme) { theme.colors() }
    val shape = RoundedCornerShape(20.dp)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.4f)
                .border(
                    width = if (selected) 2.5.dp else 1.dp,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    shape = shape
                )
                .clip(shape)
                .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                drawRect(colors.background)
                // Top and bottom bars.
                drawRect(colors.card, size = Size(w, h * 0.16f))
                drawRect(colors.card, topLeft = Offset(0f, h * 0.84f), size = Size(w, h * 0.16f))
                // Two folders.
                val folderSize = Size(w * 0.36f, h * 0.30f)
                val folderCorner = CornerRadius(h * 0.06f)
                drawRoundRect(colors.accent, Offset(w * 0.10f, h * 0.25f), folderSize, folderCorner)
                drawRoundRect(colors.accent, Offset(w * 0.54f, h * 0.25f), folderSize, folderCorner)
                // A card with a switch.
                drawRoundRect(
                    colors.card, Offset(w * 0.10f, h * 0.61f), Size(w * 0.80f, h * 0.15f), CornerRadius(h * 0.05f)
                )
                drawRoundRect(
                    colors.accent, Offset(w * 0.72f, h * 0.655f), Size(w * 0.14f, h * 0.065f), CornerRadius(h * 0.033f)
                )
            }
            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 8.dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(colors.accent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = readableOn(colors.accent),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
        Text(
            theme.displayName,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}

// --- Library, player, playback, audio ---------------------------------------------------------

@Composable
private fun FolderAppearanceCard(appearance: AppearanceState, viewModel: SettingsViewModel) {
    SettingsCard {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SettingsSwitchRow(
                title = "Show video count badge",
                subtitle = "Displays the number of videos on each folder tile",
                checked = appearance.showVideoCountBadge,
                onCheckedChange = viewModel::setShowVideoCountBadge
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            SettingsSwitchRow(
                title = "Show folder size",
                subtitle = "Adds total storage used to each folder tile",
                checked = appearance.showFolderSizeBadge,
                onCheckedChange = viewModel::setShowFolderSizeBadge
            )
        }
    }
}

@Composable
private fun GestureSensitivityCard(appearance: AppearanceState, viewModel: SettingsViewModel) {
    SettingsCard {
        Column(modifier = Modifier.padding(16.dp)) {
            SliderSetting(
                title = "Gesture scrub sensitivity",
                value = appearance.gestureSensitivity,
                valueRange = 0.5f..2.0f,
                steps = 5,
                onCommit = viewModel::setGestureSensitivity,
                valueLabel = ::sensitivityLabel,
                footer = {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Precise", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Fast", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            )
        }
    }
}

private fun sensitivityLabel(value: Float): String = when {
    value < 0.9f -> "Precise (${"%.2f".format(value)}x)"
    value > 1.1f -> "Fast (${"%.2f".format(value)}x)"
    else -> "Standard (${"%.2f".format(value)}x)"
}

@Composable
private fun SettingsSwitchRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/**
 * A slider that only saves when the finger lifts. Dragging used to write to disk on every
 * pixel of movement (dozens of DataStore writes per second); now the label and thumb follow the
 * drag locally and a single write happens on release.
 */
@Composable
private fun SliderSetting(
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onCommit: (Float) -> Unit,
    valueLabel: (Float) -> String,
    steps: Int = 0,
    description: String? = null,
    footer: (@Composable () -> Unit)? = null
) {
    var dragValue by remember(value) { mutableFloatStateOf(value) }
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                valueLabel(dragValue),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
        if (description != null) {
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Slider(
            value = dragValue,
            onValueChange = { dragValue = it },
            onValueChangeFinished = { onCommit(dragValue) },
            valueRange = valueRange,
            steps = steps
        )
        footer?.invoke()
    }
}

@Composable
private fun PlaybackBehaviorCard(prefs: PlaybackPrefsState, viewModel: SettingsViewModel) {
    SettingsCard {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SettingsSwitchRow(
                title = "Resume playback",
                subtitle = "Continue from where you left off in a video",
                checked = prefs.resumePlaybackEnabled,
                onCheckedChange = viewModel::setResumePlaybackEnabled
            )
            SettingsSwitchRow(
                title = "Auto-play next video",
                subtitle = "Automatically starts the next video in the folder",
                checked = prefs.autoPlayNext,
                onCheckedChange = viewModel::setAutoPlayNext
            )
        }
    }
}

@Composable
private fun SeekDurationCard(prefs: PlaybackPrefsState, viewModel: SettingsViewModel) {
    SettingsCard {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Double-tap / seek button duration", style = MaterialTheme.typography.titleSmall)
            Text(
                "How far a double-tap or the rewind/forward buttons jump.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(5, 10, 15, 30).forEach { seconds ->
                    FilterChip(
                        selected = prefs.doubleTapSeekSeconds == seconds,
                        onClick = { viewModel.setDoubleTapSeekSeconds(seconds) },
                        label = { Text("${seconds}s") }
                    )
                }
            }
        }
    }
}

@Composable
private fun ControlAutoHideCard(prefs: PlaybackPrefsState, viewModel: SettingsViewModel) {
    SettingsCard {
        Column(modifier = Modifier.padding(16.dp)) {
            SliderSetting(
                title = "Control auto-hide",
                description = "How long player controls stay visible before hiding, while playing",
                value = prefs.controlAutoHideSeconds.toFloat(),
                valueRange = 1f..10f,
                steps = 8,
                onCommit = { viewModel.setControlAutoHideSeconds(it.roundToInt()) },
                valueLabel = { "${it.roundToInt()}s" }
            )
        }
    }
}

@Composable
private fun ScreenOptionsCard(prefs: PlaybackPrefsState, viewModel: SettingsViewModel) {
    SettingsCard {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SettingsSwitchRow(
                title = "Keep screen on while playing",
                subtitle = "Prevents the display from sleeping during playback",
                checked = prefs.keepScreenOnWhilePlaying,
                onCheckedChange = viewModel::setKeepScreenOnWhilePlaying
            )
            Column {
                Text("Player orientation", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.size(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OrientationLock.entries.forEach { lock ->
                        FilterChip(
                            selected = prefs.orientationLock == lock,
                            onClick = { viewModel.setOrientationLock(lock) },
                            label = {
                                Text(
                                    when (lock) {
                                        OrientationLock.AUTO -> "Auto"
                                        OrientationLock.PORTRAIT -> "Portrait"
                                        OrientationLock.LANDSCAPE -> "Landscape"
                                    }
                                )
                            }
                        )
                    }
                }
            }
            Column {
                Text("Default aspect ratio", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.size(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AspectRatioMode.entries.forEach { mode ->
                        FilterChip(
                            selected = prefs.defaultAspectRatio == mode,
                            onClick = { viewModel.setDefaultAspectRatio(mode) },
                            label = { Text(mode.label) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AudioBoostCard(prefs: PlaybackPrefsState, viewModel: SettingsViewModel) {
    SettingsCard {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SettingsSwitchRow(
                title = "Audio boost",
                subtitle = "Amplifies quiet audio beyond 100% volume, MX Player-style",
                checked = prefs.audioBoostEnabled,
                onCheckedChange = viewModel::setAudioBoostEnabled
            )
            if (prefs.audioBoostEnabled) {
                SliderSetting(
                    title = "Boost level",
                    value = prefs.audioBoostLevel,
                    valueRange = 0f..100f,
                    onCommit = viewModel::setAudioBoostLevel,
                    valueLabel = { "${it.toInt()}%" },
                    footer = {
                        Text(
                            "Higher boost can distort audio on some devices/videos.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun SubtitleSizeCard(prefs: PlaybackPrefsState, viewModel: SettingsViewModel) {
    SettingsCard {
        Column(modifier = Modifier.padding(16.dp)) {
            SliderSetting(
                title = "Subtitle text size",
                value = prefs.subtitleTextSizeSp,
                valueRange = 12f..28f,
                onCommit = viewModel::setSubtitleTextSize,
                valueLabel = { "${it.toInt()}sp" }
            )
        }
    }
}

// --- Data & reset ----------------------------------------------------------------------------

@Composable
private fun DataCards(viewModel: SettingsViewModel) {
    var showClearHistoryConfirm by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        DataActionCard(
            icon = Icons.Outlined.DeleteSweep,
            title = "Clear watch history",
            description = "Removes all resume positions, watched flags, and favorites. This can't be undone.",
            actionLabel = "Clear history",
            onAction = { showClearHistoryConfirm = true }
        )
        DataActionCard(
            icon = Icons.Outlined.RestartAlt,
            title = "Reset appearance",
            description = "Restores the theme, folder badges, gesture sensitivity, sorting and view mode to their defaults.",
            actionLabel = "Reset",
            onAction = { showResetConfirm = true }
        )
    }

    if (showClearHistoryConfirm) {
        AlertDialog(
            onDismissRequest = { showClearHistoryConfirm = false },
            title = { Text("Clear watch history?") },
            text = { Text("This removes all resume positions, watched flags, and favorites across your whole library.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearWatchHistory()
                    showClearHistoryConfirm = false
                }) { Text("Clear", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryConfirm = false }) { Text("Cancel") }
            }
        )
    }
    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Reset appearance?") },
            text = { Text("All appearance settings go back to their defaults. Your videos and watch history are not affected.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.resetToDefaults()
                    showResetConfirm = false
                }) { Text("Reset", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun DataActionCard(
    icon: ImageVector,
    title: String,
    description: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    SettingsCard {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                Text(title, style = MaterialTheme.typography.titleSmall)
            }
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onAction) {
                    Text(actionLabel, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
