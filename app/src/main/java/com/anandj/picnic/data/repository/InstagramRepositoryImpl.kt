// Copyright (C) 2026 Anand Jesudason
// SPDX-License-Identifier: Apache-2.0

package com.anandj.picnic.data.repository

import android.content.Context
import android.net.Uri
import com.anandj.picnic.data.db.AppDatabase
import com.anandj.picnic.data.db.entity.CommentEntity
import com.anandj.picnic.data.db.entity.ConnectionEntity
import com.anandj.picnic.data.db.entity.LikeEntity
import com.anandj.picnic.data.db.entity.ReelEntity
import com.anandj.picnic.data.db.entity.StoryEntity
import com.anandj.picnic.data.db.relation.PostWithMedia
import com.anandj.picnic.data.zip.ZipParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class InstagramRepositoryImpl(
    private val context: Context,
    private val db: AppDatabase = AppDatabase.getInstance(context)
) : InstagramRepository {

    private val _importState = MutableStateFlow<ImportState>(ImportState.Idle)
    override val importState: StateFlow<ImportState> = _importState.asStateFlow()

    private val parser = ZipParser(context, db)

    override suspend fun hasData(): Boolean = withContext(Dispatchers.IO) { db.postDao().getCount() > 0 }

    override suspend fun clearData() = withContext(Dispatchers.IO) { db.clearAllTables() }

    override suspend fun importFromZip(uri: Uri) {
        _importState.value = ImportState.InProgress("Starting import…")
        try {
            withContext(Dispatchers.IO) { db.clearAllTables() }
            val result = parser.parse(uri) { message ->
                _importState.value = ImportState.InProgress(message)
            }
            if (result.posts == 0) {
                _importState.value = ImportState.Error(
                    "No posts found. Make sure you selected your Instagram data export ZIP."
                )
            } else {
                _importState.value = ImportState.Done(result.posts)
            }
        } catch (e: Exception) {
            _importState.value = ImportState.Error(
                "Couldn't read the ZIP file. Make sure you selected your Instagram data export."
            )
        }
    }

    override fun getFeed(): Flow<List<PostWithMedia>> = db.postDao().getAllWithMedia()

    override fun getReels(): Flow<List<ReelEntity>> = db.reelDao().getAll()

    override fun getStories(): Flow<List<StoryEntity>> = db.storyDao().getAll()

    override fun getFollowers(): Flow<List<ConnectionEntity>> = db.connectionDao().getFollowers()

    override fun getFollowing(): Flow<List<ConnectionEntity>> = db.connectionDao().getFollowing()

    override fun getLikes(): Flow<List<LikeEntity>> = db.likeDao().getAll()

    override fun getComments(): Flow<List<CommentEntity>> = db.commentDao().getAll()
}
