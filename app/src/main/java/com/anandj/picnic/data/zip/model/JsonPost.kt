// Copyright (C) 2026 Anand Jesudason
// SPDX-License-Identifier: Apache-2.0

package com.anandj.picnic.data.zip.model

import com.google.gson.annotations.SerializedName

// Matches Instagram's content/posts_*.json format.
// Top-level is a JSON array of post objects.
data class JsonPost(
    @SerializedName("media") val media: List<JsonMedia>? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("creation_timestamp") val creationTimestamp: Long? = null
)

data class JsonMedia(
    @SerializedName("uri") val uri: String = "",
    @SerializedName("creation_timestamp") val creationTimestamp: Long = 0L,
    @SerializedName("title") val title: String? = null,
    @SerializedName("media_metadata") val mediaMetadata: JsonMediaMetadata? = null
)

data class JsonMediaMetadata(
    @SerializedName("video_metadata") val videoMetadata: JsonVideoMetadata? = null,
    @SerializedName("photo_metadata") val photoMetadata: JsonPhotoMetadata? = null
)

data class JsonVideoMetadata(
    @SerializedName("exif_data") val exifData: List<JsonExifData>? = null
)

data class JsonPhotoMetadata(
    @SerializedName("exif_data") val exifData: List<JsonExifData>? = null
)

data class JsonExifData(
    @SerializedName("latitude") val latitude: Double? = null,
    @SerializedName("longitude") val longitude: Double? = null
)
