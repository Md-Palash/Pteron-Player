package com.pteron.player

import android.app.PictureInPictureParams
import android.content.Intent
import android.net.Uri
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
import kotlinx.coroutines.flow.MutableStateFlow

@UnstableApi
class MainActivity : ComponentActivity() {

    /** A video handed to us by another app (ACTION_VIEW), waiting for the nav graph to open it. */
    private val pendingExternalVideo = MutableStateFlow<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as PteronApp

        // Only on a fresh launch: after a recreation the very same intent is still attached
        // to the activity and would re-open the video the person already closed.
        if (savedInstanceState == null) handleViewIntent(intent)

        setContent {
            val appearance by app.appearancePrefsRepository.state
                .collectAsState(initial = AppearanceState())
            val externalVideo by pendingExternalVideo.collectAsState()

            PteronTheme(appearance = appearance) {
                Surface(modifier = Modifier.fillMaxSize()) {
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && PipController.isEligibleForAutoPip.value) {
            val aspect = PipController.videoAspectRatio.value.coerceIn(0.42f, 2.39f)
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational((aspect * 100).toInt(), 100))
                .build()
            enterPictureInPictureMode(params)
        }
    }
}
