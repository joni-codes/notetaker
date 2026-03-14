package com.jonicodes.notetaker.data.source

import android.content.Context
import com.google.mlkit.genai.common.DownloadCallback
import com.google.mlkit.genai.common.GenAiException
import com.google.mlkit.genai.summarization.Summarization
import com.google.mlkit.genai.summarization.SummarizationRequest
import com.google.mlkit.genai.summarization.Summarizer
import com.google.mlkit.genai.summarization.SummarizerOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class SummarizationDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var summarizer: Summarizer? = null

    private fun getOrCreateSummarizer(): Summarizer {
        return summarizer ?: run {
            val options = SummarizerOptions.builder(context)
                .setInputType(SummarizerOptions.InputType.CONVERSATION)
                .setOutputType(SummarizerOptions.OutputType.THREE_BULLETS)
                .setLanguage(SummarizerOptions.Language.ENGLISH)
                .setLongInputAutoTruncationEnabled(true)
                .build()
            Summarization.getClient(options).also { summarizer = it }
        }
    }

    suspend fun checkAvailability(): Int {
        return getOrCreateSummarizer().checkFeatureStatus().await()
    }

    suspend fun downloadModel(): Boolean {
        return suspendCancellableCoroutine { continuation ->
            getOrCreateSummarizer().downloadFeature(object : DownloadCallback {
                override fun onDownloadStarted(bytesToDownload: Long) {}

                override fun onDownloadProgress(totalBytesDownloaded: Long) {}

                override fun onDownloadCompleted() {
                    if (continuation.isActive) {
                        continuation.resume(true)
                    }
                }

                override fun onDownloadFailed(e: GenAiException) {
                    if (continuation.isActive) {
                        continuation.resume(false)
                    }
                }
            })
        }
    }

    suspend fun summarize(text: String): String {
        val client = getOrCreateSummarizer()
        val request = SummarizationRequest.builder(text).build()
        val result = client.runInference(request).await()
        return result.summary
    }

    fun close() {
        summarizer?.close()
        summarizer = null
    }
}
