package com.example.utilityhub.features.text

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.example.utilityhub.R
import com.example.utilityhub.data.api.OfflineTranslator
import com.example.utilityhub.data.api.RetrofitInstance
import com.example.utilityhub.ui.HistoryViewModel
import com.example.utilityhub.ui.components.CameraPreview
import com.example.utilityhub.ui.theme.PrimaryAmber
import com.example.utilityhub.ui.utils.NetworkUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun TranslatorScreen(historyViewModel: HistoryViewModel) {
    var input by remember { mutableStateOf("") }
    var output by remember { mutableStateOf("") }
    var pronunciation by remember { mutableStateOf("") }
    var fromLang by remember { mutableStateOf("en") }
    var toLang by remember { mutableStateOf("ml") }
    var showCamera by remember { mutableStateOf(false) }
    var isListening by remember { mutableStateOf(false) }
    
    // Auto-detect network state
    val context = LocalContext.current
    var isNetworkAvailable by remember { mutableStateOf(NetworkUtils.isNetworkAvailable(context)) }
    
    var fromModelDownloaded by remember { mutableStateOf(false) }
    var toModelDownloaded by remember { mutableStateOf(false) }
    var isDownloadingFrom by remember { mutableStateOf(false) }
    var isDownloadingTo by remember { mutableStateOf(false) }
    
    var showOfflineManager by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val clipboardManager = LocalClipboardManager.current
    val offlineTranslator = remember { OfflineTranslator() }

    // Periodic network check
    LaunchedEffect(Unit) {
        while (true) {
            isNetworkAvailable = NetworkUtils.isNetworkAvailable(context)
            delay(5000.milliseconds)
        }
    }

    val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }
    val recognizerIntent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) showCamera = true
    }

    val voicePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            isListening = true
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, fromLang)
            speechRecognizer.startListening(recognizerIntent)
        }
    }

    DisposableEffect(Unit) {
        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { isListening = false }
            override fun onError(error: Int) { isListening = false }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    input = matches[0]
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    input = matches[0]
                }
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
        speechRecognizer.setRecognitionListener(listener)
        onDispose {
            speechRecognizer.destroy()
        }
    }

    val tts = remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(context) {
        val speech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                // Engine initialized successfully
            }
        }
        tts.value = speech
        onDispose {
            speech.stop()
            speech.shutdown()
        }
    }

    val languages = mapOf(
        "en" to "English", "ml" to "Malayalam", "hi" to "Hindi", "ta" to "Tamil", "te" to "Telugu",
        "kn" to "Kannada", "bn" to "Bengali", "mr" to "Marathi", "gu" to "Gujarati", "pa" to "Punjabi",
        "ur" to "Urdu", "fr" to "French", "es" to "Spanish", "de" to "German", "it" to "Italian",
        "ja" to "Japanese", "ko" to "Korean", "zh" to "Chinese", "ru" to "Russian", "ar" to "Arabic"
    )

    val supportedOffline = setOf(
        "af", "sq", "ar", "be", "bn", "bg", "ca", "zh", "hr", "cs", "da", "nl", "en", "eo", "et", 
        "fi", "fr", "gl", "ka", "de", "el", "gu", "ht", "he", "hi", "hu", "is", "id", "ga", "it", 
        "ja", "kn", "ko", "lv", "lt", "mk", "ms", "mt", "mr", "no", "fa", "pl", "pt", "pa", "ro", 
        "ru", "sk", "sl", "es", "sw", "sv", "ta", "te", "th", "tr", "uk", "ur", "vi", "cy", "ml"
    )

    fun speak(text: String, lang: String) {
        tts.value?.let { speech ->
            val locale = if (lang == "en") Locale.ENGLISH else Locale.forLanguageTag(lang)
            speech.language = locale
            speech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    LaunchedEffect(fromLang, toLang) {
        fromModelDownloaded = offlineTranslator.isModelDownloaded(fromLang)
        toModelDownloaded = offlineTranslator.isModelDownloaded(toLang)
    }

    fun translate() {
        if (input.isBlank()) return
        output = "Translating..."
        pronunciation = ""
        scope.launch {
            try {
                if (!isNetworkAvailable) {
                    // Force offline mode if network is unavailable
                    val fromReady = offlineTranslator.isModelDownloaded(fromLang)
                    val toReady = offlineTranslator.isModelDownloaded(toLang)
                    
                    if (fromReady && toReady) {
                        output = offlineTranslator.translateOffline(input, fromLang, toLang)
                    } else {
                        val missing = mutableListOf<String>()
                        if (!fromReady) missing.add(languages[fromLang] ?: fromLang)
                        if (!toReady) missing.add(languages[toLang] ?: toLang)
                        
                        output = "⚠️ Offline: Please connect to network or download models for: ${missing.joinToString(", ")}"
                        Toast.makeText(context, "Network connection required for online translation", Toast.LENGTH_LONG).show()
                    }
                } else {
                    // Try Cloud translation if network is available
                    try {
                        val response = RetrofitInstance.translationApi.translate(sl = fromLang, tl = toLang, q = input)
                        val resultText = StringBuilder()
                        if (response.size() > 0 && response[0].isJsonArray) {
                            val segments = response[0].asJsonArray
                            for (i in 0 until segments.size()) {
                                val segment = segments[i]
                                if (segment.isJsonArray) {
                                    val parts = segment.asJsonArray
                                    if (parts.size() > 0 && parts[0].isJsonPrimitive) {
                                        resultText.append(parts[0].asString)
                                    }
                                }
                            }
                            try {
                                val lastSegment = segments[segments.size() - 1].asJsonArray
                                if (lastSegment.size() >= 3 && lastSegment[2].isJsonPrimitive) {
                                    pronunciation = lastSegment[2].asString
                                }
                            } catch (_: Exception) {}
                        }
                        
                        if (resultText.isNotEmpty()) {
                            output = resultText.toString()
                            historyViewModel.addHistory("Translator", input, output)
                        } else {
                            // Fallback to offline if cloud parse fails but models are ready
                            if (offlineTranslator.isModelDownloaded(fromLang) && offlineTranslator.isModelDownloaded(toLang)) {
                                output = offlineTranslator.translateOffline(input, fromLang, toLang)
                            } else {
                                output = "Error: Could not parse cloud translation."
                            }
                        }
                    } catch (_: Exception) {
                        // Fallback to offline if cloud fails
                        if (offlineTranslator.isModelDownloaded(fromLang) && offlineTranslator.isModelDownloaded(toLang)) {
                            output = offlineTranslator.translateOffline(input, fromLang, toLang)
                        } else {
                            output = "Error: Cloud translation failed and offline models not ready."
                        }
                    }
                }
            } catch (e: Exception) {
                output = "Error: ${e.message}"
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Unified Language Selector Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    LanguageSelector("From", fromLang, languages) { fromLang = it }
                }
                
                IconButton(onClick = { 
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    val temp = fromLang
                    fromLang = toLang
                    toLang = temp
                    val tempText = input
                    input = output
                    output = tempText
                }) {
                    Icon(Icons.Default.SwapHoriz, null, tint = MaterialTheme.colorScheme.primary)
                }

                Box(modifier = Modifier.weight(1f)) {
                    LanguageSelector("To", toLang, languages) { toLang = it }
                }
                
                Spacer(Modifier.width(8.dp))
                
                // Network Status Indicator (Auto-detected)
                Surface(
                    onClick = { showOfflineManager = true },
                    color = if (isNetworkAvailable) Color(0xFFE8F5E9) else Color(0xFFFBE9E7),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, if (isNetworkAvailable) Color(0xFF4CAF50).copy(0.2f) else Color(0xFFFF5722).copy(0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isNetworkAvailable) "☁️ Cloud" else "🟢 Offline",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isNetworkAvailable) Color(0xFF2E7D32) else Color(0xFFD84315),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Manage Offline Link
        TextButton(
            onClick = { showOfflineManager = true },
            modifier = Modifier.align(Alignment.End)
        ) {
            Icon(Icons.Default.Settings, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Manage Offline Models", style = MaterialTheme.typography.labelSmall)
        }

        // 2. Input Section
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.Transparent,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    TextField(
                        value = input,
                        onValueChange = { input = it },
                        placeholder = { Text("Enter text to translate...") },
                        modifier = Modifier.fillMaxWidth().height(160.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )
                    
                    // Paste/Clear Actions top-right
                    Row(modifier = Modifier.align(Alignment.TopEnd)) {
                        if (input.isNotEmpty()) {
                            IconButton(onClick = { input = "" }) {
                                Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp))
                            }
                        }
                        IconButton(onClick = {
                            val clip = clipboardManager.getText()
                            if (clip != null) input = clip.text
                        }) {
                            Icon(Icons.Outlined.ContentPaste, null, modifier = Modifier.size(18.dp))
                        }
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row {
                        IconButton(onClick = {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                isListening = true
                                recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, fromLang)
                                speechRecognizer.startListening(recognizerIntent)
                            } else {
                                voicePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }) {
                            Icon(Icons.Default.Mic, null, tint = if (isListening) Color.Red else MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                showCamera = true
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        }) {
                            Icon(Icons.Default.PhotoCamera, null)
                        }
                    }
                    
                    Text("${input.length} ch", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
            }
        }

        // 3. Translation Action with Gradient
        Button(
            onClick = { translate() },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(0.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.horizontalGradient(listOf(PrimaryAmber, Color(0xFFB45309)))),
                contentAlignment = Alignment.Center
            ) {
                Text("Translate Now", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        // 4. Output Section
        if (output.isNotEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.03f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = output,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    if (pronunciation.isNotEmpty()) {
                        Text(
                            text = pronunciation,
                            style = MaterialTheme.typography.bodyMedium,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    
                    Spacer(Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { speak(output, toLang) }) {
                            Icon(Icons.AutoMirrored.Filled.VolumeUp, null)
                        }
                        IconButton(onClick = { 
                            clipboardManager.setText(AnnotatedString(output))
                            Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.ContentCopy, null)
                        }
                        IconButton(onClick = { 
                            Toast.makeText(context, "Saved to Notes", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Outlined.Save, null)
                        }
                    }
                }
            }
        }
    }

    if (showCamera) {
        Dialog(onDismissRequest = { showCamera = false }) {
            Surface(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.fillMaxSize()) {
                    CameraPreview { text ->
                        input = text
                        showCamera = false
                    }
                    IconButton(
                        onClick = { showCamera = false },
                        modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close Camera")
                    }
                }
            }
        }
    }

    if (showOfflineManager) {
        OfflineManagerDialog(
            currentFrom = fromLang,
            currentTo = toLang,
            languages = languages,
            supportedOffline = supportedOffline,
            offlineTranslator = offlineTranslator,
            onDismiss = { 
                showOfflineManager = false
                // Refresh downloaded status
                scope.launch {
                    fromModelDownloaded = offlineTranslator.isModelDownloaded(fromLang)
                    toModelDownloaded = offlineTranslator.isModelDownloaded(toLang)
                }
            }
        )
    }
}

@Composable
fun OfflineManagerDialog(
    currentFrom: String,
    currentTo: String,
    languages: Map<String, String>,
    supportedOffline: Set<String>,
    offlineTranslator: OfflineTranslator,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var fromDownloaded by remember { mutableStateOf(false) }
    var toDownloaded by remember { mutableStateOf(false) }
    var isDownloadingFrom by remember { mutableStateOf(false) }
    var isDownloadingTo by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        fromDownloaded = offlineTranslator.isModelDownloaded(currentFrom)
        toDownloaded = offlineTranslator.isModelDownloaded(currentTo)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Offline Languages", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Download models for offline use when network is unavailable.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                
                OfflineItem(
                    label = languages[currentFrom] ?: currentFrom,
                    isDownloaded = fromDownloaded,
                    isDownloading = isDownloadingFrom,
                    isSupported = supportedOffline.contains(currentFrom),
                    onDownload = {
                        isDownloadingFrom = true
                        scope.launch {
                            val success = offlineTranslator.downloadModel(currentFrom)
                            isDownloadingFrom = false
                            if (success) fromDownloaded = true
                        }
                    }
                )

                OfflineItem(
                    label = languages[currentTo] ?: currentTo,
                    isDownloaded = toDownloaded,
                    isDownloading = isDownloadingTo,
                    isSupported = supportedOffline.contains(currentTo),
                    onDownload = {
                        isDownloadingTo = true
                        scope.launch {
                            val success = offlineTranslator.downloadModel(currentTo)
                            isDownloadingTo = false
                            if (success) toDownloaded = true
                        }
                    }
                )
                
                Text(
                    "Note: Each language model is approximately 30MB.",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Done") }
        }
    )
}

@Composable
fun OfflineItem(label: String, isDownloaded: Boolean, isDownloading: Boolean, isSupported: Boolean, onDownload: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontWeight = FontWeight.Medium)
        if (!isSupported) {
            Text("Cloud Only", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        } else if (isDownloaded) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Downloaded", style = MaterialTheme.typography.labelSmall, color = Color(0xFF4CAF50))
            }
        } else if (isDownloading) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
        } else {
            IconButton(onClick = onDownload) {
                Icon(Icons.Default.Download, null, tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSelector(label: String, selected: String, languages: Map<String, String>, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Surface(
            modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { expanded = true },
            color = Color.Transparent
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = languages[selected] ?: selected,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(Icons.Default.ArrowDropDown, null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
        
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            languages.forEach { (code, name) ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        onSelected(code)
                        expanded = false
                    }
                )
            }
        }
    }
}
