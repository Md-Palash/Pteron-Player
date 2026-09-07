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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioTrackSheet(
    tracks: List<TrackOption>,
    selectedTrack: TrackOption?,
    onTrackSelected: (TrackOption) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Text(
                text = "Audio Track",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(
                    horizontal = 16.dp,
                    vertical = 12.dp
                )
            )

            LazyColumn {
                items(tracks) { track ->
                    ListItem(
                        headlineContent = {
                            Text(track.label)
                        },
                        trailingContent = {
                            if (track == selectedTrack) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "Selected"
                                )
                            }
                        },
                        modifier = Modifier.detectTapAndSelect {
                            onTrackSelected(track)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubtitleTrackSheet(
    tracks: List<TrackOption>,
    selectedTrack: TrackOption?,
    onTrackSelected: (TrackOption) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Text(
                text = "Subtitles",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(
                    horizontal = 16.dp,
                    vertical = 12.dp
                )
            )

            LazyColumn {
                item {
                    ListItem(
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Filled.SubtitlesOff,
                                contentDescription = "Subtitles off"
                            )
                        },
                        headlineContent = {
                            Text("Off")
                        },
                        trailingContent = {
                            if (selectedTrack == null) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "Selected"
                                )
                            }
                        },
                        modifier = Modifier.detectTapAndSelect {
                            onTrackSelected(
                                TrackOption(
                                    id = -1,
                                    label = "Off"
                                )
                            )
                        }
                    )
                }

                items(tracks) { track ->
                    ListItem(
                        headlineContent = {
                            Text(track.label)
                        },
                        trailingContent = {
                            if (track == selectedTrack) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "Selected"
                                )
                            }
                        },
                        modifier = Modifier.detectTapAndSelect {
                            onTrackSelected(track)
                        }
                    )
                }
            }
        }
    }
}

private fun Modifier.detectTapAndSelect(
    onSelect: () -> Unit
): Modifier {
    return this.then(
        Modifier.pointerInput(Unit) {
            detectTapGestures {
                onSelect()
            }
        }
    )
}
