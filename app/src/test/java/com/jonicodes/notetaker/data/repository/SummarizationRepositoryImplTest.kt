package com.jonicodes.notetaker.data.repository

import com.google.mlkit.genai.common.FeatureStatus
import com.jonicodes.notetaker.data.source.SummarizationDataSource
import com.jonicodes.notetaker.domain.repository.SummarizationResult
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*

class SummarizationRepositoryImplTest {

    private lateinit var dataSource: SummarizationDataSource
    private lateinit var repository: SummarizationRepositoryImpl

    @BeforeEach
    fun setup() {
        dataSource = mock()
        repository = SummarizationRepositoryImpl(dataSource)
    }

    @Nested
    @DisplayName("extractTitle() tests")
    inner class ExtractTitleTests {

        @Test
        @DisplayName("Extracts title from bullet-prefixed summaries")
        fun extractTitle_withBulletPrefixes() {
            val transcript = "Dummy transcript"
            assertEquals("Summary point", repository.extractTitle("• Summary point", transcript))
            assertEquals("Summary point", repository.extractTitle("- Summary point", transcript))
            assertEquals("Summary point", repository.extractTitle("* Summary point", transcript))
            assertEquals("Summary point", repository.extractTitle("1. Summary point", transcript))
            assertEquals("Summary point", repository.extractTitle("2. Summary point", transcript))
            assertEquals("Summary point", repository.extractTitle("3. Summary point", transcript))
        }

        @Test
        @DisplayName("Splits clause on punctuation marks")
        fun extractTitle_splitsOnPunctuation() {
            val transcript = "Dummy transcript"
            assertEquals("First clause", repository.extractTitle("First clause, second clause", transcript))
            assertEquals("First clause", repository.extractTitle("First clause; second clause", transcript))
            assertEquals("First clause", repository.extractTitle("First clause: second clause", transcript))
        }

        @Test
        @DisplayName("Splits clause on dash variants")
        fun extractTitle_splitsOnDashes() {
            val transcript = "Dummy transcript"
            assertEquals("First clause", repository.extractTitle("First clause- second clause", transcript))
            assertEquals("First clause", repository.extractTitle("First clause – second clause", transcript))
            assertEquals("First clause", repository.extractTitle("First clause — second clause", transcript))
        }

        @Test
        @DisplayName("Splits clause on conjunctions 'and', 'that', 'which'")
        fun extractTitle_splitsOnConjunctions() {
            val transcript = "Dummy transcript"
            assertEquals("First clause", repository.extractTitle("First clause and second clause", transcript))
            assertEquals("First clause", repository.extractTitle("First clause that second clause", transcript))
            assertEquals("First clause", repository.extractTitle("First clause which second clause", transcript))
        }

        @Test
        @DisplayName("Caps the extracted title at 8 words")
        fun extractTitle_capsAtEightWords() {
            val summary = "One two three four five six seven eight nine ten"
            assertEquals("One two three four five six seven eight", repository.extractTitle(summary, "Dummy transcript"))
        }

        @Test
        @DisplayName("Caps at 45 chars and truncates with ellipsis")
        fun extractTitle_capsAt45CharsWithEllipsis() {
            // Build a summary whose first 8 words exceed 45 characters so the char-cap kicks in
            val summary = "Extraordinary unprecedented revolutionary groundbreaking monumental transformative spectacular phenomenal"
            val result = repository.extractTitle(summary, "Dummy transcript")
            // The 8-word cap produces >45 chars, so it should be truncated to 42 + "..."
            assertTrue(result.endsWith("..."))
            assertTrue(result.length <= 45)
        }

        @Test
        @DisplayName("Falls back to first 6 words of transcript when summary is empty")
        fun extractTitle_emptySummaryFallsBackToTranscript() {
            val summary = "   \n  "
            val transcript = "One two three four five six seven eight"
            assertEquals("One two three four five six", repository.extractTitle(summary, transcript))
        }

        @Test
        @DisplayName("Returns 'Conversation Summary' when summary and transcript yields blank title")
        fun extractTitle_blankSummaryAndBlankTranscriptReturnsFallback() {
            val summary = "• ,"
            val transcript = ""
            assertEquals("Conversation Summary", repository.extractTitle(summary, transcript))
        }

        @Test
        @DisplayName("Removes trailing periods and commas")
        fun extractTitle_removesTrailingDotAndComma() {
            val transcript = "Dummy transcript"
            assertEquals("Hello world", repository.extractTitle("Hello world.", transcript))
            assertEquals("Hello world", repository.extractTitle("Hello world,", transcript))
        }
    }

    @Nested
    @DisplayName("summarize() tests")
    inner class SummarizeTests {

        @Test
        @DisplayName("Blank transcript returns Error")
        fun summarize_blankTranscriptReturnsError() = runTest {
            val result = repository.summarize("   ", emptyList())
            
            assertTrue(result is SummarizationResult.Error)
            assertEquals("No conversation was recorded", (result as SummarizationResult.Error).message)
            verify(dataSource, never()).checkAvailability()
        }

        @Test
        @DisplayName("UNAVAILABLE status returns ModelUnavailable")
        fun summarize_unavailableReturnsModelUnavailable() = runTest {
            whenever(dataSource.checkAvailability()).thenReturn(FeatureStatus.UNAVAILABLE)
            
            val result = repository.summarize("Valid transcript", emptyList())
            
            assertEquals(SummarizationResult.ModelUnavailable, result)
            verify(dataSource, never()).downloadModel()
        }

        @Test
        @DisplayName("AVAILABLE status calls dataSource and returns Success with correct title")
        fun summarize_availableReturnsSuccess() = runTest {
            val transcript = "This is a test transcript."
            val summaryText = "• This is the summary."
            
            whenever(dataSource.checkAvailability()).thenReturn(FeatureStatus.AVAILABLE)
            whenever(dataSource.summarize(transcript)).thenReturn(summaryText)
            
            val result = repository.summarize(transcript, emptyList())
            
            assertTrue(result is SummarizationResult.Success)
            val success = result as SummarizationResult.Success
            assertEquals("This is the summary", success.title)
            assertEquals(summaryText, success.summary)
            verify(dataSource, never()).downloadModel()
        }

        @Test
        @DisplayName("DOWNLOADABLE status triggers download and then summarizes")
        fun summarize_downloadableTriggersDownloadAndSummarizes() = runTest {
            val transcript = "Valid transcript"
            val summaryText = "• Downloaded summary"
            
            whenever(dataSource.checkAvailability()).thenReturn(FeatureStatus.DOWNLOADABLE)
            whenever(dataSource.downloadModel()).thenReturn(true)
            whenever(dataSource.summarize(transcript)).thenReturn(summaryText)
            
            val result = repository.summarize(transcript, emptyList())
            
            verify(dataSource).downloadModel()
            verify(dataSource).summarize(transcript)
            assertTrue(result is SummarizationResult.Success)
            assertEquals("Downloaded summary", (result as SummarizationResult.Success).title)
        }

        @Test
        @DisplayName("DOWNLOADING status behaves same as DOWNLOADABLE")
        fun summarize_downloadingTriggersDownloadAndSummarizes() = runTest {
            val transcript = "Valid transcript"
            
            whenever(dataSource.checkAvailability()).thenReturn(FeatureStatus.DOWNLOADING)
            whenever(dataSource.downloadModel()).thenReturn(true)
            whenever(dataSource.summarize(transcript)).thenReturn("• Downloading summary")
            
            val result = repository.summarize(transcript, emptyList())
            
            verify(dataSource).downloadModel()
            verify(dataSource).summarize(transcript)
            assertTrue(result is SummarizationResult.Success)
        }

        @Test
        @DisplayName("Failed download returns Error")
        fun summarize_failedDownloadReturnsError() = runTest {
            whenever(dataSource.checkAvailability()).thenReturn(FeatureStatus.DOWNLOADABLE)
            whenever(dataSource.downloadModel()).thenReturn(false)
            
            val result = repository.summarize("Valid transcript", emptyList())
            
            verify(dataSource).downloadModel()
            verify(dataSource, never()).summarize(any())
            assertTrue(result is SummarizationResult.Error)
            assertEquals("Failed to download AI model", (result as SummarizationResult.Error).message)
        }

        @Test
        @DisplayName("Exception thrown returns Error with message")
        fun summarize_exceptionReturnsError() = runTest {
            whenever(dataSource.checkAvailability()).thenReturn(FeatureStatus.AVAILABLE)
            whenever(dataSource.summarize(any())).thenThrow(RuntimeException("Network error"))
            
            val result = repository.summarize("Valid transcript", emptyList())
            
            assertTrue(result is SummarizationResult.Error)
            assertEquals("Network error", (result as SummarizationResult.Error).message)
        }
        
        @Test
        @DisplayName("Exception thrown with null message returns default error string")
        fun summarize_exceptionWithNullMessageReturnsDefaultError() = runTest {
            whenever(dataSource.checkAvailability()).thenReturn(FeatureStatus.AVAILABLE)
            whenever(dataSource.summarize(any())).thenThrow(RuntimeException())
            
            val result = repository.summarize("Valid transcript", emptyList())
            
            assertTrue(result is SummarizationResult.Error)
            assertEquals("Summarization failed", (result as SummarizationResult.Error).message)
        }
    }
}
