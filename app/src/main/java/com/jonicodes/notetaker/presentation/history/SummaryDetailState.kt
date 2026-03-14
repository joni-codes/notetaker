package com.jonicodes.notetaker.presentation.history

import com.jonicodes.notetaker.domain.model.NoteSummary

data class SummaryDetailState(
    val summary: NoteSummary? = null,
    val isLoading: Boolean = true,
    val showDeleteConfirmation: Boolean = false,
    val isEditing: Boolean = false,
    val editedTitle: String = "",
    val editedParticipants: List<String> = emptyList(),
    val participantInput: String = "",
    val isSaving: Boolean = false,
)

sealed class SummaryDetailEffect {
    data object NavigateBack : SummaryDetailEffect()
    data class ShowError(val message: String) : SummaryDetailEffect()
    data object SavedSuccessfully : SummaryDetailEffect()
}
