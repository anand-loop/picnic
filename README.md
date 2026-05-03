# Picnic

An Android app for browsing your own Instagram archive locally. Import the official Instagram data-export ZIP and explore your posts, reels, and stories in a familiar Instagram-style UI — fully offline, no account login, no network access to Meta.

Built with Kotlin + Jetpack Compose, Hilt, Room, and Coil.

> **Note:** Picnic is a personal archive viewer, not a social network simulation. See [docs/instagram-export-format.md](docs/instagram-export-format.md) for what the Instagram export contains and the structural limits that constrain what any clone can do.

---

## Current features

### Data ingestion
- **SAF-based ZIP picker** — no manifest permissions required.
- **Two-pass ZIP parser** that extracts media to local storage and resolves URI references in JSON.
- **Room database** persisting posts, media, stories, reels, connections (followers/following), likes, and comments.
- **Live import progress** with Lottie loading animation and per-stage status text.
- **Re-import / clear data** flow from Settings, with confirmation dialog.

### Navigation & chrome
- **Shared top app bar** (`PicnicTopAppBar`) — brand title that fades on scroll, plus a settings action.
- **Floating bottom nav bar** with animated selection across three home tabs: Posts • Reels • Stories.
- **6 routes**: Import, Feed (host for Posts + Reels + Stories tabs), Settings, Post detail, Reel detail, Story player.
- **Shared element transitions** between grid cells and detail screens.

### Posts (Feed tab)
- Configurable grid (1, 2, or 3 columns).
- Paginated with bento's `PaginatedList` (offset-based, page size 20).
- Pull-to-refresh.
- Tap a cell to open the post in detail.

### Post detail
- Carousel pager for multi-image / multi-video posts (`MediaPager`).
- Caption + post date, with a location action when EXIF coordinates are present.
- "Inspect mode" overlay for examining media at full resolution.
- Share action via system intent.
- Shared element transition from feed grid.

### Reels (Reels tab)
- Grid of reel thumbnails, paginated.
- Tap to open reel detail with video playback.
- Shared element transition into the detail screen.

### Stories (Stories tab)
- 2-column paginated grid **grouped by calendar day** at query time via SQLite `date(timestamp, 'unixepoch', 'localtime')` in `StoryDao.getGroupedPage`.
- Count badge on multi-item days; single-item days open the player directly.
- Full-screen story player with segmented progress bar, frame-aligned fills (`withFrameMillis`), 5s dwell on images, and ExoPlayer autoplay/loop for video.
- Gestures: tap right third → next, tap left third → rewind (or previous if within first 10%), long-press → pause.

### Settings
- **Theme**: system / light / dark (segmented control).
- **Grid columns**: 1 / 2 / 3 (segmented control).
- **Clear data**: removes all imported data after confirmation; returns to the import screen.

### Theme & polish
- Explicit **monochrome** Material 3 light and dark color schemes (grayscale only, no hue).
- Borel display font for the brand title.
- Material Symbols icons via vector drawables.

---

## Architecture

Two Gradle modules:

- **`:app`** — the Android application. One MVI contract per feature (`Action` / `Effect` / `State`) wired to a screen-level ViewModel: posts, reels, stories, post detail, reel detail, story player, settings, import. Navigation graph in `main/MainNavHost.kt`, routes in `main/Routes.kt`.
- **`:bento`** — in-tree Compose library of reusable primitives: `BaseViewModel`, `PaginatedViewModel` / `PaginatedState` (offset-based pagination state machine), `PaginatedList` (list + grid layouts, optional pull-to-refresh), plus shared widgets `Carousel` and `ExpandableText`. Published locally as `com.anandj.bento:bento:1.0.0` via maven-publish for future extraction.

Data layer lives under `app/src/main/java/com/anandj/picnic/data/`: Room entities + DAOs in `data/db/`, ZIP parsing in `data/zip/` (`ZipParser` does a two-pass walk — extract media in pass 1, parse JSON with resolved URIs in pass 2), repository in `data/repository/` exposing a `StateFlow<ImportState>` plus Flow accessors.

---

## Building

Standard Android Studio project — open the root in Android Studio and run the `app` configuration on a device or emulator (min SDK 24, target SDK 34).

```bash
./gradlew :app:installDebug
```

To use the app, request your Instagram data export from **Settings → Accounts Center → Your information and permissions → Download your information** (choose JSON format), then pick the resulting `.zip` from the import screen.

---

## License

Apache 2.0 — see [LICENSE](LICENSE).
