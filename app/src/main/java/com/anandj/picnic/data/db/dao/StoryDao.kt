// Copyright (C) 2026 Anand Jesudason
// SPDX-License-Identifier: Apache-2.0

package com.anandj.picnic.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.anandj.picnic.data.db.entity.StoryEntity
import com.anandj.picnic.data.db.relation.StoryGroup
import kotlinx.coroutines.flow.Flow

@Dao
interface StoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<StoryEntity>)

    @Query("""
        SELECT
            date(timestamp, 'unixepoch', 'localtime') AS dayKey,
            MAX(timestamp) AS timestamp,
            uri AS coverUri,
            mediaType AS coverMediaType,
            id AS coverId,
            COUNT(*) AS itemCount
        FROM stories
        GROUP BY dayKey
        ORDER BY timestamp DESC
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getGroupedPage(limit: Int, offset: Int): List<StoryGroup>

    @Query("SELECT COUNT(DISTINCT date(timestamp, 'unixepoch', 'localtime')) FROM stories")
    suspend fun getGroupCount(): Int

    @Query("""
        SELECT * FROM stories
        WHERE date(timestamp, 'unixepoch', 'localtime') = :dayKey
        ORDER BY timestamp ASC
    """)
    suspend fun getByDay(dayKey: String): List<StoryEntity>

    @Query("SELECT * FROM stories ORDER BY timestamp DESC")
    fun getAll(): Flow<List<StoryEntity>>
}
