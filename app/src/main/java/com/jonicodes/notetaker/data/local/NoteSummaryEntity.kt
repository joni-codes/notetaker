package com.jonicodes.notetaker.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "note_summaries")
data class NoteSummaryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val summary: String,
    val rawTranscript: String,
    val participants: String,
    val createdAt: Long,
)
