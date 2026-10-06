package com.pteron.player.navigation

import android.net.Uri

sealed class Screen(val route: String) {
    data object Library : Screen("library")
    data object Videos : Screen("videos")
    data object Audio : Screen("audio")
    data object Playlists : Screen("playlists")
    data object Settings : Screen("settings")

    data object Folder : Screen("folder/{bucketId}/{folderName}") {
        fun createRoute(bucketId: String, folderName: String) =
            "folder/${Uri.encode(bucketId)}/${Uri.encode(folderName)}"
    }

    /**
     * A video from the MediaStore library, played together with the rest of its folder.
     * [shuffle] starts the folder queue with shuffle turned on (used by "Shuffle play").
     */
    data object Player : Screen("player/{videoId}/{bucketId}?shuffle={shuffle}&playlistId={playlistId}") {
        /** [playlistId] >= 0 plays that playlist's queue instead of the folder's. */
        fun createRoute(videoId: Long, bucketId: String, shuffle: Boolean = false, playlistId: Long = -1L) =
            "player/$videoId/${Uri.encode(bucketId)}?shuffle=$shuffle&playlistId=$playlistId"
    }

    data object PlaylistDetail : Screen("playlist/{playlistId}") {
        fun createRoute(playlistId: Long) = "playlist/$playlistId"
    }

    data object AudioArtist : Screen("audio_artist/{artist}") {
        fun createRoute(artist: String) = "audio_artist/${Uri.encode(artist)}"
    }

    data object AudioPlaylist : Screen("audio_playlist/{playlistId}") {
        fun createRoute(playlistId: Long) = "audio_playlist/$playlistId"
    }

    data object NowPlaying : Screen("now_playing")

    /** A video opened from another app (file manager, browser, ...) via ACTION_VIEW. */
    data object ExternalPlayer : Screen("external/{uri}") {
        fun createRoute(uri: String) = "external/${Uri.encode(uri)}"
    }
}

/** The five destinations shown in the persistent bottom navigation bar. */
enum class BottomNavDestination(val screen: Screen, val label: String) {
    LIBRARY(Screen.Library, "Library"),
    VIDEOS(Screen.Videos, "Videos"),
    AUDIO(Screen.Audio, "Audio"),
    PLAYLISTS(Screen.Playlists, "Playlists"),
    SETTINGS(Screen.Settings, "Settings")
}
