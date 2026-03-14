package com.jonicodes.notetaker.di

import android.content.Context
import androidx.room.Room
import com.jonicodes.notetaker.data.local.AppDatabase
import com.jonicodes.notetaker.data.local.NoteSummaryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "notetaker_db"
        ).build()
    }

    @Provides
    @Singleton
    fun provideNoteSummaryDao(database: AppDatabase): NoteSummaryDao {
        return database.noteSummaryDao()
    }
}
