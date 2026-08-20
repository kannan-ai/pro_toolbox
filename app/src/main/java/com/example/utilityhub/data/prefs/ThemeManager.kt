package com.example.utilityhub.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class ThemeManager(private val context: Context) {
    companion object {
        val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
        val PINNED_SCREENS_KEY = stringSetPreferencesKey("pinned_screens")
        val APP_LANGUAGE_KEY = stringPreferencesKey("app_language")
        val GALLERY_ACCESS_KEY = booleanPreferencesKey("gallery_access_enabled")
        val INCOGNITO_MODE_KEY = booleanPreferencesKey("incognito_mode")
        val MENU_MODE_KEY = stringPreferencesKey("menu_mode")
        val ACCENT_COLOR_KEY = stringPreferencesKey("accent_color")
        val SEEN_TUTORIALS_KEY = stringSetPreferencesKey("seen_tutorials")
        val SWARA_READY_KEY = booleanPreferencesKey("swara_ready")
        val SWARA_ENABLED_KEY = booleanPreferencesKey("swara_enabled")
        val VOICE_WAKE_KEY = booleanPreferencesKey("voice_wake_enabled")
        val HIDE_COMING_SOON_KEY = booleanPreferencesKey("hide_coming_soon")
        val PLAYER_SEEK_TIME_KEY = androidx.datastore.preferences.core.intPreferencesKey("player_seek_time")
        val SUBTITLE_FONT_SIZE_KEY = androidx.datastore.preferences.core.intPreferencesKey("subtitle_font_size")
        val SUBTITLE_COLOR_KEY = stringPreferencesKey("subtitle_color")
        val SUBTITLE_OPACITY_KEY = androidx.datastore.preferences.core.floatPreferencesKey("subtitle_opacity")
        val SUBTITLE_EDGE_TYPE_KEY = androidx.datastore.preferences.core.intPreferencesKey("subtitle_edge_type")
        val NIGHT_FILTER_ENABLED_KEY = booleanPreferencesKey("night_filter_enabled")
        val NIGHT_FILTER_INTENSITY_KEY = androidx.datastore.preferences.core.floatPreferencesKey("night_filter_intensity")
        val VIVID_MODE_ENABLED_KEY = booleanPreferencesKey("vivid_mode_enabled")
        val OLED_STEALTH_MODE_KEY = booleanPreferencesKey("oled_stealth_mode")
        val VAULT_LOCKED_KEY = booleanPreferencesKey("vault_locked")
        val DEVICE_NAME_KEY = stringPreferencesKey("device_name")
        val UI_PULSE_MODE_KEY = stringPreferencesKey("ui_pulse_mode")
    }

    val isDarkMode: Flow<Boolean?> = context.dataStore.data.map { preferences ->
        preferences[DARK_MODE_KEY]
    }

    val pinnedScreens: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[PINNED_SCREENS_KEY] ?: emptySet()
    }

    val appLanguage: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[APP_LANGUAGE_KEY] ?: "en"
    }

    val isGalleryAccessEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[GALLERY_ACCESS_KEY] ?: true
    }

    val isIncognitoMode: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[INCOGNITO_MODE_KEY] ?: false
    }

    val menuMode: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[MENU_MODE_KEY] ?: "BASIC"
    }

    val accentColor: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[ACCENT_COLOR_KEY] ?: "AMBER" // Default
    }

    val seenTutorials: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[SEEN_TUTORIALS_KEY] ?: emptySet()
    }

    val isSwaraReady: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SWARA_READY_KEY] ?: false
    }

    val isSwaraEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SWARA_ENABLED_KEY] ?: true // Enabled by default
    }

    val isVoiceWakeEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[VOICE_WAKE_KEY] ?: false
    }

    val hideComingSoon: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[HIDE_COMING_SOON_KEY] ?: false
    }

    val playerSeekTime: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[PLAYER_SEEK_TIME_KEY] ?: 10 // Default 10s
    }

    val subtitleFontSize: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[SUBTITLE_FONT_SIZE_KEY] ?: 18
    }

    val subtitleColor: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[SUBTITLE_COLOR_KEY] ?: "White"
    }

    val subtitleOpacity: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[SUBTITLE_OPACITY_KEY] ?: 0.5f
    }

    val subtitleEdgeType: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[SUBTITLE_EDGE_TYPE_KEY] ?: 0 // NONE
    }

    val nightFilterEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[NIGHT_FILTER_ENABLED_KEY] ?: false
    }

    val nightFilterIntensity: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[NIGHT_FILTER_INTENSITY_KEY] ?: 0.15f
    }

    val vividModeEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[VIVID_MODE_ENABLED_KEY] ?: false
    }

    val isOledStealth: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[OLED_STEALTH_MODE_KEY] ?: false
    }

    val isVaultLocked: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[VAULT_LOCKED_KEY] ?: false
    }

    val deviceName: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[DEVICE_NAME_KEY] ?: android.os.Build.MODEL
    }

    val uiPulseMode: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[UI_PULSE_MODE_KEY] ?: "NEUTRAL"
    }

    suspend fun setDarkMode(isDark: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DARK_MODE_KEY] = isDark
        }
    }

    suspend fun setAppLanguage(lang: String) {
        context.dataStore.edit { preferences ->
            preferences[APP_LANGUAGE_KEY] = lang
        }
    }

    suspend fun setGalleryAccessEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[GALLERY_ACCESS_KEY] = enabled
        }
    }

    suspend fun setIncognitoMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[INCOGNITO_MODE_KEY] = enabled
        }
    }

    suspend fun setMenuMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[MENU_MODE_KEY] = mode
        }
    }

    suspend fun setAccentColor(colorName: String) {
        context.dataStore.edit { preferences ->
            preferences[ACCENT_COLOR_KEY] = colorName
        }
    }

    suspend fun togglePin(route: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[PINNED_SCREENS_KEY] ?: emptySet()
            val updated = if (current.contains(route)) {
                current - route
            } else {
                current + route
            }
            preferences[PINNED_SCREENS_KEY] = updated
        }
    }

    suspend fun savePlaybackPosition(uri: String, position: Long) {
        context.dataStore.edit { preferences ->
            preferences[longPreferencesKey("pos_$uri")] = position
        }
    }

    fun getPlaybackPosition(uri: String): Flow<Long> {
        return context.dataStore.data.map { preferences ->
            preferences[longPreferencesKey("pos_$uri")] ?: 0L
        }
    }

    suspend fun clearPlaybackPosition(uri: String) {
        context.dataStore.edit { preferences ->
            preferences.remove(longPreferencesKey("pos_$uri"))
        }
    }

    suspend fun markTutorialSeen(tutorialId: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[SEEN_TUTORIALS_KEY] ?: emptySet()
            preferences[SEEN_TUTORIALS_KEY] = current + tutorialId
        }
    }

    suspend fun setSwaraReady(ready: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SWARA_READY_KEY] = ready
        }
    }

    suspend fun setSwaraEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SWARA_ENABLED_KEY] = enabled
        }
    }

    suspend fun setVoiceWakeEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[VOICE_WAKE_KEY] = enabled
        }
    }

    suspend fun setHideComingSoon(hide: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[HIDE_COMING_SOON_KEY] = hide
        }
    }

    suspend fun setPlayerSeekTime(seconds: Int) {
        context.dataStore.edit { preferences ->
            preferences[PLAYER_SEEK_TIME_KEY] = seconds
        }
    }

    suspend fun setSubtitleFontSize(size: Int) {
        context.dataStore.edit { preferences ->
            preferences[SUBTITLE_FONT_SIZE_KEY] = size
        }
    }

    suspend fun setSubtitleColor(color: String) {
        context.dataStore.edit { preferences ->
            preferences[SUBTITLE_COLOR_KEY] = color
        }
    }

    suspend fun setSubtitleOpacity(opacity: Float) {
        context.dataStore.edit { preferences ->
            preferences[SUBTITLE_OPACITY_KEY] = opacity
        }
    }

    suspend fun setSubtitleEdgeType(type: Int) {
        context.dataStore.edit { preferences ->
            preferences[SUBTITLE_EDGE_TYPE_KEY] = type
        }
    }

    suspend fun setNightFilterEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[NIGHT_FILTER_ENABLED_KEY] = enabled
        }
    }

    suspend fun setNightFilterIntensity(intensity: Float) {
        context.dataStore.edit { preferences ->
            preferences[NIGHT_FILTER_INTENSITY_KEY] = intensity
        }
    }

    suspend fun setVividModeEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[VIVID_MODE_ENABLED_KEY] = enabled
        }
    }

    suspend fun setOledStealth(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[OLED_STEALTH_MODE_KEY] = enabled
        }
    }

    suspend fun setVaultLocked(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[VAULT_LOCKED_KEY] = enabled
        }
    }

    suspend fun setDeviceName(name: String) {
        context.dataStore.edit { preferences ->
            preferences[DEVICE_NAME_KEY] = name
        }
    }

    suspend fun setUiPulseMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[UI_PULSE_MODE_KEY] = mode
        }
    }

    suspend fun resetTheme() {
        context.dataStore.edit { preferences ->
            preferences.remove(DARK_MODE_KEY)
            preferences[ACCENT_COLOR_KEY] = "AMBER"
        }
    }
}
