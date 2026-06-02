# Picnic

A simple, offline viewer for your own Instagram archive. Import the official Instagram data-export ZIP and browse your posts, reels, stories, and connections in a familiar Instagram-style layout — no login, no network, nothing leaves your device.

Built with Kotlin + Jetpack Compose, Hilt, Room, and Coil.

> Picnic is a personal archive viewer, not a social network. It only shows what your export contains. See [docs/instagram-export-format.md](docs/instagram-export-format.md) for what's inside the export and what it can't include.

---

## Getting started

1. **Get your data.** In Instagram: **Settings → Accounts Center → Your information and permissions → Download your information**. Choose **JSON** format and download the `.zip`.
2. **Install Picnic** on an Android device (min Android 7.0 / SDK 24).
3. **Open the app** and import the ZIP (see below).

---

## User manual

### Import
The first screen you see. Tap to pick your Instagram `.zip` using the system file picker, then watch the import progress. Picnic extracts your media to local storage and loads everything into an on-device database. When it finishes, you land on **Home**. You only do this once — to load a fresh export later, use **Clear data** in Settings.

### Home & navigation
Home shows your content in a grid and has a **floating bottom bar** with four tabs. Tap a tab to switch; the title bar at the top stays put and offers a **Settings** button. Grid density (1, 2, or 3 columns) is set in Settings.

| Tab | What it shows |
| --- | --- |
| **Posts** | Your photo and video posts as a scrollable grid. |
| **Reels** | Your reels as a thumbnail grid. |
| **Stories** | Your stories, grouped by the day they were posted. |
| **People** | Your followers and following. |

#### Posts
A grid of every post. Pull down to refresh. Tap any post to open it full-screen.

#### Reels
A grid of reel thumbnails. Tap one to play it full-screen.

#### Stories
A grid of your stories, one cell per day. Days with several stories show a count badge; tap to play that day's stories in sequence. A day with a single story opens straight into the player.

#### People
Your connections, with a **Followers / Following** toggle at the top. Switch between the two lists; tap a person to open their Instagram profile.

### Detail screens

#### Post
The full post. Swipe sideways through multiple photos/videos in a carousel, read the caption and date, and — if the post has location data — tap to view where it was taken. You can **share** the post, or use **Inspect mode** to view the media at full resolution. Swipe back to return to the grid.

#### Reel
Full-screen reel playback with video controls. Swipe back to the grid.

#### Story player
Full-screen, auto-advancing stories with a segmented progress bar across the top (one segment per story in the day):
- **Tap the right side** → next story
- **Tap the left side** → rewind, or go to the previous story
- **Press and hold** → pause

Images show for a few seconds each; videos play and loop. It moves to the next story automatically.

### Settings
Reached from the button in the top bar:
- **Theme** — System, Light, or Dark.
- **Grid columns** — 1, 2, or 3 across.
- **Clear data** — removes everything imported (after a confirmation) and returns you to the Import screen so you can load a new export.

---

## For developers

Two Gradle modules:

- **`:app`** — the Android application. One MVI contract per feature (`Action` / `Effect` / `State`) wired to a screen-level ViewModel. Navigation graph in `main/MainNavHost.kt`, routes in `main/Routes.kt`. Data layer under `data/`: Room entities + DAOs in `data/db/`, ZIP parsing in `data/zip/` (`ZipParser` extracts media in pass 1, parses JSON with resolved URIs in pass 2), repository in `data/repository/`.
- **`:bento`** — in-tree Compose library of reusable primitives: `BaseViewModel`, `PaginatedViewModel` / `PaginatedState` (offset-based pagination), `PaginatedList` (list + grid, optional pull-to-refresh), plus `Carousel` and `ExpandableText`.

Open the root in Android Studio and run the `app` configuration (min SDK 24, target SDK 34), or:

```bash
./gradlew :app:installDebug
```

---

## License

Apache 2.0 — see [LICENSE](LICENSE).
