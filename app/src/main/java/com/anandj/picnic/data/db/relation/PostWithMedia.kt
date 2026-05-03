// Copyright (C) 2026 Anand Jesudason
// SPDX-License-Identifier: Apache-2.0

package com.anandj.picnic.data.db.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.anandj.picnic.data.db.entity.MediaEntity
import com.anandj.picnic.data.db.entity.PostEntity

data class PostWithMedia(
    @Embedded val post: PostEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "postId"
    )
    val media: List<MediaEntity>
)
