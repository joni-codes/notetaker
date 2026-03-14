package com.jonicodes.notetaker.data.repository

import com.jonicodes.notetaker.data.local.NoteSummaryDao
import com.jonicodes.notetaker.data.mapper.toDomain
import com.jonicodes.notetaker.data.mapper.toEntity
import com.jonicodes.notetaker.domain.model.NoteSummary
import com.jonicodes.notetaker.domain.repository.SummaryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SummaryRepositoryImpl @Inject constructor(
    private val dao: NoteSummaryDao
) : SummaryRepository {

    override fun getAllSummaries(): Flow<List<NoteSummary>> {
        return dao.getAllSummaries().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getSummaryById(id: Long): NoteSummary? {
        return dao.getSummaryById(id)?.toDomain()
    }

    override suspend fun insertSummary(summary: NoteSummary): Long {
        return dao.insertSummary(summary.toEntity())
    }

    override suspend fun updateSummary(summary: NoteSummary) {
        dao.updateSummary(summary.toEntity())
    }

    override suspend fun deleteSummary(id: Long) {
        dao.deleteSummary(id)
    }
}
