// Copyright (C) 2026 Anand Jesudason
// SPDX-License-Identifier: Apache-2.0

package com.anandj.picnic.data.zip.model

import com.google.gson.annotations.SerializedName

// Shared shape for likes (likes/liked_posts.json) and comments (comments/post_comments*.json).
// Both files use the same outer wrapper with a key containing a list of engagement items.

data class JsonLikesWrapper(
    @SerializedName("likes_media_likes") val likes: List<JsonEngagementItem>? = null
)

data class JsonCommentsWrapper(
    @SerializedName("comments_media_comments") val comments: List<JsonCommentItem>? = null
)

data class JsonEngagementItem(
    @SerializedName("title") val title: String? = null,
    @SerializedName("string_list_data") val stringListData: List<JsonEngagementData>? = null
)

data class JsonCommentItem(
    @SerializedName("string_map_data") val stringMapData: Map<String, JsonEngagementData>? = null,
    @SerializedName("media_owner") val mediaOwner: String? = null
)

data class JsonEngagementData(
    @SerializedName("href") val href: String? = null,
    @SerializedName("value") val value: String = "",
    @SerializedName("timestamp") val timestamp: Long = 0L
)
