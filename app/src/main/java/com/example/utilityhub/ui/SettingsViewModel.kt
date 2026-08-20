package com.example.utilityhub.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.utilityhub.data.prefs.ThemeManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val themeManager: ThemeManager) : ViewModel() {
    val isDarkMode: StateFlow<Boolean?> = themeManager.isDarkMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val pinnedScreens: StateFlow<Set<String>> = themeManager.pinnedScreens
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val appLanguage: StateFlow<String> = themeManager.appLanguage
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "en")

    val isGalleryAccessEnabled: StateFlow<Boolean> = themeManager.isGalleryAccessEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isIncognitoMode: StateFlow<Boolean> = themeManager.isIncognitoMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val menuMode: StateFlow<String> = themeManager.menuMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "BASIC")

    val accentColor: StateFlow<String> = themeManager.accentColor
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "AMBER")

    val isSwaraEnabled: StateFlow<Boolean> = themeManager.isSwaraEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isVoiceWakeEnabled: StateFlow<Boolean> = themeManager.isVoiceWakeEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val hideComingSoon: StateFlow<Boolean> = themeManager.hideComingSoon
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val playerSeekTime: StateFlow<Int> = themeManager.playerSeekTime
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 10)

    val isOledStealth: StateFlow<Boolean> = themeManager.isOledStealth
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isVaultLocked: StateFlow<Boolean> = themeManager.isVaultLocked
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun toggleDarkMode(isDark: Boolean) {
        viewModelScope.launch {
            themeManager.setDarkMode(isDark)
        }
    }

    fun setLanguage(lang: String) {
        viewModelScope.launch {
            themeManager.setAppLanguage(lang)
        }
    }

    fun setGalleryAccess(enabled: Boolean) {
        viewModelScope.launch {
            themeManager.setGalleryAccessEnabled(enabled)
        }
    }

    fun setIncognitoMode(enabled: Boolean) {
        viewModelScope.launch {
            themeManager.setIncognitoMode(enabled)
        }
    }

    fun setMenuMode(mode: String) {
        viewModelScope.launch {
            themeManager.setMenuMode(mode)
        }
    }

    fun setAccentColor(colorName: String) {
        viewModelScope.launch {
            themeManager.setAccentColor(colorName)
        }
    }

    fun togglePin(route: String) {
        viewModelScope.launch {
            themeManager.togglePin(route)
        }
    }

    fun setSwaraEnabled(enabled: Boolean) {
        viewModelScope.launch {
            themeManager.setSwaraEnabled(enabled)
        }
    }

    fun setVoiceWakeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            themeManager.setVoiceWakeEnabled(enabled)
        }
    }

    fun setHideComingSoon(hide: Boolean) {
        viewModelScope.launch {
            themeManager.setHideComingSoon(hide)
        }
    }

    fun setPlayerSeekTime(seconds: Int) {
        viewModelScope.launch {
            themeManager.setPlayerSeekTime(seconds)
        }
    }

    fun setOledStealth(enabled: Boolean) {
        viewModelScope.launch {
            themeManager.setOledStealth(enabled)
        }
    }

    fun setVaultLocked(enabled: Boolean) {
        viewModelScope.launch {
            themeManager.setVaultLocked(enabled)
        }
    }

    fun resetTheme() {
        viewModelScope.launch {
            themeManager.resetTheme()
        }
    }
}
