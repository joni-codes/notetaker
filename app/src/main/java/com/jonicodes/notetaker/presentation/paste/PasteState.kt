package com.jonicodes.notetaker.presentation.paste

data class PasteState(
    val transcript: String = "",
    val participants: List<String> = emptyList(),
    val participantInput: String = "",
    val error: String? = null,
)

sealed class PasteEffect {
    data class NavigateToSummary(val transcript: String, val participants: List<String>) : PasteEffect()
    data class ShowError(val message: String) : PasteEffect()
}
