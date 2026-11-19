package com.example.r34viewer.di

import com.example.r34viewer.data.repository.PostRepositoryImpl
import com.example.r34viewer.data.repository.SearchHistoryRepositoryImpl
import com.example.r34viewer.data.repository.SettingsRepositoryImpl
import com.example.r34viewer.data.repository.TagRepositoryImpl
import com.example.r34viewer.domain.repository.PostRepository
import com.example.r34viewer.domain.repository.SearchHistoryRepository
import com.example.r34viewer.domain.repository.SettingsRepository
import com.example.r34viewer.domain.repository.TagRepository
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
    abstract fun bindPostRepository(impl: PostRepositoryImpl): PostRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindSearchHistoryRepository(impl: SearchHistoryRepositoryImpl): SearchHistoryRepository

    @Binds
    @Singleton
    abstract fun bindTagRepository(impl: TagRepositoryImpl): TagRepository
}
