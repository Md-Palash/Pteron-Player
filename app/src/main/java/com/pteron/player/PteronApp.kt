package com.pteron.player

import android.app.Application
import coil.Coil
import coil.ImageLoader
import coil.memory.MemoryCache
import com.pteron.player.data.media.MediaStoreRepository
import com.pteron.player.data.media.VideoThumbnailFetcher
import com.pteron.player.data.prefs.AppearancePrefsRepository
import com.pteron.player.data.prefs.PlaybackStateRepository

/**
 * Manual dependency container. The app intentionally avoids a DI framework
 * (Hilt/Koin) -- three small repositories don't justify the extra
 * dependency and build-time cost, per the project's dependency policy.
 */
class PteronApp : Application() {

    lateinit var mediaStoreRepository: MediaStoreRepository
        private set
    lateinit var appearancePrefsRepository: AppearancePrefsRepository
        private set
    lateinit var playbackStateRepository: PlaybackStateRepository
        private set

    override fun onCreate() {
        super.onCreate()
        mediaStoreRepository = MediaStoreRepository(this)
        appearancePrefsRepository = AppearancePrefsRepository(this)
        playbackStateRepository = PlaybackStateRepository(this)

        // A shared Coil ImageLoader with a bounded memory cache and the custom
        // video-thumbnail fetcher, so folder/video lists scroll smoothly even
        // on large libraries without decoding full-resolution frames.
        Coil.setImageLoader(
            ImageLoader.Builder(this)
                .components { add(VideoThumbnailFetcher.Factory(this@PteronApp)) }
                .memoryCache {
                    MemoryCache.Builder(this)
                        .maxSizePercent(0.2)
                        .build()
                }
                .crossfade(150)
                .build()
        )
    }
}
