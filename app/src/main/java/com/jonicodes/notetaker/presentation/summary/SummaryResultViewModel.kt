package com.jonicodes.notetaker.presentation.summary

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jonicodes.notetaker.domain.model.NoteSummary
import com.jonicodes.notetaker.domain.repository.SummarizationResult
import com.jonicodes.notetaker.domain.usecase.SaveSummaryUseCase
import com.jonicodes.notetaker.domain.usecase.SummarizeTranscriptUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.URLDecoder
import javax.inject.Inject

@HiltViewModel
class SummaryResultViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val summarizeTranscriptUseCase: SummarizeTranscriptUseCase,
    private val saveSummaryUseCase: SaveSummaryUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(SummaryResultState())
    val state = _state.asStateFlow()

    private val _effect = MutableSharedFlow<SummaryResultEffect>()
    val effect = _effect.asSharedFlow()

    init {
        val transcript = URLDecoder.decode(
            savedStateHandle.get<String>("transcript").orEmpty(), "UTF-8"
        )
        val participantsRaw = URLDecoder.decode(
            savedStateHandle.get<String>("participants").orEmpty(), "UTF-8"
        )
        val participants = if (participantsRaw.isBlank()) emptyList()
        else participantsRaw.split("||")

        _state.update {
            it.copy(
                transcript = transcript,
                participants = participants,
            )
        }

        summarize(transcript, participants)
    }

    private fun summarize(transcript: String, participants: List<String>) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            when (val result = summarizeTranscriptUseCase(transcript, participants)) {
                is SummarizationResult.Success -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            title = result.title,
                            summary = result.summary,
                        )
                    }
                }
                is SummarizationResult.Error -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = result.message,
                        )
                    }
                }
                is SummarizationResult.ModelUnavailable -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isModelUnavailable = true,
                            error = "AI model is not available on this device",
                        )
                    }
                }
                is SummarizationResult.Downloading -> {
                    _state.update {
                        it.copy(isLoading = true)
                    }
                }
            }
        }
    }

    fun onTitleChanged(title: String) {
        _state.update { it.copy(title = title) }
    }

    fun onSave() {
        val currentState = _state.value
        if (currentState.summary.isBlank()) return

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            try {
                val summary = NoteSummary(
                    title = currentState.title.ifBlank { "Untitled Conversation" },
                    summary = currentState.summary,
                    rawTranscript = currentState.transcript,
                    participants = currentState.participants,
                    createdAt = System.currentTimeMillis(),
                )
                saveSummaryUseCase(summary)
                _effect.emit(SummaryResultEffect.SavedSuccessfully)
            } catch (e: Exception) {
                _state.update { it.copy(isSaving = false) }
                _effect.emit(SummaryResultEffect.ShowError(e.message ?: "Failed to save"))
            }
        }
    }

    fun onRetry() {
        summarize(_state.value.transcript, _state.value.participants)
    }

    fun onDiscard() {
        viewModelScope.launch {
            _effect.emit(SummaryResultEffect.NavigateBack)
        }
    }
}
