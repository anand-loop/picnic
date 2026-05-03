// Copyright (C) 2026 Anand Jesudason
// SPDX-License-Identifier: Apache-2.0

package com.anandj.picnic.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.anandj.picnic.data.db.dao.CommentDao
import com.anandj.picnic.data.db.dao.ConnectionDao
import com.anandj.picnic.data.db.dao.LikeDao
import com.anandj.picnic.data.db.dao.MediaDao
import com.anandj.picnic.data.db.dao.PostDao
import com.anandj.picnic.data.db.dao.ReelDao
import com.anandj.picnic.data.db.dao.StoryDao
import com.anandj.picnic.data.db.entity.CommentEntity
import com.anandj.picnic.data.db.entity.ConnectionEntity
import com.anandj.picnic.data.db.entity.LikeEntity
import com.anandj.picnic.data.db.entity.MediaEntity
import com.anandj.picnic.data.db.entity.PostEntity
import com.anandj.picnic.data.db.entity.ReelEntity
import com.anandj.picnic.data.db.entity.StoryEntity

@Database(
    entities = [
        PostEntity::class,
        MediaEntity::class,
        StoryEntity::class,
        ConnectionEntity::class,
        LikeEntity::class,
        CommentEntity::class,
        ReelEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun postDao(): PostDao
    abstract fun mediaDao(): MediaDao
    abstract fun storyDao(): StoryDao
    abstract fun connectionDao(): ConnectionDao
    abstract fun likeDao(): LikeDao
    abstract fun commentDao(): CommentDao
    abstract fun reelDao(): ReelDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE posts ADD COLUMN latitude REAL")
                database.execSQL("ALTER TABLE posts ADD COLUMN longitude REAL")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS reels (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "uri TEXT NOT NULL, " +
                        "timestamp INTEGER NOT NULL, " +
                        "caption TEXT, " +
                        "latitude REAL, " +
                        "longitude REAL)"
                )
            }
        }

        fun getInstance(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "picnic.db"
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { instance = it }
        }
    }
}
