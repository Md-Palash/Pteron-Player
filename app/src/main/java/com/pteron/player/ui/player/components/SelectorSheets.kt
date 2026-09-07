package com.pteron.player.ui.player.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.SubtitlesOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.pteron.player.ui.player.TrackOption

private val speedOptions = listOf(
    0.5f,
    0.75f,
    1.0f,
    1.25f,
    1.5f,
    1.75f,
    2.0f
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeedSelectorSheet(
    currentSpeed: Float,
    onSelect: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            Text(
                text = "Playback speed",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(
                    horizontal = 20.dp,
                    vertical = 8.dp
                )
            )

            speedOptions.forEach { speed ->
                ListItem(
                    headlineContent = {
                        Text("${speed}x")
                    },
                    trailingContent = {
                        if (speed == currentSpeed) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = null
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(speed) {
                            detectTapAndSelect {
                                onSelect(speed)
                            }
                        }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackSelectorSheet(
    title: String,
    tracks: List<TrackOption>,
    allowDisable: Boolean,
    onSelect: (TrackOption?) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(
                    horizontal = 20.dp,
                    vertical = 8.dp
                )
            )

            if (tracks.isEmpty()) {
                Text(
                    text = "No tracks available for this video.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(
                        horizontal = 20.dp,
                        vertical = 12.dp
                    )
                )

                return@Column
            }

            if (allowDisable) {
                ListItem(
                    headlineContent = {
                        Text("Off")
                    },
                    leadingContent = {
                        Icon(
                            Icons.Filled.SubtitlesOff,
                            contentDescription = null
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(Unit) {
                            detectTapAndSelect {
                                onSelect(null)
                            }
                        }
                )
            }

            LazyColumn {
                items(tracks) { track ->
                    ListItem(
                        headlineContent = {
                            Text(track.label)
                        },
                        trailingContent = {
                            if (track.isSelected) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = null
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput(track) {
                                detectTapAndSelect {
                                    onSelect(track)
                                }
                            }
                    )
                }
            }
        }
    }
}

private suspend fun PointerInputScope.detectTapAndSelect(
    onSelect: () -> Unit
) {
    detectTapGestures {
        onSelect()
    }
}
