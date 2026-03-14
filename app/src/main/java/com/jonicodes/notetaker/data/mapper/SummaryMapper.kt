package com.jonicodes.notetaker.data.mapper

import com.jonicodes.notetaker.data.local.NoteSummaryEntity
import com.jonicodes.notetaker.domain.model.NoteSummary

fun NoteSummaryEntity.toDomain(): NoteSummary {
    return NoteSummary(
        id = id,
        title = title,
        summary = summary,
        rawTranscript = rawTranscript,
        participants = if (participants.isBlank()) emptyList()
        else participants.split("||").filter { it.isNotBlank() },
        createdAt = createdAt,
    )
}

fun NoteSummary.toEntity(): NoteSummaryEntity {
    return NoteSummaryEntity(
        id = id,
        title = title,
        summary = summary,
        rawTranscript = rawTranscript,
        participants = participants.joinToString("||"),
        createdAt = createdAt,
    )
}
