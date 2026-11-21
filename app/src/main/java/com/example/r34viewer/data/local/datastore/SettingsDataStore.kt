package com.example.r34viewer.data.local.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.r34viewer.domain.model.AppColors
import com.example.r34viewer.domain.model.FeedImageSize
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val FEED_IMAGE_SIZE = stringPreferencesKey("feed_image_size")
        val HIDE_AI_GENERATED = booleanPreferencesKey("hide_ai_generated")
        val FULLSCREEN_MAX_QUALITY = booleanPreferencesKey("fullscreen_max_quality")
        val CUSTOM_PRIMARY_COLOR = stringPreferencesKey("custom_primary_color")
        val CUSTOM_SECONDARY_COLOR = stringPreferencesKey("custom_secondary_color")
        val CUSTOM_BACKGROUND_COLOR = stringPreferencesKey("custom_background_color")
        val NEGATIVE_TAGS = stringSetPreferencesKey("negative_tags")
    }

    val isDarkThemeFlow: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[Keys.DARK_THEME] ?: true // Dark theme by default (NSFW-friendly)
    }

    val feedImageSizeFlow: Flow<FeedImageSize> = context.settingsDataStore.data.map { prefs ->
        FeedImageSize.fromStoredValue(prefs[Keys.FEED_IMAGE_SIZE])
    }

    val hideAiGeneratedFlow: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[Keys.HIDE_AI_GENERATED] ?: false
    }

    val fullscreenMaxQualityFlow: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[Keys.FULLSCREEN_MAX_QUALITY] ?: false
    }

    val customColorsFlow: Flow<AppColors> = context.settingsDataStore.data.map { prefs ->
        AppColors(
            primary = prefs[Keys.CUSTOM_PRIMARY_COLOR],
            secondary = prefs[Keys.CUSTOM_SECONDARY_COLOR],
            background = prefs[Keys.CUSTOM_BACKGROUND_COLOR]
        )
    }

    val negativeTagsFlow: Flow<List<String>> = context.settingsDataStore.data.map { prefs ->
        (prefs[Keys.NEGATIVE_TAGS] ?: emptySet()).toList().sorted()
    }

    suspend fun setDarkTheme(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.DARK_THEME] = enabled
        }
    }

    suspend fun setFeedImageSize(size: FeedImageSize) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.FEED_IMAGE_SIZE] = size.name
        }
    }

    suspend fun setHideAiGenerated(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.HIDE_AI_GENERATED] = enabled
        }
    }

    suspend fun setFullscreenMaxQuality(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.FULLSCREEN_MAX_QUALITY] = enabled
        }
    }

    suspend fun setCustomColors(colors: AppColors) {
        context.settingsDataStore.edit { prefs ->
            setOrRemove(prefs, Keys.CUSTOM_PRIMARY_COLOR, colors.primary)
            setOrRemove(prefs, Keys.CUSTOM_SECONDARY_COLOR, colors.secondary)
            setOrRemove(prefs, Keys.CUSTOM_BACKGROUND_COLOR, colors.background)
        }
    }

    suspend fun addNegativeTag(tag: String) {
        context.settingsDataStore.edit { prefs ->
            val current = prefs[Keys.NEGATIVE_TAGS] ?: emptySet()
            prefs[Keys.NEGATIVE_TAGS] = current + tag
        }
    }

    suspend fun removeNegativeTag(tag: String) {
        context.settingsDataStore.edit { prefs ->
            val current = prefs[Keys.NEGATIVE_TAGS] ?: emptySet()
            prefs[Keys.NEGATIVE_TAGS] = current - tag
        }
    }

    private fun setOrRemove(
        prefs: MutablePreferences,
        key: Preferences.Key<String>,
        value: String?
    ) {
        if (value.isNullOrBlank()) prefs.remove(key) else prefs[key] = value
    }
}
