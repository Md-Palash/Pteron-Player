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

    data object Player : Screen("player/{videoId}/{bucketId}") {
        fun createRoute(videoId: Long, bucketId: String) =
            "player/$videoId/${Uri.encode(bucketId)}"
    }
}

/** The four destinations shown in the persistent bottom navigation bar. */
enum class BottomNavDestination(val screen: Screen, val label: String) {
    LIBRARY(Screen.Library, "Library"),
    VIDEOS(Screen.Videos, "Videos"),
    PLAYLISTS(Screen.Playlists, "Playlists"),
    SETTINGS(Screen.Settings, "Settings")
}
