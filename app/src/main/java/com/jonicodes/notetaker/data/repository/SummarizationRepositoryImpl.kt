package com.jonicodes.notetaker.data.repository

import com.google.mlkit.genai.common.FeatureStatus
import com.jonicodes.notetaker.data.source.SummarizationDataSource
import com.jonicodes.notetaker.domain.repository.SummarizationRepository
import com.jonicodes.notetaker.domain.repository.SummarizationResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SummarizationRepositoryImpl @Inject constructor(
    private val dataSource: SummarizationDataSource
) : SummarizationRepository {

    override suspend fun checkAvailability(): Boolean {
        return try {
            val status = dataSource.checkAvailability()
            status == FeatureStatus.AVAILABLE || status == FeatureStatus.DOWNLOADABLE
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun downloadModelIfNeeded(): Boolean {
        return try {
            val status = dataSource.checkAvailability()
            when (status) {
                FeatureStatus.AVAILABLE -> true
                FeatureStatus.DOWNLOADABLE -> dataSource.downloadModel()
                else -> false
            }
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun summarize(
        transcript: String,
        participants: List<String>
    ): SummarizationResult {
        if (transcript.isBlank()) {
            return SummarizationResult.Error("No conversation was recorded")
        }

        return try {
            val status = dataSource.checkAvailability()
            if (status == FeatureStatus.UNAVAILABLE) {
                return SummarizationResult.ModelUnavailable
            }
            if (status == FeatureStatus.DOWNLOADABLE || status == FeatureStatus.DOWNLOADING) {
                val downloaded = dataSource.downloadModel()
                if (!downloaded) {
                    return SummarizationResult.Error("Failed to download AI model")
                }
            }

            val participantContext = if (participants.isNotEmpty()) {
                "Participants: ${participants.joinToString(", ")}.\n\n"
            } else {
                ""
            }

            val formattedInput = "${participantContext}${transcript}"
            val summaryText = dataSource.summarize(formattedInput)

            val title = extractTitle(summaryText, transcript)

            SummarizationResult.Success(
                title = title,
                summary = summaryText
            )
        } catch (e: Exception) {
            SummarizationResult.Error(e.message ?: "Summarization failed")
        }
    }

    private fun extractTitle(summary: String, transcript: String): String {
        val bullets = summary.lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .map {
                it.removePrefix("•").removePrefix("-").removePrefix("*")
                    .removePrefix("1.").removePrefix("2.").removePrefix("3.")
                    .trim()
            }
            .filter { it.isNotBlank() }
            .toList()

        if (bullets.isEmpty()) {
            val words = transcript.split(" ").take(6).joinToString(" ")
            return if (words.length > 40) words.take(37) + "..." else words
        }

        // Condense first bullet into a short headline: take first clause, cap at ~8 words
        val firstBullet = bullets.first()

        val clause = firstBullet
            .split(Regex("[,;:\\-–—]|\\band\\b|\\bthat\\b|\\bwhich\\b"))
            .first()
            .trim()

        val words = clause.split(Regex("\\s+"))
        val titleWords = words.take(8).joinToString(" ")
            .removeSuffix(".")
            .removeSuffix(",")
            .trim()

        return if (titleWords.length > 45) {
            titleWords.take(42) + "..."
        } else {
            titleWords.ifBlank { "Conversation Summary" }
        }
    }
}
