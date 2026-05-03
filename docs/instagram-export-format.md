# Instagram Data Export Format

A reference for the structure of the official "Download Your Information" archive that Instagram (Meta) produces for an account holder.

> All examples in this document use **mock data**. Field names and shapes match real exports; values do not.

> **Source export:** this document is based on a JSON export downloaded on **2025-07-31**. Instagram changes the export schema periodically — adding files, renaming wrapper keys, splitting/merging sections — so newer or older archives may differ. Treat this as a snapshot, not a versioned spec, and re-verify against a fresh export if precision matters.

## Overview

Any Instagram user can request a copy of their data from **Settings → Accounts Center → Your information and permissions → Download your information**. Meta delivers a `.zip` containing JSON or HTML (the user picks the format at request time). This document covers the **JSON** variant — the HTML variant has the same data wrapped in `<table>` markup and is generally less useful for programmatic consumption.

The export reflects only data tied to the requesting account: posts you authored, accounts you follow, threads you participated in, your settings, etc. **It does not contain any other user's content** beyond plain-text usernames and external URLs (see [Gaps & Limitations](#gaps--limitations)).

### Archive name

The zip's name and root-folder name follow the pattern:

```
instagram-{username}-{YYYY-MM-DD}-{token}.zip
└── instagram-{username}-{YYYY-MM-DD}-{token}/
```

The token is a short random string. Both the username and the date should be treated as variable — parsers must detect the root prefix at unzip time rather than hard-coding it.

## Top-level layout

```
instagram-{username}-{YYYY-MM-DD}-{token}/
├── your_instagram_activity/
│   ├── media/                  # Posts, stories, reels, IGTV, profile photos
│   ├── comments/               # Comments YOU made
│   ├── likes/                  # Posts and comments YOU liked
│   ├── saved/                  # Saved posts and collections
│   ├── messages/               # DMs
│   │   ├── inbox/{thread_id}/  # One folder per conversation
│   │   ├── message_requests/
│   │   └── secret_conversations.json
│   ├── story_interactions/     # Polls, sliders, story likes
│   ├── shopping/
│   ├── subscriptions/
│   ├── threads/                # Cross-app data with the Threads app
│   ├── monetization/
│   └── other_activity/
├── connections/
│   └── followers_and_following/
├── media/                      # The actual photo/video files referenced from JSON
│   ├── posts/
│   ├── stories/
│   ├── reels/
│   ├── igtv/
│   └── other/
├── personal_information/
│   ├── personal_information/   # Profile, bio, friend map
│   ├── device_information/     # Devices, camera
│   ├── information_about_you/  # Inferred location
│   └── autofill_information/
├── preferences/
│   ├── settings/
│   └── your_topics/
├── security_and_login_information/
│   └── login_and_profile_creation/
├── ads_information/
│   ├── ads_and_topics/
│   └── instagram_ads_and_businesses/
├── logged_information/
│   ├── recent_searches/
│   └── link_history/
└── apps_and_websites_off_of_instagram/
    └── apps_and_websites/
```

> **macOS artifact:** zips opened on macOS may be re-archived with a parallel `__MACOSX/` tree of `._<filename>` resource-fork stubs. Strip these (`__MACOSX/`, anything starting with `._`) at parse time.

## Common conventions

A handful of patterns recur throughout the archive. Recognizing them up front avoids repeating the same explanation for every file.

### 1. Sharding

Files that may grow large are split into `_1.json`, `_2.json`, … shards once they cross an internal size threshold:

```
your_instagram_activity/media/posts_1.json
your_instagram_activity/media/posts_2.json
your_instagram_activity/comments/post_comments_1.json
connections/followers_and_following/followers_1.json
```

Smaller files (`stories.json`, `reels.json`, `following.json`) are unsharded. A robust parser globs by regex (e.g. `posts_\d+\.json`) and concatenates the results.

### 2. Two top-level wrapping styles

Some files are bare JSON arrays:

```json
[ { ... }, { ... } ]
```

Others wrap the array in a single-key object:

```json
{ "ig_stories": [ { ... }, { ... } ] }
```

The wrapping is **per-file and not predictable from the filename**. The wrapper key is conventionally a domain prefix (`ig_*`, `relationships_*`, `impressions_history_*`, `profile_*`, `inferred_data_*`, `account_history_*`). Specific keys are listed per file below.

### 3. The "activity item" envelope

Most settings, log, and metadata files wrap each entry in a uniform envelope:

```json
{
  "title": "Optional short label",
  "media_map_data": { },
  "string_map_data": {
    "<Field Name>": { "href": "", "value": "...", "timestamp": 1718409600 }
  },
  "string_list_data": [
    { "href": "", "value": "...", "timestamp": 1718409600 }
  ]
}
```

- `string_map_data` is a dict: a record with multiple labeled fields (e.g. a device with `Last Login`, `User Agent`).
- `string_list_data` is an array: a record with one or more equivalent values (e.g. a follower row with the username + profile URL + follow timestamp).
- `media_map_data` is the same shape but each value is a media object (URI + creation timestamp + metadata) — used when a record carries an attached photo (e.g. profile photo).
- `title` is the entry's short label or `""`.

A given file uses **either** `string_map_data` **or** `string_list_data`, rarely both. The same file may carry `media_map_data` alongside either.

### 4. Timestamps

| Field name              | Unit         | Where it appears |
|-------------------------|--------------|------------------|
| `creation_timestamp`    | Unix seconds | Media items, profile photos |
| `timestamp`             | Unix seconds | Activity items (`string_map_data`, `string_list_data`) |
| `timestamp_ms`          | Unix **milliseconds** | DMs only |
| `last_active_time`      | Unix seconds | Secret-conversation device records |

### 5. Media URIs

Media-bearing entries carry a `uri` field with a path **relative to the zip root**:

```json
{ "uri": "media/posts/202406/IMG_1234.jpg", "creation_timestamp": 1718409600 }
```

These resolve to actual files under the top-level `media/` directory after extraction.

### 6. UTF-8 mojibake

Instagram exports text fields as UTF-8 bytes re-interpreted through Latin-1, then JSON-escaped. A caption containing `'` shows up as `â` rather than the proper `’`. Readers must repair this:

```kotlin
val repaired = String(rawString.toByteArray(Charsets.ISO_8859_1), Charsets.UTF_8)
```

This affects captions, message bodies, hashtags, and any other free-text field. Settings labels and field names are ASCII and unaffected.

---

## Section-by-section reference

### Media — `your_instagram_activity/media/`

#### `posts_N.json`
Posts you authored. **Top-level: bare array.** Sharded.

```json
[
  {
    "media": [
      {
        "uri": "media/posts/202406/IMG_1234.jpg",
        "creation_timestamp": 1718409600,
        "title": "",
        "cross_post_source": { "source_app": "FB" },
        "media_metadata": {
          "camera_metadata": { "has_camera_metadata": false },
          "photo_metadata": {
            "exif_data": [
              {
                "latitude": 45.5152,
                "longitude": -122.6784,
                "date_time_original": "2024:06:15 18:42:11",
                "date_time_digitized": "2024:06:15 18:42:11",
                "device_id": "00000000-0000-0000-0000-000000000000",
                "source_type": "library",
                "camera_position": "back",
                "lens_make": "Apple",
                "lens_model": "iPhone 14 Pro back camera 6.86mm f/1.78",
                "aperture": "1.78",
                "shutter_speed": "1/120",
                "iso": 100,
                "focal_length": "6.86",
                "focal_plane_x_resolution": "5778.6",
                "focal_plane_y_resolution": "5778.6",
                "focal_plane_resolution_unit": 2,
                "metering_mode": 5,
                "scene_type": 1,
                "scene_capture_type": 0,
                "software": "17.5.1"
              }
            ]
          }
        }
      }
    ],
    "title": "Sunset at the lake #pdx",
    "creation_timestamp": 1718409600
  }
]
```

- A post may have one item in `media` (single image/video) or many (carousel — order is implicit by array position).
- Image vs. video is detected by which sub-key is present under `media_metadata`: `photo_metadata` or `video_metadata`.
- Outer `title` is the caption; per-media `title` is usually empty for posts but may contain a per-frame caption for some legacy posts.

**EXIF data:**
- `photo_metadata.exif_data[]` may carry GPS (`latitude`, `longitude`), capture timestamps (`date_time_original`, `date_time_digitized`), camera/lens info (`lens_make`, `lens_model`, `aperture`, `shutter_speed`, `iso`, `focal_length`), capture context (`camera_position`, `scene_type`, `scene_capture_type`, `metering_mode`), and a `software` version string.
- `video_metadata.exif_data[]` is a smaller subset: `latitude`, `longitude`, `date_time_original`, `device_id`, `source_type`, sometimes `camera_position` and `software`.
- **Every EXIF field is optional.** Fields appear only if the source media carried them. Photos uploaded after EXIF stripping (e.g. screenshots, downloaded images, posts where the user disabled location access) will have a sparse or empty `exif_data` array.
- **EXIF GPS ≠ user-tagged location.** The `latitude`/`longitude` here is the camera's recorded position at capture time. It is **not** the named place ("Eiffel Tower", "Joe's Coffee Shop") that a user picks at posting time — see [Gaps & Limitations](#location-data-gaps).

#### `stories.json`
**Wrapper key:** `ig_stories`. Unsharded.

```json
{
  "ig_stories": [
    {
      "uri": "media/stories/202406/story_001.jpg",
      "creation_timestamp": 1718409600,
      "media_metadata": { "photo_metadata": { } }
    }
  ]
}
```

Stories have no caption and no carousel — each entry is one image or video.

#### `reels.json`
**Wrapper key:** `ig_reels_media`. Each entry is a "reel group" with its own `media` array (always length 1 in observed exports, but the array exists).

```json
{
  "ig_reels_media": [
    {
      "media": [
        {
          "uri": "media/reels/202406/reel_001.mp4",
          "creation_timestamp": 1718409600,
          "title": "",
          "media_metadata": {
            "video_metadata": {
              "exif_data": [{ "device_id": "00000000-0000-0000-0000-000000000000" }]
            }
          }
        }
      ]
    }
  ]
}
```

#### `igtv_videos.json`
**Wrapper key:** `ig_igtv_media`. Same shape as `reels.json`, with extra fields:

```json
{
  "ig_igtv_media": [
    {
      "media": [
        {
          "uri": "media/igtv/202301/video_001.mp4",
          "creation_timestamp": 1672531200,
          "media_metadata": {
            "video_metadata": {
              "subtitles": {
                "uri": "media/igtv/202301/video_001.srt",
                "creation_timestamp": 1672531200
              },
              "exif_data": [{ "device_id": "00000000-0000-0000-0000-000000000000" }]
            }
          },
          "title": "How to bake bread",
          "cross_post_source": { "source_app": "FB" },
          "dubbing_info": [],
          "media_variants": []
        }
      ]
    }
  ]
}
```

#### `profile_photos.json`
**Wrapper key:** `ig_profile_picture`. History of your profile pictures.

```json
{
  "ig_profile_picture": [
    {
      "uri": "media/other/profile_2024.jpg",
      "creation_timestamp": 1718409600,
      "media_metadata": { "camera_metadata": { "has_camera_metadata": false } },
      "title": "",
      "cross_post_source": { "source_app": "FB" }
    }
  ]
}
```

#### Other files in this folder
- `archived_posts.json` — same shape as `posts_*.json`, posts you've archived.
- `recently_deleted_content.json` — recently deleted media within the 30-day grace window.

### Engagement — `your_instagram_activity/likes/` and `comments/`

#### `liked_posts.json`
**Wrapper key:** `likes_media_likes`. Each entry uses `string_list_data`.

```json
{
  "likes_media_likes": [
    {
      "title": "jane_doe",
      "string_list_data": [
        {
          "href": "https://www.instagram.com/p/AbCdEfGhIjK/",
          "value": "👍",
          "timestamp": 1718409600
        }
      ]
    }
  ]
}
```

- `title` is the username of the post owner.
- `string_list_data[].value` is the reaction emoji (almost always `❤️` or `👍`).
- `string_list_data[].href` is the public Instagram URL of the liked post.

#### `liked_comments.json`
Same shape as `liked_posts.json` (`likes_media_likes` wrapper, `string_list_data` per entry). `href` points to the post the comment lives on, not the comment itself.

#### `post_comments_N.json`
**Top-level: bare array.** Sharded. Each entry uses `string_map_data` plus a sibling `media_owner` field.

```json
[
  {
    "media_owner": "jane_doe",
    "string_map_data": {
      "Comment": { "value": "Beautiful shot!", "timestamp": 0 },
      "Time":    { "value": "", "timestamp": 1718409600 },
      "Media Owner": { "value": "jane_doe", "timestamp": 0 }
    }
  }
]
```

> ⚠️ **Comments are not linked to a specific post.** Only the owner's username is recorded — see [Gaps & Limitations](#gaps--limitations).

#### `reels_comments.json`
Same shape as `post_comments_N.json`, scoped to comments on reels.

### Story interactions — `your_instagram_activity/story_interactions/`

A folder of small JSON files, each a record of one type of action you took on others' stories. All use the activity-item envelope.

| File                          | Wrapper key                            | Body shape         |
|-------------------------------|----------------------------------------|--------------------|
| `story_likes.json`            | `story_activities_story_likes`         | `string_list_data` |
| `polls.json`                  | `story_activities_polls`               | `string_list_data` |
| `emoji_sliders.json`          | `story_activities_emoji_sliders`       | `string_list_data` |
| `questions.json`              | `story_activities_questions`           | `string_list_data` |
| `quizzes.json`                | `story_activities_quizzes`             | `string_list_data` |
| `countdowns.json`             | `story_activities_countdowns`          | `string_list_data` |
| `story_link_clicks.json`      | `story_activities_link_clicks`         | `string_list_data` |

Mock example (`emoji_sliders.json`):

```json
{
  "story_activities_emoji_sliders": [
    {
      "title": "jane_doe",
      "string_list_data": [
        { "value": "8.5", "timestamp": 1718409600 }
      ]
    }
  ]
}
```

`title` is the story owner's username; `value` is the slider position (0–10) or the poll/quiz answer chosen.

### Saved — `your_instagram_activity/saved/`

#### `saved_posts.json`
**Wrapper key:** `saved_saved_media`. Each entry references an external Instagram URL (the actual media is not in the export).

```json
{
  "saved_saved_media": [
    {
      "title": "jane_doe",
      "string_map_data": {
        "Saved on": {
          "href": "https://www.instagram.com/p/AbCdEfGhIjK/",
          "value": "",
          "timestamp": 1718409600
        }
      }
    }
  ]
}
```

#### `saved_collections.json`
**Wrapper key:** `saved_saved_collections`. Names of any collections you organized saved posts into. Membership of posts in collections is implicit (re-derive by matching collection creation/update timestamps).

### Connections — `connections/followers_and_following/`

#### `followers_N.json`
**Top-level: bare array.** Sharded.

```json
[
  {
    "string_list_data": [
      {
        "href": "https://www.instagram.com/jane_doe",
        "value": "jane_doe",
        "timestamp": 1718409600
      }
    ]
  }
]
```

`timestamp` is when they started following you.

#### `following.json`
**Wrapper key:** `relationships_following`. Same `string_list_data` shape per entry. `timestamp` is when *you* started following *them*.

#### Other files in this folder
| File                                     | Wrapper key                                       | Notes |
|------------------------------------------|---------------------------------------------------|-------|
| `close_friends.json`                     | `relationships_close_friends`                     | Members of your close-friends list |
| `following_hashtags.json`                | `relationships_following_hashtags`                | Hashtags you follow |
| `recently_unfollowed_profiles.json`      | `relationships_unfollowed_users`                  | Who you recently unfollowed |
| `pending_follow_requests.json`           | `relationships_follow_requests_sent`              | Outbound follow requests pending response |
| `follow_requests_you've_received.json`   | `relationships_follow_requests_received`          | Inbound follow requests pending |
| `blocked_profiles.json`                  | `relationships_blocked_users`                     | Accounts you've blocked |
| `restricted_profiles.json`               | `relationships_restricted_users`                  | Accounts you've restricted |
| `hide_story_from.json`                   | `relationships_hide_stories_from`                 | Accounts hidden from your stories |
| `removed_suggestions.json`               | `relationships_dismissed_suggested_users`         | Suggested-follow rejections |

All share the `string_list_data` envelope.

### Messages — `your_instagram_activity/messages/`

Layout:

```
messages/
├── inbox/
│   └── {handle}_{thread_id}/        # One folder per conversation
│       ├── message_1.json           # Sharded — newer threads in higher-numbered files
│       ├── message_2.json
│       ├── photos/                  # Photos sent in this thread
│       ├── videos/                  # Videos sent in this thread
│       ├── audio/                   # Voice messages
│       ├── gifs/
│       └── files/
├── message_requests/                # Same shape as inbox/, threads not yet accepted
│   └── {handle}_{thread_id}/
└── secret_conversations.json
```

Folder names use the pattern `{username_or_groupslug}_{thread_id}` where `thread_id` is a numeric Meta IGSC ID. Group threads concatenate participant handles up to a limit, then append `andNothers`.

#### `message_N.json`
A single conversation's messages, sharded. **Note:** higher numbers = older messages (Meta exports newest-first into `message_1.json`).

```json
{
  "participants": [
    { "name": "jane_doe" },
    { "name": "Your Name" }
  ],
  "messages": [
    {
      "sender_name": "Your Name",
      "timestamp_ms": 1718409600000,
      "content": "Sounds good!",
      "is_geoblocked_for_viewer": false,
      "is_unsent_image_by_messenger_kid_parent": false
    },
    {
      "sender_name": "jane_doe",
      "timestamp_ms": 1718409500000,
      "content": "jane_doe sent an attachment.",
      "share": {
        "link": "https://www.instagram.com/reel/AbCdEfGhIjK/?id=12345_67890",
        "share_text": "Check this out!",
        "original_content_owner": "another_user"
      },
      "is_geoblocked_for_viewer": false,
      "is_unsent_image_by_messenger_kid_parent": false
    }
  ],
  "title": "jane_doe",
  "is_still_participant": true,
  "thread_path": "inbox/jane_doe_12345/",
  "magic_words": []
}
```

Per-message optional fields (presence depends on message type):

| Field            | Type     | Meaning |
|------------------|----------|---------|
| `content`        | string   | Text body, or a Meta-generated placeholder like `"<sender> sent an attachment."` |
| `share`          | object   | `{ link, share_text, original_content_owner }` for shared posts/reels |
| `photos`         | array    | `[ { uri, creation_timestamp } ]` — `uri` is relative to the zip root |
| `videos`         | array    | Same shape as `photos` |
| `audio_files`    | array    | Voice messages |
| `gifs`           | array    | `[ { uri } ]` for sent GIFs |
| `reactions`      | array    | `[ { reaction, actor } ]` — emoji reactions on this message |
| `call_duration`  | integer  | Seconds — present on call records |
| `is_unsent`      | boolean  | True if the sender deleted/unsent this message |

> ⚠️ Strings here suffer the [UTF-8 mojibake](#6-utf-8-mojibake) issue and need repair.

#### `secret_conversations.json`
**Wrapper key:** `ig_secret_conversations`. Metadata for end-to-end encrypted conversations — the message bodies themselves are *not* exported (E2EE — Meta cannot decrypt them). What is included:

```json
{
  "ig_secret_conversations": {
    "armadillo_devices": [
      {
        "device_type": "Igd:Android-XYZ",
        "device_manufacturer": "Google",
        "device_model": "Pixel 7",
        "device_os_version": "14",
        "last_connected_ip": "203.0.113.10",
        "last_active_time": 1718409600
      }
    ],
    "calls": []
  }
}
```

### Personal information — `personal_information/`

#### `personal_information/personal_information.json`
**Wrapper key:** `profile_user`. Your profile core fields — uses `string_map_data` plus `media_map_data` for the profile photo.

```json
{
  "profile_user": [
    {
      "title": "User Information",
      "media_map_data": {
        "Profile Photo": {
          "uri": "media/other/profile.jpg",
          "creation_timestamp": 1718409600,
          "media_metadata": { "camera_metadata": { "has_camera_metadata": false } },
          "title": ""
        }
      },
      "string_map_data": {
        "Email":    { "value": "jane@example.com", "timestamp": 0 },
        "Phone Number": { "value": "+15555550100", "timestamp": 0 },
        "Username": { "value": "jane_doe", "timestamp": 0 },
        "Name":     { "value": "Jane Doe", "timestamp": 0 },
        "Bio":      { "value": "Photographer & traveler", "timestamp": 0 },
        "Gender":   { "value": "female", "timestamp": 0 },
        "Date of birth": { "value": "1990-01-01", "timestamp": 0 },
        "Private Account": { "value": "False", "timestamp": 0 }
      }
    }
  ]
}
```

#### `personal_information/instagram_profile_information.json`
**Wrapper key:** `profile_account_insights`. Inferred / computed account stats: `Contact Syncing`, `First Country Code`, `First Story Time`, `Last Story Time`, `Has Shared Live Video`, `Last Login`, `Last Logout`, etc. Same envelope shape as above.

#### `personal_information/profile_changes.json`
**Wrapper key:** `profile_profile_change`. Each entry records one profile field that changed (e.g. bio update, username change), with the previous value, new value, and a timestamp.

#### `personal_information/professional_information.json`
**Wrapper key:** `profile_business`. Business-account fields. Empty `string_map_data` for personal accounts.

#### `personal_information/note_interactions.json`
**Wrapper key:** `profile_note_interactions`. Activity around the "Notes" feature (`Last Notes Seen Time`, etc.).

#### `personal_information/instagram_friend_map.json`
**Wrapper key:** `profile_friend_map`. Snapshot counts of incoming/outgoing follow requests at export time.

#### `personal_information/device_information/devices.json`
**Wrapper key:** `devices_devices`. One entry per device that has logged in to your account — captures `Last Login` (timestamp) and `User Agent` (UA string with model, OS, app version).

```json
{
  "devices_devices": [
    {
      "string_map_data": {
        "Last Login": { "value": "", "timestamp": 1718409600 },
        "User Agent": {
          "value": "Instagram 390.0.0 Android (35/15; Pixel 7; en_US)",
          "timestamp": 0
        }
      }
    }
  ]
}
```

#### `personal_information/device_information/camera_information.json`
**Wrapper key:** `devices_camera`. AR-camera SDK details (`Device ID`, `Compression`, `Face Tracker Version`, `Supported SDK Versions`).

#### `personal_information/information_about_you/profile_based_in.json`
**Wrapper key:** `inferred_data_primary_location`. Meta's inferred home city (from device + activity data).

```json
{
  "inferred_data_primary_location": [
    {
      "string_map_data": {
        "City Name": { "value": "Portland, Oregon", "timestamp": 0 }
      }
    }
  ]
}
```

#### `personal_information/information_about_you/locations_of_interest.json`
**Wrapper key:** `label_values` array under `inferred_data_inferred_locations`. Larger inferred-location list with confidence labels.

#### `personal_information/autofill_information/in-app_browser_autofill_settings.json`
In-app browser autofill preferences. Activity-item envelope.

### Preferences — `preferences/`

#### `preferences/settings/consents.json`
**Top-level: object** (not the activity envelope). One record summarizing consent state.

```json
{
  "timestamp": 1577836800,
  "media": [],
  "label_values": [
    { "label": "Consent for the processing of user's data by ad partners on Instagram" },
    { "label": "Update time", "timestamp_value": 1718409600 }
  ],
  "fbid": "00000000000000000"
}
```

#### `preferences/settings/comments_allowed_from.json`
**Wrapper key:** `settings_allow_comments_from`. Activity envelope; the field is `Comments Allowed From` with `value` of `Everyone` / `People you follow` / etc.

#### Other files under `preferences/settings/`
- `use_cross-app_messaging.json` — flag for Meta's cross-app messaging interop
- `language_and_translations.json` — UI language and translation prefs
- `notification_settings.json` — granular notification toggles

All use the activity-item envelope with one `string_map_data` field per setting.

#### `preferences/your_topics/recommended_topics.json`
**Wrapper key:** `topics_your_topics`. Inferred interests Meta uses to populate the Explore feed. Activity envelope, `string_map_data` with a `Name` field per topic.

### Security & login — `security_and_login_information/login_and_profile_creation/`

| File                              | Wrapper key                              | Notes |
|-----------------------------------|------------------------------------------|-------|
| `signup_details.json`             | `account_history_registration_info`      | Username, email, phone, IP, device, time at signup |
| `login_activity.json`             | `account_history_login_history`          | Per-login: IP, UA, cookie, language, port, time |
| `logout_activity.json`            | `account_history_logout_history`         | Per-logout: same fields as login |
| `last_known_location.json`        | `account_history_imprecise_last_known_location` | Coarse last location (city granularity) |
| `password_change_activity.json`   | `account_history_password_change_history`| Each password change with timestamp |
| `profile_privacy_changes.json`    | `account_history_account_privacy_history`| Public ↔ Private toggles with timestamps |
| `profile_status_changes.json`     | `account_history_profile_status_changes` | Account state changes (deactivation, etc.) |

All use the activity-item envelope. Mock from `signup_details.json`:

```json
{
  "account_history_registration_info": [
    {
      "string_map_data": {
        "Username":     { "value": "jane_doe", "timestamp": 0 },
        "Email":        { "value": "jane@example.com", "timestamp": 0 },
        "IP Address":   { "value": "203.0.113.10", "timestamp": 0 },
        "Phone Number": { "value": "", "timestamp": 0 },
        "Time":         { "value": "", "timestamp": 1577836800 },
        "Device":       { "value": "", "timestamp": 0 }
      }
    }
  ]
}
```

### Ads — `ads_information/`

| File                                                                   | Wrapper key                                   | What it records |
|------------------------------------------------------------------------|-----------------------------------------------|-----------------|
| `ads_and_topics/ads_viewed.json`                                       | `impressions_history_ads_seen`                | Ads you scrolled past |
| `ads_and_topics/ads_clicked.json`                                      | `impressions_history_ads_clicked`             | Ads you tapped |
| `ads_and_topics/posts_viewed.json`                                     | `impressions_history_posts_seen`              | Organic posts you viewed |
| `ads_and_topics/videos_watched.json`                                   | `impressions_history_videos_watched`          | Videos played |
| `ads_and_topics/suggested_profiles_viewed.json`                        | `impressions_history_chaining_seen`           | Suggested-account impressions |
| `ads_and_topics/posts_you're_not_interested_in.json`                   | `impressions_history_posts_not_interested`    | "Not interested" actions |
| `ads_and_topics/profiles_you're_not_interested_in.json`                | `impressions_history_recs_hidden_authors`     | Hidden suggested accounts |
| `ads_and_topics/in-app_message.json`                                   | `impressions_history_app_message`             | In-app banner impressions |
| `instagram_ads_and_businesses/ad_preferences.json`                     | `label_values` envelope                       | Your ad-targeting preferences |
| `instagram_ads_and_businesses/advertisers_using_your_activity_or_information.json` | `ig_custom_audiences` | List of advertisers who uploaded data matching you |
| `instagram_ads_and_businesses/subscription_for_no_ads.json`            | `label_values` envelope                       | Premium subscription state |

The `impressions_history_*` files use the activity envelope with a `title` (the advertiser handle, often empty for organic) and `string_list_data` of one or more `{ timestamp }` entries:

```json
{
  "impressions_history_ads_clicked": [
    {
      "title": "Acme Coffee Co.",
      "string_list_data": [
        { "timestamp": 1718409600 }
      ]
    }
  ]
}
```

### Logged information — `logged_information/`

- `recent_searches/account_searches.json` — your recent profile-search queries, activity envelope.
- `recent_searches/word_or_phrase_searches.json` — non-profile search queries.
- `link_history/link_history.json` — links you've opened from in-app browser.

### Apps & websites off Instagram — `apps_and_websites_off_of_instagram/apps_and_websites/`

- `apps_and_websites.json` — third-party apps you've authorized via Instagram OAuth.
- `your_activity_off_meta_technologies.json` — off-Meta data shared with Meta by partners (the "Off-Facebook Activity" equivalent).

Both use `label_values` style envelopes.

### Other activity — `your_instagram_activity/other_activity/`

- `your_information_download_requests.json` — history of past data-export requests (a record of the request that produced this very file is here).

### Shopping — `your_instagram_activity/shopping/`

- `recently_viewed_items.json` — products viewed in Shop, activity envelope.

### Subscriptions — `your_instagram_activity/subscriptions/`

- `your_muted_story_teaser_creators.json` — creators whose subscription teasers you've muted.
- `show_exclusive_story_promo_setting.json` — boolean flag.

### Threads (the app) — `your_instagram_activity/threads/`

If you've used the Threads app, separate data appears here:
- `threads_viewed.json`
- `personal_information.json` — Threads-specific profile fields
- `follow_requests_you've_received.json`

Schema follows the activity envelope.

### Monetization — `your_instagram_activity/monetization/`

- `eligibility.json` — wrapper key `monetization_eligibility`. Per-product (`BRANDED CONTENT`, `SUBSCRIPTIONS`, etc.) `Decision` of `Eligible` / `Not Eligible` plus a `Reason`.

---

## Media folder layout

The top-level `media/` directory holds the actual photo and video files referenced by `uri` fields throughout the JSON. Subfolders mirror the content type, then are date-grouped (`YYYYMM`):

```
media/
├── posts/
│   ├── 202301/
│   ├── 202302/
│   └── 202406/
│       ├── IMG_1234.jpg
│       └── IMG_5678.mp4
├── stories/
│   └── 202406/
├── reels/
│   └── 202406/
├── igtv/
│   └── 202301/
│       ├── video_001.mp4
│       └── video_001.srt    # optional sidecar subtitles
└── other/
    ├── profile_2024.jpg
    └── ...                  # profile photos and miscellany
```

Filenames use Meta's internal CDN naming (long alphanumeric IDs). Treat them as opaque — the only stable handle is the `uri` field in the corresponding JSON entry.

---

## Edge cases & gotchas

- **Variable root folder.** Detect at parse time; never hard-code the `instagram-{username}-{date}-{token}/` prefix.
- **`__MACOSX/` and `._*` resource forks.** Strip these before iterating.
- **Mixed wrapping.** Sibling files in the same folder may use different top-level shapes (bare array vs. wrapped object). Don't assume uniformity — dispatch by filename.
- **Per-key field optionality.** Within `string_map_data`, missing keys are simply absent (not present-as-null). Always treat every field as optional.
- **Carousel ordering is implicit.** A multi-image post relies on JSON array order; there's no explicit `position` integer.
- **Image vs. video by structure, not extension.** Detect via presence of `media_metadata.video_metadata` or `photo_metadata`, not by file suffix.
- **Empty `media_map_data: {}` everywhere.** Most activity-item entries include an empty `media_map_data`. Don't take its presence as a signal.
- **`timestamp_ms` only in DMs.** Everywhere else, timestamps are seconds. Mixing units is a common bug source.
- **Some fields nest three levels deep** — e.g. `media[].media_metadata.photo_metadata.exif_data[].latitude`. Defensive null-checking required at each step.
- **Date format depends on user choice.** When requesting the export, the user picks JSON or HTML and a date range. Older exports may have extra files no longer present, or lack newer ones (e.g. `reels.json` only exists post-Reels-launch).

---

## Gaps & limitations

This export is a **personal archive**, not a database snapshot of Instagram. Building a faithful Instagram clone purely from this data hits hard limits — most of them by design (privacy-preserving) and a few that look like oversights.

### Engagement attribution gaps

- **Comments don't link to a specific media item.** `post_comments_*.json` records only `media_owner` (a username) per comment — there is no post ID, shortcode, or media URI on the comment record. This blocks any "show comments under this post" UI; comments can only be aggregated per-author.
- **Likes only link via an external URL.** `liked_posts.json` entries store the post URL as `string_list_data[].href` (e.g. `https://www.instagram.com/p/AbCdEfGhIjK/`) plus the owner's username. The actual liked media is not in the export, so a "liked posts" page cannot render thumbnails.
- **No inbound engagement.** The export contains likes and comments **you made on others' content**. There is no record of who liked or commented on **your** posts. Engagement counts on your own feed items are not reconstructable.
- **No engagement counts on your own posts.** Entries in `posts_*.json` carry no `like_count`, `comment_count`, or `view_count` — so the "237 likes" line every IG post displays cannot be shown.
- **Comment threads are flat.** No `parent_comment_id` or reply linkage. Nested comment threads cannot be reconstructed.

### Other-user data gaps

- **No profile metadata for other users.** Followers, following, close friends, comment authors — every reference to another user is just `value` (handle), `href` (profile URL), and a `timestamp`. No display name, no bio, no avatar. Other users render as bare usernames in any clone.
- **Other users' content is not in the export.** Saved posts, liked posts, posts you've commented on, posts you were tagged in — all reference external URLs only. No media, no captions, no metadata. A "Saved" or "Explore" tab built from this export would be a list of links.
- **No story viewers.** Who viewed *your* stories isn't exported.
- **No tag-of-you data.** Posts where other accounts tagged you are not present.

### Discovery & activity gaps

- **No notifications history.** No timeline of likes/comments/follows received over time.
- **No algorithmic Explore feed.** `recommended_topics.json` lists the inferred topics that drive the Explore feed, but the actual ranked posts cannot be reproduced (and depend on real-time data anyway).
- **No hashtag ↔ post linkage.** `following_hashtags.json` lists hashtags you follow; there is no record of which posts use which hashtags. Captions in `posts_*.json` may contain `#tags` as plain text only — extracting and indexing them is on the consumer.

### Location data gaps

- **No user-tagged named locations on posts.** Instagram lets users tag a post with a named place (e.g. "Eiffel Tower, Paris", "Joe's Coffee Shop") backed by a Facebook Places ID. **None of this is in the export.** A full key audit of `posts_*.json`, `stories.json`, `reels.json`, and `igtv_videos.json` finds no `location`, `place`, `place_id`, `place_name`, `venue`, `geo`, or `tagged_location` field — only the EXIF `latitude`/`longitude` pair from the camera. A clone cannot show the "📍 Eiffel Tower" line that appears above an IG post's caption.
- **EXIF GPS is unreliable as a substitute.** Camera-captured `latitude`/`longitude` is often missing (uploaded screenshots, photos with EXIF stripped, users with camera location services off) and, when present, reflects where the camera was at *capture* time, not where the user said the post was *about*.
- **No location stickers from stories.** Story location stickers (the tappable place sticker users add at post time) are not represented either — stories export the raw media with EXIF, nothing else.
- **Inferred location is coarse and account-level only.** `personal_information/information_about_you/profile_based_in.json` and `locations_of_interest.json` carry Meta's *inferred* home city/region for the account, not per-post locations.

### Media metadata gaps

- **No reel audio/music metadata.** Reels include the video file but no info about the audio track (artist, title, original sound owner). The "audio" attribution feature cannot be reconstructed.
- **No story highlights.** Stories are exported flat; the highlight collections you organized them into are not represented.
- **Carousel ordering is implicit** (array order), so it's preserved but not explicit — minor.

### DM-specific gaps

- **Secret (E2EE) conversation bodies are not present** — only device metadata in `secret_conversations.json`. Meta cannot decrypt these to export them.
- **Read receipts and typing state aren't included** — only sent messages with timestamps.
- **DMs only contain threads you participated in** — no membership history of group threads you've left.

### Bottom line

A faithful Instagram clone built from this export alone would essentially be a **personal archive viewer**: you can render your own posts, stories, and reels with full fidelity; you can list who you follow and who follows you; you can read your own DMs. You **cannot** show post engagement, render others' avatars, populate a "for you" feed with real content, attribute comments to posts, or simulate any social network feature that depends on data the export doesn't include.

The minimum viable approach for an IG-clone built on this data is to treat external usernames as opaque handles, render placeholder avatars, omit engagement counts, and accept that "feed" means "your own posts in reverse chronological order."
