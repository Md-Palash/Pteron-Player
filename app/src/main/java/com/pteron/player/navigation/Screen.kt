package com.pteron.player.navigation

import android.net.Uri

sealed class Screen(val route: String) {
    data object Library : Screen("library")
    data object Videos : Screen("videos")
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
    data object Player : Screen("player/{videoId}/{bucketId}?shuffle={shuffle}") {
        fun createRoute(videoId: Long, bucketId: String, shuffle: Boolean = false) =
            "player/$videoId/${Uri.encode(bucketId)}?shuffle=$shuffle"
    }

    /** A video opened from another app (file manager, browser, ...) via ACTION_VIEW. */
    data object ExternalPlayer : Screen("external/{uri}") {
        fun createRoute(uri: String) = "external/${Uri.encode(uri)}"
    }
}

/** The four destinations shown in the persistent bottom navigation bar. */
enum class BottomNavDestination(val screen: Screen, val label: String) {
    LIBRARY(Screen.Library, "Library"),
    VIDEOS(Screen.Videos, "Videos"),
    PLAYLISTS(Screen.Playlists, "Playlists"),
    SETTINGS(Screen.Settings, "Settings")
}
