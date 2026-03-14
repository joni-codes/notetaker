package com.jonicodes.notetaker.domain.usecase

import com.jonicodes.notetaker.domain.repository.SummarizationRepository
import javax.inject.Inject

class CheckAiAvailabilityUseCase @Inject constructor(
    private val summarizationRepository: SummarizationRepository
) {
    suspend operator fun invoke(): Boolean {
        return summarizationRepository.checkAvailability()
    }
}
