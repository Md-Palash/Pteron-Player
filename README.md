# Pteron Player

A native Android local video player built with Kotlin, Jetpack Compose, and
Media3/ExoPlayer. This project implements the full flow described in the
build instructions: MediaStore → real folders/videos → thumbnails →
Media3 playback, plus themes, search, sort, filters, gestures, favorites,
watch history, and Picture-in-Picture.

## ⚠️ Important — read this first

This codebase was written directly as Kotlin/Compose source files in a
sandboxed environment **without an Android SDK, emulator, or physical
device available** — so it has **not been compiled, run, or tested**. There
is no "PteronPlayer-Stage1" project anywhere in this environment either, so
everything here was written from scratch against the Stitch designs and the
master instruction document.

Before you treat this as done, you need to, in Android Studio:

1. Open the project and let Gradle sync (see **Setup** below).
2. Fix whatever the first sync/compile turns up — dependency versions
   drift constantly, and I was not able to verify these against Maven
   Central/Google's Maven from this environment.
3. Run it on an emulator or device with some real video files, and work
   through the manual test list in **§60–62** of the master instruction
   (large libraries, PiP, rotation, gestures, subtitle/audio tracks on a
   real device).
4. Only then move on to release signing and Play Store prep.

Treat this as a complete, serious first implementation to build on — not
as a package that's guaranteed to build on the first try.

## What's implemented

- **MediaStore integration** (`data/media/MediaStoreRepository`) — real
  folders and videos, no mock data, with a `ContentObserver` so the library
  can refresh when files change instead of polling.
- **Thumbnails** (`data/media/VideoThumbnailFetcher`) — a custom Coil
  fetcher using `ContentResolver.loadThumbnail` (API 29+) with a
  `MediaMetadataRetriever` fallback for older versions, cached by Coil so
  scrolling stays smooth on large libraries.
- **Library screen** — search, folder grid/list toggle, Continue Watching
  carousel, permission/empty/error/loading states.
- **Folder screen** — filter chips (All / Unwatched / 4K / Subtitled), sort
  (name/date/size/duration, persisted), grid/list, Shuffle Play.
- **Player** (`ui/player`) — Media3 `ExoPlayer` wrapped in a Compose
  `PlayerView`, with a from-scratch Compose control surface: play/pause,
  scrub bar with buffered progress, ±10s, previous/next, speed (0.5x–2x),
  aspect ratio cycling (fit/crop/stretch), audio & subtitle track pickers,
  favorite toggle, lock, rotation, and Picture-in-Picture (manual button +
  automatic on home-button press while playing).
- **Gestures** (`ui/player/components/GestureOverlay`) — tap to
  show/hide controls, double-tap left/right to seek, vertical drag for
  brightness (left half) / volume (right half), horizontal drag to scrub,
  all scaled by the persisted gesture-sensitivity setting.
- **Settings** (`ui/settings`) — background theme (2 light/2 dark),
  accent color, folder wood tone, badge toggles, "match controls to
  accent," OLED pure-black controls flag, gesture sensitivity slider — all
  backed by DataStore and actually wired into the theme/player, not just
  visual.
- **Persistence** (`data/prefs`) — resume position, watched flag (auto at
  90% + manual), favorites, and appearance/settings, all via DataStore
  Preferences (no network, no account, no ads).
- **Manifest / release scaffolding** — adaptive launcher icon, no
  `INTERNET` permission, R8/ProGuard rules for Media3 + kotlinx.serialization,
  a commented-out release signing block that reads from a
  git-ignored `keystore.properties` instead of hardcoding secrets.

## Known gaps and simplifications (documented, not hidden)

- **"Subtitled" filter** can't be known for a video until it's actually been
  opened once in the player — MediaStore doesn't expose subtitle-track
  presence, and probing every file with `MediaExtractor` up front would be
  exactly the kind of expensive full-library scan the spec says to avoid.
  Once you've played a video, its subtitle availability is cached
  (`PlaybackStateRepository.recordSubtitleAvailability`) and the filter
  becomes accurate for it from then on.
- **Folder "color" persistence** is app-level only (per §7) — Android gives
  no API to recolor a real filesystem folder, so the folder tone is a
  global appearance setting, not per-folder, matching what's actually
  representable in Stitch (a single "Folder Wood Tone" picker).
- **DataStore instead of Room** for playback state. This is fine at
  the scale described (a personal video library), but if you test with
  many thousands of videos and see the preferences file getting slow, that
  is the place to swap in a small Room table — the spec allows Room "when
  genuinely necessary," and this would be the trigger.
- **Typography** uses the system sans-serif family rather than bundling
  Plus Jakarta Sans/Inter as font files, to keep the app lightweight. Swap
  in real font files under `res/font` and reference them from
  `theme/Type.kt` if pixel-perfect typography matters to you.
- **Dependencies used**: Coil (thumbnail caching) and Accompanist
  Permissions were added because they replace meaningfully more custom
  code than they cost; everything else is AndroidX/Media3/Kotlin standard
  library, per the dependency policy in §57.

## Setup

1. Install Android Studio (Ladybug/2024.2 or newer) with SDK Platform 35
   and a recent NDK/build-tools via the SDK Manager.
2. Open this folder (`PteronPlayer/`) as a project.
3. Let Gradle sync. If a dependency version has moved on since this was
   written, Android Studio will usually offer a one-click upgrade — take it.
4. Run on an emulator or device (`minSdk 26`).

## Building

```bash
# Debug APK
./gradlew assembleDebug

# Release AAB (requires signing config, see below)
./gradlew bundleRelease
```

### Release signing

`app/build.gradle.kts` intentionally does **not** hardcode a keystore or
passwords. To produce a signed release build:

1. Generate a keystore if you don't have one:
   ```bash
   keytool -genkey -v -keystore pteron-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias pteron
   ```
2. Create `app/keystore.properties` (already covered by `.gitignore`):
   ```properties
   storeFile=/absolute/path/to/pteron-release.jks
   storePassword=your-store-password
   keyAlias=pteron
   keyPassword=your-key-password
   ```
3. Uncomment the `signingConfigs { create("release") { ... } }` block and
   the `signingConfig = signingConfigs.getByName("release")` line in
   `app/build.gradle.kts`.
4. Run `./gradlew bundleRelease`. The AAB lands in
   `app/build/outputs/bundle/release/`.

From there: create the listing in Google Play Console, upload the AAB,
complete Google's data-safety/content declarations, and submit for review.
This project does not do any of that submission itself (per §66).

## Project structure

```
app/src/main/java/com/pteron/player/
├── MainActivity.kt / PteronApp.kt        # Activity + manual DI container
├── navigation/                            # Screen routes + NavHost
├── theme/                                  # Compose Material3 theme, driven by settings
├── data/
│   ├── model/                              # VideoFolder, VideoItem, enums
│   ├── media/                               # MediaStoreRepository, thumbnail fetcher
│   └── prefs/                               # DataStore-backed appearance + playback state
├── ui/
│   ├── library/                             # Home screen
│   ├── folder/                              # Per-folder video list
│   ├── player/                              # ExoPlayer screen, gestures, controls
│   ├── settings/                            # Theme & appearance
│   └── common/                              # Shared empty/error/permission states
└── util/                                    # Formatting, PiP bridge
```
