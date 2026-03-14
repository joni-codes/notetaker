package com.jonicodes.notetaker.presentation.history

import com.jonicodes.notetaker.domain.model.NoteSummary

enum class SortOrder { NEWEST_FIRST, OLDEST_FIRST }

data class HistoryState(
    val summaries: List<NoteSummary> = emptyList(),
    val filteredSummaries: List<NoteSummary> = emptyList(),
    val sortOrder: SortOrder = SortOrder.NEWEST_FIRST,
    val participantFilter: String? = null,
    val allParticipants: List<String> = emptyList(),
    val isLoading: Boolean = true,
    val showFilterSheet: Boolean = false,
)

sealed class HistoryEffect {
    data class NavigateToDetail(val summaryId: Long) : HistoryEffect()
    data class ShowError(val message: String) : HistoryEffect()
}
