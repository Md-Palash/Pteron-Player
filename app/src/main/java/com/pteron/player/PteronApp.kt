package com.pteron.player

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.memory.MemoryCache
import com.pteron.player.data.media.MediaStoreRepository
import com.pteron.player.data.media.VideoThumbnailFetcher
import com.pteron.player.data.prefs.AppearancePrefsRepository
import com.pteron.player.data.prefs.PlaybackPrefsRepository
import com.pteron.player.data.prefs.PlaybackStateRepository

/**
 * Manual dependency container. The app intentionally avoids a DI framework
 * (Hilt/Koin) -- a handful of small repositories don't justify the extra
 * dependency and build-time cost, per the project's dependency policy.
 *
 * Everything here is created lazily, on first use, instead of in [onCreate]: nothing runs on
 * the cold-start path until a screen actually needs it, so the first frame appears sooner.
 */
class PteronApp : Application(), ImageLoaderFactory {

    val mediaStoreRepository: MediaStoreRepository by lazy { MediaStoreRepository(this) }
    val appearancePrefsRepository: AppearancePrefsRepository by lazy { AppearancePrefsRepository(this) }
    val playbackStateRepository: PlaybackStateRepository by lazy { PlaybackStateRepository(this) }
    val playbackPrefsRepository: PlaybackPrefsRepository by lazy { PlaybackPrefsRepository(this) }

    /**
     * Coil calls this the first time an image is requested (not at app start). A shared
     * ImageLoader with a bounded memory cache and the custom video-thumbnail fetcher, so
     * folder/video lists scroll smoothly even on large libraries without decoding
     * full-resolution frames.
     */
    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .components { add(VideoThumbnailFetcher.Factory(this@PteronApp)) }
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.2)
                    .build()
            }
            .crossfade(150)
            .build()
}
