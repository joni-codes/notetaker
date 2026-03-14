package com.jonicodes.notetaker.presentation.recording

data class RecordingState(
    val isRecording: Boolean = false,
    val isPreparing: Boolean = false,
    val liveTranscript: String = "",
    val partialText: String = "",
    val participants: List<String> = emptyList(),
    val participantInput: String = "",
    val hasPermission: Boolean = false,
    val audioLevel: Float = 0f,
    val elapsedSeconds: Long = 0,
    val error: String? = null,
)

sealed class RecordingEffect {
    data class NavigateToSummary(val transcript: String, val participants: List<String>) : RecordingEffect()
    data class ShowError(val message: String) : RecordingEffect()
    data object RequestPermission : RecordingEffect()
}
