package com.pteron.player.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.pteron.player.data.model.AppThemeMode
import com.pteron.player.data.model.FolderColor
import com.pteron.player.ui.state.AppSettingsViewModel
import com.pteron.player.ui.theme.themedColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSettingsScreen(
    settingsViewModel: AppSettingsViewModel,
    isDarkTheme: Boolean,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Theme") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 20.dp)
        ) {
            SectionLabel("Appearance")
            Spacer(Modifier.height(8.dp))
            AppearanceOptions(
                selected = settingsViewModel.themeMode,
                onSelect = settingsViewModel::updateThemeMode
            )

            Spacer(Modifier.height(28.dp))

            SectionLabel("Folder color")
            Spacer(Modifier.height(12.dp))
            FolderColorGrid(
                selected = settingsViewModel.defaultFolderColor,
                isDarkTheme = isDarkTheme,
                onSelect = settingsViewModel::updateFolderColor
            )

            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun AppearanceOptions(
    selected: AppThemeMode,
    onSelect: (AppThemeMode) -> Unit
) {
    val modes = AppThemeMode.values()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surface)
    ) {
        modes.forEachIndexed { index, mode ->
            AppearanceRow(
                label = mode.displayName(),
                selected = mode == selected,
                onClick = { onSelect(mode) }
            )
            if (index != modes.lastIndex) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

@Composable
private fun AppearanceRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .semantics {
                contentDescription = if (selected) "$label, selected" else label
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private fun AppThemeMode.displayName(): String = when (this) {
    AppThemeMode.LIGHT -> "Light"
    AppThemeMode.DARK -> "Dark"
    AppThemeMode.SYSTEM -> "System default"
}

@Composable
private fun FolderColorGrid(
    selected: FolderColor,
    isDarkTheme: Boolean,
    onSelect: (FolderColor) -> Unit
) {
    val colors = FolderColor.values().toList()
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 56.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.heightIn(max = 220.dp)
    ) {
        items(items = colors, key = { it.name }) { color ->
            FolderColorSwatch(
                color = color,
                isDarkTheme = isDarkTheme,
                selected = color == selected,
                onClick = { onSelect(color) }
            )
        }
    }
}

@Composable
private fun FolderColorSwatch(
    color: FolderColor,
    isDarkTheme: Boolean,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(color.themedColor(isDarkTheme))
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = if (selected) "${color.label}, selected" else color.label
            },
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = Color.White
            )
        }
    }
}
