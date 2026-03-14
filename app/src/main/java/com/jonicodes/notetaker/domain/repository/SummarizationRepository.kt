package com.jonicodes.notetaker.domain.repository

sealed class SummarizationResult {
    data class Success(val title: String, val summary: String) : SummarizationResult()
    data class Error(val message: String) : SummarizationResult()
    data object ModelUnavailable : SummarizationResult()
    data class Downloading(val progress: Long) : SummarizationResult()
}

interface SummarizationRepository {
    suspend fun checkAvailability(): Boolean
    suspend fun downloadModelIfNeeded(): Boolean
    suspend fun summarize(transcript: String, participants: List<String>): SummarizationResult
}
