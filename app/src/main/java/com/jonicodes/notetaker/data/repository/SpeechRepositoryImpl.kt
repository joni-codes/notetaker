package com.jonicodes.notetaker.data.repository

import com.jonicodes.notetaker.data.source.SpeechDataSource
import com.jonicodes.notetaker.data.source.SpeechEvent
import com.jonicodes.notetaker.domain.repository.SpeechRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpeechRepositoryImpl @Inject constructor(
    private val speechDataSource: SpeechDataSource
) : SpeechRepository {

    private val _transcriptStream = MutableSharedFlow<String>(replay = 1)
    override val transcriptStream: Flow<String> = _transcriptStream.asSharedFlow()

    private val _isListening = MutableStateFlow(false)
    override val isListening: Flow<Boolean> = _isListening.asStateFlow()

    private val _errorStream = MutableSharedFlow<String>()
    override val errorStream: Flow<String> = _errorStream.asSharedFlow()

    private var listeningJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun startListening() {
        listeningJob?.cancel()
        _transcriptStream.resetReplayCache()
        _isListening.value = true

        listeningJob = scope.launch {
            speechDataSource.startListening().collect { event ->
                when (event) {
                    is SpeechEvent.FinalResult -> {
                        _transcriptStream.emit(event.fullTranscript)
                    }
                    is SpeechEvent.PartialResult -> {
                        _transcriptStream.emit(event.text)
                    }
                    is SpeechEvent.Error -> {
                        _errorStream.emit(event.message)
                    }
                    is SpeechEvent.Ready -> {}
                    is SpeechEvent.RmsChanged -> {}
                }
            }
        }
    }

    override suspend fun stopListening(): String {
        _isListening.value = false
        listeningJob?.cancel()
        listeningJob = null
        return speechDataSource.stopListening()
    }
}
