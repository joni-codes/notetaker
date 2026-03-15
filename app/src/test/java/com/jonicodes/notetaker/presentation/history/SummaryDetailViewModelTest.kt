package com.jonicodes.notetaker.presentation.history

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.jonicodes.notetaker.domain.model.NoteSummary
import com.jonicodes.notetaker.domain.repository.SummaryRepository
import com.jonicodes.notetaker.domain.usecase.DeleteSummaryUseCase
import com.jonicodes.notetaker.domain.usecase.UpdateSummaryUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class SummaryDetailViewModelTest {

    private val summaryRepository: SummaryRepository = mock()
    private val deleteSummaryUseCase: DeleteSummaryUseCase = mock()
    private val updateSummaryUseCase: UpdateSummaryUseCase = mock()
    
    private lateinit var viewModel: SummaryDetailViewModel
    
    private val testDispatcher = UnconfinedTestDispatcher()
    
    private val dummySummary = NoteSummary(
        id = 42L,
        title = "Test Title",
        summary = "Test Summary",
        rawTranscript = "Test Transcript",
        participants = listOf("Alice", "Bob"),
        createdAt = 123456789L
    )

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(summaryId: Long = 42L) {
        val savedStateHandle = SavedStateHandle(mapOf("summaryId" to summaryId))
        viewModel = SummaryDetailViewModel(
            savedStateHandle = savedStateHandle,
            summaryRepository = summaryRepository,
            deleteSummaryUseCase = deleteSummaryUseCase,
            updateSummaryUseCase = updateSummaryUseCase
        )
    }

    @Nested
    @DisplayName("Initialization Tests")
    inner class InitializationTests {
        
        @Test
        @DisplayName("loadSummary success sets state correctly")
        fun loadSummarySuccess() = runTest {
            whenever(summaryRepository.getSummaryById(42L)).thenReturn(dummySummary)
            
            createViewModel()
            
            val state = viewModel.state.value
            assertEquals(dummySummary, state.summary)
            assertEquals("Test Title", state.editedTitle)
            assertEquals(listOf("Alice", "Bob"), state.editedParticipants)
            assertFalse(state.isLoading)
        }

        @Test
        @DisplayName("loadSummary not found emits ShowError and NavigateBack")
        fun loadSummaryNotFound() = runTest {
            val stdDispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(stdDispatcher)

            whenever(summaryRepository.getSummaryById(99L)).thenReturn(null)
            
            val savedStateHandle = SavedStateHandle(mapOf("summaryId" to 99L))
            val vm = SummaryDetailViewModel(
                savedStateHandle, summaryRepository, deleteSummaryUseCase, updateSummaryUseCase
            )

            vm.effect.test {
                advanceUntilIdle()

                val effect1 = awaitItem()
                assertTrue(effect1 is SummaryDetailEffect.ShowError)
                assertEquals("Summary not found", (effect1 as SummaryDetailEffect.ShowError).message)
                
                val effect2 = awaitItem()
                assertEquals(SummaryDetailEffect.NavigateBack, effect2)
            }
        }
    }

    @Nested
    @DisplayName("Editing State Tests")
    inner class EditingStateTests {
        
        @BeforeEach
        fun setupViewModel() = runTest {
            whenever(summaryRepository.getSummaryById(42L)).thenReturn(dummySummary)
            createViewModel()
        }

        @Test
        @DisplayName("onEditClicked sets isEditing to true")
        fun onEditClicked() {
            viewModel.onEditClicked()
            assertTrue(viewModel.state.value.isEditing)
        }

        @Test
        @DisplayName("onCancelEdit reverts changes and sets isEditing to false")
        fun onCancelEdit() {
            viewModel.onEditClicked()
            viewModel.onTitleChanged("New Title")
            viewModel.onParticipantInputChanged("Charlie")
            
            viewModel.onCancelEdit()
            
            val state = viewModel.state.value
            assertFalse(state.isEditing)
            assertEquals("Test Title", state.editedTitle)
            assertEquals(listOf("Alice", "Bob"), state.editedParticipants)
            assertEquals("", state.participantInput)
        }

        @Test
        @DisplayName("onTitleChanged updates editedTitle")
        fun onTitleChanged() {
            viewModel.onTitleChanged("Updated Title")
            assertEquals("Updated Title", viewModel.state.value.editedTitle)
        }

        @Test
        @DisplayName("onParticipantInputChanged updates participantInput")
        fun onParticipantInputChanged() {
            viewModel.onParticipantInputChanged("Charlie")
            assertEquals("Charlie", viewModel.state.value.participantInput)
        }
    }

    @Nested
    @DisplayName("Participant Management Tests")
    inner class ParticipantManagementTests {
        
        @BeforeEach
        fun setupViewModel() = runTest {
            whenever(summaryRepository.getSummaryById(42L)).thenReturn(dummySummary)
            createViewModel()
        }

        @Test
        @DisplayName("onAddParticipant adds name and clears input")
        fun onAddParticipant() {
            viewModel.onParticipantInputChanged("Charlie")
            viewModel.onAddParticipant()
            
            val state = viewModel.state.value
            assertTrue(state.editedParticipants.contains("Charlie"))
            assertEquals("", state.participantInput)
        }

        @Test
        @DisplayName("onAddParticipant with blank input does nothing")
        fun onAddParticipantBlank() {
            val initialParticipants = viewModel.state.value.editedParticipants
            viewModel.onParticipantInputChanged("   ")
            viewModel.onAddParticipant()
            
            assertEquals(initialParticipants, viewModel.state.value.editedParticipants)
            assertEquals("   ", viewModel.state.value.participantInput)
        }

        @Test
        @DisplayName("onAddParticipant with duplicate clears input but does not add")
        fun onAddParticipantDuplicate() {
            viewModel.onParticipantInputChanged("Alice")
            viewModel.onAddParticipant()
            
            val state = viewModel.state.value
            assertEquals(listOf("Alice", "Bob"), state.editedParticipants)
            assertEquals("", state.participantInput)
        }

        @Test
        @DisplayName("onRemoveParticipant removes from editedParticipants")
        fun onRemoveParticipant() {
            viewModel.onRemoveParticipant("Alice")
            assertEquals(listOf("Bob"), viewModel.state.value.editedParticipants)
        }
    }

    @Nested
    @DisplayName("Saving Edits Tests")
    inner class SavingEditsTests {
        
        @BeforeEach
        fun setupViewModel() = runTest {
            whenever(summaryRepository.getSummaryById(42L)).thenReturn(dummySummary)
            createViewModel()
        }

        @Test
        @DisplayName("onSaveEdits success updates summary and emits SavedSuccessfully")
        fun onSaveEditsSuccess() = runTest {
            viewModel.onTitleChanged("Saved Title")
            
            viewModel.effect.test {
                viewModel.onSaveEdits()
                
                verify(updateSummaryUseCase).invoke(any())
                val state = viewModel.state.value
                assertFalse(state.isEditing)
                assertFalse(state.isSaving)
                assertEquals("Saved Title", state.summary?.title)
                
                assertEquals(SummaryDetailEffect.SavedSuccessfully, awaitItem())
            }
        }

        @Test
        @DisplayName("onSaveEdits with blank title uses 'Untitled Conversation'")
        fun onSaveEditsBlankTitle() = runTest {
            viewModel.onTitleChanged("   ")
            viewModel.onSaveEdits()
            
            val state = viewModel.state.value
            assertEquals("Untitled Conversation", state.summary?.title)
        }

        @Test
        @DisplayName("onSaveEdits failure emits ShowError")
        fun onSaveEditsFailure() = runTest {
            whenever(updateSummaryUseCase.invoke(any())).doThrow(RuntimeException("Network Error"))
            
            viewModel.effect.test {
                viewModel.onSaveEdits()
                
                val state = viewModel.state.value
                assertFalse(state.isSaving)
                
                val effect = awaitItem()
                assertTrue(effect is SummaryDetailEffect.ShowError)
                assertEquals("Network Error", (effect as SummaryDetailEffect.ShowError).message)
            }
        }
        
        @Test
        @DisplayName("onSaveEdits when summary is null does nothing")
        fun onSaveEditsNullSummary() = runTest {
            whenever(summaryRepository.getSummaryById(99L)).thenReturn(null)
            createViewModel(99L)
            
            viewModel.onSaveEdits()
            verifyNoInteractions(updateSummaryUseCase)
        }
    }

    @Nested
    @DisplayName("Deletion Tests")
    inner class DeletionTests {
        
        @BeforeEach
        fun setupViewModel() = runTest {
            whenever(summaryRepository.getSummaryById(42L)).thenReturn(dummySummary)
            createViewModel()
        }

        @Test
        @DisplayName("onDeleteClicked sets showDeleteConfirmation to true")
        fun onDeleteClicked() {
            viewModel.onDeleteClicked()
            assertTrue(viewModel.state.value.showDeleteConfirmation)
        }

        @Test
        @DisplayName("onDismissDeleteConfirmation sets showDeleteConfirmation to false")
        fun onDismissDeleteConfirmation() {
            viewModel.onDeleteClicked()
            viewModel.onDismissDeleteConfirmation()
            assertFalse(viewModel.state.value.showDeleteConfirmation)
        }

        @Test
        @DisplayName("onConfirmDelete calls deleteSummaryUseCase and emits NavigateBack")
        fun onConfirmDeleteSuccess() = runTest {
            viewModel.effect.test {
                viewModel.onConfirmDelete()
                verify(deleteSummaryUseCase).invoke(42L)
                assertEquals(SummaryDetailEffect.NavigateBack, awaitItem())
            }
        }

        @Test
        @DisplayName("onConfirmDelete failure emits ShowError")
        fun onConfirmDeleteFailure() = runTest {
            whenever(deleteSummaryUseCase.invoke(42L)).doThrow(RuntimeException("Delete Error"))
            
            viewModel.effect.test {
                viewModel.onConfirmDelete()
                val effect = awaitItem()
                assertTrue(effect is SummaryDetailEffect.ShowError)
                assertEquals("Delete Error", (effect as SummaryDetailEffect.ShowError).message)
            }
        }
    }

    @Nested
    @DisplayName("Navigation Tests")
    inner class NavigationTests {
        
        @BeforeEach
        fun setupViewModel() = runTest {
            whenever(summaryRepository.getSummaryById(42L)).thenReturn(dummySummary)
            createViewModel()
        }

        @Test
        @DisplayName("onBack emits NavigateBack")
        fun onBack() = runTest {
            viewModel.effect.test {
                viewModel.onBack()
                assertEquals(SummaryDetailEffect.NavigateBack, awaitItem())
            }
        }
    }
}
