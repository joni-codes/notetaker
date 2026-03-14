package com.jonicodes.notetaker.domain.usecase

import com.jonicodes.notetaker.domain.model.NoteSummary
import com.jonicodes.notetaker.domain.repository.SummaryRepository
import javax.inject.Inject

class UpdateSummaryUseCase @Inject constructor(
    private val summaryRepository: SummaryRepository
) {
    suspend operator fun invoke(summary: NoteSummary) {
        summaryRepository.updateSummary(summary)
    }
}
