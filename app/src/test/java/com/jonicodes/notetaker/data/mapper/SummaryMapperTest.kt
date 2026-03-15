package com.jonicodes.notetaker.data.mapper

import com.jonicodes.notetaker.data.local.NoteSummaryEntity
import com.jonicodes.notetaker.domain.model.NoteSummary
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class SummaryMapperTest {

    @Nested
    @DisplayName("toDomain")
    inner class ToDomain {

        @Test
        fun `maps all fields correctly`() {
            val entity = NoteSummaryEntity(
                id = 42,
                title = "Morning standup",
                summary = "Discussed sprint goals",
                rawTranscript = "We need to finish the auth module...",
                participants = "Alice||Bob||Charlie",
                createdAt = 1700000000L,
            )

            val result = entity.toDomain()

            assertEquals(42L, result.id)
            assertEquals("Morning standup", result.title)
            assertEquals("Discussed sprint goals", result.summary)
            assertEquals("We need to finish the auth module...", result.rawTranscript)
            assertEquals(listOf("Alice", "Bob", "Charlie"), result.participants)
            assertEquals(1700000000L, result.createdAt)
        }

        @Test
        fun `blank participants string produces empty list`() {
            val entity = NoteSummaryEntity(
                id = 1,
                title = "Solo note",
                summary = "summary",
                rawTranscript = "transcript",
                participants = "",
                createdAt = 1700000000L,
            )

            val result = entity.toDomain()

            assertTrue(result.participants.isEmpty())
        }

        @Test
        fun `single participant without delimiter`() {
            val entity = NoteSummaryEntity(
                id = 1,
                title = "title",
                summary = "summary",
                rawTranscript = "transcript",
                participants = "Alice",
                createdAt = 1700000000L,
            )

            val result = entity.toDomain()

            assertEquals(listOf("Alice"), result.participants)
        }

        @Test
        fun `filters blank entries from split`() {
            val entity = NoteSummaryEntity(
                id = 1,
                title = "title",
                summary = "summary",
                rawTranscript = "transcript",
                participants = "Alice||||Bob",
                createdAt = 1700000000L,
            )

            val result = entity.toDomain()

            assertEquals(listOf("Alice", "Bob"), result.participants)
        }
    }

    @Nested
    @DisplayName("toEntity")
    inner class ToEntity {

        @Test
        fun `maps all fields correctly`() {
            val domain = NoteSummary(
                id = 7,
                title = "Project review",
                summary = "Reviewed milestones",
                rawTranscript = "Let's look at the timeline...",
                participants = listOf("Dana", "Eve"),
                createdAt = 1700000000L,
            )

            val result = domain.toEntity()

            assertEquals(7L, result.id)
            assertEquals("Project review", result.title)
            assertEquals("Reviewed milestones", result.summary)
            assertEquals("Let's look at the timeline...", result.rawTranscript)
            assertEquals("Dana||Eve", result.participants)
            assertEquals(1700000000L, result.createdAt)
        }

        @Test
        fun `empty participants list produces empty string`() {
            val domain = NoteSummary(
                id = 1,
                title = "title",
                summary = "summary",
                rawTranscript = "transcript",
                participants = emptyList(),
                createdAt = 1700000000L,
            )

            val result = domain.toEntity()

            assertEquals("", result.participants)
        }

        @Test
        fun `single participant has no delimiter`() {
            val domain = NoteSummary(
                id = 1,
                title = "title",
                summary = "summary",
                rawTranscript = "transcript",
                participants = listOf("Alice"),
                createdAt = 1700000000L,
            )

            val result = domain.toEntity()

            assertEquals("Alice", result.participants)
        }
    }

    @Nested
    @DisplayName("roundtrip")
    inner class Roundtrip {

        @Test
        fun `domain to entity and back preserves data`() {
            val original = NoteSummary(
                id = 99,
                title = "Roundtrip test",
                summary = "Testing mapper roundtrip",
                rawTranscript = "Full transcript here",
                participants = listOf("X", "Y", "Z"),
                createdAt = 1700000000L,
            )

            val result = original.toEntity().toDomain()

            assertEquals(original, result)
        }

        @Test
        fun `roundtrip with empty participants`() {
            val original = NoteSummary(
                id = 1,
                title = "title",
                summary = "summary",
                rawTranscript = "transcript",
                participants = emptyList(),
                createdAt = 1700000000L,
            )

            val result = original.toEntity().toDomain()

            assertEquals(original, result)
        }
    }
}
