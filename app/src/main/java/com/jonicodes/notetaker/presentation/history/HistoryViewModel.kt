package com.jonicodes.notetaker.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jonicodes.notetaker.domain.model.NoteSummary
import com.jonicodes.notetaker.domain.usecase.DeleteSummaryUseCase
import com.jonicodes.notetaker.domain.usecase.GetAllSummariesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val getAllSummariesUseCase: GetAllSummariesUseCase,
    private val deleteSummaryUseCase: DeleteSummaryUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(HistoryState())
    val state = _state.asStateFlow()

    private val _effect = MutableSharedFlow<HistoryEffect>()
    val effect = _effect.asSharedFlow()

    init {
        viewModelScope.launch {
            getAllSummariesUseCase().collect { summaries ->
                val allParticipants = summaries
                    .flatMap { it.participants }
                    .distinct()
                    .sorted()

                _state.update {
                    it.copy(
                        summaries = summaries,
                        allParticipants = allParticipants,
                        isLoading = false,
                    )
                }
                applyFiltersAndSort()
            }
        }
    }

    fun onSummaryClicked(summaryId: Long) {
        viewModelScope.launch {
            _effect.emit(HistoryEffect.NavigateToDetail(summaryId))
        }
    }

    fun onDeleteSummary(summaryId: Long) {
        viewModelScope.launch {
            try {
                deleteSummaryUseCase(summaryId)
            } catch (e: Exception) {
                _effect.emit(HistoryEffect.ShowError(e.message ?: "Failed to delete"))
            }
        }
    }

    fun onSortOrderChanged(order: SortOrder) {
        _state.update { it.copy(sortOrder = order) }
        applyFiltersAndSort()
    }

    fun onParticipantFilterChanged(participant: String?) {
        _state.update { it.copy(participantFilter = participant) }
        applyFiltersAndSort()
    }

    fun onSearchQueryChanged(query: String) {
        _state.update { it.copy(searchQuery = query) }
        applyFiltersAndSort()
    }

    fun onToggleFilterSheet() {
        _state.update { it.copy(showFilterSheet = !it.showFilterSheet) }
    }

    fun onDismissFilterSheet() {
        _state.update { it.copy(showFilterSheet = false) }
    }

    private fun applyFiltersAndSort() {
        val current = _state.value
        var result = current.summaries

        val query = current.searchQuery.trim()
        if (query.isNotEmpty()) {
            result = result.filter { summary ->
                summary.title.contains(query, ignoreCase = true) ||
                    summary.summary.contains(query, ignoreCase = true) ||
                    summary.rawTranscript.contains(query, ignoreCase = true)
            }
        }

        current.participantFilter?.let { filter ->
            result = result.filter { summary ->
                summary.participants.any { it.equals(filter, ignoreCase = true) }
            }
        }

        result = when (current.sortOrder) {
            SortOrder.NEWEST_FIRST -> result.sortedByDescending { it.createdAt }
            SortOrder.OLDEST_FIRST -> result.sortedBy { it.createdAt }
        }

        _state.update { it.copy(filteredSummaries = result) }
    }
}
