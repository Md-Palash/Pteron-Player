package com.pteron.player.navigation

import android.net.Uri

sealed class Screen(val route: String) {
    data object Library : Screen("library")
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
