// Copyright (C) 2026 Anand Jesudason
// SPDX-License-Identifier: Apache-2.0

package com.anandj.picnic.data.db.relation

data class StoryGroup(
    val dayKey: String,
    val timestamp: Long,
    val coverUri: String,
    val coverMediaType: String,
    val coverId: Long,
    val itemCount: Int
)
