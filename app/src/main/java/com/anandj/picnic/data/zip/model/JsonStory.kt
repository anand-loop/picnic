// Copyright (C) 2026 Anand Jesudason
// SPDX-License-Identifier: Apache-2.0

package com.anandj.picnic.data.zip.model

import com.google.gson.annotations.SerializedName

// Matches Instagram's content/stories.json format.
// Top-level object with an "ig_stories" array.
data class JsonStoriesWrapper(
    @SerializedName("ig_stories") val igStories: List<JsonStory>? = null
)

data class JsonStory(
    @SerializedName("uri") val uri: String = "",
    @SerializedName("creation_timestamp") val creationTimestamp: Long = 0L,
    @SerializedName("media_metadata") val mediaMetadata: JsonStoryMetadata? = null
)

data class JsonStoryMetadata(
    @SerializedName("video_metadata") val videoMetadata: Any? = null
)
