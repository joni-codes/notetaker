package com.jonicodes.notetaker.domain.repository

import com.jonicodes.notetaker.domain.model.NoteSummary
import kotlinx.coroutines.flow.Flow

interface SummaryRepository {
    fun getAllSummaries(): Flow<List<NoteSummary>>
    suspend fun getSummaryById(id: Long): NoteSummary?
    suspend fun insertSummary(summary: NoteSummary): Long
    suspend fun updateSummary(summary: NoteSummary)
    suspend fun deleteSummary(id: Long)
}
