package com.jonicodes.notetaker.domain.repository

import kotlinx.coroutines.flow.Flow

interface SpeechRepository {
    val transcriptStream: Flow<String>
    val isListening: Flow<Boolean>
    val errorStream: Flow<String>
    suspend fun startListening()
    suspend fun stopListening(): String
}
