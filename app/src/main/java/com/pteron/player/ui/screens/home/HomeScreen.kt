package com.pteron.player.ui.screens.home

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pteron.player.data.mock.MockFolderRepository
import com.pteron.player.data.model.VideoFolder
import com.pteron.player.ui.theme.themedColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    isDarkTheme: Boolean,
    onFolderClick: (VideoFolder) -> Unit,
    onSettingsClick: () -> Unit
) {
    // Stage 1: mock data only, computed once. Swap MockFolderRepository
    // for a real media repository in a later stage.
    val folders = remember { MockFolderRepository.getFolders() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pteron Player") },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Theme settings"
                        )
                    }
                }
            )
        }
    ) { padding ->
        if (folders.isEmpty()) {
            HomeEmptyState(modifier = Modifier.padding(padding))
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 150.dp),
                contentPadding = PaddingValues(20.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                items(items = folders, key = { it.id }) { folder ->
                    FolderCard(
                        folder = folder,
                        folderColor = folder.color.themedColor(isDarkTheme),
                        onClick = { onFolderClick(folder) }
                    )
                }
            }
        }
    }
}
