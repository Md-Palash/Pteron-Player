package com.pteron.player.playback

import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * Gives Android's system media UI (notification-shade transport controls,
 * lock-screen controls, headphone/Bluetooth media-button routing) something
 * to bind to. It does not own a player itself -- see [PlaybackSessionHolder]
 * for why sharing the ViewModel's session in-process is the right call here
 * rather than moving player ownership into this service.
 *
 * If no video is currently open, [PlaybackSessionHolder.session] is null and
 * this correctly returns null: per the Media3 contract, that just means no
 * session is available yet, which is expected when nothing is playing.
 */
class PlaybackSessionService : MediaSessionService() {

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        PlaybackSessionHolder.session

    override fun onDestroy() {
        // Do not release the session here -- it's owned by PlayerViewModel and
        // released when that ViewModel is cleared, not when this service stops.
        super.onDestroy()
    }
}
