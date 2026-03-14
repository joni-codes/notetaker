package com.jonicodes.notetaker.presentation.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jonicodes.notetaker.domain.usecase.DeleteSummaryUseCase
import com.jonicodes.notetaker.domain.usecase.UpdateSummaryUseCase
import com.jonicodes.notetaker.domain.repository.SummaryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SummaryDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val summaryRepository: SummaryRepository,
    private val deleteSummaryUseCase: DeleteSummaryUseCase,
    private val updateSummaryUseCase: UpdateSummaryUseCase,
) : ViewModel() {

    private val summaryId: Long = savedStateHandle.get<Long>("summaryId") ?: -1L

    private val _state = MutableStateFlow(SummaryDetailState())
    val state = _state.asStateFlow()

    private val _effect = MutableSharedFlow<SummaryDetailEffect>()
    val effect = _effect.asSharedFlow()

    init {
        loadSummary()
    }

    private fun loadSummary() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val summary = summaryRepository.getSummaryById(summaryId)
            if (summary != null) {
                _state.update {
                    it.copy(
                        summary = summary,
                        isLoading = false,
                        editedTitle = summary.title,
                        editedParticipants = summary.participants,
                    )
                }
            } else {
                _state.update { it.copy(isLoading = false) }
                _effect.emit(SummaryDetailEffect.ShowError("Summary not found"))
                _effect.emit(SummaryDetailEffect.NavigateBack)
            }
        }
    }

    fun onEditClicked() {
        _state.update { it.copy(isEditing = true) }
    }

    fun onCancelEdit() {
        val summary = _state.value.summary ?: return
        _state.update {
            it.copy(
                isEditing = false,
                editedTitle = summary.title,
                editedParticipants = summary.participants,
                participantInput = "",
            )
        }
    }

    fun onTitleChanged(title: String) {
        _state.update { it.copy(editedTitle = title) }
    }

    fun onParticipantInputChanged(input: String) {
        _state.update { it.copy(participantInput = input) }
    }

    fun onAddParticipant() {
        val name = _state.value.participantInput.trim()
        if (name.isBlank()) return
        if (_state.value.editedParticipants.contains(name)) {
            _state.update { it.copy(participantInput = "") }
            return
        }
        _state.update {
            it.copy(
                editedParticipants = it.editedParticipants + name,
                participantInput = "",
            )
        }
    }

    fun onRemoveParticipant(name: String) {
        _state.update {
            it.copy(editedParticipants = it.editedParticipants - name)
        }
    }

    fun onSaveEdits() {
        val summary = _state.value.summary ?: return
        val currentState = _state.value

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            try {
                val updated = summary.copy(
                    title = currentState.editedTitle.ifBlank { "Untitled Conversation" },
                    participants = currentState.editedParticipants,
                )
                updateSummaryUseCase(updated)
                _state.update {
                    it.copy(
                        summary = updated,
                        isEditing = false,
                        isSaving = false,
                    )
                }
                _effect.emit(SummaryDetailEffect.SavedSuccessfully)
            } catch (e: Exception) {
                _state.update { it.copy(isSaving = false) }
                _effect.emit(SummaryDetailEffect.ShowError(e.message ?: "Failed to save changes"))
            }
        }
    }

    fun onDeleteClicked() {
        _state.update { it.copy(showDeleteConfirmation = true) }
    }

    fun onDismissDeleteConfirmation() {
        _state.update { it.copy(showDeleteConfirmation = false) }
    }

    fun onConfirmDelete() {
        viewModelScope.launch {
            try {
                deleteSummaryUseCase(summaryId)
                _effect.emit(SummaryDetailEffect.NavigateBack)
            } catch (e: Exception) {
                _effect.emit(SummaryDetailEffect.ShowError(e.message ?: "Failed to delete"))
            }
        }
    }

    fun onBack() {
        viewModelScope.launch {
            _effect.emit(SummaryDetailEffect.NavigateBack)
        }
    }
}
