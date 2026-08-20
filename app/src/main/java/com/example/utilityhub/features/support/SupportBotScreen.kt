package com.example.utilityhub.features.support

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicNone
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.utilityhub.data.db.SwaraDao
import com.example.utilityhub.data.prefs.ThemeManager
import com.example.utilityhub.navigation.Screen
import com.example.utilityhub.ui.utils.ScreenshotUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class Message(val text: String, val isUser: Boolean, val mood: SwaraMood = SwaraMood.NEUTRAL, val audioAction: SwaraAudioAction = SwaraAudioAction.NONE)

data class EmojiParticle(val id: Long, val emoji: String, val xOffset: Float, val yOffset: Float)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportBotScreen(
    themeManager: ThemeManager, 
    swaraDao: SwaraDao, 
    historyViewModel: com.example.utilityhub.ui.HistoryViewModel,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val haptic = LocalHapticFeedback.current
    val isSwaraReady by themeManager.isSwaraReady.collectAsState(initial = false)
    val history by historyViewModel.allHistory.collectAsState()
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    
    var inputText by remember { mutableStateOf("") }
    val messages = remember { 
        mutableStateListOf<Message>(
            Message(
                text = "Hi! I'm Swara, your companion. How was your day? I'm here if you want to chat or need help with your tools.",
                isUser = false
            )
        ) 
    }

    // Semantic Ghosting Check
    LaunchedEffect(isSwaraReady, history) {
        if (isSwaraReady && messages.size == 1) {
            val ghosting = SwaraEngine.getPredictiveGhosting(history)
            if (ghosting != null) {
                delay(1000)
                messages.add(Message(ghosting.text, false, ghosting.mood))
            }
        }
    }
    var isTyping by remember { mutableStateOf(false) }
    var currentThought by remember { mutableStateOf("Swara is thinking...") }
    var currentMood by remember { mutableStateOf(SwaraMood.NEUTRAL) }
    var showBreathing by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableFloatStateOf(-1f) }
    var isListening by remember { mutableStateOf(false) }
    val emojiParticles = remember { mutableStateListOf<EmojiParticle>() }

    fun triggerEmojis(mood: SwaraMood, action: SwaraAudioAction) {
        val emojis = mutableListOf<String>()
        if (mood == SwaraMood.HEART_EYES) emojis.add("💕")
        if (mood == SwaraMood.WINK) emojis.add("😉")
        if (action == SwaraAudioAction.LAUGH) emojis.add("😂")
        if (action == SwaraAudioAction.GIGGLE) emojis.add("🤭")

        if (emojis.isNotEmpty()) {
            scope.launch {
                repeat(5) {
                    val id = System.currentTimeMillis() + it
                    emojiParticles.add(EmojiParticle(id, emojis.random(), (Math.random() * 200 - 100).toFloat(), (Math.random() * -200).toFloat()))
                    delay(100)
                }
            }
        }
    }

    val voiceAssistant = remember {
        SwaraVoiceAssistant(
            context = context,
            onResults = { spokenText ->
                isListening = false
                inputText = spokenText
            },
            onError = { error ->
                isListening = false
                android.widget.Toast.makeText(context, error, android.widget.Toast.LENGTH_SHORT).show()
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose { voiceAssistant.destroy() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Swara Bot", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(if (isSwaraReady) "Advanced AI • Offline" else "Basic Assistant", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                },
                actions = {
                    if (!isSwaraReady && downloadProgress == -1f) {
                        TextButton(onClick = {
                            scope.launch {
                                SwaraEngine.downloadAIAssets(swaraDao).collect { progress ->
                                    downloadProgress = progress
                                    if (progress >= 1f) {
                                        themeManager.setSwaraReady(true)
                                        downloadProgress = -1f
                                    }
                                }
                            }
                        }) {
                            Text("Setup AI")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .imePadding()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(messages) { msg ->
                        ChatBubble(msg, particles = if (!msg.isUser && msg == messages.lastOrNull()) emojiParticles else emptyList(), onParticleEnd = { emojiParticles.remove(it) })
                    }
                    if (isTyping) {
                        item {
                            TypingIndicator(currentMood, currentThought, particles = emojiParticles, onParticleEnd = { emojiParticles.remove(it) })
                        }
                    }
                }

                if (downloadProgress != -1f) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Downloading AI Assets... ${(downloadProgress * 100).toInt()}%", style = MaterialTheme.typography.labelSmall)
                        LinearProgressIndicator(progress = { downloadProgress }, modifier = Modifier.fillMaxWidth())
                    }
                }

                Surface(tonalElevation = 8.dp) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (isListening) {
                                    voiceAssistant.stopListening()
                                    isListening = false
                                } else {
                                    voiceAssistant.startListening()
                                    isListening = true
                                }
                            },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = if (isListening) Color.Red.copy(alpha = 0.1f) else Color.Transparent
                            )
                        ) {
                            Icon(
                                if (isListening) Icons.Default.Mic else Icons.Default.MicNone, 
                                null, 
                                tint = if (isListening) Color.Red else MaterialTheme.colorScheme.primary
                            )
                        }

                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = { Text("Ask Swara anything...") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(24.dp),
                            enabled = downloadProgress == -1f
                        )
                        Spacer(Modifier.width(12.dp))
                        IconButton(
                            onClick = {
                                if (inputText.isNotBlank()) {
                                    val userMsg = inputText
                                    messages.add(Message(userMsg, true))
                                    inputText = ""
                                    
                                    scope.launch {
                                        isTyping = true
                                        currentThought = "Swara is processing..."
                                        val response = SwaraEngine.getResponse(swaraDao, userMsg, isSwaraReady)
                                        
                                        response.thoughts.forEach { thought ->
                                            currentThought = thought
                                            delay(800)
                                        }

                                        currentMood = response.mood
                                        messages.add(Message(response.text, false, response.mood, response.audioAction))
                                        listState.animateScrollToItem(messages.size - 1)
                                        
                                        if (response.isEmpathy) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        }
                                        
                                        triggerEmojis(response.mood, response.audioAction)
                                        voiceAssistant.speak(response.text, response.audioAction)
                                        isTyping = false
                                        
                                        // Handle Intent
                                        when (response.intent) {
                                            SwaraIntent.NAVIGATE -> response.route?.let { onNavigate(it) }
                                            SwaraIntent.ADAPT_UI -> {
                                                response.suggestedPulse?.let { pulse ->
                                                    scope.launch { themeManager.setUiPulseMode(pulse.name) }
                                                }
                                            }
                                            SwaraIntent.ORCHESTRATE -> {
                                                // Handle Orchestration Sequence
                                                messages.add(Message("Pipeline started. Launching Module 1: AI Scanner.", false, SwaraMood.ANALYTICAL))
                                                delay(1000)
                                                onNavigate(Screen.QR.route) // Using QR as a placeholder for general scanning
                                            }
                                            SwaraIntent.SCREENSHOT -> {
                                                delay(500)
                                                val bitmap = ScreenshotUtils.captureView(view)
                                                val activity = context as? android.app.Activity
                                                if (activity != null) {
                                                    val res = ScreenshotUtils.saveScreenshot(activity, bitmap)
                                                    if (res != null) {
                                                        messages.add(Message("Screenshot saved to Gallery/ProToolbox!", false))
                                                    }
                                                }
                                            }
                                            SwaraIntent.EXPORT -> {
                                                messages.add(Message("Video Editor features are currently disabled.", false))
                                            }
                                            SwaraIntent.BREATHE -> {
                                                showBreathing = true
                                            }
                                            else -> {}
                                        }
                                    }
                                }
                            },
                            colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primary),
                            enabled = downloadProgress == -1f
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, null, tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }
            }
        }

        if (showBreathing) {
            BreathingOverlay(onDismiss = { showBreathing = false })
        }
    }
}

@Composable
fun AnimateSwaraIcon(mood: SwaraMood, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    
    val color by animateColorAsState(
        targetValue = when (mood) {
            SwaraMood.HAPPY -> Color(0xFFFFD700) // Golden
            SwaraMood.SUPPORTIVE -> Color(0xFFFFE4E1) // Misty Rose
            SwaraMood.CALM -> Color(0xFF008080) // Teal
            SwaraMood.ENERGIZED -> Color(0xFFFF4500) // Orange Red
            SwaraMood.BLUSH -> Color(0xFFFFB6C1) // Peach/Rose-Pink
            SwaraMood.WINK -> Color(0xFFADD8E6) // Light Blue for wink
            SwaraMood.HEART_EYES -> Color(0xFFFF1493) // Deep Pink
            else -> MaterialTheme.colorScheme.primary
        },
        animationSpec = tween(1000), label = "moodColor"
    )

    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = when (mood) {
            SwaraMood.SUPPORTIVE -> 1.2f // Heartbeat
            SwaraMood.ENERGIZED -> 1.3f // Rapid pulse
            SwaraMood.CALM -> 1.1f // Breathing
            SwaraMood.BLUSH -> 1.25f // Charming pulse
            SwaraMood.WINK -> 1.15f
            SwaraMood.HEART_EYES -> 1.4f // Strong heartbeat
            else -> 1f
        },
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (mood) {
                    SwaraMood.SUPPORTIVE -> 800
                    SwaraMood.ENERGIZED -> 400
                    SwaraMood.CALM -> 2000
                    SwaraMood.BLUSH -> 600
                    SwaraMood.WINK -> 300 // Quick ping
                    SwaraMood.HEART_EYES -> 300 // Rapid heartbeat
                    else -> 1000
                },
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ), label = "iconScale"
    )

    Box(contentAlignment = Alignment.Center, modifier = modifier.scale(scale)) {
        if (mood != SwaraMood.NEUTRAL) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(color.copy(alpha = 0.3f), CircleShape)
            )
        }
        Icon(Icons.Default.SmartToy, null, tint = color, modifier = Modifier.size(24.dp))
    }
}

@Composable
fun FloatingEmoji(particle: EmojiParticle, onAnimationEnd: () -> Unit) {
    val alpha = remember { Animatable(0f) }
    val y = remember { Animatable(0f) }
    val x = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch { alpha.animateTo(1f, tween(500)) }
        launch { x.animateTo(particle.xOffset, tween(2000, easing = LinearOutSlowInEasing)) }
        launch { 
            y.animateTo(particle.yOffset, tween(2000, easing = LinearOutSlowInEasing))
            alpha.animateTo(0f, tween(500))
            onAnimationEnd()
        }
    }

    Text(
        text = particle.emoji,
        fontSize = 24.sp,
        modifier = Modifier
            .offset(x = x.value.dp, y = y.value.dp)
            .scale(alpha.value)
    )
}

@Composable
fun BreathingOverlay(onDismiss: () -> Unit) {
    var step by remember { mutableStateOf("Breathe In") }
    var progress by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            step = "Breathe In (4s)"
            animate(0f, 1f, animationSpec = tween(4000, easing = LinearEasing)) { value, _ -> progress = value }
            step = "Hold (7s)"
            delay(7000)
            step = "Exhale (8s)"
            animate(1f, 0f, animationSpec = tween(8000, easing = LinearEasing)) { value, _ -> progress = value }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.8f)).clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(step, style = MaterialTheme.typography.headlineMedium, color = Color.White)
            Spacer(Modifier.height(32.dp))
            Box(contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(200.dp)) {
                    drawCircle(color = Color.Cyan.copy(alpha = 0.3f), radius = 100.dp.toPx() * progress)
                    drawCircle(color = Color.Cyan, radius = 100.dp.toPx() * progress, style = Stroke(4.dp.toPx()))
                }
                Text("${(progress * 100).toInt()}%", color = Color.White)
            }
            Spacer(Modifier.height(48.dp))
            Button(onClick = onDismiss) { Text("Finish") }
        }
    }
}

@Composable
fun TypingIndicator(mood: SwaraMood, thought: String = "Swara is thinking...", particles: List<EmojiParticle> = emptyList(), onParticleEnd: (EmojiParticle) -> Unit = {}) {
    Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(contentAlignment = Alignment.Center) {
            AnimateSwaraIcon(mood, modifier = Modifier.size(24.dp))
            particles.forEach { particle ->
                FloatingEmoji(particle = particle, onAnimationEnd = { onParticleEnd(particle) })
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(thought, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
    }
}

@Composable
fun ChatBubble(msg: Message, particles: List<EmojiParticle> = emptyList(), onParticleEnd: (EmojiParticle) -> Unit = {}) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = if (msg.isUser) Alignment.CenterEnd else Alignment.CenterStart) {
        Row(verticalAlignment = Alignment.Bottom) {
            if (!msg.isUser) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(end = 4.dp)) {
                    AnimateSwaraIcon(msg.mood, modifier = Modifier.size(24.dp))
                    particles.forEach { particle ->
                        FloatingEmoji(particle = particle, onAnimationEnd = { onParticleEnd(particle) })
                    }
                }
            }
            Surface(
                color = if (msg.isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (msg.isUser) 16.dp else 0.dp,
                    bottomEnd = if (msg.isUser) 0.dp else 16.dp
                )
            ) {
                Text(
                    text = msg.text,
                    modifier = Modifier.padding(12.dp),
                    color = if (msg.isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            }
        }
    }
}
