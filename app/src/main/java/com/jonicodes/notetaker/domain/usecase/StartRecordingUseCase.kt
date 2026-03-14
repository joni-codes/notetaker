package com.jonicodes.notetaker.domain.usecase

import com.jonicodes.notetaker.domain.repository.SpeechRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class StartRecordingUseCase @Inject constructor(
    private val speechRepository: SpeechRepository
) {
    val transcriptStream: Flow<String> get() = speechRepository.transcriptStream
    val isListening: Flow<Boolean> get() = speechRepository.isListening
    val errorStream: Flow<String> get() = speechRepository.errorStream

    suspend operator fun invoke() {
        speechRepository.startListening()
    }
}
