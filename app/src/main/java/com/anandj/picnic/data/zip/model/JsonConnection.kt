// Copyright (C) 2026 Anand Jesudason
// SPDX-License-Identifier: Apache-2.0

package com.anandj.picnic.data.zip.model

import com.google.gson.annotations.SerializedName

// followers_*.json — top-level is a JSON array of these objects.
// following.json  — top-level is a dict {"relationships_following": [...]}.
data class JsonConnection(
    @SerializedName("string_list_data") val stringListData: List<JsonConnectionData>? = null
)

data class JsonFollowingWrapper(
    @SerializedName("relationships_following") val relationships: List<JsonConnection>? = null
)

data class JsonConnectionData(
    @SerializedName("href") val href: String? = null,
    @SerializedName("value") val value: String = "",
    @SerializedName("timestamp") val timestamp: Long = 0L
)
