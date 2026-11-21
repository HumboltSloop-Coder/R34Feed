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
                AppDatabase.MIGRATION_4_5,
                AppDatabase.MIGRATION_5_6,
                AppDatabase.MIGRATION_6_7,
                AppDatabase.MIGRATION_7_8,
                AppDatabase.MIGRATION_8_9
            )
            // Never wipe favorites/folders on an unexpected schema: fail loudly instead so
            // users keep their data and a missing migration can be fixed in an update.
            .build()

    @Provides
    fun provideFavoritesDao(db: AppDatabase): FavoritesDao = db.favoritesDao()

    @Provides
    fun provideSearchHistoryDao(db: AppDatabase): SearchHistoryDao = db.searchHistoryDao()

    @Provides
    fun provideTagTypeDao(db: AppDatabase): TagTypeDao = db.tagTypeDao()
}
