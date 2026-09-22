package com.pteron.player

import android.app.PictureInPictureParams
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.media3.common.util.UnstableApi
import com.pteron.player.navigation.PteronNavGraph
import com.pteron.player.theme.PteronTheme
import com.pteron.player.util.PipController
import kotlinx.coroutines.flow.MutableStateFlow

@UnstableApi
class MainActivity : ComponentActivity() {

    /** A video handed to us by another app (ACTION_VIEW), waiting for the nav graph to open it. */
    private val pendingExternalVideo = MutableStateFlow<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as PteronApp

        // The last used theme is read synchronously, so the window and the very first frame are
        // already in the right colors (DataStore itself only answers a moment later).
        val initialTheme = app.appearancePrefsRepository.cachedTheme()
        window.setBackgroundDrawable(ColorDrawable(initialTheme.backgroundArgb))

        // Only on a fresh launch: after a recreation the very same intent is still attached
        // to the activity and would re-open the video the person already closed.
        if (savedInstanceState == null) handleViewIntent(intent)

        setContent {
            // Only the theme is observed here (it emits when the theme itself changes), so
            // flipping any other appearance setting doesn't touch the app-wide theme at all.
            val appTheme by app.appearancePrefsRepository.theme.collectAsState(initial = initialTheme)
            val externalVideo by pendingExternalVideo.collectAsState()

            PteronTheme(theme = appTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PteronNavGraph(
                        app = app,
                        externalVideoUri = externalVideo,
                        onExternalVideoHandled = { pendingExternalVideo.value = null }
                    )
                }
            }
        }
    }

    // launchMode="singleTop": a second "Open with" while the app is already running lands here.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleViewIntent(intent)
    }

    private fun handleViewIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            intent.data?.let { pendingExternalVideo.value = it }
        }
    }

    /**
     * Automatically enters Picture-in-Picture when the user backgrounds the
     * app (home button / recents) while a video is actively playing, matching
     * standard Android video-player behavior.
     */
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // With background play on, the point is to keep just the audio going -- like a music
        // player -- so a floating PiP window is skipped entirely rather than opened and then
        // immediately fought with.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            PipController.isEligibleForAutoPip.value &&
            !PipController.backgroundPlaybackEnabled.value
        ) {
            val aspect = PipController.videoAspectRatio.value.coerceIn(0.42f, 2.39f)
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational((aspect * 100).toInt(), 100))
                .build()
            enterPictureInPictureMode(params)
        }
    }
}
