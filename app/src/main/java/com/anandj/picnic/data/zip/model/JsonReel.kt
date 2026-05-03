// Copyright (C) 2026 Anand Jesudason
// SPDX-License-Identifier: Apache-2.0

package com.anandj.picnic.data.zip.model

import com.google.gson.annotations.SerializedName

data class JsonReelsWrapper(
    @SerializedName("ig_reels_media") val igReelsMedia: List<JsonReelGroup>? = null
)

data class JsonReelGroup(
    @SerializedName("media") val media: List<JsonMedia>? = null
)
