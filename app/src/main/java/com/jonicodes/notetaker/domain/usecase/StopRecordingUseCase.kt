package com.jonicodes.notetaker.domain.usecase

import com.jonicodes.notetaker.domain.repository.SpeechRepository
import javax.inject.Inject

class StopRecordingUseCase @Inject constructor(
    private val speechRepository: SpeechRepository
) {
    suspend operator fun invoke(): String {
        return speechRepository.stopListening()
    }
}
