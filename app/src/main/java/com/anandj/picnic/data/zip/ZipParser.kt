// Copyright (C) 2026 Anand Jesudason
// SPDX-License-Identifier: Apache-2.0

package com.anandj.picnic.data.zip

import android.content.Context
import android.net.Uri
import com.anandj.picnic.data.db.AppDatabase
import com.anandj.picnic.data.db.entity.CommentEntity
import com.anandj.picnic.data.db.entity.ConnectionEntity
import com.anandj.picnic.data.db.entity.LikeEntity
import com.anandj.picnic.data.db.entity.MediaEntity
import com.anandj.picnic.data.db.entity.PostEntity
import com.anandj.picnic.data.db.entity.ReelEntity
import com.anandj.picnic.data.db.entity.StoryEntity
import com.anandj.picnic.data.zip.model.JsonCommentItem
import com.anandj.picnic.data.zip.model.JsonConnection
import com.anandj.picnic.data.zip.model.JsonFollowingWrapper
import com.anandj.picnic.data.zip.model.JsonLikesWrapper
import com.anandj.picnic.data.zip.model.JsonPost
import com.anandj.picnic.data.zip.model.JsonReelsWrapper
import com.anandj.picnic.data.zip.model.JsonStoriesWrapper
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import java.io.File
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ParseResult(
    val posts: Int,
    val media: Int,
    val stories: Int,
    val reels: Int,
    val followers: Int,
    val following: Int,
    val likes: Int,
    val comments: Int
) {
    fun summary(): String = "$posts posts · $media media\n" +
        "$stories stories · $reels reels\n" +
        "$followers followers · $following following\n" +
        "$likes likes · $comments comments"
}

class ZipParser(
    private val context: Context,
    private val db: AppDatabase
) {

    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(String::class.java, InstagramStringAdapter())
        .create()
    private val mediaDir: File
        get() = File(context.filesDir, "instagram/media").also { it.mkdirs() }

    /**
     * Two-pass ZIP parse:
     *  Pass 1 — detect root prefix, extract all media files, build relPath→localPath map.
     *  Pass 2 — parse JSON files using paths relative to root.
     */
    suspend fun parse(uri: Uri, onProgress: (String) -> Unit): ParseResult = withContext(Dispatchers.IO) {
        // Detect the variable root folder (e.g. "instagram-user-2025-07-31-Xyz/")
        val root = detectRoot(uri)

        onProgress("Extracting media…")
        val mediaMap = extractMediaFiles(uri, root)

        onProgress("Parsing posts…")
        val (posts, media) = parsePosts(uri, root, mediaMap)

        onProgress("Parsing stories…")
        val stories = parseStories(uri, root, mediaMap)

        onProgress("Parsing reels…")
        val reels = parseReels(uri, root, mediaMap)

        onProgress("Parsing followers…")
        val followers = parseConnections(uri, root, "follower")

        onProgress("Parsing following…")
        val following = parseFollowing(uri, root)

        onProgress("Parsing likes…")
        val likes = parseLikes(uri, root)

        onProgress("Parsing comments…")
        val comments = parseComments(uri, root)

        ParseResult(posts, media, stories, reels, followers, following, likes, comments)
    }

    // ─── Root detection ───────────────────────────────────────────────────────

    /** Returns the variable top-level folder prefix, e.g. "instagram-user-2025-07-31-Xyz/". */
    private fun detectRoot(uri: Uri): String {
        var root = ""
        openZip(uri) { zip ->
            val entry = zip.nextEntry
            if (entry != null) {
                val name = entry.name
                val slash = name.indexOf('/')
                root = if (slash >= 0) name.substring(0, slash + 1) else ""
            }
        }
        return root
    }

    // ─── Pass 1: media extraction ─────────────────────────────────────────────

    /**
     * Extracts files under "{root}media/" and builds a map from the relative path
     * (as referenced in post JSON uris, e.g. "media/posts/202507/foo.webp")
     * to the absolute path on device.
     */
    private fun extractMediaFiles(uri: Uri, root: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        openZip(uri) { zip ->
            var entry: ZipEntry? = zip.nextEntry
            while (entry != null) {
                val rel = entry.name.removePrefix(root)
                if (!entry.isDirectory && rel.startsWith("media/")) {
                    val localFile = File(mediaDir, rel.removePrefix("media/"))
                    localFile.parentFile?.mkdirs()
                    localFile.outputStream().use { out -> zip.copyTo(out) }
                    // Key matches the "uri" field in post/story JSON (e.g. "media/posts/…")
                    map[rel] = localFile.absolutePath
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        return map
    }

    // ─── Pass 2: JSON parsing ─────────────────────────────────────────────────

    /** Returns Pair(postCount, mediaCount). */
    private suspend fun parsePosts(uri: Uri, root: String, mediaMap: Map<String, String>): Pair<Int, Int> {
        val parsedGroups = mutableListOf<List<JsonPost>>()

        openZip(uri) { zip ->
            var entry: ZipEntry? = zip.nextEntry
            while (entry != null) {
                val rel = entry.name.removePrefix(root)
                if (!entry.isDirectory &&
                    rel.matches(Regex("your_instagram_activity/media/posts_\\d+\\.json"))
                ) {
                    val jsonPosts: List<JsonPost> = gson.fromJson(
                        zip.bufferedReader(),
                        object : TypeToken<List<JsonPost>>() {}.type
                    )
                    parsedGroups.add(jsonPosts)
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }

        var totalPosts = 0
        val allMedia = mutableListOf<MediaEntity>()
        for (jsonPosts in parsedGroups) {
            val postEntities = jsonPosts.map { jp ->
                val exif = jp.media?.firstOrNull()
                    ?.mediaMetadata?.photoMetadata?.exifData
                    ?.firstOrNull { it.latitude != null }
                PostEntity(
                    timestamp = jp.creationTimestamp
                        ?: jp.media?.firstOrNull()?.creationTimestamp
                        ?: 0L,
                    caption = (
                        jp.title?.takeIf { it.isNotBlank() }
                            ?: jp.media?.firstOrNull()?.title?.takeIf { it.isNotBlank() }
                        ),
                    latitude = exif?.latitude,
                    longitude = exif?.longitude
                )
            }
            val postIds = db.postDao().insertAll(postEntities)
            totalPosts += postIds.size
            jsonPosts.forEachIndexed { idx, jp ->
                val postId = postIds[idx]
                jp.media?.forEach { jm ->
                    val localPath = mediaMap[jm.uri] ?: jm.uri
                    val mediaType = if (jm.mediaMetadata?.videoMetadata != null) "video" else "image"
                    allMedia.add(
                        MediaEntity(
                            postId = postId,
                            uri = localPath,
                            mediaType = mediaType,
                            timestamp = jm.creationTimestamp
                        )
                    )
                }
            }
        }
        db.mediaDao().insertAll(allMedia)
        return totalPosts to allMedia.size
    }

    private suspend fun parseStories(uri: Uri, root: String, mediaMap: Map<String, String>): Int {
        val stories = mutableListOf<StoryEntity>()
        openZip(uri) { zip ->
            var entry: ZipEntry? = zip.nextEntry
            while (entry != null) {
                val rel = entry.name.removePrefix(root)
                if (!entry.isDirectory && rel == "your_instagram_activity/media/stories.json") {
                    val wrapper: JsonStoriesWrapper = gson.fromJson(
                        zip.bufferedReader(),
                        JsonStoriesWrapper::class.java
                    )
                    wrapper.igStories?.forEach { js ->
                        val localPath = mediaMap[js.uri] ?: js.uri
                        val mediaType = if (isVideoUri(js.uri) || js.mediaMetadata?.videoMetadata != null) "video" else "image"
                        stories.add(
                            StoryEntity(
                                uri = localPath,
                                timestamp = js.creationTimestamp,
                                mediaType = mediaType
                            )
                        )
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        db.storyDao().insertAll(stories)
        return stories.size
    }

    private suspend fun parseReels(uri: Uri, root: String, mediaMap: Map<String, String>): Int {
        val reels = mutableListOf<ReelEntity>()
        openZip(uri) { zip ->
            var entry: ZipEntry? = zip.nextEntry
            while (entry != null) {
                val rel = entry.name.removePrefix(root)
                if (!entry.isDirectory && rel == "your_instagram_activity/media/reels.json") {
                    val wrapper: JsonReelsWrapper = gson.fromJson(
                        zip.bufferedReader(),
                        JsonReelsWrapper::class.java
                    )
                    wrapper.igReelsMedia?.forEach { group ->
                        val jm = group.media?.firstOrNull() ?: return@forEach
                        val localPath = mediaMap[jm.uri] ?: jm.uri
                        val exif = jm.mediaMetadata?.videoMetadata?.exifData
                            ?.firstOrNull { it.latitude != null }
                        reels.add(
                            ReelEntity(
                                uri = localPath,
                                timestamp = jm.creationTimestamp,
                                caption = jm.title,
                                latitude = exif?.latitude,
                                longitude = exif?.longitude
                            )
                        )
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        db.reelDao().insertAll(reels)
        return reels.size
    }

    /** Parses followers_*.json (plain array format). */
    private suspend fun parseConnections(uri: Uri, root: String, type: String): Int {
        val connections = mutableListOf<ConnectionEntity>()

        openZip(uri) { zip ->
            var entry: ZipEntry? = zip.nextEntry
            while (entry != null) {
                val rel = entry.name.removePrefix(root)
                if (!entry.isDirectory &&
                    rel.matches(Regex("connections/followers_and_following/followers_\\d+\\.json"))
                ) {
                    val items: List<JsonConnection> = gson.fromJson(
                        zip.bufferedReader(),
                        object : TypeToken<List<JsonConnection>>() {}.type
                    )
                    items.forEach { jc ->
                        val data = jc.stringListData?.firstOrNull() ?: return@forEach
                        connections.add(
                            ConnectionEntity(
                                username = data.value,
                                href = data.href,
                                timestamp = data.timestamp,
                                type = type
                            )
                        )
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        db.connectionDao().insertAll(connections)
        return connections.size
    }

    /** Parses following.json (dict format: {"relationships_following": [...]}). */
    private suspend fun parseFollowing(uri: Uri, root: String): Int {
        val connections = mutableListOf<ConnectionEntity>()

        openZip(uri) { zip ->
            var entry: ZipEntry? = zip.nextEntry
            while (entry != null) {
                val rel = entry.name.removePrefix(root)
                if (!entry.isDirectory &&
                    rel == "connections/followers_and_following/following.json"
                ) {
                    val wrapper: JsonFollowingWrapper = gson.fromJson(
                        zip.bufferedReader(),
                        JsonFollowingWrapper::class.java
                    )
                    wrapper.relationships?.forEach { jc ->
                        val data = jc.stringListData?.firstOrNull() ?: return@forEach
                        connections.add(
                            ConnectionEntity(
                                username = data.value,
                                href = data.href,
                                timestamp = data.timestamp,
                                type = "following"
                            )
                        )
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        db.connectionDao().insertAll(connections)
        return connections.size
    }

    private suspend fun parseLikes(uri: Uri, root: String): Int {
        val likes = mutableListOf<LikeEntity>()
        openZip(uri) { zip ->
            var entry: ZipEntry? = zip.nextEntry
            while (entry != null) {
                val rel = entry.name.removePrefix(root)
                if (!entry.isDirectory && rel == "your_instagram_activity/likes/liked_posts.json") {
                    val wrapper: JsonLikesWrapper = gson.fromJson(
                        zip.bufferedReader(),
                        JsonLikesWrapper::class.java
                    )
                    wrapper.likes?.forEach { item ->
                        val data = item.stringListData?.firstOrNull() ?: return@forEach
                        val href = data.href ?: return@forEach
                        likes.add(
                            LikeEntity(
                                title = item.title,
                                href = href,
                                timestamp = data.timestamp
                            )
                        )
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        db.likeDao().insertAll(likes)
        return likes.size
    }

    private suspend fun parseComments(uri: Uri, root: String): Int {
        val comments = mutableListOf<CommentEntity>()
        openZip(uri) { zip ->
            var entry: ZipEntry? = zip.nextEntry
            while (entry != null) {
                val rel = entry.name.removePrefix(root)
                if (!entry.isDirectory &&
                    rel.matches(Regex("your_instagram_activity/comments/post_comments_\\d+\\.json"))
                ) {
                    // post_comments_*.json is a plain array (not wrapped in a dict key)
                    val items: List<JsonCommentItem> = gson.fromJson(
                        zip.bufferedReader(),
                        object : TypeToken<List<JsonCommentItem>>() {}.type
                    )
                    items.forEach { item ->
                        val map = item.stringMapData ?: return@forEach
                        val text = map["Comment"]?.value?.takeIf { it.isNotBlank() } ?: return@forEach
                        val timestamp = map["Time"]?.timestamp ?: 0L
                        val mediaOwner = map["Media Owner"]?.value
                        comments.add(
                            CommentEntity(
                                text = text,
                                timestamp = timestamp,
                                mediaOwner = mediaOwner
                            )
                        )
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        db.commentDao().insertAll(comments)
        return comments.size
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private fun openZip(uri: Uri, block: (ZipInputStream) -> Unit) {
        val inputStream: InputStream = context.contentResolver.openInputStream(uri)
            ?: error("Cannot open URI: $uri")
        ZipInputStream(inputStream.buffered()).use(block)
    }

    private fun isVideoUri(uri: String): Boolean {
        val lower = uri.lowercase()
        return lower.endsWith(".mp4") || lower.endsWith(".mov") || lower.endsWith(".webm") || lower.endsWith(".mkv")
    }
}

/**
 * Instagram exports UTF-8 text incorrectly: each byte of a multi-byte UTF-8 sequence is stored
 * as a separate Latin-1 character (e.g. ✌️ becomes â\u009c\u008cï¸). This adapter repairs
 * every parsed string by re-interpreting its chars as Latin-1 bytes and decoding as UTF-8.
 */
private class InstagramStringAdapter : TypeAdapter<String>() {
    override fun write(out: JsonWriter, value: String?) {
        out.value(value)
    }

    override fun read(input: JsonReader): String? {
        if (input.peek() == JsonToken.NULL) {
            input.nextNull()
            return null
        }
        return input.nextString().fixEncoding()
    }

    private fun String.fixEncoding(): String = try {
        String(toByteArray(Charsets.ISO_8859_1), Charsets.UTF_8)
    } catch (_: Exception) {
        this
    }
}
