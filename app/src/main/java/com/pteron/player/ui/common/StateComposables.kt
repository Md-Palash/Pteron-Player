package com.pteron.player.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderOff
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun FullScreenMessage(
    icon: ImageVector,
    title: String,
    description: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(PaddingValues(32.dp)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.outline
        )
        androidx.compose.foundation.layout.Spacer(Modifier.size(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        androidx.compose.foundation.layout.Spacer(Modifier.size(8.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (actionLabel != null && onAction != null) {
            androidx.compose.foundation.layout.Spacer(Modifier.size(20.dp))
            Button(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun PermissionRationaleState(onRequestPermission: () -> Unit, permanentlyDenied: Boolean, onOpenSettings: () -> Unit) {
    FullScreenMessage(
        icon = Icons.Outlined.Lock,
        title = "Access your videos",
        description = if (permanentlyDenied) {
            "Video access was denied. Enable it from system Settings to browse your library."
        } else {
            "Pteron Player needs permission to see the videos already stored on your device. Nothing leaves your phone."
        },
        actionLabel = if (permanentlyDenied) "Open settings" else "Grant access",
        onAction = if (permanentlyDenied) onOpenSettings else onRequestPermission
    )
}

@Composable
fun EmptyLibraryState(onRefresh: () -> Unit) {
    FullScreenMessage(
        icon = Icons.Outlined.FolderOff,
        title = "No videos found",
        description = "We couldn't find any videos on this device yet. Add some, then refresh.",
        actionLabel = "Refresh",
        onAction = onRefresh
    )
}

@Composable
fun NoSearchResultsState() {
    FullScreenMessage(
        icon = Icons.Outlined.SearchOff,
        title = "No matches",
        description = "Try a different search term or clear your filters."
    )
}

@Composable
fun ErrorState(message: String, onRetry: () -> Unit) {
    FullScreenMessage(
        icon = Icons.Outlined.WarningAmber,
        title = "Something went wrong",
        description = message,
        actionLabel = "Try again",
        onAction = onRetry
    )
}
