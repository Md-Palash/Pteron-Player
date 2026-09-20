package com.pteron.player.playback

import android.content.Intent
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * Gives Android's system media UI (notification-shade transport controls,
 * lock-screen controls, headphone/Bluetooth media-button routing) something
 * to bind to. It does not own a player itself -- see [PlaybackSessionHolder]
 * for why sharing the ViewModel's session in-process is the right call here
 * rather than moving player ownership into this service.
 *
 * Media3 only calls [onGetSession] when some MediaController connects, and nothing in this app
 * ever creates one -- so on its own the notification would never be posted. The session is
 * therefore registered explicitly with [addSession] as soon as the service starts.
 */
class PlaybackSessionService : MediaSessionService() {

    override fun onCreate() {
        super.onCreate()
        attachSession()
    }

    // startService() is called again for every new player; the service may already be alive.
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        attachSession()
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        PlaybackSessionHolder.session

    /** Swiping the app out of recents should stop playback, not leave audio running headless. */
    override fun onTaskRemoved(rootIntent: Intent?) {
        sessions.forEach { it.player.pause() }
        stopSelf()
    }

    override fun onDestroy() {
        // Do not release the session here -- it's owned by PlayerViewModel and
        // released when that ViewModel is cleared, not when this service stops.
        super.onDestroy()
    }

    private fun attachSession() {
        val session = PlaybackSessionHolder.session ?: return
        if (sessions.contains(session)) return
        // A previous player's session (same default id) may still be registered; ids must be unique.
        sessions.filter { it.id == session.id }.forEach { removeSession(it) }
        addSession(session)
    }
}
