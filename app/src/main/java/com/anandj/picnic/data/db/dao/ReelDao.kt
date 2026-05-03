// Copyright (C) 2026 Anand Jesudason
// SPDX-License-Identifier: Apache-2.0

package com.anandj.picnic.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.anandj.picnic.data.db.entity.ReelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReelDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ReelEntity>)

    @Query("SELECT * FROM reels ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    suspend fun getPage(limit: Int, offset: Int): List<ReelEntity>

    @Query("SELECT COUNT(*) FROM reels")
    suspend fun getCount(): Int

    @Query("SELECT * FROM reels WHERE id = :id")
    suspend fun getById(id: Long): ReelEntity?

    @Query("SELECT * FROM reels ORDER BY timestamp DESC")
    fun getAll(): Flow<List<ReelEntity>>
}
