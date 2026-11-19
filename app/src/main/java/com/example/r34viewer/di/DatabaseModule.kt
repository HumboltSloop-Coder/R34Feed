package com.example.r34viewer.di

import android.content.Context
import androidx.room.Room
import com.example.r34viewer.data.local.db.AppDatabase
import com.example.r34viewer.data.local.db.FavoritesDao
import com.example.r34viewer.data.local.db.SearchHistoryDao
import com.example.r34viewer.data.local.db.TagTypeDao
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
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "r34viewer.db")
            .addMigrations(
                AppDatabase.MIGRATION_1_2,
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4,
                AppDatabase.MIGRATION_4_5
            )
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideFavoritesDao(db: AppDatabase): FavoritesDao = db.favoritesDao()

    @Provides
    fun provideSearchHistoryDao(db: AppDatabase): SearchHistoryDao = db.searchHistoryDao()

    @Provides
    fun provideTagTypeDao(db: AppDatabase): TagTypeDao = db.tagTypeDao()
}
