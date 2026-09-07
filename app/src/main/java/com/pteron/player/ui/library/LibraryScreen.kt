package com.pteron.player.ui.library

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.pteron.player.R
import com.pteron.player.data.model.ViewMode
import com.pteron.player.ui.common.EmptyLibraryState
import com.pteron.player.ui.common.ErrorState
import com.pteron.player.ui.common.LoadingState
import com.pteron.player.ui.common.NoSearchResultsState
import com.pteron.player.ui.common.PermissionRationaleState
import com.pteron.player.ui.common.videoLibraryPermission
import com.pteron.player.ui.library.components.ContinueWatchingCard
import com.pteron.player.ui.library.components.FolderCard
import com.pteron.player.ui.library.components.FolderListRow
import com.pteron.player.theme.toComposeColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    onOpenFolder: (bucketId: String, name: String) -> Unit,
    onOpenVideo: (videoId: Long, bucketId: String) -> Unit,
    onOpenSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val activity = context as? android.app.Activity
        val canAskAgain = granted || activity == null ||
            androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(activity, videoLibraryPermission)
        viewModel.onPermissionResult(granted, canAskAgain)
    }

    val alreadyGranted = ContextCompat.checkSelfPermission(context, videoLibraryPermission) ==
        android.content.pm.PackageManager.PERMISSION_GRANTED

    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (alreadyGranted) {
            viewModel.onPermissionResult(true, true)
        } else {
            permissionLauncher.launch(videoLibraryPermission)
        }
    }

    var viewModeOverride by remember { mutableStateOf<ViewMode?>(null) }
    val effectiveViewMode = viewModeOverride ?: uiState.appearance.viewMode

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = { viewModeOverride = if (effectiveViewMode == ViewMode.GRID) ViewMode.LIST else ViewMode.GRID }) {
                        Icon(
                            imageVector = if (effectiveViewMode == ViewMode.GRID) Icons.Filled.ViewList else Icons.Filled.GridView,
                            contentDescription = "Toggle grid or list view"
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                uiState.permission == PermissionUiState.DENIED -> PermissionRationaleState(
                    onRequestPermission = { permissionLauncher.launch(videoLibraryPermission) },
                    permanentlyDenied = false,
                    onOpenSettings = {}
                )
                uiState.permission == PermissionUiState.PERMANENTLY_DENIED -> PermissionRationaleState(
                    onRequestPermission = {},
                    permanentlyDenied = true,
                    onOpenSettings = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    }
                )
                uiState.errorMessage != null -> ErrorState(uiState.errorMessage!!, onRetry = viewModel::refresh)
                uiState.isLoading && uiState.allFolders.isEmpty() -> LoadingState()
                uiState.permission == PermissionUiState.GRANTED && uiState.allFolders.isEmpty() -> EmptyLibraryState(onRefresh = viewModel::refresh)
                else -> LibraryContent(
                    uiState = uiState,
                    viewMode = effectiveViewMode,
                    onSearchQueryChange = viewModel::onSearchQueryChange,
                    onOpenFolder = onOpenFolder,
                    onOpenVideo = onOpenVideo
                )
            }
        }
    }
}

@Composable
private fun LibraryContent(
    uiState: LibraryUiState,
    viewMode: ViewMode,
    onSearchQueryChange: (String) -> Unit,
    onOpenFolder: (String, String) -> Unit,
    onOpenVideo: (Long, String) -> Unit
) {
    val folderTone = uiState.appearance.folderTone.toComposeColor()
    val filtered = uiState.filteredFolders

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            placeholder = { Text("Search folders...") },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            singleLine = true
        )

        if (uiState.searchQuery.isNotBlank() && filtered.isEmpty()) {
            NoSearchResultsState()
            return@Column
        }

        if (uiState.continueWatching.isNotEmpty() && uiState.searchQuery.isBlank()) {
            Text(
                "Continue Watching",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.continueWatching, key = { it.id }) { video ->
                    ContinueWatchingCard(video = video, onClick = { onOpenVideo(video.id, video.bucketId) })
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Folders (${filtered.size})", style = MaterialTheme.typography.headlineSmall)
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            }
        }

        when (viewMode) {
            ViewMode.GRID -> LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filtered, key = { it.bucketId }) { folder ->
                    FolderCard(
                        folder = folder,
                        folderTone = folderTone,
                        showVideoCount = uiState.appearance.showVideoCountBadge,
                        showFolderSize = uiState.appearance.showFolderSizeBadge,
                        onClick = { onOpenFolder(folder.bucketId, folder.name) }
                    )
                }
            }
            ViewMode.LIST -> androidx.compose.foundation.lazy.LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filtered, key = { it.bucketId }) { folder ->
                    FolderListRow(
                        folder = folder,
                        folderTone = folderTone,
                        showVideoCount = uiState.appearance.showVideoCountBadge,
                        showFolderSize = uiState.appearance.showFolderSizeBadge,
                        onClick = { onOpenFolder(folder.bucketId, folder.name) }
                    )
                }
            }
        }
    }
}
