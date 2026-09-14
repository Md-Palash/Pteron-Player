package com.pteron.player.ui.common

import android.Manifest
import android.os.Build

/** The single runtime permission Pteron Player needs, resolved for the running OS version. */
val videoLibraryPermission: String
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_VIDEO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
