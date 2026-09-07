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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pteron.player.data.prefs.AccentColor
import com.pteron.player.data.prefs.AppearanceState
import com.pteron.player.data.prefs.BackgroundTheme
import com.pteron.player.data.prefs.FolderTone
import com.pteron.player.theme.toComposeColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val appearance by viewModel.appearance.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Theme & Appearance") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") } },
                actions = {
                    TextButton(onClick = viewModel::resetToDefaults) {
                        Icon(Icons.Filled.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(4.dp))
                        Text("Reset")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item { SectionTitle("Base Background Theme") }
            item {
                BackgroundThemeGrid(appearance.backgroundTheme, viewModel::setBackgroundTheme)
            }

            item { SectionTitle("Accent Color") }
            item {
                AccentColorRow(appearance.accentColor, viewModel::setAccentColor)
            }

            item { SectionTitle("Folder Appearance") }
            item {
                FolderAppearanceCard(appearance, viewModel)
            }

            item { SectionTitle("Playback UI Options") }
            item {
                PlaybackUiCard(appearance, viewModel)
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.headlineSmall)
}

@Composable
private fun BackgroundThemeGrid(selected: BackgroundTheme, onSelect: (BackgroundTheme) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        BackgroundTheme.entries.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { theme ->
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSelect(theme) },
                        colors = androidx.compose.material3.CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                ColorSwatch(theme.toComposeColor(), size = 28.dp, isCircle = true)
                                if (selected == theme) {
                                    Icon(
                                        Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer)
                                            .padding(2.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.size(6.dp))
                            Text(theme.displayName, style = MaterialTheme.typography.titleSmall)
                            Text(
                                if (theme.isDark) "Calm night cinema" else "Warm daylight paper",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
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
