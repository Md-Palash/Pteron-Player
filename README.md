# Pteron Player

A native Android video player built with Kotlin, Jetpack Compose, and
Media3/ExoPlayer, styled after the Stitch "warm terracotta" design system.

## ⚠️ Still read this before assuming it's done

Written directly as Kotlin/Compose source with no Android SDK, emulator, or
device available to me — nothing here has been compiled by me. Static
verification (brace/paren balance, unused/duplicate/malformed imports) has
been run after every change in every pass on this project, and it's clean
as of this one, but that is not a substitute for a real build. Please build
it and send me the log if anything breaks.

## This pass: feature checklist refinement

You gave a full checklist (Playback / Gestures / Video / Audio / Subtitles /
Library / Android / Settings) and asked me to refine against it and
optimize where I found the chance. Here's what changed, organized the same
way, with what's real vs. what I deliberately didn't fake:

### 🎬 Playback
- **Native ExoPlayer playlist refactor** — this is the "found scope to
  optimize" item. The player used to manually track a `playlist: List<VideoItem>`
  and `currentIndex`, reimplementing previous/next itself. Replaced with
  ExoPlayer's own playlist (`setMediaItems`), which is less custom code,
  fewer edge cases, and is what makes repeat/shuffle work natively instead
  of being bolted on.
- **Repeat** (off / one / all) via `player.repeatMode` — real, native.
- **Shuffle** via `player.shuffleModeEnabled` — real, native.
- **Auto-play next** was already there, but now means something more
  precise: the queue always advances so shuffle/repeat-all keep working
  structurally, but if this is off, playback pauses on arrival at the next
  item instead of continuing — you see what's next without it starting
  itself. A manual Next/Previous tap always plays regardless.
- Play/pause, ±seek, seek bar, previous/next, speed (0.5–2×), resume — all
  already existed and are unchanged in behavior.

### 👆 Gesture Controls
All of these already existed from earlier passes (tap to show/hide,
double-tap seek, swipe-to-seek, brightness/volume swipes) — unchanged here.

### 🖥️ Video
- **Fullscreen (immersive mode)** — genuinely didn't exist before. The
  player now hides the status bar and navigation bar while open (swipe from
  the edge still reveals them briefly, as expected), restoring them on exit.
- Portrait/landscape, orientation lock, keep screen on — already existed.
- **Aspect ratio renamed** to Fit / Fill / Crop (was Fit / Crop / "16:9") to
  match standard terminology, and it's now a real **default aspect ratio**
  setting, not just an in-player cycle button.

### 🔊 Audio
- **Mute** — new. Toggles `player.volume` between 0 and its last value,
  with its own button in the player controls.
- Volume control, audio track selection — already existed.
- **Audio delay — not implemented, on purpose.** Real AV-sync correction
  needs a custom `AudioProcessor` doing raw PCM buffer manipulation (queued
  silence, buffer accounting, format renegotiation). I can describe how
  it'd work, but writing it blind with no device to verify against risks
  shipping something that silently does nothing or — worse — audibly
  glitches. I'd rather say plainly that this needs real device testing to
  do responsibly than fake a slider that doesn't actually shift anything.

### 💬 Subtitles
- **External subtitle loading** — new. A "Load subtitle file..." option in
  the subtitle sheet opens the system file picker (`.srt`/`.vtt`/`.ass`),
  and rebuilds the current video's subtitle configuration to include it,
  preserving playback position.
- Enable/disable, track selection, size — already existed.
- **Subtitle delay — not implemented, on purpose**, for a related but
  distinct reason from audio delay: doing this properly means replacing
  Media3's built-in subtitle rendering with manually-timed cue display (so
  cues can be held longer or shown earlier), which is a real architecture
  change to the player surface. Same call as audio delay: didn't want to
  half-build it.

### 📁 Library
- **Favorites** and **Recently Played** are now real, accessible views —
  implemented as a new `Favorites` filter chip and a new `Recently played`
  sort option, added to the shared `VideoFilter`/`SortOption` enums. This
  was a deliberate simplification over building two entirely separate
  screens: both the Folder view and the new Videos tab already had
  filter-chip and sort-menu UI, so extending those enums makes Favorites
  and Recently Played show up in **both places for free**, instead of
  duplicating list/grid/search code a third and fourth time.
- Browsing, search, sort, continue watching — already existed.

### 📱 Android Features
- **System media controls + headphone/Bluetooth media-button support** —
  genuinely new, and the most architecturally significant addition this
  pass. Added a `MediaSession` (for headphone-button routing and session
  metadata) and a `MediaSessionService` (so the system can post
  notification-shade/lock-screen transport controls). Deliberately did
  **not** restructure player ownership into the service — see
  `playback/PlaybackSessionHolder.kt` for the reasoning — because that
  would mean routing every playback command through `MediaController`/Binder
  IPC and losing direct access to ExoPlayer-only APIs the app already
  depends on (the HW-decoder badge's `AnalyticsListener`, the audio-boost
  `LoudnessEnhancer`). The service and the ViewModel share the same
  in-process session object instead, which is safe specifically because
  they always run in the same process here.
- PiP — already existed.

### ⚙️ Settings
- **Light / Dark / System theme mode** — new. Previously there were 6
  fixed palettes with no "follow the phone's setting" option. Added a
  `ThemeMode` (Light/Dark/System) that resolves against the saved palette;
  if System mode disagrees with the saved palette's light/dark family
  (phone switched to dark mode but "Light Cream" was selected), it falls
  back to a sensible default in the right family rather than showing wrong
  contrast.
- **Default aspect ratio** and **control auto-hide duration** — new
  settings, both wired to real behavior (the player reads
  `controlAutoHideSeconds` instead of a hardcoded 4-second delay).
- Default speed, auto-play, resume, double-tap seek duration, gesture
  sensitivity, orientation behavior, keep screen on, subtitle size — all
  already existed.

## What's still not done

- Audio delay, subtitle delay (explained above — deliberately deferred,
  not faked).
- Playlists (user-created) — still an honest placeholder screen; needs a
  real data model and picker UI, which is a feature to build deliberately.
- No background/lock-screen playback *survival* has been stress-tested —
  the MediaSession/notification infrastructure is in place and should give
  real headphone-button and notification control, but I can't verify the
  service correctly survives Android's background execution limits without
  a real device.

## Setup & building

1. Android Studio (Ladybug/2024.2+), SDK Platform 35.
2. Open the project, let Gradle sync.
3. `./gradlew assembleDebug` for a quick installable build.
4. `./gradlew assembleBenchmark` for a realistic size check (minified +
   shrunk, debug-signed) — use this number against any size target, not
   the debug APK's.
5. `./gradlew bundleRelease` for a production build (see signing setup
   below).

### Release signing

`app/build.gradle.kts` doesn't hardcode a keystore. Generate one, put its
path/passwords in `app/keystore.properties` (already `.gitignore`d),
uncomment the `signingConfigs { create("release") { ... } }` block and its
`signingConfig` line, then `./gradlew bundleRelease`.

## Project structure

```
app/src/main/java/com/pteron/player/
├── MainActivity.kt / PteronApp.kt        # Activity + manual DI container
├── navigation/                            # Screen routes, bottom-nav destinations, NavHost
├── playback/                               # MediaSession / MediaSessionService for system controls
├── theme/                                  # Compose Material3 theme, driven by settings
├── data/
│   ├── model/                               # VideoFolder, VideoItem, enums (incl. AspectRatioMode)
│   ├── media/                                # MediaStoreRepository, thumbnail fetcher
│   └── prefs/                                # DataStore-backed appearance + playback prefs/state
├── ui/
│   ├── library/                              # Home screen (folders, continue watching)
│   ├── videos/                                # All-videos tab (favorites/recently-played live here too)
│   ├── playlists/                             # Placeholder tab
│   ├── folder/                                # Per-folder video list
│   ├── player/                                # ExoPlayer screen, gestures, HUD, controls
│   ├── settings/                              # Theme, appearance & playback-behavior settings
│   └── common/                                # Shared chrome (bottom nav, headers), bouncyClickable
└── util/                                      # Formatting, PiP bridge
```
