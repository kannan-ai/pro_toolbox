package com.example.utilityhub

import android.app.PictureInPictureParams
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.core.os.LocaleListCompat
import com.example.utilityhub.data.db.AppDatabase
import com.example.utilityhub.data.prefs.ThemeManager
import com.example.utilityhub.ui.HistoryViewModel
import com.example.utilityhub.ui.UtilityHubApp
import com.example.utilityhub.ui.theme.UtilityHubTheme
import com.example.utilityhub.features.support.SwaraWakeWordAssistant

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private var isPipMode by mutableStateOf(false)
    private var wakeWordAssistant: SwaraWakeWordAssistant? = null
    private val openBotFlow = mutableStateOf(false)
    private var startScreen by mutableStateOf<String?>(null)

    companion object {
        var isVideoPlaying = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Handle Intent Extras for Quick Settings Tiles
        startScreen = intent?.getStringExtra("START_SCREEN")

        val themeManager = ThemeManager(this)
        val db = AppDatabase.getDatabase(this)
        val historyViewModel = HistoryViewModel(db.historyDao(), themeManager)
        
        // Handle language changes outside composition to avoid loops/crashes
        lifecycleScope.launch {
            themeManager.appLanguage.collect { lang ->
                val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(lang)
                if (AppCompatDelegate.getApplicationLocales() != appLocale) {
                    AppCompatDelegate.setApplicationLocales(appLocale)
                }
            }
        }

        // Initialize Wake Word Assistant
        wakeWordAssistant = SwaraWakeWordAssistant(this) {
            openBotFlow.value = true
        }

        lifecycleScope.launch {
            themeManager.isVoiceWakeEnabled.collect { enabled ->
                val swaraEnabled = themeManager.isSwaraEnabled.first()
                val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                    this@MainActivity, 
                    android.Manifest.permission.RECORD_AUDIO
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                if (enabled && swaraEnabled && hasPermission) {
                    wakeWordAssistant?.start()
                } else {
                    wakeWordAssistant?.stop()
                }
            }
        }

        enableEdgeToEdge()
        setContent {
            val isDarkPref by themeManager.isDarkMode.collectAsState(initial = null)
            val isDark = isDarkPref ?: isSystemInDarkTheme()
            val accentColor by themeManager.accentColor.collectAsState(initial = "AMBER")
            val isOledStealth by themeManager.isOledStealth.collectAsState(initial = false)
            val pulseMode by themeManager.uiPulseMode.collectAsState(initial = "NEUTRAL")
            
            // Watch for wake word trigger
            val triggerOpenBot = openBotFlow.value
            LaunchedEffect(triggerOpenBot) {
                if (triggerOpenBot) {
                    // Handled in UtilityHubApp via a passed boolean or state
                }
            }

            UtilityHubTheme(
                darkTheme = isDark, 
                accentColor = accentColor, 
                isOledStealth = isOledStealth,
                pulseMode = pulseMode
            ) {
                UtilityHubApp(
                    themeManager = themeManager, 
                    historyViewModel = historyViewModel, 
                    playlistDao = db.playlistDao(), 
                    swaraDao = db.swaraDao(), 
                    currencyCacheDao = db.currencyCacheDao(),
                    isPipMode = isPipMode,
                    triggerOpenBot = triggerOpenBot,
                    onBotOpened = { openBotFlow.value = false },
                    startScreen = startScreen,
                    onStartScreenHandled = { startScreen = null }
                )
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (isVideoPlaying && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .build()
            enterPictureInPictureMode(params)
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isPipMode = isInPictureInPictureMode
    }

    override fun onDestroy() {
        super.onDestroy()
        wakeWordAssistant?.destroy()
    }
}
