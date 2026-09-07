package com.pteron.player.ui.player.components

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.dp
import com.pteron.player.ui.player.TrackOption

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeedSelectorSheet(
    currentSpeed: Float,
    onSelect: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val speeds = listOf(
        0.25f,
        0.5f,
        0.75f,
        1.0f,
        1.25f,
        1.5f,
        1.75f,
        2.0f
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Text(
                text = "Playback speed",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(
                    horizontal = 16.dp,
                    vertical = 12.dp
                )
            )

            LazyColumn {
                items(speeds) { speed ->
                    ListItem(
                        headlineContent = {
                            Text(
                                text = if (speed == 1.0f) {
                                    "1.0x (Normal)"
                                } else {
                                    "${speed}x"
                                }
                            )
                        },
                        trailingContent = {
                            if (speed == currentSpeed) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "Selected"
                                )
                            }
                        },
                        modifier = Modifier.clickable {
                            onSelect(speed)
                        }
                    )
                }
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(
                    horizontal = 16.dp,
                    vertical = 12.dp
                )
            )

            LazyColumn {
                if (allowDisable) {
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
                                if (tracks.none { it.isSelected }) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = "Selected"
                                    )
                                }
                            },
                            modifier = Modifier.clickable {
                                onSelect(null)
                            }
                        )
                    }
                }

                items(tracks) { track ->
                    ListItem(
                        headlineContent = {
                            Text(track.label)
                        },
                        trailingContent = {
                            if (track.isSelected) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "Selected"
                                )
                            }
                        },
                        modifier = Modifier.clickable {
                            onSelect(track)
                        }
                    )
                }
            }
        }
    }
}
