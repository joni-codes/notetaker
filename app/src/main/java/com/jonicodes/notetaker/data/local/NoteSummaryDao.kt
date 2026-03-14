package com.jonicodes.notetaker.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteSummaryDao {

    @Query("SELECT * FROM note_summaries ORDER BY createdAt DESC")
    fun getAllSummaries(): Flow<List<NoteSummaryEntity>>

    @Query("SELECT * FROM note_summaries WHERE id = :id")
    suspend fun getSummaryById(id: Long): NoteSummaryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSummary(entity: NoteSummaryEntity): Long

    @Update
    suspend fun updateSummary(entity: NoteSummaryEntity)

    @Query("DELETE FROM note_summaries WHERE id = :id")
    suspend fun deleteSummary(id: Long)
}
