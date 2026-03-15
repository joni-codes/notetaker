package com.jonicodes.notetaker.presentation.history

import app.cash.turbine.test
import com.jonicodes.notetaker.domain.model.NoteSummary
import com.jonicodes.notetaker.domain.usecase.DeleteSummaryUseCase
import com.jonicodes.notetaker.domain.usecase.GetAllSummariesUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    private lateinit var getAllSummariesUseCase: GetAllSummariesUseCase
    private lateinit var deleteSummaryUseCase: DeleteSummaryUseCase
    private lateinit var viewModel: HistoryViewModel

    private lateinit var summariesFlow: MutableStateFlow<List<NoteSummary>>

    private val testDispatcher = UnconfinedTestDispatcher()

    private fun createSummary(
        id: Long = 1L,
        title: String = "Test Title",
        summary: String = "Test Summary",
        rawTranscript: String = "Test Transcript",
        participants: List<String> = emptyList(),
        createdAt: Long = 1000L
    ) = NoteSummary(id, title, summary, rawTranscript, participants, createdAt)

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        getAllSummariesUseCase = mock()
        deleteSummaryUseCase = mock()
        
        summariesFlow = MutableStateFlow(emptyList())
        whenever(getAllSummariesUseCase()).thenReturn(summariesFlow)
        
        viewModel = HistoryViewModel(getAllSummariesUseCase, deleteSummaryUseCase)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Nested
    @DisplayName("Initialization Tests")
    inner class Initialization {
        @Test
        @DisplayName("Initial state after init collects empty list has isLoading=false")
        fun initialState() = runTest {
            val uninitializedFlow = MutableStateFlow<List<NoteSummary>>(emptyList())
            val useCase = mock<GetAllSummariesUseCase>()
            whenever(useCase()).thenReturn(uninitializedFlow)
            
            val localViewModel = HistoryViewModel(useCase, deleteSummaryUseCase)
            
            val state = localViewModel.state.value
            assertFalse(state.isLoading)
            assertTrue(state.summaries.isEmpty())
            assertTrue(state.filteredSummaries.isEmpty())
            assertTrue(state.allParticipants.isEmpty())
        }

        @Test
        @DisplayName("Loading summaries from use case populates state correctly")
        fun loadsSummaries() = runTest {
            val summary = createSummary(id = 1L, title = "Meeting")
            summariesFlow.value = listOf(summary)
            
            advanceUntilIdle()
            
            val state = viewModel.state.value
            assertFalse(state.isLoading)
            assertEquals(1, state.summaries.size)
            assertEquals("Meeting", state.summaries[0].title)
            assertEquals(1, state.filteredSummaries.size)
        }

        @Test
        @DisplayName("allParticipants is derived from summaries (distinct, sorted)")
        fun derivesAllParticipants() = runTest {
            val summary1 = createSummary(id = 1L, participants = listOf("Zack", "Alice"))
            val summary2 = createSummary(id = 2L, participants = listOf("Bob", "Alice"))
            summariesFlow.value = listOf(summary1, summary2)
            
            advanceUntilIdle()
            
            val state = viewModel.state.value
            assertEquals(listOf("Alice", "Bob", "Zack"), state.allParticipants)
        }
    }

    @Nested
    @DisplayName("Search Filtering Tests")
    inner class SearchFiltering {
        @Test
        @DisplayName("Search filtering by title (case-insensitive)")
        fun filterByTitle() = runTest {
            val s1 = createSummary(id = 1L, title = "Alpha Meeting")
            val s2 = createSummary(id = 2L, title = "Beta Sync")
            summariesFlow.value = listOf(s1, s2)
            advanceUntilIdle()
            
            viewModel.onSearchQueryChanged("alpha")
            
            val state = viewModel.state.value
            assertEquals(1, state.filteredSummaries.size)
            assertEquals(1L, state.filteredSummaries[0].id)
        }

        @Test
        @DisplayName("Search filtering by summary content (case-insensitive)")
        fun filterBySummary() = runTest {
            val s1 = createSummary(id = 1L, summary = "Discussed new features")
            val s2 = createSummary(id = 2L, summary = "Fixed bugs")
            summariesFlow.value = listOf(s1, s2)
            advanceUntilIdle()
            
            viewModel.onSearchQueryChanged("FEATURE")
            
            val state = viewModel.state.value
            assertEquals(1, state.filteredSummaries.size)
            assertEquals(1L, state.filteredSummaries[0].id)
        }

        @Test
        @DisplayName("Search filtering by rawTranscript (case-insensitive)")
        fun filterByTranscript() = runTest {
            val s1 = createSummary(id = 1L, rawTranscript = "Hello world")
            val s2 = createSummary(id = 2L, rawTranscript = "Goodbye everyone")
            summariesFlow.value = listOf(s1, s2)
            advanceUntilIdle()
            
            viewModel.onSearchQueryChanged("WORLD")
            
            val state = viewModel.state.value
            assertEquals(1, state.filteredSummaries.size)
            assertEquals(1L, state.filteredSummaries[0].id)
        }

        @Test
        @DisplayName("Empty search query returns all summaries")
        fun emptySearchReturnsAll() = runTest {
            summariesFlow.value = listOf(createSummary(1L), createSummary(2L))
            advanceUntilIdle()
            
            viewModel.onSearchQueryChanged("Test")
            viewModel.onSearchQueryChanged("")
            
            assertEquals(2, viewModel.state.value.filteredSummaries.size)
        }
    }

    @Nested
    @DisplayName("Sorting Tests")
    inner class Sorting {
        @Test
        @DisplayName("Sort by NEWEST_FIRST (descending by createdAt)")
        fun sortNewestFirst() = runTest {
            val old = createSummary(id = 1L, createdAt = 100L)
            val new = createSummary(id = 2L, createdAt = 500L)
            summariesFlow.value = listOf(old, new)
            advanceUntilIdle()
            
            viewModel.onSortOrderChanged(SortOrder.NEWEST_FIRST)
            
            val state = viewModel.state.value
            assertEquals(2L, state.filteredSummaries[0].id)
            assertEquals(1L, state.filteredSummaries[1].id)
        }

        @Test
        @DisplayName("Sort by OLDEST_FIRST (ascending by createdAt)")
        fun sortOldestFirst() = runTest {
            val old = createSummary(id = 1L, createdAt = 100L)
            val new = createSummary(id = 2L, createdAt = 500L)
            summariesFlow.value = listOf(new, old)
            advanceUntilIdle()
            
            viewModel.onSortOrderChanged(SortOrder.OLDEST_FIRST)
            
            val state = viewModel.state.value
            assertEquals(1L, state.filteredSummaries[0].id)
            assertEquals(2L, state.filteredSummaries[1].id)
        }
    }

    @Nested
    @DisplayName("Participant Filtering Tests")
    inner class ParticipantFiltering {
        @Test
        @DisplayName("Participant filter shows only matching summaries")
        fun filterByParticipant() = runTest {
            val s1 = createSummary(id = 1L, participants = listOf("Alice", "Bob"))
            val s2 = createSummary(id = 2L, participants = listOf("Charlie"))
            summariesFlow.value = listOf(s1, s2)
            advanceUntilIdle()
            
            viewModel.onParticipantFilterChanged("Alice")
            
            assertEquals(1, viewModel.state.value.filteredSummaries.size)
            assertEquals(1L, viewModel.state.value.filteredSummaries[0].id)
        }

        @Test
        @DisplayName("Null participant filter shows all")
        fun nullFilterShowsAll() = runTest {
            val s1 = createSummary(id = 1L, participants = listOf("Alice"))
            val s2 = createSummary(id = 2L, participants = listOf("Bob"))
            summariesFlow.value = listOf(s1, s2)
            advanceUntilIdle()
            
            viewModel.onParticipantFilterChanged("Alice")
            viewModel.onParticipantFilterChanged(null)
            
            assertEquals(2, viewModel.state.value.filteredSummaries.size)
        }

        @Test
        @DisplayName("Combined search + participant filter")
        fun combinedSearchAndParticipant() = runTest {
            val s1 = createSummary(id = 1L, title = "Meeting A", participants = listOf("Alice"))
            val s2 = createSummary(id = 2L, title = "Meeting B", participants = listOf("Alice"))
            val s3 = createSummary(id = 3L, title = "Meeting A", participants = listOf("Bob"))
            summariesFlow.value = listOf(s1, s2, s3)
            advanceUntilIdle()
            
            viewModel.onSearchQueryChanged("Meeting A")
            viewModel.onParticipantFilterChanged("Alice")
            
            val filtered = viewModel.state.value.filteredSummaries
            assertEquals(1, filtered.size)
            assertEquals(1L, filtered[0].id)
        }

        @Test
        @DisplayName("Combined search + sort")
        fun combinedSearchAndSort() = runTest {
            val s1 = createSummary(id = 1L, title = "Meeting", createdAt = 100L)
            val s2 = createSummary(id = 2L, title = "Meeting", createdAt = 200L)
            val s3 = createSummary(id = 3L, title = "Other", createdAt = 300L)
            summariesFlow.value = listOf(s1, s2, s3)
            advanceUntilIdle()
            
            viewModel.onSearchQueryChanged("Meeting")
            viewModel.onSortOrderChanged(SortOrder.NEWEST_FIRST)
            
            val filtered = viewModel.state.value.filteredSummaries
            assertEquals(2, filtered.size)
            assertEquals(2L, filtered[0].id)
            assertEquals(1L, filtered[1].id)
        }
    }

    @Nested
    @DisplayName("Actions and Effects Tests")
    inner class ActionsAndEffects {
        @Test
        @DisplayName("onSummaryClicked emits NavigateToDetail effect")
        fun clickEmitsNavigate() = runTest {
            viewModel.effect.test {
                viewModel.onSummaryClicked(42L)
                val effect = awaitItem()
                assertTrue(effect is HistoryEffect.NavigateToDetail)
                assertEquals(42L, (effect as HistoryEffect.NavigateToDetail).summaryId)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("onDeleteSummary calls delete use case")
        fun deleteCallsUseCase() = runTest {
            viewModel.onDeleteSummary(42L)
            advanceUntilIdle()
            verify(deleteSummaryUseCase).invoke(42L)
        }

        @Test
        @DisplayName("onDeleteSummary with exception emits ShowError effect")
        fun deleteErrorEmitsEffect() = runTest {
            whenever(deleteSummaryUseCase.invoke(42L)).thenThrow(RuntimeException("DB Error"))
            
            viewModel.effect.test {
                viewModel.onDeleteSummary(42L)
                val effect = awaitItem()
                assertTrue(effect is HistoryEffect.ShowError)
                assertEquals("DB Error", (effect as HistoryEffect.ShowError).message)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    @DisplayName("UI State Toggles Tests")
    inner class UIState {
        @Test
        @DisplayName("onToggleFilterSheet toggles showFilterSheet")
        fun toggleSheet() = runTest {
            assertFalse(viewModel.state.value.showFilterSheet)
            
            viewModel.onToggleFilterSheet()
            assertTrue(viewModel.state.value.showFilterSheet)
            
            viewModel.onToggleFilterSheet()
            assertFalse(viewModel.state.value.showFilterSheet)
        }

        @Test
        @DisplayName("onDismissFilterSheet sets showFilterSheet to false")
        fun dismissSheet() = runTest {
            viewModel.onToggleFilterSheet() // set to true
            assertTrue(viewModel.state.value.showFilterSheet)
            
            viewModel.onDismissFilterSheet()
            assertFalse(viewModel.state.value.showFilterSheet)
        }
    }
}
