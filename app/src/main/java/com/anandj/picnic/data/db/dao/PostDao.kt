// Copyright (C) 2026 Anand Jesudason
// SPDX-License-Identifier: Apache-2.0

package com.anandj.picnic.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.anandj.picnic.data.db.entity.PostEntity
import com.anandj.picnic.data.db.relation.PostWithMedia
import kotlinx.coroutines.flow.Flow

@Dao
interface PostDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<PostEntity>): List<Long>

    @Transaction
    @Query("SELECT * FROM posts ORDER BY timestamp DESC")
    fun getAllWithMedia(): Flow<List<PostWithMedia>>

    @Transaction
    @Query("SELECT * FROM posts ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    suspend fun getPage(limit: Int, offset: Int): List<PostWithMedia>

    @Transaction
    @Query("SELECT * FROM posts WHERE id = :id")
    suspend fun getById(id: Long): PostWithMedia?

    @Query("SELECT COUNT(*) FROM posts")
    suspend fun getCount(): Int
}
