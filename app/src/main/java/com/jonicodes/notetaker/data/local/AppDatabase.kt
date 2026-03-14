package com.jonicodes.notetaker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [NoteSummaryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteSummaryDao(): NoteSummaryDao
}
