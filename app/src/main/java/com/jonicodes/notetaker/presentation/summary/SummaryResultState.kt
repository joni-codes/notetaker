package com.jonicodes.notetaker.presentation.summary

import com.jonicodes.notetaker.domain.model.NoteSummary

data class SummaryResultState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val title: String = "",
    val summary: String = "",
    val transcript: String = "",
    val participants: List<String> = emptyList(),
    val error: String? = null,
    val isModelUnavailable: Boolean = false,
)

sealed class SummaryResultEffect {
    data object NavigateBack : SummaryResultEffect()
    data class ShowError(val message: String) : SummaryResultEffect()
    data object SavedSuccessfully : SummaryResultEffect()
}
