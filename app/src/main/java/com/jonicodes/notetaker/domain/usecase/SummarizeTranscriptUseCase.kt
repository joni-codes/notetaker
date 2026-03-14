package com.jonicodes.notetaker.domain.usecase

import com.jonicodes.notetaker.domain.repository.SummarizationRepository
import com.jonicodes.notetaker.domain.repository.SummarizationResult
import javax.inject.Inject

class SummarizeTranscriptUseCase @Inject constructor(
    private val summarizationRepository: SummarizationRepository
) {
    suspend operator fun invoke(
        transcript: String,
        participants: List<String>
    ): SummarizationResult {
        return summarizationRepository.summarize(transcript, participants)
    }
}
