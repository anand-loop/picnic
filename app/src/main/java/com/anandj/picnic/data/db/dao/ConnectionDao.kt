// Copyright (C) 2026 Anand Jesudason
// SPDX-License-Identifier: Apache-2.0

package com.anandj.picnic.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.anandj.picnic.data.db.entity.ConnectionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConnectionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ConnectionEntity>)

    @Query("SELECT * FROM connections WHERE type = 'follower' ORDER BY timestamp DESC")
    fun getFollowers(): Flow<List<ConnectionEntity>>

    @Query("SELECT * FROM connections WHERE type = 'following' ORDER BY timestamp DESC")
    fun getFollowing(): Flow<List<ConnectionEntity>>

    @Query("SELECT * FROM connections WHERE type = :type ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    suspend fun getPage(type: String, limit: Int, offset: Int): List<ConnectionEntity>

    @Query("SELECT COUNT(*) FROM connections WHERE type = :type")
    suspend fun getCount(type: String): Int
}
