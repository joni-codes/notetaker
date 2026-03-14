package com.jonicodes.notetaker.di

import com.jonicodes.notetaker.data.repository.SpeechRepositoryImpl
import com.jonicodes.notetaker.data.repository.SummarizationRepositoryImpl
import com.jonicodes.notetaker.data.repository.SummaryRepositoryImpl
import com.jonicodes.notetaker.domain.repository.SpeechRepository
import com.jonicodes.notetaker.domain.repository.SummarizationRepository
import com.jonicodes.notetaker.domain.repository.SummaryRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindSummaryRepository(impl: SummaryRepositoryImpl): SummaryRepository

    @Binds
    @Singleton
    abstract fun bindSpeechRepository(impl: SpeechRepositoryImpl): SpeechRepository

    @Binds
    @Singleton
    abstract fun bindSummarizationRepository(impl: SummarizationRepositoryImpl): SummarizationRepository
}
