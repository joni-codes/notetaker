package com.jonicodes.notetaker.presentation.paste

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PasteViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow(PasteState())
    val state = _state.asStateFlow()

    private val _effect = MutableSharedFlow<PasteEffect>()
    val effect = _effect.asSharedFlow()

    fun onTranscriptChanged(text: String) {
        _state.update { it.copy(transcript = text, error = null) }
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

    fun onSubmit() {
        val transcript = _state.value.transcript.trim()
        if (transcript.isBlank()) {
            _state.update { it.copy(error = "Please enter or paste a transcript first") }
            return
        }
        viewModelScope.launch {
            _effect.emit(
                PasteEffect.NavigateToSummary(
                    transcript = transcript,
                    participants = _state.value.participants,
                )
            )
        }
    }

    fun onDismissError() {
        _state.update { it.copy(error = null) }
    }

    fun resetSession() {
        _state.update {
            PasteState()
        }
    }
}
