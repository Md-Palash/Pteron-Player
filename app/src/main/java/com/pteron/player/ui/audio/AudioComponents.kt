package com.pteron.player.ui.audio

import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.PlaylistAdd
import androidx.compose.material.icons.outlined.PlaylistPlay
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.pteron.player.data.model.AudioItem
import com.pteron.player.data.model.Playlist
import com.pteron.player.playback.AudioPlayerController
import com.pteron.player.ui.common.PteronClickableCard
import com.pteron.player.ui.common.bouncyClickable
import com.pteron.player.ui.playlists.PlaylistNameDialog
import com.pteron.player.util.formatTimecode

// --- Artwork ----------------------------------------------------------------------------------

/** Cover art for a song; a music-note glyph sits behind the image so a card is never blank. */
@Composable
fun AudioArt(contentUri: String?, modifier: Modifier = Modifier, iconSize: Dp = 32.dp) {
    val context = LocalContext.current
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.MusicNote,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(iconSize)
        )
        if (contentUri != null) {
            val request = remember(contentUri) {
                ImageRequest.Builder(context).data(Uri.parse(contentUri)).crossfade(150).build()
            }
            AsyncImage(
                model = request,
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}

// --- Cards ------------------------------------------------------------------------------------

private val CardShape = RoundedCornerShape(18.dp)

/** Square, rounded card filled with the cover; the song name and artist sit under it. */
@Composable
fun SongCard(song: AudioItem, onClick: () -> Unit, onMenu: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.bouncyClickable(pressedScale = 0.97f, onClick = onClick)) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(CardShape)) {
            AudioArt(song.contentUri, Modifier.fillMaxSize())
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(5.dp)
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable(onClick = onMenu),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.MoreVert, contentDescription = "More options", tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }
        CardCaption(song.title, song.artist)
    }
}

/** The same square card for a playlist or an artist ([coverUri] == null shows an accent tile with [icon]). */
@Composable
fun CollectionCard(
    title: String,
    subtitle: String,
    coverUri: String?,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.bouncyClickable(pressedScale = 0.97f, onClick = onClick)) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(CardShape)) {
            if (coverUri != null) {
                AudioArt(coverUri, Modifier.fillMaxSize())
            } else {
                Box(
                    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(34.dp))
                }
            }
        }
        CardCaption(title, subtitle)
    }
}

@Composable
private fun CardCaption(title: String, subtitle: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(top = 6.dp, start = 2.dp, end = 2.dp)
    )
    Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(horizontal = 2.dp)
    )
}

/** A song as a list row (artist and playlist pages). */
@Composable
fun SongListRow(
    song: AudioItem,
    isCurrent: Boolean,
    onClick: () -> Unit,
    onMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    PteronClickableCard(onClick = onClick, modifier = modifier.fillMaxWidth(), shape = CardShape) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AudioArt(song.contentUri, Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    song.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${song.artist} • ${formatTimecode(song.durationMs)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onMenu) { Icon(Icons.Outlined.MoreVert, contentDescription = "More options") }
        }
    }
}

// --- Headings, tabs, rows ---------------------------------------------------------------------

private val AudioHeaderHeight = 46.dp

/** A bold, moderately large heading with a thin separator line under it. */
@Composable
fun AudioSectionHeader(title: String, modifier: Modifier = Modifier, onSeeAll: (() -> Unit)? = null) {
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        // A fixed row height: the "See all" button is taller than a bare heading, which used to
        // leave the gap above the separator different from section to section.
        Row(
            modifier = Modifier.fillMaxWidth().height(AudioHeaderHeight),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            if (onSeeAll != null) TextButton(onClick = onSeeAll) { Text("See all") }
        }
        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f))
    }
}

@Composable
fun EmptyHint(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(horizontal = 20.dp, vertical = 8.dp)
    )
}

/** One line of song cards, scrolling sideways. */
@Composable
fun SongCardRow(
    songs: List<AudioItem>,
    emptyText: String,
    onPlay: (List<AudioItem>, Int) -> Unit,
    onMenu: (AudioItem) -> Unit
) {
    if (songs.isEmpty()) {
        EmptyHint(emptyText)
        return
    }
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        itemsIndexed(songs.take(30), key = { _, song -> song.id }) { index, song ->
            SongCard(
                song = song,
                onClick = { onPlay(songs, index) },
                onMenu = { onMenu(song) },
                modifier = Modifier.size(width = 132.dp, height = 190.dp)
            )
        }
    }
}

/** The tabloid (pill) buttons at the top of the Audio section. */
@Composable
fun AudioTabRow(labels: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        itemsIndexed(labels) { index, label -> AudioTab(label, index == selected) { onSelect(index) } }
    }
}

@Composable
private fun AudioTab(label: String, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val container by animateColorAsState(if (selected) scheme.primary else scheme.surfaceContainer, tween(180), label = "audioTabColor")
    val content by animateColorAsState(if (selected) scheme.onPrimary else scheme.onSurface, tween(180), label = "audioTabContent")
    PteronClickableCard(
        onClick = onClick,
        shape = RoundedCornerShape(percent = 50),
        color = container,
        borderColor = if (selected) Color.Transparent else scheme.outlineVariant.copy(alpha = 0.5f)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.titleSmall,
            color = content,
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp)
        )
    }
}

// --- Mini player ------------------------------------------------------------------------------

/**
 * A wide rounded rectangle above the bottom bar: a rounded-square cover on the left, the title in
 * the middle and round previous / play-pause / next buttons on the right. Play-pause is the biggest
 * and the darkest (accent) one; previous and next are smaller and in a lighter tint of it.
 * Tapping the card opens Now Playing.
 */
@Composable
fun MiniPlayer(controller: AudioPlayerController, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val state by controller.state.collectAsState()
    val song = state.current ?: return
    val scheme = MaterialTheme.colorScheme
    PteronClickableCard(
        onClick = onOpen,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        color = scheme.surfaceContainerHighest
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 6.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ProgressThumb(controller, song.contentUri, state.durationMs)
            Column(modifier = Modifier.weight(1f)) {
                Text(song.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    song.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                MiniRoundButton(
                    icon = Icons.Rounded.FastRewind,
                    description = "Previous",
                    size = 36.dp,
                    container = scheme.primary.copy(alpha = 0.16f),
                    tint = scheme.primary,
                    enabled = state.hasPrevious,
                    onClick = controller::previous
                )
                MiniRoundButton(
                    icon = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    description = if (state.isPlaying) "Pause" else "Play",
                    size = 56.dp,
                    container = scheme.primary,
                    tint = scheme.onPrimary,
                    onClick = controller::togglePlayPause
                )
                MiniRoundButton(
                    icon = Icons.Rounded.FastForward,
                    description = "Next",
                    size = 36.dp,
                    container = scheme.primary.copy(alpha = 0.16f),
                    tint = scheme.primary,
                    enabled = state.hasNext,
                    onClick = controller::next
                )
            }
        }
    }
}

/** Round cover with a thin progress ring around it. The position is read only while drawing. */
@Composable
private fun ProgressThumb(controller: AudioPlayerController, contentUri: String, durationMs: Long) {
    val scheme = MaterialTheme.colorScheme
    val position = controller.positionMs.collectAsStateWithLifecycle()
    val track = scheme.outlineVariant.copy(alpha = 0.6f)
    val accent = scheme.primary
    Box(modifier = Modifier.size(58.dp), contentAlignment = Alignment.Center) {
        AudioArt(contentUri, Modifier.size(46.dp).clip(CircleShape), iconSize = 22.dp)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 3.dp.toPx()
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(track, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
            val p = (position.value.toFloat() / durationMs.coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)
            if (p > 0f) {
                drawArc(accent, -90f, 360f * p, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            }
        }
    }
}

@Composable
private fun MiniRoundButton(
    icon: ImageVector,
    description: String,
    size: Dp,
    container: Color,
    tint: Color,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(container)
            .bouncyClickable(enabled = enabled, pressedScale = 0.92f, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = if (enabled) tint else tint.copy(alpha = 0.35f),
            modifier = Modifier.size(size * 0.5f)
        )
    }
}

// --- Song menu & add-to-playlist ----------------------------------------------------------------

data class SheetAction(val label: String, val icon: ImageVector, val onClick: () -> Unit)

/** Which song's menu / add-to-playlist sheet is open; shared by a screen and its [SongMenuHost]. */
class SongMenuState {
    var options by mutableStateOf<AudioItem?>(null)
    var addToPlaylist by mutableStateOf<AudioItem?>(null)
}

/**
 * Hosts the song "..." sheet (favorite, play next, add to playlist and any [extra] actions) and the
 * playlist picker behind it.
 */
@Composable
fun SongMenuHost(
    menu: SongMenuState,
    viewModel: AudioViewModel,
    controller: AudioPlayerController,
    playlists: List<Playlist>,
    extra: (AudioItem) -> List<SheetAction> = { emptyList() }
) {
    val context = LocalContext.current
    fun toast(message: String) = Toast.makeText(context, message, Toast.LENGTH_SHORT).show()

    menu.options?.let { song ->
        val actions = buildList {
            add(
                SheetAction(
                    if (song.isFavorite) "Remove from favorites" else "Add to favorites",
                    if (song.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder
                ) { viewModel.toggleFavorite(song) }
            )
            add(SheetAction("Play next", Icons.Outlined.SkipNext) {
                controller.playNext(song)
                toast("Playing next")
            })
            add(SheetAction("Add to playlist", Icons.Outlined.PlaylistAdd) { menu.addToPlaylist = song })
            addAll(extra(song))
        }
        SongOptionsSheet(song = song, actions = actions, onDismiss = { menu.options = null })
    }
    menu.addToPlaylist?.let { song ->
        AddToAudioPlaylistSheet(
            song = song,
            playlists = playlists,
            onToggle = { playlist, contains ->
                if (contains) {
                    viewModel.removeFromPlaylist(playlist.id, song.id)
                    toast("Removed from ${playlist.name}")
                } else {
                    viewModel.addToPlaylist(playlist.id, song.id)
                    toast("Added to ${playlist.name}")
                }
            },
            onCreate = { name ->
                viewModel.createPlaylist(name, song.id)
                toast("Added to ${name.trim()}")
            },
            onDismiss = { menu.addToPlaylist = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SongOptionsSheet(song: AudioItem, actions: List<SheetAction>, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AudioArt(song.contentUri, Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)), iconSize = 22.dp)
                Column {
                    Text(song.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(song.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
            }
            actions.forEach { action ->
                SheetRow(
                    icon = action.icon,
                    label = action.label,
                    supporting = null,
                    selected = false,
                    onClick = {
                        onDismiss()
                        action.onClick()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddToAudioPlaylistSheet(
    song: AudioItem,
    playlists: List<Playlist>,
    onToggle: (Playlist, Boolean) -> Unit,
    onCreate: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var showCreate by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Add to playlist",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
            )
            SheetRow(Icons.Outlined.Add, "New playlist", null, false) { showCreate = true }
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(playlists, key = { _, p -> p.id }) { _, playlist ->
                    val contains = song.id in playlist.videoIds
                    SheetRow(
                        icon = Icons.Outlined.PlaylistPlay,
                        label = playlist.name,
                        supporting = "${playlist.videoIds.size} song${if (playlist.videoIds.size == 1) "" else "s"}",
                        selected = contains
                    ) {
                        onToggle(playlist, contains)
                        onDismiss()
                    }
                }
            }
        }
    }
    if (showCreate) {
        PlaylistNameDialog(
            title = "New playlist",
            confirmLabel = "Create",
            onConfirm = {
                showCreate = false
                onCreate(it)
                onDismiss()
            },
            onDismiss = { showCreate = false }
        )
    }
}

/** The card row shared by the audio sheets: medium shade normally, the accent when selected. */
@Composable
private fun SheetRow(
    icon: ImageVector,
    label: String,
    supporting: String?,
    selected: Boolean,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val content = if (selected) scheme.onPrimary else scheme.onSurface
    PteronClickableCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) scheme.primary else scheme.surfaceContainer,
        borderColor = if (selected) Color.Transparent else scheme.outlineVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(22.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelLarge, color = content, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (supporting != null) {
                    Text(supporting, style = MaterialTheme.typography.bodySmall, color = content.copy(alpha = 0.75f))
                }
            }
            if (selected) {
                Icon(Icons.Filled.Check, contentDescription = "Selected", tint = content, modifier = Modifier.size(18.dp))
            }
        }
    }
}
