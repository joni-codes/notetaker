package com.jonicodes.notetaker.domain.usecase

import com.jonicodes.notetaker.domain.repository.SummaryRepository
import javax.inject.Inject

class DeleteSummaryUseCase @Inject constructor(
    private val summaryRepository: SummaryRepository
) {
    suspend operator fun invoke(id: Long) {
        summaryRepository.deleteSummary(id)
    }
}
