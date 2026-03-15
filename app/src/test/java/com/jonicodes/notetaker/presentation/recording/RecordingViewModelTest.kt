package com.jonicodes.notetaker.presentation.recording

import app.cash.turbine.test
import com.jonicodes.notetaker.domain.usecase.StartRecordingUseCase
import com.jonicodes.notetaker.domain.usecase.StopRecordingUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
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
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class RecordingViewModelTest {

    private lateinit var startRecordingUseCase: StartRecordingUseCase
    private lateinit var stopRecordingUseCase: StopRecordingUseCase
    private lateinit var viewModel: RecordingViewModel

    private lateinit var transcriptStream: MutableSharedFlow<String>
    private lateinit var errorStream: MutableSharedFlow<String>
    private lateinit var isListening: MutableSharedFlow<Boolean>

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        startRecordingUseCase = mock()
        stopRecordingUseCase = mock()

        transcriptStream = MutableSharedFlow()
        errorStream = MutableSharedFlow()
        isListening = MutableSharedFlow()

        whenever(startRecordingUseCase.transcriptStream).thenReturn(transcriptStream)
        whenever(startRecordingUseCase.errorStream).thenReturn(errorStream)
        whenever(startRecordingUseCase.isListening).thenReturn(isListening)

        viewModel = RecordingViewModel(startRecordingUseCase, stopRecordingUseCase)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Nested
    @DisplayName("Initial State")
    inner class InitialStateTests {
        @Test
        @DisplayName("Initial state should be correct")
        fun checkInitialState() {
            val state = viewModel.state.value
            assertFalse(state.isRecording)
            assertFalse(state.isPreparing)
            assertEquals("", state.liveTranscript)
            assertEquals("", state.partialText)
            assertTrue(state.participants.isEmpty())
            assertEquals("", state.participantInput)
            assertFalse(state.hasPermission)
            assertEquals(0, state.elapsedSeconds)
            assertNull(state.error)
        }
    }

    @Nested
    @DisplayName("Permissions")
    inner class PermissionTests {
        @Test
        @DisplayName("onPermissionResult(true) sets hasPermission=true")
        fun grantPermission() {
            viewModel.onPermissionResult(true)
            assertTrue(viewModel.state.value.hasPermission)
            assertNull(viewModel.state.value.error)
        }

        @Test
        @DisplayName("onPermissionResult(false) sets hasPermission=false and sets error")
        fun denyPermission() {
            viewModel.onPermissionResult(false)
            assertFalse(viewModel.state.value.hasPermission)
            assertEquals("Microphone permission is required to record conversations", viewModel.state.value.error)
        }
    }

    @Nested
    @DisplayName("Start Recording")
    inner class StartRecordingTests {
        @Test
        @DisplayName("onStartRecording without permission emits RequestPermission effect")
        fun startWithoutPermission() = runTest {
            viewModel.effect.test {
                viewModel.onStartRecording()
                assertEquals(RecordingEffect.RequestPermission, awaitItem())
                expectNoEvents()
            }
        }

        @Test
        @DisplayName("onStartRecording with permission sets isRecording=true and isPreparing=true")
        fun startWithPermission() {
            viewModel.onPermissionResult(true)
            viewModel.onStartRecording()
            
            val state = viewModel.state.value
            assertTrue(state.isRecording)
            assertEquals("", state.liveTranscript)
            assertEquals("", state.partialText)
            assertEquals(0, state.elapsedSeconds)
            assertNull(state.error)
        }
    }

    @Nested
    @DisplayName("Stop Recording")
    inner class StopRecordingTests {
        @Test
        @DisplayName("onStopRecording with blank transcript emits ShowError effect")
        fun stopWithBlankTranscript() = runTest {
            whenever(stopRecordingUseCase.invoke()).thenReturn("   ")
            
            viewModel.effect.test {
                viewModel.onStopRecording()
                val effect = awaitItem() as RecordingEffect.ShowError
                assertEquals("No speech was detected. Please try again.", effect.message)
                
                val state = viewModel.state.value
                assertFalse(state.isRecording)
                assertFalse(state.isPreparing)
                assertEquals("", state.liveTranscript)
                assertEquals(0, state.elapsedSeconds)
            }
        }

        @Test
        @DisplayName("onStopRecording with valid transcript emits NavigateToSummary effect")
        fun stopWithValidTranscript() = runTest {
            val transcript = "Hello world"
            viewModel.onParticipantInputChanged("Alice")
            viewModel.onAddParticipant()
            whenever(stopRecordingUseCase.invoke()).thenReturn(transcript)
            
            viewModel.effect.test {
                viewModel.onStopRecording()
                val effect = awaitItem() as RecordingEffect.NavigateToSummary
                assertEquals(transcript, effect.transcript)
                assertEquals(listOf("Alice"), effect.participants)
            }
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
        @DisplayName("onAddParticipant adds name and clears input")
        fun addParticipant() {
            viewModel.onParticipantInputChanged("Bob")
            viewModel.onAddParticipant()
            
            assertTrue(viewModel.state.value.participants.contains("Bob"))
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
        @DisplayName("onRemoveParticipant removes name")
        fun removeParticipant() {
            viewModel.onParticipantInputChanged("Bob")
            viewModel.onAddParticipant()
            viewModel.onRemoveParticipant("Bob")
            
            assertTrue(viewModel.state.value.participants.isEmpty())
        }
    }

    @Nested
    @DisplayName("Error & Session Management")
    inner class MiscTests {
        @Test
        @DisplayName("onDismissError clears error")
        fun dismissError() {
            viewModel.onPermissionResult(false)
            assertNotNull(viewModel.state.value.error)
            viewModel.onDismissError()
            assertNull(viewModel.state.value.error)
        }

        @Test
        @DisplayName("resetSession clears state but preserves hasPermission")
        fun resetSession() {
            viewModel.onPermissionResult(true)
            viewModel.onParticipantInputChanged("Alice")
            viewModel.onAddParticipant()
            
            viewModel.resetSession()
            
            val state = viewModel.state.value
            assertFalse(state.isRecording)
            assertTrue(state.participants.isEmpty())
            assertEquals("", state.participantInput)
            assertNull(state.error)
            assertTrue(state.hasPermission)
        }
    }
}
