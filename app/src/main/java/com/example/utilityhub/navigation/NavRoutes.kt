package com.example.utilityhub.navigation

import com.example.utilityhub.R

sealed class Screen(val route: String, val titleRes: Int, val icon: String) {
    object Home : Screen("home", R.string.app_name, "🏠")
    object TextStudio : Screen("text_studio", R.string.title_text_studio, "🌐")
    object TextToAudio : Screen("text_to_audio", R.string.title_text_to_audio, "🎙️")
    object Currency : Screen("currency", R.string.title_currency, "💱")
    object QuickCalc : Screen("quick_calc", R.string.title_quick_calc, "%")
    object SmartPriceHub : Screen("smart_price_hub", R.string.title_smart_price_hub, "🏷️")
    object QR : Screen("qr", R.string.title_qr, "📱")
    object Measurement : Screen("measurement", R.string.title_measurement, "📏")
    object Password : Screen("password", R.string.title_password, "🔑")
    object History : Screen("history", R.string.title_history, "📜")
    object MediaStudio : Screen("media_studio", R.string.title_media_studio, "🎨")
    object Creations : Screen("creations", R.string.title_creations, "📁")
    object VideoPlayer : Screen("video_player", R.string.title_video_player, "🎬")
    object MusicPlayer : Screen("music_player", R.string.title_music_player, "🎵")
    object SystemHealth : Screen("system_health", R.string.title_system_health, "🌡️")
    object FileTransfer : Screen("file_transfer", R.string.title_file_transfer, "🚀")
    object SupportBot : Screen("support_bot", R.string.title_support_bot, "🤖")
    object Settings : Screen("settings", R.string.title_settings, "⚙️")
}

val allScreens = listOf(
    Screen.Home,
    Screen.TextStudio,
    Screen.TextToAudio,
    Screen.Currency,
    Screen.QuickCalc,
    Screen.QR,
    Screen.Measurement,
    Screen.Password,
    Screen.MediaStudio,
    Screen.Creations,
    Screen.VideoPlayer,
    Screen.MusicPlayer,
    Screen.SystemHealth,
    Screen.FileTransfer,
    Screen.SupportBot,
    Screen.History,
    Screen.Settings
)
