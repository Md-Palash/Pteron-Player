package com.pteron.player.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.MusicNote
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.sp
import com.pteron.player.data.model.AspectRatioMode
import com.pteron.player.data.prefs.AppFont
import com.pteron.player.data.prefs.AppTheme
import com.pteron.player.data.prefs.AppearanceState
import com.pteron.player.data.prefs.MusicPrefsState
import com.pteron.player.data.prefs.NowPlayingStyle
import com.pteron.player.data.prefs.OrientationLock
import com.pteron.player.data.prefs.PlaybackPrefsState
import com.pteron.player.navigation.BottomNavDestination
import com.pteron.player.theme.colors
import com.pteron.player.theme.fontFamily
import com.pteron.player.theme.readableOn
import com.pteron.player.ui.common.DarkModeButton
import com.pteron.player.ui.common.SettingsSet
import com.pteron.player.ui.common.SettingsSetCard
import com.pteron.player.ui.common.SettingsSetRow
import com.pteron.player.ui.common.PteronClickableCard
import com.pteron.player.ui.common.setItemShape
import com.pteron.player.util.TabReselect
import com.pteron.player.ui.common.TwoLineTitle
import com.pteron.player.ui.common.bouncyClickable
import kotlin.math.roundToInt

/**
 * The layers of the Settings screen. The first layer shows only these as header cards; opening
 * one swaps in the settings that belong to it.
 */
private enum class SettingsSection(val title: String, val subtitle: String, val icon: ImageVector) {
    APPEARANCE("Appearance", "Theme, shades and font", Icons.Outlined.Palette),
    LIBRARY("Library & folders", "Folder badges and video grid", Icons.Outlined.FolderOpen),
    PLAYER("Player controls", "Gestures, seeking and control style", Icons.Outlined.TouchApp),
    PLAYBACK("Playback", "Resume, auto-play and screen behavior", Icons.Outlined.PlayCircle),
    AUDIO_SUBTITLES("Audio & subtitles", "Volume boost and subtitle size", Icons.Outlined.GraphicEq),
    MUSIC("Music Player", "Sound, library and playback", Icons.Outlined.MusicNote),
    DATA("Data & reset", "Watch history and default settings", Icons.Outlined.Storage)
}

private const val ThemesPerRow = 4

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
fun SettingsScreen(viewModel: SettingsViewModel) {
    val appearance by viewModel.appearance.collectAsState()
    val playbackPrefs by viewModel.playbackPrefs.collectAsState()
    val musicPrefs by viewModel.musicPrefs.collectAsState()

    // Survives rotation and returning from another tab, so the person stays where they were.
    var openSectionName by rememberSaveable { mutableStateOf<String?>(null) }
    val openSection = openSectionName?.let { name -> SettingsSection.entries.firstOrNull { it.name == name } }

    BackHandler(enabled = openSection != null) { openSectionName = null }

    // Tapping the Settings tab while already on it steps back out to the header list.
    LaunchedEffect(Unit) {
        TabReselect.events.collect { if (it == BottomNavDestination.SETTINGS) openSectionName = null }
    }

    Scaffold { insets ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(insets)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AnimatedVisibility(
                            visible = openSection != null,
                            enter = fadeIn() + expandHorizontally(),
                            exit = fadeOut() + shrinkHorizontally()
                        ) {
                            Box(modifier = Modifier.padding(end = 8.dp).bouncyClickable(onClick = { openSectionName = null })) {
                                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back to settings")
                            }
                        }
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
                    DarkModeButton(onClick = viewModel::toggleDarkMode)
                }

                // Only the layer being shown is composed, so an unopened section costs nothing.
                AnimatedContent(
                    modifier = Modifier.weight(1f),
                    targetState = openSection,
                    transitionSpec = { settingsTransition(opening = targetState != null) },
                    label = "settingsLayer"
                ) { section ->
                    if (section == null) {
                        SettingsHome(onOpen = { openSectionName = it.name })
                    } else {
                        SettingsSectionContent(section, appearance, playbackPrefs, musicPrefs, viewModel)
                    }
                }
            }
        }
    }
}

// --- Layer 1: header cards ------------------------------------------------------------------

@Composable
private fun SettingsPage(spacing: Dp = 8.dp, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 108.dp),
        verticalArrangement = Arrangement.spacedBy(spacing),
        content = content
    )
}

@Composable
private fun SettingsHome(onOpen: (SettingsSection) -> Unit) {
    SettingsPage {
        // One set holding every section card. When the list grows, split it into several sets.
        val sections = SettingsSection.entries
        SettingsSet(count = sections.size) { index, shape ->
            val section = sections[index]
            SettingsSetRow(
                icon = section.icon,
                title = section.title,
                subtitle = section.subtitle,
                shape = shape,
                onClick = { onOpen(section) }
            )
        }
    }
}

/** A card in the theme's medium shade, its left and right sides fully rounded -- every detail
 *  card inside a Settings section goes through here. */
@Composable
private fun SettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        content = content
    )
}

// --- Layer 2: the settings under one header ---------------------------------------------------

@Composable
private fun SettingsSectionContent(
    section: SettingsSection,
    appearance: AppearanceState,
    playbackPrefs: PlaybackPrefsState,
    musicPrefs: MusicPrefsState,
    viewModel: SettingsViewModel
) {
    SettingsPage {
        when (section) {
            SettingsSection.APPEARANCE -> {
                ThemeSection(appearance.theme, viewModel::setTheme)
                LabeledGroup("Shades") { ShadeCard(appearance, viewModel) }
                FontSection(appearance.font, appearance.fontScale, viewModel::setFont, viewModel::setFontScale)
            }
            SettingsSection.LIBRARY -> {
                FolderAppearanceCard(appearance, viewModel)
                LabeledGroup("Video grid") { VideoGridCard(appearance, viewModel) }
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
            SettingsSection.MUSIC -> {
                LabeledGroup("Sound") { MusicSoundCard(musicPrefs, viewModel) }
                LabeledGroup("Library") { MusicLibraryCard(musicPrefs, viewModel) }
                LabeledGroup("Playback") { MusicPlaybackCard(musicPrefs, viewModel) }
                LabeledGroup("Now Playing") { NowPlayingStyleCard(musicPrefs, viewModel) }
                DataActionCard(
                    icon = Icons.Outlined.DeleteSweep,
                    title = "Clear recently played",
                    description = "Empties the Recently played row in the Audio section. Your songs and favorites are not affected.",
                    actionLabel = "Clear",
                    onAction = viewModel::clearRecentlyPlayed
                )
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
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        themes.chunked(ThemesPerRow).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { theme ->
                    ThemePreviewCard(
                        theme = theme,
                        selected = theme == selected,
                        onClick = { onSelect(theme) },
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(ThemesPerRow - row.size) { Spacer(Modifier.weight(1f)) }
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
    val shape = RoundedCornerShape(12.dp)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.95f)
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
                drawRoundRect(colors.folder, Offset(w * 0.10f, h * 0.25f), folderSize, folderCorner)
                drawRoundRect(colors.folder, Offset(w * 0.54f, h * 0.25f), folderSize, folderCorner)
                drawCircle(colors.accent, radius = h * 0.04f, center = Offset(w * 0.17f, h * 0.31f))
                drawCircle(colors.accent, radius = h * 0.04f, center = Offset(w * 0.61f, h * 0.31f))
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
                        .padding(top = 4.dp, end = 4.dp)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(colors.accent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = readableOn(colors.accent),
                        modifier = Modifier.size(10.dp)
                    )
                }
            }
        }
        Text(
            theme.displayName,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 2.dp)
        )
    }
}

// --- Shades and font ---------------------------------------------------------------------------

private fun shadeLabel(value: Float): String = when {
    value > 0.005f -> "Darker ${(value * 100).roundToInt()}%"
    value < -0.005f -> "Lighter ${(-value * 100).roundToInt()}%"
    else -> "Theme default"
}

/** Three sliders -- canvas, cards, folders -- each saved once, when the finger lifts. */
@Composable
private fun ShadeCard(appearance: AppearanceState, viewModel: SettingsViewModel) {
    SettingsCard {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            ShadeSlider("Canvas", appearance.canvasShade, viewModel::setCanvasShade)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ShadeSlider("Cards", appearance.cardShade, viewModel::setCardShade)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ShadeSlider("Folders", appearance.folderShade, viewModel::setFolderShade)
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = viewModel::resetShades) { Text("Reset shades") }
            }
        }
    }
}

@Composable
private fun ShadeSlider(title: String, value: Float, onCommit: (Float) -> Unit) {
    SliderSetting(
        title = title,
        value = value,
        valueRange = -1f..1f,
        steps = 19,
        onCommit = onCommit,
        valueLabel = ::shadeLabel,
        footer = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Lighter", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Darker", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

/**
 * A collapsed "Theme" card showing the current theme. Tapping it unfolds the light and dark theme
 * pickers right underneath (four small previews per row); tapping again folds them away. Nothing
 * inside is composed while it is folded, so the Appearance page opens quickly.
 */
@Composable
private fun ThemeSection(selected: AppTheme, onSelect: (AppTheme) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val chevron by animateFloatAsState(
        targetValue = if (expanded) 90f else 0f,
        animationSpec = tween(260, easing = FastOutSlowInEasing),
        label = "themeChevron"
    )
    Column {
        SettingsSetCard(shape = setItemShape(0, 1), onClick = { expanded = !expanded }) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ThemeSwatch(selected)
                Column(modifier = Modifier.weight(1f)) {
                    Text("Theme", style = MaterialTheme.typography.titleMedium)
                    Text(
                        selected.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = if (expanded) "Hide themes" else "Show themes",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.graphicsLayer { rotationZ = chevron }
                )
            }
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(tween(300, easing = FastOutSlowInEasing)) + fadeIn(tween(240, delayMillis = 60)),
            exit = shrinkVertically(tween(240, easing = FastOutSlowInEasing)) + fadeOut(tween(120))
        ) {
            Column(modifier = Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                LabeledGroup("Light themes") { ThemeGrid(lightThemes, selected, onSelect) }
                LabeledGroup("Dark themes") { ThemeGrid(darkThemes, selected, onSelect) }
            }
        }
    }
}

/** The round icon bubble of the Theme card: filled completely with the selected theme's color. */
@Composable
private fun ThemeSwatch(theme: AppTheme) {
    val colors = remember(theme) { theme.colors() }
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(colors.accent)
    )
}

private val fontSizeOptions = listOf(0.85f to "Small", 1.0f to "Default", 1.15f to "Large", 1.3f to "Extra large")

private fun fontSizeLabel(scale: Float): String =
    fontSizeOptions.minByOrNull { kotlin.math.abs(it.first - scale) }?.second ?: "Default"

/**
 * The "Font" card. Like the Theme card it unfolds when tapped, showing two cards underneath: one to
 * choose the font (opens the font sheet) and one to choose the text size (opens the size sheet).
 * Both bubbles preview the current choice.
 */
@Composable
private fun FontSection(
    font: AppFont,
    scale: Float,
    onSelectFont: (AppFont) -> Unit,
    onSelectScale: (Float) -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var showFontSheet by rememberSaveable { mutableStateOf(false) }
    var showSizeSheet by rememberSaveable { mutableStateOf(false) }
    val scheme = MaterialTheme.colorScheme
    val chevron by animateFloatAsState(
        targetValue = if (expanded) 90f else 0f,
        animationSpec = tween(260, easing = FastOutSlowInEasing),
        label = "fontChevron"
    )
    Column {
        SettingsSetCard(shape = setItemShape(0, 1), onClick = { expanded = !expanded }) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FontBubble(font)
                Column(modifier = Modifier.weight(1f)) {
                    Text("Font", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${font.displayName} \u2022 ${fontSizeLabel(scale)}",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = font.fontFamily()),
                        color = scheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = if (expanded) "Hide font options" else "Show font options",
                    tint = scheme.onSurfaceVariant,
                    modifier = Modifier.graphicsLayer { rotationZ = chevron }
                )
            }
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(tween(300, easing = FastOutSlowInEasing)) + fadeIn(tween(240, delayMillis = 60)),
            exit = shrinkVertically(tween(240, easing = FastOutSlowInEasing)) + fadeOut(tween(120))
        ) {
            Column(modifier = Modifier.padding(top = 12.dp)) {
                SettingsSet(count = 2) { index, shape ->
                    if (index == 0) {
                        SettingsSetCard(shape = shape, onClick = { showFontSheet = true }) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                FontBubble(font)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Choose font", style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        font.displayName,
                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = font.fontFamily()),
                                        color = scheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = scheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        SettingsSetCard(shape = shape, onClick = { showSizeSheet = true }) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Box(
                                    modifier = Modifier.size(48.dp).clip(CircleShape).background(scheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Outlined.FormatSize, contentDescription = null, tint = scheme.onPrimary, modifier = Modifier.size(24.dp))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Font size", style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "${fontSizeLabel(scale)} (${(scale * 100).roundToInt()}%)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = scheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = scheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
    if (showFontSheet) {
        FontSheet(
            selected = font,
            onSelect = {
                onSelectFont(it)
                showFontSheet = false
            },
            onDismiss = { showFontSheet = false }
        )
    }
    if (showSizeSheet) {
        FontSizeSheet(
            selected = scale,
            font = font,
            onSelect = {
                onSelectScale(it)
                showSizeSheet = false
            },
            onDismiss = { showSizeSheet = false }
        )
    }
}

/** The "Aa" bubble: the sample is drawn in the selected font. */
@Composable
private fun FontBubble(font: AppFont) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier.size(48.dp).clip(CircleShape).background(scheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "Aa",
            style = MaterialTheme.typography.titleMedium.copy(fontFamily = font.fontFamily()),
            color = scheme.onPrimary
        )
    }
}

/** Bottom sheet with a live preview line and one pill per size; each pill is drawn at its own size. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FontSizeSheet(selected: Float, font: AppFont, onSelect: (Float) -> Unit, onDismiss: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    var preview by remember { mutableFloatStateOf(selected) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Font size",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
            )
            SettingsCard {
                Text(
                    "The quick brown fox jumps over the lazy dog.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = font.fontFamily(),
                        fontSize = (14f * preview / selected.coerceAtLeast(0.1f)).sp * selected
                    ),
                    modifier = Modifier.padding(16.dp)
                )
            }
            fontSizeOptions.forEach { (value, label) ->
                val isSelected = kotlin.math.abs(value - selected) < 0.01f
                val content = if (isSelected) scheme.onPrimary else scheme.onSurface
                PteronClickableCard(
                    onClick = { onSelect(value) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(percent = 50),
                    color = if (isSelected) scheme.primary else scheme.surfaceContainer,
                    borderColor = if (isSelected) androidx.compose.ui.graphics.Color.Transparent
                    else scheme.outlineVariant.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            label,
                            style = MaterialTheme.typography.titleSmall.copy(fontSize = (14f * value / selected.coerceAtLeast(0.1f)).sp * selected),
                            color = content,
                            modifier = Modifier.weight(1f)
                        )
                        if (isSelected) {
                            Icon(Icons.Filled.Check, contentDescription = "Selected", tint = content, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

/** Bottom sheet with one pill-shaped card per font, each in its own typeface; the chosen one takes the accent shade. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FontSheet(selected: AppFont, onSelect: (AppFont) -> Unit, onDismiss: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Font",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
            )
            AppFont.entries.forEach { font ->
                val isSelected = font == selected
                val content = if (isSelected) scheme.onPrimary else scheme.onSurface
                PteronClickableCard(
                    onClick = { onSelect(font) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(percent = 50),
                    color = if (isSelected) scheme.primary else scheme.surfaceContainer,
                    borderColor = if (isSelected) androidx.compose.ui.graphics.Color.Transparent
                    else scheme.outlineVariant.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            font.displayName,
                            style = MaterialTheme.typography.titleSmall.copy(fontFamily = font.fontFamily()),
                            color = content,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (isSelected) {
                            Icon(Icons.Filled.Check, contentDescription = "Selected", tint = content, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
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
private fun VideoGridCard(appearance: AppearanceState, viewModel: SettingsViewModel) {
    SettingsCard {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Videos per row", style = MaterialTheme.typography.titleSmall)
            Text(
                "How many video tiles fit in one line of the grid view.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(2, 3).forEach { columns ->
                    FilterChip(
                        selected = appearance.videoGridColumns == columns,
                        onClick = { viewModel.setVideoGridColumns(columns) },
                        label = { Text("$columns videos") }
                    )
                }
            }
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
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            SettingsSwitchRow(
                title = "Background play",
                subtitle = "Keep audio playing when you minimize the app or lock the screen",
                checked = prefs.backgroundPlaybackEnabled,
                onCheckedChange = viewModel::setBackgroundPlaybackEnabled
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
                subtitle = "Amplifies quiet audio beyond 100% volume",
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

// --- Music player -----------------------------------------------------------------------------

@Composable
private fun MusicSoundCard(prefs: MusicPrefsState, viewModel: SettingsViewModel) {
    val divider = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    SettingsCard {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SettingsSwitchRow(
                title = "Skip silence",
                subtitle = "Jumps over silent stretches inside a track",
                checked = prefs.skipSilence,
                onCheckedChange = viewModel::setSkipSilence
            )
            HorizontalDivider(color = divider)
            SettingsSwitchRow(
                title = "Loudness boost",
                subtitle = "Lifts quiet tracks above the normal volume",
                checked = prefs.loudnessBoostEnabled,
                onCheckedChange = viewModel::setLoudnessBoostEnabled
            )
            if (prefs.loudnessBoostEnabled) {
                SliderSetting(
                    title = "Boost level",
                    value = prefs.loudnessBoostLevel,
                    valueRange = 0f..100f,
                    onCommit = viewModel::setLoudnessBoostLevel,
                    valueLabel = { "${it.toInt()}%" },
                    footer = {
                        Text(
                            "Higher boost can distort audio on some devices and tracks.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }
            HorizontalDivider(color = divider)
            SliderSetting(
                title = "Bass boost",
                description = "Adds low-end punch. Not available on every device.",
                value = prefs.bassBoostLevel,
                valueRange = 0f..100f,
                onCommit = viewModel::setBassBoostLevel,
                valueLabel = { if (it < 1f) "Off" else "${it.toInt()}%" }
            )
            SliderSetting(
                title = "Surround",
                description = "Widens the stereo image, best with headphones. Not available on every device.",
                value = prefs.virtualizerLevel,
                valueRange = 0f..100f,
                onCommit = viewModel::setVirtualizerLevel,
                valueLabel = { if (it < 1f) "Off" else "${it.toInt()}%" }
            )
        }
    }
}

@Composable
private fun MusicLibraryCard(prefs: MusicPrefsState, viewModel: SettingsViewModel) {
    SettingsCard {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Ignore short tracks", style = MaterialTheme.typography.titleSmall)
            Text(
                "Hides ringtones, notification sounds and clips shorter than this from your music library.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0 to "Off", 15 to "15s", 30 to "30s", 60 to "60s").forEach { (seconds, label) ->
                    FilterChip(
                        selected = prefs.minTrackSeconds == seconds,
                        onClick = { viewModel.setMinTrackSeconds(seconds) },
                        label = { Text(label) }
                    )
                }
            }
        }
    }
}

@Composable
private fun NowPlayingStyleCard(prefs: MusicPrefsState, viewModel: SettingsViewModel) {
    SettingsCard {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Card style", style = MaterialTheme.typography.titleSmall)
            Text(
                "Round shows the cover in a flower-shaped frame with a progress line around it. Square shows a full-screen card: the cover on the top 60% and the controls below.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NowPlayingStyle.entries.forEach { style ->
                    FilterChip(
                        selected = prefs.nowPlayingStyle == style,
                        onClick = { viewModel.setNowPlayingStyle(style) },
                        label = { Text(style.displayName) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MusicPlaybackCard(prefs: MusicPrefsState, viewModel: SettingsViewModel) {
    val divider = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    SettingsCard {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SettingsSwitchRow(
                title = "Pause when headphones unplug",
                subtitle = "Stops the music when wired or Bluetooth headphones disconnect",
                checked = prefs.pauseOnHeadphonesUnplugged,
                onCheckedChange = viewModel::setPauseOnHeadphonesUnplugged
            )
            HorizontalDivider(color = divider)
            SettingsSwitchRow(
                title = "Open Now Playing on start",
                subtitle = "Jumps to the full player when you start a song",
                checked = prefs.openNowPlayingOnPlay,
                onCheckedChange = viewModel::setOpenNowPlayingOnPlay
            )
            HorizontalDivider(color = divider)
            SettingsSwitchRow(
                title = "Keep screen on in Now Playing",
                subtitle = "Prevents the display from sleeping while the full player is open",
                checked = prefs.keepScreenOnInNowPlaying,
                onCheckedChange = viewModel::setKeepScreenOnInNowPlaying
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
            description = "Restores the theme, shades, font, folder badges, gesture sensitivity, sorting and view mode to their defaults.",
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
