package com.pteron.player

import android.app.PictureInPictureParams
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.media3.common.util.UnstableApi
import com.pteron.player.data.prefs.AppearanceState
import com.pteron.player.navigation.PteronNavGraph
import com.pteron.player.theme.PteronTheme
import com.pteron.player.util.PipController

@UnstableApi
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as PteronApp

        setContent {
            val appearance by app.appearancePrefsRepository.state
                .collectAsState(initial = AppearanceState())

            PteronTheme(appearance = appearance) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PteronNavGraph(app = app)
                }
            }
        }
    }

    /**
     * Automatically enters Picture-in-Picture when the user backgrounds the
     * app (home button / recents) while a video is actively playing, matching
     * standard Android video-player behavior.
     */
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && PipController.isEligibleForAutoPip.value) {
            val aspect = PipController.videoAspectRatio.value.coerceIn(0.42f, 2.39f)
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational((aspect * 100).toInt(), 100))
                .build()
            enterPictureInPictureMode(params)
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        // The player screen keeps observing the same ExoPlayer instance regardless of PiP
        // state, so no extra wiring is needed here beyond the system's own view resizing.
    }
}
