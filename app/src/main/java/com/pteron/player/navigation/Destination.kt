package com.pteron.player.navigation

sealed class Destination(val route: String) {
    data object Home : Destination("home")
    data object ThemeSettings : Destination("theme_settings")

    data object FolderDetail : Destination("folder/{folderId}") {
        fun createRoute(folderId: String) = "folder/$folderId"
    }
}
