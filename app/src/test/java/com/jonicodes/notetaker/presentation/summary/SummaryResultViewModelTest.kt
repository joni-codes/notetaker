package com.jonicodes.notetaker.presentation.summary

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.jonicodes.notetaker.domain.model.NoteSummary
import com.jonicodes.notetaker.domain.repository.SummarizationResult
import com.jonicodes.notetaker.domain.usecase.SaveSummaryUseCase
import com.jonicodes.notetaker.domain.usecase.SummarizeTranscriptUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.net.URLEncoder

@OptIn(ExperimentalCoroutinesApi::class)
class SummaryResultViewModelTest {

    private val summarizeTranscriptUseCase: SummarizeTranscriptUseCase = mock()
    private val saveSummaryUseCase: SaveSummaryUseCase = mock()
    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(
        transcript: String = "Test transcript",
        participants: String = "Alice||Bob"
    ): SummaryResultViewModel {
        val savedStateHandle = SavedStateHandle(
            mapOf(
                "transcript" to URLEncoder.encode(transcript, "UTF-8"),
                "participants" to URLEncoder.encode(participants, "UTF-8")
            )
        )
        return SummaryResultViewModel(
            savedStateHandle,
            summarizeTranscriptUseCase,
            saveSummaryUseCase
        )
    }

    @Nested
    @DisplayName("Initialization and Summarization Tests")
    inner class InitializationTests {

        @Test
        @DisplayName("Init decodes transcript and participants and triggers summarize")
        fun initDecodesAndTriggersSummarize() = runTest {
            whenever(summarizeTranscriptUseCase.invoke(any(), any())).thenReturn(
                SummarizationResult.Success("Title", "Summary")
            )

            val viewModel = createViewModel(transcript = "Hello World", participants = "Alice||Bob")
            
            verify(summarizeTranscriptUseCase).invoke("Hello World", listOf("Alice", "Bob"))
            val state = viewModel.state.value
            assertEquals("Hello World", state.transcript)
            assertEquals(listOf("Alice", "Bob"), state.participants)
            assertEquals("Title", state.title)
            assertEquals("Summary", state.summary)
            assertFalse(state.isLoading)
        }

        @Test
        @DisplayName("Init with blank participants results in empty list")
        fun initWithBlankParticipants() = runTest {
            whenever(summarizeTranscriptUseCase.invoke(any(), any())).thenReturn(
                SummarizationResult.Success("Title", "Summary")
            )

            val viewModel = createViewModel(participants = "")
            
            assertTrue(viewModel.state.value.participants.isEmpty())
            verify(summarizeTranscriptUseCase).invoke("Test transcript", emptyList())
        }

        @Test
        @DisplayName("Summarize success sets title, summary, and isLoading=false")
        fun summarizeSuccess() = runTest {
            whenever(summarizeTranscriptUseCase.invoke(any(), any())).thenReturn(
                SummarizationResult.Success("Great Title", "Great Summary")
            )

            val viewModel = createViewModel()
            
            val state = viewModel.state.value
            assertEquals("Great Title", state.title)
            assertEquals("Great Summary", state.summary)
            assertFalse(state.isLoading)
            assertNull(state.error)
        }

        @Test
        @DisplayName("Summarize error sets error message and isLoading=false")
        fun summarizeError() = runTest {
            whenever(summarizeTranscriptUseCase.invoke(any(), any())).thenReturn(
                SummarizationResult.Error("Failed to summarize")
            )

            val viewModel = createViewModel()
            
            val state = viewModel.state.value
            assertEquals("Failed to summarize", state.error)
            assertFalse(state.isLoading)
        }

        @Test
        @DisplayName("Summarize model unavailable sets flag, error message, and isLoading=false")
        fun summarizeModelUnavailable() = runTest {
            whenever(summarizeTranscriptUseCase.invoke(any(), any())).thenReturn(
                SummarizationResult.ModelUnavailable
            )

            val viewModel = createViewModel()
            
            val state = viewModel.state.value
            assertTrue(state.isModelUnavailable)
            assertEquals("AI model is not available on this device", state.error)
            assertFalse(state.isLoading)
        }

        @Test
        @DisplayName("Summarize downloading keeps isLoading=true")
        fun summarizeDownloading() = runTest {
            whenever(summarizeTranscriptUseCase.invoke(any(), any())).thenReturn(
                SummarizationResult.Downloading(50L)
            )

            val viewModel = createViewModel()
            
            val state = viewModel.state.value
            assertTrue(state.isLoading)
            assertNull(state.error)
        }
    }

    @Nested
    @DisplayName("Action Tests")
    inner class ActionTests {

        @Test
        @DisplayName("onTitleChanged updates title in state")
        fun onTitleChangedUpdatesState() = runTest {
            whenever(summarizeTranscriptUseCase.invoke(any(), any())).thenReturn(
                SummarizationResult.Success("Old Title", "Summary")
            )
            val viewModel = createViewModel()
            
            viewModel.onTitleChanged("New Title")
            
            assertEquals("New Title", viewModel.state.value.title)
        }

        @Test
        @DisplayName("onSave with valid summary creates NoteSummary, calls usecase, emits effect")
        fun onSaveValidSummary() = runTest {
            whenever(summarizeTranscriptUseCase.invoke(any(), any())).thenReturn(
                SummarizationResult.Success("My Title", "My Summary")
            )
            whenever(saveSummaryUseCase.invoke(any())).thenReturn(1L)
            val viewModel = createViewModel(transcript = "Hello", participants = "Alice")
            
            viewModel.effect.test {
                viewModel.onSave()
                
                val captor = argumentCaptor<NoteSummary>()
                verify(saveSummaryUseCase).invoke(captor.capture())
                
                val savedNote = captor.firstValue
                assertEquals("My Title", savedNote.title)
                assertEquals("My Summary", savedNote.summary)
                assertEquals("Hello", savedNote.rawTranscript)
                assertEquals(listOf("Alice"), savedNote.participants)
                assertTrue(savedNote.createdAt > 0)
                
                assertEquals(SummaryResultEffect.SavedSuccessfully, awaitItem())
                expectNoEvents()
            }
        }

        @Test
        @DisplayName("onSave with blank summary returns early doing nothing")
        fun onSaveBlankSummary() = runTest {
            whenever(summarizeTranscriptUseCase.invoke(any(), any())).thenReturn(
                SummarizationResult.Success("Title", "   ")
            )
            val viewModel = createViewModel()
            
            viewModel.onSave()
            
            verifyNoInteractions(saveSummaryUseCase)
            assertFalse(viewModel.state.value.isSaving)
        }

        @Test
        @DisplayName("onSave with blank title uses 'Untitled Conversation'")
        fun onSaveBlankTitle() = runTest {
            whenever(summarizeTranscriptUseCase.invoke(any(), any())).thenReturn(
                SummarizationResult.Success("   ", "Valid Summary")
            )
            whenever(saveSummaryUseCase.invoke(any())).thenReturn(1L)
            val viewModel = createViewModel()
            
            viewModel.onSave()
            
            val captor = argumentCaptor<NoteSummary>()
            verify(saveSummaryUseCase).invoke(captor.capture())
            
            assertEquals("Untitled Conversation", captor.firstValue.title)
        }

        @Test
        @DisplayName("onSave failure sets isSaving=false and emits ShowError")
        fun onSaveFailure() = runTest {
            whenever(summarizeTranscriptUseCase.invoke(any(), any())).thenReturn(
                SummarizationResult.Success("Title", "Summary")
            )
            whenever(saveSummaryUseCase.invoke(any())).thenThrow(RuntimeException("Database full"))
            val viewModel = createViewModel()
            
            viewModel.effect.test {
                viewModel.onSave()
                
                assertFalse(viewModel.state.value.isSaving)
                assertEquals(SummaryResultEffect.ShowError("Database full"), awaitItem())
                expectNoEvents()
            }
        }

        @Test
        @DisplayName("onRetry re-triggers summarization")
        fun onRetryReTriggersSummarize() = runTest {
            whenever(summarizeTranscriptUseCase.invoke(any(), any())).thenReturn(
                SummarizationResult.Error("Error 1"),
                SummarizationResult.Success("Retry Title", "Retry Summary")
            )
            val viewModel = createViewModel("Transcript", "P1")
            
            verify(summarizeTranscriptUseCase, times(1)).invoke("Transcript", listOf("P1"))
            
            viewModel.onRetry()
            
            verify(summarizeTranscriptUseCase, times(2)).invoke("Transcript", listOf("P1"))
            assertEquals("Retry Title", viewModel.state.value.title)
            assertEquals("Retry Summary", viewModel.state.value.summary)
        }

        @Test
        @DisplayName("onDiscard emits NavigateBack")
        fun onDiscardEmitsNavigateBack() = runTest {
            whenever(summarizeTranscriptUseCase.invoke(any(), any())).thenReturn(
                SummarizationResult.Success("T", "S")
            )
            val viewModel = createViewModel()
            
            viewModel.effect.test {
                viewModel.onDiscard()
                
                assertEquals(SummaryResultEffect.NavigateBack, awaitItem())
                expectNoEvents()
            }
        }
    }
}
