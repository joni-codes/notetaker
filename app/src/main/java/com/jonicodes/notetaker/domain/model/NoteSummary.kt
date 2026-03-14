package com.jonicodes.notetaker.domain.model

data class NoteSummary(
    val id: Long = 0,
    val title: String,
    val summary: String,
    val rawTranscript: String,
    val participants: List<String>,
    val createdAt: Long,
)
