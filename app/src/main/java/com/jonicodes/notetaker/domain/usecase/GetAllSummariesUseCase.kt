package com.jonicodes.notetaker.domain.usecase

import com.jonicodes.notetaker.domain.model.NoteSummary
import com.jonicodes.notetaker.domain.repository.SummaryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAllSummariesUseCase @Inject constructor(
    private val summaryRepository: SummaryRepository
) {
    operator fun invoke(): Flow<List<NoteSummary>> {
        return summaryRepository.getAllSummaries()
    }
}
