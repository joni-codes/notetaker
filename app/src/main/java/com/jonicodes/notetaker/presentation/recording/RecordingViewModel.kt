package com.jonicodes.notetaker.presentation.recording

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jonicodes.notetaker.domain.usecase.StartRecordingUseCase
import com.jonicodes.notetaker.domain.usecase.StopRecordingUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecordingViewModel @Inject constructor(
    private val startRecordingUseCase: StartRecordingUseCase,
    private val stopRecordingUseCase: StopRecordingUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(RecordingState())
    val state = _state.asStateFlow()

    private val _effect = MutableSharedFlow<RecordingEffect>()
    val effect = _effect.asSharedFlow()

    private var timerJob: Job? = null
    private var transcriptJob: Job? = null
    private var errorJob: Job? = null

    fun onPermissionResult(granted: Boolean) {
        _state.update { it.copy(hasPermission = granted) }
        if (!granted) {
            _state.update { it.copy(error = "Microphone permission is required to record conversations") }
        }
    }

    fun onStartRecording() {
        if (!_state.value.hasPermission) {
            viewModelScope.launch {
                _effect.emit(RecordingEffect.RequestPermission)
            }
            return
        }

        _state.update {
            it.copy(
                isRecording = true,
                isPreparing = true,
                liveTranscript = "",
                partialText = "",
                elapsedSeconds = 0,
                error = null,
            )
        }

        transcriptJob = viewModelScope.launch {
            startRecordingUseCase.transcriptStream.collect { text ->
                _state.update {
                    it.copy(
                        liveTranscript = text,
                        isPreparing = false,
                    )
                }
            }
        }

        errorJob = viewModelScope.launch {
            startRecordingUseCase.errorStream.collect { errorMsg ->
                _state.update { it.copy(error = errorMsg) }
            }
        }

        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _state.update { it.copy(elapsedSeconds = it.elapsedSeconds + 1) }
            }
        }

        viewModelScope.launch {
            startRecordingUseCase()
            _state.update { it.copy(isPreparing = false) }
        }
    }

    fun onStopRecording() {
        viewModelScope.launch {
            timerJob?.cancel()
            transcriptJob?.cancel()
            errorJob?.cancel()

            val finalTranscript = stopRecordingUseCase()

            _state.update {
                it.copy(
                    isRecording = false,
                    isPreparing = false,
                    liveTranscript = "",
                    partialText = "",
                    elapsedSeconds = 0,
                )
            }

            if (finalTranscript.isBlank()) {
                _effect.emit(RecordingEffect.ShowError("No speech was detected. Please try again."))
            } else {
                _effect.emit(
                    RecordingEffect.NavigateToSummary(
                        transcript = finalTranscript,
                        participants = _state.value.participants,
                    )
                )
            }
        }
    }

    fun onParticipantInputChanged(input: String) {
        _state.update { it.copy(participantInput = input) }
    }

    fun onAddParticipant() {
        val name = _state.value.participantInput.trim()
        if (name.isNotBlank() && name !in _state.value.participants) {
            _state.update {
                it.copy(
                    participants = it.participants + name,
                    participantInput = "",
                )
            }
        }
    }

    fun onRemoveParticipant(name: String) {
        _state.update {
            it.copy(participants = it.participants - name)
        }
    }

    fun onDismissError() {
        _state.update { it.copy(error = null) }
    }

    fun resetSession() {
        _state.update {
            it.copy(
                isRecording = false,
                isPreparing = false,
                liveTranscript = "",
                partialText = "",
                participants = emptyList(),
                participantInput = "",
                elapsedSeconds = 0,
                error = null,
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        transcriptJob?.cancel()
        errorJob?.cancel()
    }
}
