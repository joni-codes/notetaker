package com.jonicodes.notetaker.presentation.paste

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PasteViewModelTest {

    private lateinit var viewModel: PasteViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        viewModel = PasteViewModel()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Nested
    @DisplayName("Initial State")
    inner class InitialStateTests {
        @Test
        @DisplayName("Initial state should have empty fields and no error")
        fun checkInitialState() {
            val state = viewModel.state.value
            assertEquals("", state.transcript)
            assertTrue(state.participants.isEmpty())
            assertEquals("", state.participantInput)
            assertNull(state.error)
        }
    }

    @Nested
    @DisplayName("Transcript Input")
    inner class TranscriptTests {
        @Test
        @DisplayName("onTranscriptChanged updates transcript")
        fun updateTranscript() {
            viewModel.onTranscriptChanged("Hello world transcript")
            assertEquals("Hello world transcript", viewModel.state.value.transcript)
        }

        @Test
        @DisplayName("onTranscriptChanged clears existing error")
        fun transcriptChangeClearsError() {
            viewModel.onSubmit()

            assertNotNull(viewModel.state.value.error)

            viewModel.onTranscriptChanged("Some text")
            assertNull(viewModel.state.value.error)
        }
    }

    @Nested
    @DisplayName("Participants Management")
    inner class ParticipantTests {
        @Test
        @DisplayName("onParticipantInputChanged updates input")
        fun updateInput() {
            viewModel.onParticipantInputChanged("Bob")
            assertEquals("Bob", viewModel.state.value.participantInput)
        }

        @Test
        @DisplayName("onAddParticipant adds trimmed name and clears input")
        fun addParticipant() {
            viewModel.onParticipantInputChanged("  Bob  ")
            viewModel.onAddParticipant()

            assertEquals(listOf("Bob"), viewModel.state.value.participants)
            assertEquals("", viewModel.state.value.participantInput)
        }

        @Test
        @DisplayName("onAddParticipant with blank input does nothing")
        fun addBlankParticipant() {
            viewModel.onParticipantInputChanged("   ")
            viewModel.onAddParticipant()
            assertTrue(viewModel.state.value.participants.isEmpty())
        }

        @Test
        @DisplayName("onAddParticipant with duplicate name does nothing")
        fun addDuplicateParticipant() {
            viewModel.onParticipantInputChanged("Bob")
            viewModel.onAddParticipant()
            viewModel.onParticipantInputChanged("Bob")
            viewModel.onAddParticipant()

            assertEquals(1, viewModel.state.value.participants.size)
            assertEquals("Bob", viewModel.state.value.participants[0])
        }

        @Test
        @DisplayName("onAddParticipant with multiple participants maintains order")
        fun addMultipleParticipants() {
            viewModel.onParticipantInputChanged("Alice")
            viewModel.onAddParticipant()
            viewModel.onParticipantInputChanged("Bob")
            viewModel.onAddParticipant()
            viewModel.onParticipantInputChanged("Charlie")
            viewModel.onAddParticipant()

            assertEquals(listOf("Alice", "Bob", "Charlie"), viewModel.state.value.participants)
        }

        @Test
        @DisplayName("onRemoveParticipant removes name")
        fun removeParticipant() {
            viewModel.onParticipantInputChanged("Bob")
            viewModel.onAddParticipant()
            viewModel.onRemoveParticipant("Bob")

            assertTrue(viewModel.state.value.participants.isEmpty())
        }

        @Test
        @DisplayName("onRemoveParticipant with non-existent name does nothing")
        fun removeNonExistentParticipant() {
            viewModel.onParticipantInputChanged("Bob")
            viewModel.onAddParticipant()
            viewModel.onRemoveParticipant("Alice")

            assertEquals(listOf("Bob"), viewModel.state.value.participants)
        }
    }

    @Nested
    @DisplayName("Submit")
    inner class SubmitTests {
        @Test
        @DisplayName("onSubmit with blank transcript sets error")
        fun submitBlankTranscript() {
            viewModel.onTranscriptChanged("   ")
            viewModel.onSubmit()
            assertEquals("Please enter or paste a transcript first", viewModel.state.value.error)
        }

        @Test
        @DisplayName("onSubmit with empty transcript sets error")
        fun submitEmptyTranscript() {
            viewModel.onSubmit()
            assertEquals("Please enter or paste a transcript first", viewModel.state.value.error)
        }

        @Test
        @DisplayName("onSubmit with valid transcript emits NavigateToSummary effect")
        fun submitValidTranscript() = runTest {
            viewModel.onTranscriptChanged("Alice said hello. Bob replied goodbye.")
            viewModel.onParticipantInputChanged("Alice")
            viewModel.onAddParticipant()

            viewModel.effect.test {
                viewModel.onSubmit()
                val effect = awaitItem() as PasteEffect.NavigateToSummary
                assertEquals("Alice said hello. Bob replied goodbye.", effect.transcript)
                assertEquals(listOf("Alice"), effect.participants)
            }
        }

        @Test
        @DisplayName("onSubmit with valid transcript and no participants emits NavigateToSummary with empty list")
        fun submitValidTranscriptNoParticipants() = runTest {
            viewModel.onTranscriptChanged("Some conversation transcript")

            viewModel.effect.test {
                viewModel.onSubmit()
                val effect = awaitItem() as PasteEffect.NavigateToSummary
                assertEquals("Some conversation transcript", effect.transcript)
                assertTrue(effect.participants.isEmpty())
            }
        }

        @Test
        @DisplayName("onSubmit trims transcript before emitting")
        fun submitTrimsTranscript() = runTest {
            viewModel.onTranscriptChanged("  Hello world  ")

            viewModel.effect.test {
                viewModel.onSubmit()
                val effect = awaitItem() as PasteEffect.NavigateToSummary
                assertEquals("Hello world", effect.transcript)
            }
        }
    }

    @Nested
    @DisplayName("Error & Session Management")
    inner class MiscTests {
        @Test
        @DisplayName("onDismissError clears error")
        fun dismissError() {
            viewModel.onSubmit()
            assertNotNull(viewModel.state.value.error)
            viewModel.onDismissError()
            assertNull(viewModel.state.value.error)
        }

        @Test
        @DisplayName("resetSession clears all state to defaults")
        fun resetSession() {
            viewModel.onTranscriptChanged("Some text")
            viewModel.onParticipantInputChanged("Alice")
            viewModel.onAddParticipant()
            viewModel.onParticipantInputChanged("Bob")

            viewModel.resetSession()

            val state = viewModel.state.value
            assertEquals("", state.transcript)
            assertTrue(state.participants.isEmpty())
            assertEquals("", state.participantInput)
            assertNull(state.error)
        }

        @Test
        @DisplayName("resetSession clears error state")
        fun resetSessionClearsError() {
            viewModel.onSubmit()
            assertNotNull(viewModel.state.value.error)

            viewModel.resetSession()
            assertNull(viewModel.state.value.error)
        }
    }
}
