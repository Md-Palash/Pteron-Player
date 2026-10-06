package com.pteron.player.playback

import android.content.Intent
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/** The audio player's session, shared in-process with [AudioPlayerController] (same idea as [PlaybackSessionHolder]). */
object AudioSessionHolder {
    @Volatile
    var session: MediaSession? = null
}

/**
 * Notification-shade / lock-screen / headphone-button controls for the music player. It owns no
 * player: the session is registered explicitly with [addSession] because nothing in the app ever
 * connects a MediaController (see [PlaybackSessionService]).
 */
class AudioSessionService : MediaSessionService() {

    override fun onCreate() {
        super.onCreate()
        attachSession()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        attachSession()
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = AudioSessionHolder.session

    /** Swiping the app out of recents stops the music instead of leaving it running headless. */
    override fun onTaskRemoved(rootIntent: Intent?) {
        sessions.forEach { it.player.pause() }
        stopSelf()
    }

    private fun attachSession() {
        val session = AudioSessionHolder.session ?: return
        if (sessions.contains(session)) return
        sessions.filter { it.id == session.id }.forEach { removeSession(it) }
        addSession(session)
    }
}
