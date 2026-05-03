// Copyright (C) 2026 Anand Jesudason
// SPDX-License-Identifier: Apache-2.0

package com.anandj.picnic.data.repository

import android.net.Uri
import com.anandj.picnic.data.db.entity.CommentEntity
import com.anandj.picnic.data.db.entity.ConnectionEntity
import com.anandj.picnic.data.db.entity.LikeEntity
import com.anandj.picnic.data.db.entity.ReelEntity
import com.anandj.picnic.data.db.entity.StoryEntity
import com.anandj.picnic.data.db.relation.PostWithMedia
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface InstagramRepository {
    val importState: StateFlow<ImportState>
    suspend fun hasData(): Boolean
    suspend fun clearData()
    suspend fun importFromZip(uri: Uri)
    fun getFeed(): Flow<List<PostWithMedia>>
    fun getReels(): Flow<List<ReelEntity>>
    fun getStories(): Flow<List<StoryEntity>>
    fun getFollowers(): Flow<List<ConnectionEntity>>
    fun getFollowing(): Flow<List<ConnectionEntity>>
    fun getLikes(): Flow<List<LikeEntity>>
    fun getComments(): Flow<List<CommentEntity>>
}

sealed class ImportState {
    object Idle : ImportState()
    data class InProgress(val message: String) : ImportState()
    data class Done(val posts: Int) : ImportState()
    data class Error(val cause: String) : ImportState()
}
