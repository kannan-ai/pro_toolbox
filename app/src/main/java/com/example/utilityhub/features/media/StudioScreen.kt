package com.example.utilityhub.features.media

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.text.format.Formatter
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.CallMerge
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.utilityhub.ui.HistoryViewModel
import com.example.utilityhub.ui.theme.PrimaryAmber
import com.example.utilityhub.ui.components.TutorialOverlay
import com.example.utilityhub.ui.components.TutorialStep
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.tom_roush.pdfbox.multipdf.PDFMergerUtility
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.*
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun StudioScreen(
    historyViewModel: HistoryViewModel,
    initialTool: String? = null,
    hasSeenTutorial: Boolean = true,
    onMarkTutorialSeen: () -> Unit = {},
    hideComingSoon: Boolean = false,
    showTutorialExternal: Boolean = false,
    onDismissTutorialExternal: () -> Unit = {}
) {
    var expandedTool by remember { mutableStateOf(initialTool) }
    var lastVoiceoverUri by remember { mutableStateOf<Uri?>(null) }

    val showTutorial = (!hasSeenTutorial && expandedTool == null) || showTutorialExternal

    LaunchedEffect(initialTool) {
        initialTool?.let { expandedTool = it }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Audio Tools
            ExpandableToolCard(
                title = "Audio Tools",
                icon = Icons.Default.Headset,
                color = Color(0xFF00BCD4),
                isExpanded = expandedTool == "audio",
                onExpand = { expandedTool = if (expandedTool == "audio") null else "audio" }
            ) {
                AudioSuite(historyViewModel, onVoiceoverSaved = { lastVoiceoverUri = it })
            }

            // PDF Tools
            ExpandableToolCard(
                title = "PDF Tools",
                icon = Icons.Default.PictureAsPdf,
                color = Color(0xFFE91E63),
                isExpanded = expandedTool == "pdf",
                onExpand = { expandedTool = if (expandedTool == "pdf") null else "pdf" }
            ) {
                PdfSuite(historyViewModel)
            }

            // AI Smart Studio
            ExpandableToolCard(
                title = "AI Smart Studio",
                icon = Icons.Default.AutoAwesome,
                color = Color(0xFF9C27B0),
                isExpanded = expandedTool == "ai",
                onExpand = { expandedTool = if (expandedTool == "ai") null else "ai" }
            ) {
                AiSuite(historyViewModel)
            }

            // Video Tools
            ExpandableToolCard(
                title = "Video Tools",
                icon = Icons.Default.Movie,
                color = Color(0xFFFF5722),
                isExpanded = expandedTool == "video",
                onExpand = { expandedTool = if (expandedTool == "video") null else "video" }
            ) {
                VideoSuite(historyViewModel)
            }

            // Future Features
            if (!hideComingSoon) {
                ExpandableToolCard(
                    title = "Coming Soon",
                    icon = Icons.Default.AutoMode,
                    color = Color.Gray,
                    isExpanded = expandedTool == "soon",
                    onExpand = { expandedTool = if (expandedTool == "soon") null else "soon" }
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        FutureFeatureItem("Background Remover", Icons.Default.Portrait)
                        FutureFeatureItem("Object Eraser", Icons.Default.AutoFixNormal)
                        FutureFeatureItem("Voice Transcriber", Icons.Default.RecordVoiceOver)
                    }
                }
            }

            Spacer(Modifier.height(100.dp))
        }

        // Global Destination Footer
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            tonalElevation = 8.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        ) {
            val folder = when(expandedTool) {
                "video" -> "Movies"
                "audio" -> "Music"
                "pdf", "ai" -> "Documents"
                else -> "ProToolbox"
            }
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Folder, null, tint = PrimaryAmber, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Destination: $folder/ProToolbox",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }

    if (showTutorial) {
        TutorialOverlay(
            steps = listOf(
                TutorialStep(
                    "Creative Studio",
                    "Welcome to your production hub! Tap any category to expand its tools.",
                    Icons.Default.Brush
                ),
                TutorialStep(
                    "Audio Tools",
                    "Convert text to speech or merge multiple audio tracks into one.",
                    Icons.Default.Headset
                ),
                TutorialStep(
                    "PDF Power",
                    "Convert images to PDF, merge documents, or reduce file sizes easily.",
                    Icons.Default.PictureAsPdf
                ),
                TutorialStep(
                    "Smart Scanning",
                    "Use AI OCR to extract text from images or scan documents with filters.",
                    Icons.Default.DocumentScanner
                )
            ),
            onDismiss = {
                onMarkTutorialSeen()
                onDismissTutorialExternal()
            },
            onSkip = {
                onMarkTutorialSeen()
                onDismissTutorialExternal()
            }
        )
    }
}

@Composable
fun ExpandableToolCard(title: String, icon: ImageVector, color: Color, isExpanded: Boolean, onExpand: () -> Unit, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(), 
        shape = RoundedCornerShape(24.dp), 
        elevation = CardDefaults.cardElevation(if (isExpanded) 6.dp else 1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpand() }
                    .padding(20.dp), 
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Refined Metallic Badge
                Surface(
                    color = Color.Transparent, 
                    shape = CircleShape, 
                    modifier = Modifier.size(44.dp).border(1.dp, color.copy(alpha = 0.4f), CircleShape)
                ) { 
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, null, tint = color, modifier = Modifier.size(22.dp)) 
                    }
                }
                Spacer(Modifier.width(16.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                Icon(
                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, 
                    null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
            AnimatedVisibility(visible = isExpanded) { 
                Column(modifier = Modifier.padding(bottom = 20.dp)) { 
                    content() 
                } 
            }
        }
    }
}

@Composable
fun FeatureTile(
    title: String, 
    subtitle: String, 
    icon: ImageVector, 
    onClick: () -> Unit,
    status: @Composable (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        color = Color.Transparent
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (status != null) {
                status()
            } else {
                Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, tint = Color.Gray.copy(alpha = 0.3f), modifier = Modifier.size(12.dp))
            }
        }
    }
}

@Composable
fun FileSlot(label: String, uri: Uri?, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.width(120.dp).height(80.dp).clip(RoundedCornerShape(12.dp)).clickable { onClick() },
        color = if (uri != null) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, if (uri != null) Color(0xFF4CAF50).copy(alpha = 0.3f) else Color.Transparent)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.padding(8.dp)) {
            if (uri != null) {
                Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
                Spacer(Modifier.height(4.dp))
                Text(uri.lastPathSegment ?: "File", style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            } else {
                Icon(Icons.Default.AddCircleOutline, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                Spacer(Modifier.height(4.dp))
                Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
        }
    }
}

@Composable
fun AudioSuite(historyViewModel: HistoryViewModel, onVoiceoverSaved: (Uri) -> Unit) {
    var lastExport by remember { mutableStateOf<Uri?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TextToAudioTool(historyViewModel) { uri -> 
            lastExport = uri
            onVoiceoverSaved(uri)
        }
        HorizontalDivider(modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        AudioMergerTool(historyViewModel) { lastExport = it }
        lastExport?.let { uri -> ExportPreviewCard(uri, "audio") }
    }
}

@Composable
fun PdfSuite(historyViewModel: HistoryViewModel) {
    var lastExport by remember { mutableStateOf<Uri?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        ImageToPdfTool(historyViewModel) { lastExport = it }
        MergePdfTool(historyViewModel) { lastExport = it }
        ReducePdfSizeTool(historyViewModel) { lastExport = it }
        PdfSplitterTool(historyViewModel) { lastExport = it }
        lastExport?.let { uri -> ExportPreviewCard(uri, "pdf") }
    }
}

@Composable
fun AiSuite(historyViewModel: HistoryViewModel) {
    var lastExport by remember { mutableStateOf<Uri?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OcrTool(historyViewModel)
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        SmartDocScannerTool(historyViewModel) { lastExport = it }
        lastExport?.let { uri -> ExportPreviewCard(uri, "pdf") }
    }
}

@Composable
fun VideoSuite(historyViewModel: HistoryViewModel) {
    var lastExport by remember { mutableStateOf<Uri?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        VideoCompressorTool(historyViewModel) { lastExport = it }
        VideoToGifTool(historyViewModel) { lastExport = it }
        AudioVideoMergerTool(historyViewModel) { lastExport = it }
        VideoMuterTool(historyViewModel) { lastExport = it }
        VideoAudioExtractorTool(historyViewModel) { lastExport = it }
        lastExport?.let { uri -> ExportPreviewCard(uri, "video") }
    }
}

@Composable
fun ExportPreviewCard(uri: Uri, type: String) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), shape = CircleShape, modifier = Modifier.size(40.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(if (type == "audio") Icons.Default.MusicNote else if (type == "pdf") Icons.Default.PictureAsPdf else Icons.Default.Movie, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text("Recent Export", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(uri.lastPathSegment ?: "Exported File", style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, if (type == "audio") "audio/*" else if (type == "video") "video/*" else "application/pdf")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Open File"))
            }) {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, null, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun VideoCompressorTool(historyViewModel: HistoryViewModel, onExport: (Uri) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var videoUri by remember { mutableStateOf<Uri?>(null) }
    var isBusy by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { videoUri = it }

    FeatureTile(
        title = "Video Compressor",
        subtitle = "Reduce file size without quality loss",
        icon = Icons.Default.Compress,
        onClick = { if (!isBusy) picker.launch("video/*") },
        status = {
            if (isBusy) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            else if (videoUri != null) Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
        }
    )
    
    if (videoUri != null && !isBusy) {
        Button(
            onClick = {
                isBusy = true
                scope.launch {
                    val result = reduceVideoSize(context, videoUri!!) { progress = it }
                    isBusy = false
                    if (result != null) { videoUri = null; onExport(result); historyViewModel.addHistory("Media", "Video Compress", "Done") }
                }
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryAmber)
        ) {
            Text("Compress Video Now")
        }
    }
}

@Composable
fun VideoToGifTool(historyViewModel: HistoryViewModel, onExport: (Uri) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var videoUri by remember { mutableStateOf<Uri?>(null) }
    var isBusy by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { videoUri = it }

    FeatureTile(
        title = "Video to GIF",
        subtitle = "Create animated loops from videos",
        icon = Icons.Default.Animation,
        onClick = { if (!isBusy) picker.launch("video/*") },
        status = {
            if (isBusy) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            else if (videoUri != null) Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
        }
    )

    if (videoUri != null && !isBusy) {
        Button(
            onClick = {
                isBusy = true
                scope.launch {
                    val result = convertVideoToGif(context, videoUri!!) { progress = it }
                    isBusy = false
                    if (result != null) { videoUri = null; onExport(result); historyViewModel.addHistory("Media", "Video to GIF", "Done") }
                }
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryAmber)
        ) {
            Text("Convert to GIF")
        }
    }
}

@Composable
fun AudioVideoMergerTool(historyViewModel: HistoryViewModel, onExport: (Uri) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var videoUri by remember { mutableStateOf<Uri?>(null) }
    var audioUri by remember { mutableStateOf<Uri?>(null) }
    var isBusy by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    val vPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { videoUri = it }
    val aPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { audioUri = it }

    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        FeatureTile(
            title = "Audio + Video Merger",
            subtitle = "Replace or mix audio track with video",
            icon = Icons.Default.Merge,
            onClick = { }
        )
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FileSlot("Video Source", videoUri) { vPicker.launch("video/*") }
            FileSlot("Audio Source", audioUri) { aPicker.launch("audio/*") }
            
            if (videoUri != null && audioUri != null) {
                Button(
                    onClick = {
                        isBusy = true
                        scope.launch {
                            val result = mergeAudioVideo(context, videoUri!!, audioUri!!) { progress = it }
                            isBusy = false
                            if (result != null) { videoUri = null; audioUri = null; onExport(result); historyViewModel.addHistory("Media", "AV Merge", "Done") }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(80.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryAmber)
                ) {
                    if (isBusy) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    else Text("Merge", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun VideoMuterTool(historyViewModel: HistoryViewModel, onExport: (Uri) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var videoUri by remember { mutableStateOf<Uri?>(null) }
    var isBusy by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { videoUri = it }

    FeatureTile(
        title = "Mute Original Audio",
        subtitle = "Remove audio track instantly",
        icon = Icons.AutoMirrored.Filled.VolumeOff,
        onClick = { if (!isBusy) picker.launch("video/*") },
        status = {
            if (isBusy) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
        }
    )

    if (videoUri != null && !isBusy) {
        Button(
            onClick = {
                isBusy = true
                scope.launch {
                    val result = muteVideoAudio(context, videoUri!!) { progress = it }
                    isBusy = false
                    if (result != null) { videoUri = null; onExport(result); historyViewModel.addHistory("Media", "Video Mute", "Done") }
                }
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryAmber)
        ) {
            Text("Mute Audio Now")
        }
    }
}

@Composable
fun VideoAudioExtractorTool(historyViewModel: HistoryViewModel, onExport: (Uri) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var videoUri by remember { mutableStateOf<Uri?>(null) }
    var isBusy by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { videoUri = it }

    FeatureTile(
        title = "Extract MP3 from Video",
        subtitle = "Save audio track as a separate file",
        icon = Icons.Default.AudioFile,
        onClick = { if (!isBusy) picker.launch("video/*") }
    )

    if (videoUri != null && !isBusy) {
        Button(
            onClick = {
                isBusy = true
                scope.launch {
                    val result = extractAudioFromVideo(context, videoUri!!) { progress = it }
                    isBusy = false
                    if (result != null) { videoUri = null; onExport(result); historyViewModel.addHistory("Media", "Audio Extract", "Done") }
                }
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryAmber)
        ) {
            Text("Extract MP3 Now")
        }
    }
}

@Composable
fun TextToAudioTool(historyViewModel: HistoryViewModel, onExport: (Uri) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var text by remember { mutableStateOf("") }
    var lang by remember { mutableStateOf("en") }
    var isSaving by remember { mutableStateOf(false) }
    val tts = remember { mutableStateOf<TextToSpeech?>(null) }
    var isReady by remember { mutableStateOf(false) }
    val languages = mapOf("en" to "English", "ml" to "Malayalam", "hi" to "Hindi", "ta" to "Tamil", "te" to "Telugu")
    
    DisposableEffect(context) {
        val s = TextToSpeech(context) { status -> if (status == TextToSpeech.SUCCESS) isReady = true }
        tts.value = s
        onDispose { s.stop(); s.shutdown() }
    }
    
    Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Text to Audio", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        OutlinedTextField(
            value = text, 
            onValueChange = { text = it }, 
            placeholder = { Text("Speak or type text to synthesize...") }, 
            modifier = Modifier.fillMaxWidth().height(100.dp),
            shape = RoundedCornerShape(12.dp)
        )
        
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(languages.toList()) { (code, name) ->
                FilterChip(
                    selected = lang == code,
                    onClick = { lang = code },
                    label = { Text(name) },
                    shape = RoundedCornerShape(20.dp)
                )
            }
        }
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { 
                    tts.value?.let { 
                        it.language = if (lang == "en") Locale.ENGLISH else Locale.forLanguageTag(lang)
                        it.speak(text, TextToSpeech.QUEUE_FLUSH, null, "listen") 
                    } 
                }, 
                modifier = Modifier.weight(1f), 
                enabled = text.isNotBlank() && isReady,
                shape = RoundedCornerShape(12.dp)
            ) { Text("Listen") }
            
            Button(
                onClick = { 
                    isSaving = true
                    scope.launch { 
                        saveSpeechToFile(context, tts.value, text, lang) { ok, uri -> 
                            isSaving = false
                            if (ok && uri != null) { 
                                onExport(uri)
                                Toast.makeText(context, "Saved to Music!", Toast.LENGTH_SHORT).show()
                                historyViewModel.addHistory("Text", "Speech Saved", text.take(20)) 
                            } 
                        } 
                    } 
                }, 
                modifier = Modifier.weight(1f), 
                enabled = text.isNotBlank() && isReady && !isSaving,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryAmber)
            ) { 
                if (isSaving) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White) 
                else Text("Save File") 
            }
        }
    }
}

@Composable
fun AudioMergerTool(historyViewModel: HistoryViewModel, onExport: (Uri) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var audio1 by remember { mutableStateOf<Uri?>(null) }
    var audio2 by remember { mutableStateOf<Uri?>(null) }
    var isMerging by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    
    val p1 = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { audio1 = it }
    val p2 = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { audio2 = it }

    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        FeatureTile(
            title = "Audio Merger",
            subtitle = "Combine two tracks into one",
            icon = Icons.Default.Merge,
            onClick = { }
        )
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FileSlot("Audio 1", audio1) { p1.launch("audio/*") }
            FileSlot("Audio 2", audio2) { p2.launch("audio/*") }
            
            if (audio1 != null && audio2 != null) {
                Button(
                    onClick = {
                        isMerging = true
                        scope.launch {
                            val res = mergeAudioFiles(context, audio1!!, audio2!!) { progress = it }
                            isMerging = false
                            if (res != null) { onExport(res); audio1 = null; audio2 = null }
                        }
                    },
                    modifier = Modifier.weight(1f).height(80.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryAmber)
                ) {
                    if (isMerging) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Icon(Icons.Default.PlayArrow, null)
                    }
                }
            }
        }
    }
}

@Composable
fun OcrTool(historyViewModel: HistoryViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var recognizedText by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { imageUri = it }
    val clipboard = LocalClipboardManager.current

    FeatureTile(
        title = "OCR: Image to Text",
        subtitle = "Extract text from photos instantly",
        icon = Icons.Default.TextFields,
        onClick = { picker.launch("image/*") },
        status = { if (isProcessing) CircularProgressIndicator(modifier = Modifier.size(16.dp)) }
    )
    
    if (imageUri != null && !isProcessing && recognizedText.isEmpty()) {
        Button(
            onClick = {
                isProcessing = true
                scope.launch {
                    try {
                        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                        val image = InputImage.fromFilePath(context, imageUri!!)
                        val result = recognizer.process(image).await()
                        recognizedText = result.text
                        if (recognizedText.isNotBlank()) historyViewModel.addHistory("AI", "OCR Success", recognizedText.take(20))
                    } catch (e: Exception) {
                        Toast.makeText(context, "OCR Failed", Toast.LENGTH_SHORT).show()
                    } finally { isProcessing = false }
                }
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryAmber)
        ) { Text("Extract Text Now") }
    }
    
    if (recognizedText.isNotEmpty()) {
        OutlinedTextField(
            value = recognizedText,
            onValueChange = { recognizedText = it },
            modifier = Modifier.fillMaxWidth().padding(16.dp).height(120.dp),
            shape = RoundedCornerShape(12.dp),
            trailingIcon = {
                IconButton(onClick = { 
                    clipboard.setText(AnnotatedString(recognizedText))
                    Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show()
                }) { Icon(Icons.Default.ContentCopy, null) }
            }
        )
    }
}

@Composable
fun SmartDocScannerTool(historyViewModel: HistoryViewModel, onExport: (Uri) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var filter by remember { mutableStateOf("ENHANCED") } 
    var isProcessing by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { imageUri = it }

    FeatureTile(
        title = "Smart Document Scanner",
        subtitle = "Professional filters for documents",
        icon = Icons.Default.Scanner,
        onClick = { picker.launch("image/*") }
    )
    
    if (imageUri != null) {
        Row(modifier = Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = filter == "ENHANCED", onClick = { filter = "ENHANCED" }, label = { Text("Enhanced") })
            FilterChip(selected = filter == "BW", onClick = { filter = "BW" }, label = { Text("B&W") })
            
            Button(
                onClick = {
                    isProcessing = true
                    scope.launch {
                        val result = processAndSaveDocument(context, imageUri!!, filter)
                        isProcessing = false
                        if (result != null) { imageUri = null; onExport(result) }
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryAmber)
            ) {
                if (isProcessing) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                else Text("Save PDF")
            }
        }
    }
}

@Composable
fun ImageToPdfTool(historyViewModel: HistoryViewModel, onExport: (Uri) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var images by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var isBusy by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { images = it }

    FeatureTile(
        title = "Image to PDF",
        subtitle = "Convert multiple photos into a single PDF",
        icon = Icons.Default.PictureAsPdf,
        onClick = { picker.launch("image/*") },
        status = { if (images.isNotEmpty()) Text("${images.size} Selected", style = MaterialTheme.typography.labelSmall, color = PrimaryAmber) }
    )
    
    if (images.isNotEmpty()) {
        Row(modifier = Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            LazyRow(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                items(images) { uri -> Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(4.dp))) { AsyncImagePlaceholder(uri) } }
            }
            Button(
                onClick = {
                    isBusy = true
                    scope.launch {
                        val result = createPdfFromImages(context, images)
                        isBusy = false
                        if (result != null) { images = emptyList(); onExport(result) }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryAmber)
            ) {
                if (isBusy) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                else Text("Convert")
            }
        }
    }
}

@Composable
fun MergePdfTool(historyViewModel: HistoryViewModel, onExport: (Uri) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pdfs by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var isBusy by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { pdfs = it }

    FeatureTile(
        title = "Merge PDFs",
        subtitle = "Combine multiple PDF files into one",
        icon = Icons.AutoMirrored.Filled.CallMerge,
        onClick = { picker.launch(arrayOf("application/pdf")) },
        status = { if (pdfs.isNotEmpty()) Text("${pdfs.size} Files", style = MaterialTheme.typography.labelSmall, color = PrimaryAmber) }
    )
    
    if (pdfs.size > 1) {
        Button(
            onClick = {
                isBusy = true
                scope.launch {
                    val result = mergePdfFiles(context, pdfs)
                    isBusy = false
                    if (result != null) { pdfs = emptyList(); onExport(result) }
                }
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryAmber)
        ) { Text("Merge Files Now") }
    }
}

@Composable
fun ReducePdfSizeTool(historyViewModel: HistoryViewModel, onExport: (Uri) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pdf by remember { mutableStateOf<Uri?>(null) }
    var isBusy by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { pdf = it }

    FeatureTile(
        title = "Reduce PDF Size",
        subtitle = "Compress large PDF documents",
        icon = Icons.Default.PictureAsPdf,
        onClick = { picker.launch("application/pdf") }
    )
    
    if (pdf != null) {
        Button(
            onClick = {
                isBusy = true
                scope.launch {
                    val res = optimizePdfSize(context, pdf!!)
                    isBusy = false
                    if (res.isSuccess && res.outputUri != null) { pdf = null; onExport(res.outputUri!!) }
                }
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryAmber)
        ) { Text("Optimize & Reduce") }
    }
}

@Composable
fun PdfSplitterTool(historyViewModel: HistoryViewModel, onExport: (Uri) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pdf by remember { mutableStateOf<Uri?>(null) }
    var pageCount by remember { mutableIntStateOf(0) }
    var pageRange by remember { mutableStateOf("1-2") }
    var isBusy by remember { mutableStateOf(false) }
    
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        pdf = uri
        uri?.let {
            scope.launch {
                withContext(Dispatchers.IO) {
                    try {
                        context.contentResolver.openInputStream(it)?.use { stream ->
                            val doc = PDDocument.load(stream)
                            pageCount = doc.numberOfPages
                            doc.close()
                        }
                    } catch (_: Exception) {}
                }
            }
        }
    }

    FeatureTile(
        title = "PDF Page Splitter",
        subtitle = "Extract specific pages from a document",
        icon = Icons.AutoMirrored.Filled.LibraryBooks,
        onClick = { if (!isBusy) picker.launch("application/pdf") },
        status = {
            if (pdf != null) {
                Surface(color = Color(0xFFE8F5E9), shape = RoundedCornerShape(12.dp)) {
                    Text("$pageCount pgs", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = Color(0xFF2E7D32))
                }
            }
        }
    )

    if (pdf != null && !isBusy) {
        Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = pageRange, 
                onValueChange = { pageRange = it }, 
                label = { Text("Page Range (e.g. 1-$pageCount)") }, 
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                supportingText = { Text("Format: 1-5 or 1,3,5") }
            )
            Button(
                onClick = {
                    isBusy = true
                    scope.launch {
                        val result = splitPdfPages(context, pdf!!, pageRange)
                        isBusy = false
                        if (result != null) { onExport(result); pdf = null }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryAmber)
            ) {
                Text("Extract Selected Pages")
            }
            
            TextButton(onClick = { pdf = null }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Cancel / Change File", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

suspend fun processAndSaveDocument(context: Context, uri: Uri, filter: String): Uri? = withContext(Dispatchers.IO) {
    try {
        val originalBmp = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) } ?: return@withContext null
        val processedBmp = android.graphics.Bitmap.createBitmap(originalBmp.width, originalBmp.height, originalBmp.config ?: android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(processedBmp)
        val paint = android.graphics.Paint()
        if (filter == "BW") {
            val cm = android.graphics.ColorMatrix(); cm.setSaturation(0f)
            val m = cm.array; m[4] = m[4] + 100; m[9] = m[9] + 100; m[14] = m[14] + 100
            paint.colorFilter = android.graphics.ColorMatrixColorFilter(cm)
        } else {
            val cm = android.graphics.ColorMatrix(); cm.setScale(1.2f, 1.2f, 1.2f, 1f)
            paint.colorFilter = android.graphics.ColorMatrixColorFilter(cm)
        }
        canvas.drawBitmap(originalBmp, 0f, 0f, paint)
        val doc = PDDocument(); val page = PDPage(PDRectangle(processedBmp.width.toFloat(), processedBmp.height.toFloat())); doc.addPage(page); val img = LosslessFactory.createFromImage(doc, processedBmp); PDPageContentStream(doc, page).use { it.drawImage(img, 0f, 0f) }
        val outName = "Scan_${System.currentTimeMillis()}.pdf"; val temp = File(context.cacheDir, outName); doc.save(temp); doc.close()
        val resultUri = saveToGallery(context, temp, outName, "application/pdf", Environment.DIRECTORY_DOCUMENTS)
        originalBmp.recycle(); processedBmp.recycle(); resultUri
    } catch (e: Exception) { null }
}

suspend fun splitPdfPages(context: Context, uri: Uri, range: String): Uri? = withContext(Dispatchers.IO) {
    val tempI = File(context.cacheDir, "spi"); val outName = "Split_${System.currentTimeMillis()}.pdf"; val tempO = File(context.cacheDir, outName)
    try {
        context.contentResolver.openInputStream(uri)?.use { it.copyTo(tempI.outputStream()) }
        val doc = PDDocument.load(tempI); val newDoc = PDDocument()
        range.split(",").forEach { r ->
            if (r.contains("-")) {
                val parts = r.split("-"); val s = parts[0].toInt() - 1; val e = parts[1].toInt() - 1
                for (i in s..e) { if (i < doc.numberOfPages) newDoc.addPage(doc.getPage(i)) }
            } else {
                val p = r.trim().toInt() - 1
                if (p < doc.numberOfPages) newDoc.addPage(doc.getPage(p))
            }
        }
        newDoc.save(tempO); newDoc.close(); doc.close()
        saveToGallery(context, tempO, outName, "application/pdf", Environment.DIRECTORY_DOCUMENTS)
    } catch (e: Exception) { null } finally { tempI.delete(); tempO.delete() }
}

suspend fun optimizePdfSize(context: Context, pdfUri: Uri): ProcessingResult = withContext(Dispatchers.IO) {
    val tempI = File(context.cacheDir, "pi"); val outName = "Opt_${System.currentTimeMillis()}.pdf"; val tempO = File(context.cacheDir, outName)
    try {
        context.contentResolver.openInputStream(pdfUri)?.use { it.copyTo(tempI.outputStream()) }
        val doc = PDDocument.load(tempI); doc.save(tempO); doc.close()
        val uri = saveToGallery(context, tempO, outName, "application/pdf", Environment.DIRECTORY_DOCUMENTS)
        ProcessingResult(true, outputUri = uri)
    } catch (e: Exception) { ProcessingResult(false, e.message) } finally { tempI.delete(); tempO.delete() }
}

suspend fun mergePdfFiles(context: Context, uris: List<Uri>): Uri? = withContext(Dispatchers.IO) {
    val merger = PDFMergerUtility(); val temps = mutableListOf<File>()
    try {
        uris.forEach { uri -> val f = File(context.cacheDir, "t_${System.nanoTime()}.pdf"); context.contentResolver.openInputStream(uri)?.use { it.copyTo(f.outputStream()) }; merger.addSource(f); temps.add(f) }
        val outName = "Merged_${System.currentTimeMillis()}.pdf"; val tempO = File(context.cacheDir, outName); merger.destinationFileName = tempO.absolutePath; merger.mergeDocuments(null)
        saveToGallery(context, tempO, outName, "application/pdf", Environment.DIRECTORY_DOCUMENTS)
    } catch (e: Exception) { null } finally { temps.forEach { it.delete() } }
}

suspend fun createPdfFromImages(context: Context, uris: List<Uri>): Uri? = withContext(Dispatchers.IO) {
    val doc = PDDocument()
    try {
        uris.forEach { uri -> context.contentResolver.openInputStream(uri)?.use { s -> val b = BitmapFactory.decodeStream(s); if (b != null) { val p = PDPage(PDRectangle(b.width.toFloat(), b.height.toFloat())); doc.addPage(p); val img = LosslessFactory.createFromImage(doc, b); PDPageContentStream(doc, p).use { it.drawImage(img, 0f, 0f) }; b.recycle() } } }
        val outName = "PDF_${System.currentTimeMillis()}.pdf"; val temp = File(context.cacheDir, outName); doc.save(temp)
        saveToGallery(context, temp, outName, "application/pdf", Environment.DIRECTORY_DOCUMENTS)
    } catch (e: Exception) { null } finally { doc.close() }
}

fun saveSpeechToFile(context: Context, tts: TextToSpeech?, text: String, lang: String, onResult: (Boolean, Uri?) -> Unit) {
    if (tts == null) { onResult(false, null); return }
    val outWavName = "Synthesis_${System.currentTimeMillis()}.wav"; val tempWav = File(context.cacheDir, outWavName)
    val outMp3Name = "Speech_${System.currentTimeMillis()}.mp3"; val tempMp3 = File(context.cacheDir, outMp3Name)
    val locale = if (lang == "en") Locale.ENGLISH else Locale.forLanguageTag(lang); tts.language = locale
    tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
        override fun onStart(u: String?) {}
        override fun onDone(u: String?) { 
            CoroutineScope(Dispatchers.Main).launch { 
                val uri = withContext(Dispatchers.IO) { 
                    val command = "-i ${tempWav.absolutePath} -codec:a libmp3lame -qscale:a 2 ${tempMp3.absolutePath}"
                    val session = com.arthenica.ffmpegkit.FFmpegKit.execute(command)
                    var savedUri: Uri? = null
                    if (com.arthenica.ffmpegkit.ReturnCode.isSuccess(session.returnCode)) { 
                        savedUri = saveToGallery(context, tempMp3, outMp3Name, "audio/mpeg", Environment.DIRECTORY_MUSIC)
                    }
                    tempWav.delete()
                    tempMp3.delete()
                    savedUri
                }
                onResult(uri != null, uri)
            } 
        }
        @Deprecated("Deprecated in Java") override fun onError(u: String?) { CoroutineScope(Dispatchers.Main).launch { onResult(false, null) } }
        override fun onError(u: String?, errorCode: Int) { CoroutineScope(Dispatchers.Main).launch { onResult(false, null) } }
    })
    val params = android.os.Bundle(); params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "save"); tts.synthesizeToFile(text, params, tempWav, "save")
}

@Composable
fun FutureFeatureItem(title: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.alpha(0.6f)) {
        Surface(color = Color.Gray.copy(0.1f), shape = CircleShape, modifier = Modifier.size(32.dp)) {
            Box(contentAlignment = Alignment.Center) { Icon(icon, null, modifier = Modifier.size(16.dp), tint = Color.Gray) }
        }
        Spacer(Modifier.width(12.dp))
        Text(title, color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.weight(1f))
        Surface(color = Color.DarkGray, shape = RoundedCornerShape(4.dp)) {
            Text("SOON", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
        }
    }
}
