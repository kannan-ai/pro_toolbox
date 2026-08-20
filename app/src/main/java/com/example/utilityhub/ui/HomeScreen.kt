package com.example.utilityhub.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.utilityhub.navigation.Screen
import com.example.utilityhub.navigation.allScreens
import com.example.utilityhub.ui.components.TutorialOverlay
import com.example.utilityhub.ui.components.TutorialStep
import com.example.utilityhub.ui.theme.*

@Composable
fun HomeScreen(
    menuMode: String,
    onToggleMenuMode: (String) -> Unit,
    onNavigate: (String) -> Unit,
    hasSeenTutorial: Boolean,
    onMarkTutorialSeen: () -> Unit,
    hideComingSoon: Boolean,
    historyViewModel: HistoryViewModel
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var showTutorialManual by remember { mutableStateOf(false) }
    var showComingSoonDialog by remember { mutableStateOf<String?>(null) }
    
    val history by historyViewModel.allHistory.collectAsState()
    var swaraPrediction by remember { mutableStateOf<com.example.utilityhub.features.support.SwaraResponse?>(null) }

    val showTutorial = !hasSeenTutorial || showTutorialManual

    // Semantic Ghosting Prediction
    LaunchedEffect(history) {
        swaraPrediction = com.example.utilityhub.features.support.SwaraEngine.getPredictiveGhosting(history)
    }

    val filteredScreens = remember(searchQuery, menuMode, hideComingSoon) {
        val baseList = if (menuMode == "ADVANCED") {
            mutableListOf(Screen.MediaStudio, Screen.VideoPlayer, Screen.MusicPlayer, Screen.TextStudio, Screen.SystemHealth, Screen.FileTransfer)
        } else {
            listOf(Screen.Currency, Screen.QR, Screen.Measurement, Screen.Password, Screen.QuickCalc)
        }

        if (searchQuery.isBlank()) {
            baseList
        } else {
            allScreens.filter { it.route.contains(searchQuery, ignoreCase = true) && it.route != "home" }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // --- 4. Semantic Ghosting: Swara Proactive Pulse ---
        AnimatedVisibility(
            visible = swaraPrediction != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            swaraPrediction?.let { prediction ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { 
                        // Automatically navigate based on prediction context
                        if (prediction.text.contains("Translator")) onNavigate("text_studio")
                        if (prediction.text.contains("Price Hub")) onNavigate("smart_price_hub")
                        swaraPrediction = null
                    },
                    colors = CardDefaults.cardColors(containerColor = PrimaryAmber.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, PrimaryAmber.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(color = PrimaryAmber, shape = CircleShape, modifier = Modifier.size(40.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.AutoAwesome, null, tint = Color.Black, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Swara's Suggestion", style = MaterialTheme.typography.labelSmall, color = PrimaryAmber, fontWeight = FontWeight.Bold)
                            Text(prediction.text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                        }
                        IconButton(onClick = { swaraPrediction = null }) {
                            Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                        }
                    }
                }
            }
        }

        // Navigation Hub (Unified Mode Selector)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
        ) {
            Column {
                LazyRow(
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    item { ModeTab("Basic Tools", menuMode == "BASIC", Modifier.widthIn(min = 100.dp)) { onToggleMenuMode("BASIC") } }
                    item { ModeTab("Advanced Suite", menuMode == "ADVANCED", Modifier.widthIn(min = 100.dp)) { onToggleMenuMode("ADVANCED") } }
                }
                
                // Highlight Banner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        if (menuMode == "BASIC") Icons.Default.PrivacyTip else Icons.Default.WorkspacePremium,
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (menuMode == "BASIC") "100% Private & Local Utilities" else "Pro Studio & Hardware Accelerated Suite",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Pro Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search pro tools...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            trailingIcon = { if (searchQuery.isNotEmpty()) IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Close, null) } },
            shape = RoundedCornerShape(24.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.Transparent
            )
        )

        // Section Title
        Text(
            text = if (searchQuery.isNotBlank()) "SEARCH RESULTS" else if (menuMode == "BASIC") "GENERAL UTILITIES" else "CREATIVE & POWER SUITE",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color.Gray.copy(alpha = 0.8f),
            letterSpacing = 1.2.sp,
            modifier = Modifier.padding(start = 12.dp)
        )

        // Tool Grid
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            modifier = Modifier.heightIn(max = 2000.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            userScrollEnabled = false
        ) {
            itemsIndexed(
                items = filteredScreens,
                span = { index, _ ->
                    val isLastItem = index == filteredScreens.size - 1
                    val isOrphan = isLastItem && filteredScreens.size % 2 != 0
                    GridItemSpan(if (isOrphan) 2 else 1)
                }
            ) { index, screen ->
                val isLastItem = index == filteredScreens.size - 1
                val isOrphan = isLastItem && filteredScreens.size % 2 != 0
                ProToolCard(screen, isFullWidth = isOrphan) { onNavigate(screen.route) }
            }
            
            // Featured Coming Soon Card
            if (searchQuery.isBlank() && menuMode == "ADVANCED" && !hideComingSoon) {
                item(span = { GridItemSpan(2) }) { 
                    ComingSoonCard(
                        title = "Voice Clone", 
                        subtitle = "AI Speech Engine",
                        icon = Icons.Default.SettingsVoice, 
                        color = CategoryCreative,
                        isFullWidth = true,
                        onClick = { showComingSoonDialog = "Voice Clone" }
                    ) 
                }
            }
        }

        // Help Button
        TextButton(
            onClick = { showTutorialManual = true },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Icon(Icons.AutoMirrored.Filled.HelpOutline, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text("Show App Tour", style = MaterialTheme.typography.labelMedium)
        }
        
        Spacer(modifier = Modifier.height(40.dp))
    }

    if (showComingSoonDialog != null) {
        AlertDialog(
            onDismissRequest = { showComingSoonDialog = null },
            modifier = Modifier.widthIn(max = 560.dp),
            title = { Text("Voice Clone is Coming Soon", fontWeight = FontWeight.Bold) },
            text = {
                Text("We are focusing exclusively on our next-generation AI speech synthesis. Train your unique voice profile with a quick audio sample and generate instant voiceovers for your media projects.")
            },
            confirmButton = {
                Button(
                    onClick = { 
                        showComingSoonDialog = null
                        Toast.makeText(context, "Notification set for release!", Toast.LENGTH_SHORT).show()
                    }, 
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryAmber),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Notify Me on Release")
                }
            },
            dismissButton = {
                TextButton(onClick = { showComingSoonDialog = null }) { Text("Close") }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (showTutorial) {
        TutorialOverlay(
            steps = listOf(
                TutorialStep(
                    "Welcome to Pro Toolbox",
                    "Your all-in-one private and local tool suite. No data ever leaves your device.",
                    Icons.Default.Security
                ),
                TutorialStep(
                    "Two Power Modes",
                    "Switch between Basic for utilities and Advanced for the Creative Suite.",
                    Icons.Default.Tune
                ),
                TutorialStep(
                    "Search & Discover",
                    "Find any tool instantly by typing its name in the search bar.",
                    Icons.Default.Search
                ),
                TutorialStep(
                    "Pin Your Favorites",
                    "Long press any tool to pin it to your custom dashboard (Coming soon).",
                    Icons.Default.PushPin
                )
            ),
            onDismiss = {
                onMarkTutorialSeen()
                showTutorialManual = false
            },
            onSkip = {
                onMarkTutorialSeen()
                showTutorialManual = false
            }
        )
    }
}

@Composable
fun ModeTab(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 12.dp)) {
            Text(
                label, 
                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold, 
                fontSize = 12.sp,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ProToolCard(screen: Screen, isFullWidth: Boolean = false, onClick: () -> Unit) {
    val subtitle = when (screen.route) {
        "text_studio" -> "Multi-Language"
        "text_to_audio" -> "TTS Synthesis"
        "currency" -> "Live Exchange"
        "quick_calc" -> "Percent & Shopping"
        "qr" -> "Generator & Scanner"
        "measurement" -> "Scientific & Daily"
        "password" -> "Secure Keys"
        "media_studio" -> "Production Hub"
        "support_bot" -> "Pro Assistant"
        "file_transfer" -> "Wireless Share"
        "creations" -> "Saved Gallery"
        "video_player" -> "Cinematic 4K"
        "music_player" -> "High-Fidelity"
        "system_health" -> "Battery & RAM"
        else -> ""
    }

    val (icon, color) = when (screen.route) {
        "text_studio" -> Icons.Default.Translate to CategoryText
        "text_to_audio" -> Icons.Default.AudioFile to CategoryText
        "currency" -> Icons.Default.CurrencyExchange to PrimaryAmber
        "quick_calc" -> Icons.Default.Percent to PrimaryAmber
        "qr" -> Icons.Default.QrCodeScanner to PrimaryAmber
        "measurement" -> Icons.Default.Straighten to PrimaryAmber
        "password" -> Icons.Default.Password to PrimaryAmber
        "media_studio" -> Icons.Default.Brush to CategoryCreative
        "support_bot" -> Icons.Default.SmartToy to CategorySystem
        "file_transfer" -> Icons.Default.RocketLaunch to CategoryCreative
        "creations" -> Icons.Default.Folder to CategoryCreative
        "video_player" -> Icons.Default.Movie to CategoryCreative
        "music_player" -> Icons.Default.Headset to CategoryCreative
        "system_health" -> Icons.Default.Dns to CategorySystem
        else -> Icons.Default.Apps to CategorySystem
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isFullWidth) Modifier.height(100.dp) else Modifier.aspectRatio(1.35f))
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (isFullWidth) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    Surface(
                        color = color.copy(alpha = 0.08f),
                        shape = CircleShape,
                        modifier = Modifier.size(52.dp),
                        border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(20.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(screen.titleRes),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (subtitle.isNotEmpty()) {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray,
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForwardIos,
                        null,
                        tint = Color.Gray.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize().padding(12.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        color = color.copy(alpha = 0.08f),
                        shape = CircleShape,
                        modifier = Modifier.size(44.dp),
                        border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        stringResource(screen.titleRes),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 14.sp
                    )
                    if (subtitle.isNotEmpty()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.alpha(0.7f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ComingSoonCard(title: String, subtitle: String, icon: ImageVector, color: Color, isFullWidth: Boolean = false, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isFullWidth) Modifier.height(100.dp) else Modifier.aspectRatio(1.35f))
            .alpha(0.8f)
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Surface(
                color = Color.Gray.copy(alpha = 0.8f),
                shape = RoundedCornerShape(bottomStart = 12.dp),
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Text(
                    "BETA", 
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    fontSize = 9.sp
                )
            }
            
            if (isFullWidth) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    Surface(
                        color = color.copy(alpha = 0.1f),
                        shape = CircleShape,
                        modifier = Modifier.size(52.dp),
                        border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(20.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize().padding(12.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        color = color.copy(alpha = 0.1f),
                        shape = CircleShape,
                        modifier = Modifier.size(44.dp),
                        border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        title,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 14.sp
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
