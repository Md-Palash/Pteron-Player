package com.pteron.player.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pteron.player.data.prefs.AccentColor
import com.pteron.player.data.prefs.AppearanceState
import com.pteron.player.data.prefs.BackgroundTheme
import com.pteron.player.data.prefs.FolderTone
import com.pteron.player.data.prefs.OrientationLock
import com.pteron.player.data.prefs.PlaybackPrefsState
import com.pteron.player.data.prefs.ThemeMode
import com.pteron.player.theme.toComposeColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onNavigate: (com.pteron.player.navigation.BottomNavDestination) -> Unit) {
    val appearance by viewModel.appearance.collectAsState()
    val playbackPrefs by viewModel.playbackPrefs.collectAsState()
    var showClearHistoryConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { com.pteron.player.ui.common.TwoLineTitle(subtitle = "PTERON PLAYER", title = "Settings") },
                actions = {
                    TextButton(onClick = viewModel::resetToDefaults) {
                        Icon(Icons.Outlined.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(4.dp))
                        Text("Reset")
                    }
                }
            )
        },
        bottomBar = {
            com.pteron.player.ui.common.PteronBottomNavBar(
                current = com.pteron.player.navigation.BottomNavDestination.SETTINGS,
                onSelect = onNavigate
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item { SectionTitle("Display & Appearance", tag = "PALETTE") }
            item { ActiveCombinationCard(appearance) }
            item {
                ThemeModeRow(appearance.themeMode, viewModel::setThemeMode)
            }
            item {
                BackgroundThemeGrid(appearance.backgroundTheme, viewModel::setBackgroundTheme)
            }
            item {
                AccentColorRow(appearance.accentColor, viewModel::setAccentColor)
            }

            item { SectionTitle("Folder & Library Appearance", tag = "CARDS") }
            item {
                FolderAppearanceCard(appearance, viewModel)
            }

            item { SectionTitle("Player & Gesture Controls", tag = "PLAYBACK") }
            item {
                PlaybackUiCard(appearance, viewModel)
            }

            item { SectionTitle("Playback Behavior") }
            item {
                PlaybackBehaviorCard(playbackPrefs, viewModel)
            }

            item { SectionTitle("Gestures & Seeking") }
            item {
                SeekDurationCard(playbackPrefs, viewModel)
            }

            item { SectionTitle("Audio") }
            item {
                AudioBoostCard(playbackPrefs, viewModel)
            }

            item { SectionTitle("Subtitles") }
            item {
                SubtitleSizeCard(playbackPrefs, viewModel)
            }

            item { SectionTitle("Device") }
            item {
                DeviceOptionsCard(playbackPrefs, viewModel)
            }

            item { SectionTitle("Data") }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Clear watch history", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Removes all resume positions, watched flags, and favorites. This can't be undone.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                            TextButton(onClick = { showClearHistoryConfirm = true }) {
                                Text("Clear history", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showClearHistoryConfirm) {
        androidx.compose.material3.AlertDialog(
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
}

@Composable
private fun SectionTitle(text: String, tag: String? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
            Text(text, style = MaterialTheme.typography.headlineSmall)
        }
        if (tag != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    tag,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** A quick "here's what you've picked" preview strip, shown above the theme grid. */
@Composable
private fun ActiveCombinationCard(appearance: AppearanceState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(14.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ColorSwatch(appearance.accentColor.toComposeColor(), size = 32.dp, isCircle = true)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "ACTIVE COMBINATION",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "${appearance.backgroundTheme.displayName} • ${appearance.accentColor.displayName}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun ThemeModeRow(selected: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ThemeMode.entries.forEach { mode ->
            androidx.compose.material3.FilterChip(
                selected = selected == mode,
                onClick = { onSelect(mode) },
                label = { Text(mode.displayName) }
            )
        }
    }
}

@Composable
private fun BackgroundThemeGrid(selected: BackgroundTheme, onSelect: (BackgroundTheme) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        BackgroundTheme.entries.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { theme ->
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSelect(theme) },
                        colors = androidx.compose.material3.CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                        border = if (selected == theme) {
                            androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                        } else null
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                ColorSwatch(theme.toComposeColor(), size = 22.dp, isCircle = true)
                                if (selected == theme) {
                                    Icon(
                                        Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.size(6.dp))
                            Text(theme.displayName, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                        }
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun AccentColorRow(selected: AccentColor, onSelect: (AccentColor) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AccentColor.entries.forEach { accent ->
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (selected == accent) MaterialTheme.colorScheme.surfaceContainerHighest else Color.Transparent)
                            .clickable { onSelect(accent) },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(accent.toComposeColor()),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selected == accent) {
                                Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.size(8.dp))
            Text(
                "Selected: ${selected.displayName}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun FolderAppearanceCard(appearance: AppearanceState, viewModel: SettingsViewModel) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Folder wood tone", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FolderTone.entries.forEach { tone ->
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(tone.toComposeColor())
                            .clickable { viewModel.setFolderTone(tone) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (appearance.folderTone == tone) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
            SettingsSwitchRow(
                title = "Show video count badge",
                subtitle = "Displays the number of videos on each folder tile",
                checked = appearance.showVideoCountBadge,
                onCheckedChange = viewModel::setShowVideoCountBadge
            )
            androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
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
private fun PlaybackUiCard(appearance: AppearanceState, viewModel: SettingsViewModel) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SettingsSwitchRow(
                title = "Match player controls to accent",
                subtitle = "Uses your accent color on the scrub bar and play button",
                checked = appearance.matchControlsToAccent,
                onCheckedChange = viewModel::setMatchControlsToAccent
            )
            androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            SettingsSwitchRow(
                title = "OLED pure-black controls tray",
                subtitle = "Zero-power backdrop for the player's control bar",
                checked = appearance.oledPureBlackControls,
                onCheckedChange = viewModel::setOledPureBlackControls
            )
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Gesture scrub sensitivity", style = MaterialTheme.typography.titleSmall)
                    Text(
                        sensitivityLabel(appearance.gestureSensitivity),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Slider(
                    value = appearance.gestureSensitivity,
                    onValueChange = viewModel::setGestureSensitivity,
                    valueRange = 0.5f..2.0f,
                    steps = 5
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Precise", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Fast", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
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

@Composable
private fun ColorSwatch(color: Color, size: androidx.compose.ui.unit.Dp, isCircle: Boolean) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(if (isCircle) CircleShape else RoundedCornerShape(6.dp))
            .background(color)
    )
}

@Composable
private fun PlaybackBehaviorCard(prefs: PlaybackPrefsState, viewModel: SettingsViewModel) {
    Card(modifier = Modifier.fillMaxWidth()) {
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
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Double-tap / seek button duration", style = MaterialTheme.typography.titleSmall)
            Text(
                "How far a double-tap or the rewind/forward buttons jump.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(5, 10, 15, 30).forEach { seconds ->
                    val selected = prefs.doubleTapSeekSeconds == seconds
                    androidx.compose.material3.FilterChip(
                        selected = selected,
                        onClick = { viewModel.setDoubleTapSeekSeconds(seconds) },
                        label = { Text("${seconds}s") }
                    )
                }
            }
        }
    }
}

@Composable
private fun AudioBoostCard(prefs: PlaybackPrefsState, viewModel: SettingsViewModel) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SettingsSwitchRow(
                title = "Audio boost",
                subtitle = "Amplifies quiet audio beyond 100% volume, MX Player-style",
                checked = prefs.audioBoostEnabled,
                onCheckedChange = viewModel::setAudioBoostEnabled
            )
            if (prefs.audioBoostEnabled) {
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Boost level", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "${prefs.audioBoostLevel.toInt()}%",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = prefs.audioBoostLevel,
                        onValueChange = viewModel::setAudioBoostLevel,
                        valueRange = 0f..100f
                    )
                    Text(
                        "Higher boost can distort audio on some devices/videos.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SubtitleSizeCard(prefs: PlaybackPrefsState, viewModel: SettingsViewModel) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Subtitle text size", style = MaterialTheme.typography.titleSmall)
                Text(
                    "${prefs.subtitleTextSizeSp.toInt()}sp",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Slider(
                value = prefs.subtitleTextSizeSp,
                onValueChange = viewModel::setSubtitleTextSize,
                valueRange = 12f..28f
            )
        }
    }
}

@Composable
private fun DeviceOptionsCard(prefs: PlaybackPrefsState, viewModel: SettingsViewModel) {
    Card(modifier = Modifier.fillMaxWidth()) {
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
                        androidx.compose.material3.FilterChip(
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
                    com.pteron.player.data.model.AspectRatioMode.entries.forEach { mode ->
                        androidx.compose.material3.FilterChip(
                            selected = prefs.defaultAspectRatio == mode,
                            onClick = { viewModel.setDefaultAspectRatio(mode) },
                            label = { Text(mode.label) }
                        )
                    }
                }
            }
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Control auto-hide", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "${prefs.controlAutoHideSeconds}s",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    "How long player controls stay visible before hiding, while playing",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Slider(
                    value = prefs.controlAutoHideSeconds.toFloat(),
                    onValueChange = { viewModel.setControlAutoHideSeconds(it.toInt()) },
                    valueRange = 1f..10f,
                    steps = 8
                )
            }
        }
    }
}
