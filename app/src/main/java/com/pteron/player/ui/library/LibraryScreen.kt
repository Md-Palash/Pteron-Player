package com.pteron.player.ui.library

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.ViewList
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.pteron.player.data.model.ViewMode
import com.pteron.player.ui.common.AppIcon
import com.pteron.player.ui.common.CircularActionButton
import com.pteron.player.ui.common.DarkModeButton
import com.pteron.player.ui.common.EmptyLibraryState
import com.pteron.player.ui.common.ErrorState
import com.pteron.player.ui.common.LoadingState
import com.pteron.player.ui.common.NoSearchResultsState
import com.pteron.player.ui.common.PermissionRationaleState
import com.pteron.player.ui.common.PteronWordmark
import com.pteron.player.ui.common.SearchCard
import com.pteron.player.ui.common.SectionTitle
import com.pteron.player.ui.common.videoLibraryPermission
import com.pteron.player.ui.library.components.ContinueWatchingCard
import com.pteron.player.ui.library.components.FolderCard
import com.pteron.player.ui.library.components.FolderListRow

/** Extra bottom padding so scrollable content never sits behind the floating nav pill. */
private val BottomNavClearance = 108.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    onOpenFolder: (bucketId: String, name: String) -> Unit,
    onOpenVideo: (videoId: Long, bucketId: String) -> Unit
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
                        AppIcon(size = 36.dp)
                        Spacer(Modifier.size(10.dp))
                        PteronWordmark(height = 38.dp)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CircularActionButton(
                            icon = if (effectiveViewMode == ViewMode.GRID) Icons.Outlined.ViewList else Icons.Outlined.GridView,
                            contentDescription = "Toggle grid or list view",
                            onClick = { viewModeOverride = if (effectiveViewMode == ViewMode.GRID) ViewMode.LIST else ViewMode.GRID }
                        )
                        DarkModeButton(onClick = viewModel::toggleDarkMode)
                    }
                }

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
}

@Composable
private fun LibraryContent(
    uiState: LibraryUiState,
    viewMode: ViewMode,
    onSearchQueryChange: (String) -> Unit,
    onOpenFolder: (String, String) -> Unit,
    onOpenVideo: (Long, String) -> Unit
) {
    val filtered = uiState.filteredFolders

    Column(modifier = Modifier.fillMaxSize()) {
        // Narrower than the screen and centered between the two edges.
        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), contentAlignment = Alignment.Center) {
            SearchCard(
                value = uiState.searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = "Search folders..."
            )
        }

        if (uiState.searchQuery.isNotBlank() && filtered.isEmpty()) {
            NoSearchResultsState()
            return@Column
        }

        if (uiState.continueWatching.isNotEmpty() && uiState.searchQuery.isBlank()) {
            SectionTitle("Continue Watching")
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.continueWatching, key = { it.id }) { video ->
                    ContinueWatchingCard(video = video, onClick = { onOpenVideo(video.id, video.bucketId) })
                }
            }
        }

        SectionTitle(text = "Folders (${filtered.size})") {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            }
        }

        when (viewMode) {
            ViewMode.GRID -> LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = BottomNavClearance),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filtered, key = { it.bucketId }) { folder ->
                    FolderCard(
                        folder = folder,
                        showVideoCount = uiState.appearance.showVideoCountBadge,
                        showFolderSize = uiState.appearance.showFolderSizeBadge,
                        onClick = { onOpenFolder(folder.bucketId, folder.name) }
                    )
                }
            }
            ViewMode.LIST -> androidx.compose.foundation.lazy.LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = BottomNavClearance),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filtered, key = { it.bucketId }) { folder ->
                    FolderListRow(
                        folder = folder,
                        showVideoCount = uiState.appearance.showVideoCountBadge,
                        showFolderSize = uiState.appearance.showFolderSizeBadge,
                        onClick = { onOpenFolder(folder.bucketId, folder.name) }
                    )
                }
            }
        }
    }
}
