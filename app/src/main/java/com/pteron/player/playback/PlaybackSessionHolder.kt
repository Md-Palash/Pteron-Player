package com.pteron.player.playback

import androidx.media3.session.MediaSession

/**
 * [PlaybackSessionService] needs a [MediaSession] to hand the system in
 * `onGetSession()`, but the actual ExoPlayer instance is owned by
 * `PlayerViewModel` (it needs to survive configuration changes and be
 * directly reachable for track selection, audio boost, etc.).
 *
 * Rather than restructure player ownership into the service -- which would
 * mean routing every playback command through `MediaController`/Binder IPC
 * and losing direct access to ExoPlayer-only APIs like `AnalyticsListener`
 * (used for the HW-decoder badge) -- the service and the ViewModel simply
 * share the same session object in-process via this holder. This is safe
 * specifically because the service and the ViewModel always run in the
 * same app process (no `android:process` isolation is used anywhere in
 * this app).
 */
object PlaybackSessionHolder {
    @Volatile
    var session: MediaSession? = null
}
