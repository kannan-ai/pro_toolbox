@file:OptIn(ExperimentalMaterial3Api::class)
@file:androidx.media3.common.util.UnstableApi
package com.example.utilityhub.features.media

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.graphics.Typeface
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.os.Bundle
import android.text.format.Formatter
import android.util.TypedValue
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import com.example.utilityhub.MainActivity
import com.example.utilityhub.ui.components.TutorialOverlay
import com.example.utilityhub.ui.components.TutorialStep
import com.example.utilityhub.ui.theme.PrimaryAmber
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

@ExperimentalMaterial3Api
@Composable
fun VideoPlayerScreen(
    isPipMode: Boolean = false,
    onSavePosition: (String, Long) -> Unit,
    onGetPosition: (String) -> Flow<Long>,
    onClearPosition: (String) -> Unit,
    playerSeekTime: Flow<Int>,
    subtitleFontSizeFlow: Flow<Int>,
    subtitleColorFlow: Flow<String>,
    subtitleOpacityFlow: Flow<Float>,
    subtitleEdgeTypeFlow: Flow<Int>,
    onSetSubtitleFontSize: (Int) -> Unit,
    onSetSubtitleColor: (String) -> Unit,
    onSetSubtitleOpacity: (Float) -> Unit,
    onSetSubtitleEdgeType: (Int) -> Unit,
    nightFilterEnabledFlow: Flow<Boolean>,
    onSetNightFilterEnabled: (Boolean) -> Unit,
    vividModeEnabledFlow: Flow<Boolean>,
    onSetVividModeEnabled: (Boolean) -> Unit,
    hasSeenTutorial: Boolean = true,
    onMarkTutorialSeen: () -> Unit = {},
    onNavigateBack: () -> Unit,
) {
    val context = LocalContext.current
    var videoList by remember { mutableStateOf<List<MediaFile>>(emptyList()) }
    var isLoading by remember { mutableStateOf(value = true) }
    var showTutorialManual by remember { mutableStateOf(value = false) }
    
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(value = false) }
    var sortOrder by remember { mutableStateOf("Date") }

    var activeVideoUri by rememberSaveable { mutableStateOf<String?>(null) }
    var activePlaylist by remember { mutableStateOf<List<MediaFile>>(emptyList()) }
    var selectedFolder by rememberSaveable { mutableStateOf<String?>(null) }

    val activeVideoIndex = remember(activeVideoUri, activePlaylist) {
        activePlaylist.indexOfFirst { it.uri.toString() == activeVideoUri }
    }

    LaunchedEffect(Unit) {
        isLoading = true
        withContext(Dispatchers.IO) {
            try {
                val fetchedVideos = fetchMedia(context, isVideo = true)
                withContext(Dispatchers.Main) {
                    videoList = fetchedVideos
                }
            } catch (e: Exception) {
                android.util.Log.e("VideoPlayer", "Error loading video library", e)
            } finally {
                withContext(Dispatchers.Main) {
                    isLoading = false
                }
            }
        }
    }

    if (activeVideoIndex != -1) {
        ProVideoPlayer(
            playlist = activePlaylist,
            startIndex = activeVideoIndex,
            isPipMode = isPipMode,
            onSavePosition = onSavePosition,
            onGetPosition = onGetPosition,
            onClearPosition = onClearPosition,
            playerSeekTime = playerSeekTime,
            subtitleFontSizeFlow = subtitleFontSizeFlow,
            subtitleColorFlow = subtitleColorFlow,
            subtitleOpacityFlow = subtitleOpacityFlow,
            subtitleEdgeTypeFlow = subtitleEdgeTypeFlow,
            onSetSubtitleFontSize = onSetSubtitleFontSize,
            onSetSubtitleColor = onSetSubtitleColor,
            onSetSubtitleOpacity = onSetSubtitleOpacity,
            onSetSubtitleEdgeType = onSetSubtitleEdgeType,
            nightFilterEnabledFlow = nightFilterEnabledFlow,
            onSetNightFilterEnabled = onSetNightFilterEnabled,
            vividModeEnabledFlow = vividModeEnabledFlow,
            onSetVividModeEnabled = onSetVividModeEnabled,
            hasSeenTutorial = hasSeenTutorial,
            onMarkTutorialSeen = onMarkTutorialSeen,
            onClose = {
                activeVideoUri = null
            },
        )
    } else {
        BackHandler(enabled = (isSearching || selectedFolder != null)) {
            if (isSearching) {
                isSearching = false
                searchQuery = ""
            } else if (selectedFolder != null) {
                selectedFolder = null
            }
        }

        Scaffold(
            topBar = {
                if (isSearching) {
                    TopAppBar(
                        title = {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Search videos...", color = Color.Gray) },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                textStyle = MaterialTheme.typography.bodyLarge,
                                singleLine = true,
                                trailingIcon = {
                                    IconButton(onClick = { searchQuery = ""; isSearching = false }) {
                                        Icon(Icons.Default.Close, null)
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent
                                )
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { isSearching = false; searchQuery = "" }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                            }
                        }
                    )
                } else {
                    var showSortMenu by remember { mutableStateOf(false) }
                    TopAppBar(
                        title = { 
                            if (selectedFolder == null) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = onNavigateBack) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                                    }
                                    Text("Local History", fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { selectedFolder = null }) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                                    }
                                    Text(selectedFolder!!, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        },
                        actions = {
                            IconButton(onClick = { onNavigateBack() }) {
                                Icon(Icons.Default.Home, "Home")
                            }
                            IconButton(onClick = { showTutorialManual = true }) {
                                Icon(Icons.AutoMirrored.Filled.HelpOutline, null)
                            }
                            IconButton(onClick = { isSearching = true }) {
                                Icon(Icons.Default.Search, null)
                            }
                            Box {
                                IconButton(onClick = { showSortMenu = true }) {
                                    Icon(Icons.AutoMirrored.Filled.Sort, null)
                                }
                                DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                                    DropdownMenuItem(text = { Text("Sort by Name") }, onClick = { sortOrder = "Name"; showSortMenu = false })
                                    DropdownMenuItem(text = { Text("Sort by Date") }, onClick = { sortOrder = "Date"; showSortMenu = false })
                                    DropdownMenuItem(text = { Text("Sort by Size") }, onClick = { sortOrder = "Size"; showSortMenu = false })
                                }
                            }
                        }
                    )
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding).fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PrimaryAmber)
                    }
                } else if (videoList.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No videos found", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                    }
                } else {
                    val filteredVideos = remember(videoList, searchQuery, sortOrder) {
                        videoList.asSequence()
                            .filter { it.name.contains(searchQuery, ignoreCase = true) }
                            .sortedWith { a, b ->
                                when (sortOrder) {
                                    "Name" -> a.name.compareTo(b.name, ignoreCase = true)
                                    "Size" -> b.size.compareTo(a.size)
                                    else -> b.date.compareTo(a.date)
                                }
                            }
                            .toList()
                    }
                    
                    if (selectedFolder == null) {
                        VideoLibraryMain(
                            allVideos = filteredVideos,
                            searchQuery = searchQuery,
                            onFolderClick = { selectedFolder = it }, 
                            onVideoClick = { video, list -> 
                                activePlaylist = list
                                activeVideoUri = video.uri.toString() 
                            }
                        )
                    } else {
                        val folderVideos = filteredVideos.filter { it.bucketName == selectedFolder }
                        VideoFolderDetail(
                            videos = folderVideos,
                            onVideoClick = { video -> 
                                activePlaylist = folderVideos
                                activeVideoUri = video.uri.toString() 
                            }
                        )
                    }
                }
            }

            if (!hasSeenTutorial || showTutorialManual) {
                TutorialOverlay(
                    steps = listOf(
                        TutorialStep("Smart Folders", "Your videos are automatically grouped by folders for easy navigation.", Icons.Default.Folder),
                        TutorialStep("Pro Player Gestures", "Swipe for Volume/Brightness, and double-tap to skip while playing.", Icons.Default.TouchApp),
                        TutorialStep("Instant X-Ray", "Check resolution and codecs instantly in the player info menu.", Icons.Default.Info)
                    ),
                    onDismiss = { onMarkTutorialSeen(); showTutorialManual = false },
                    onSkip = { onMarkTutorialSeen(); showTutorialManual = false }
                )
            }
        }
    }
}

@Composable
fun VideoLibraryMain(
    allVideos: List<MediaFile>, 
    searchQuery: String,
    onFolderClick: (String) -> Unit, 
    onVideoClick: (MediaFile, List<MediaFile>) -> Unit,
) {
    val folders = remember(allVideos) { 
        allVideos.groupBy { it.bucketName }
            .asSequence()
            .map { (name, list) -> 
                val hasRecent = list.any { (System.currentTimeMillis() / 1000 - it.date) < (86400 * 2) }
                Triple(name, list.size, hasRecent) 
            }
            .sortedBy { it.first }
            .toList()
    }

    val recentVideos = remember(allVideos) { allVideos.take(5) }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        if (recentVideos.isNotEmpty() && searchQuery.isEmpty()) {
            LazyRow(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(recentVideos) { video ->
                    Box(modifier = Modifier.width(160.dp).clickable { onVideoClick(video, recentVideos) }) {
                        Column {
                            Box(modifier = Modifier.fillMaxWidth().aspectRatio(16/9f).clip(RoundedCornerShape(8.dp))) {
                                // Blurred Thumbnail for Privacy
                                val blurRadius = if (android.os.Build.VERSION.SDK_INT >= 31) 15.dp else 0.dp
                                MediaThumbnail(
                                    uri = video.uri, 
                                    isVideo = true,
                                    modifier = Modifier.blur(blurRadius)
                                )
                                
                                // Privacy & Status Overlay
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            if (android.os.Build.VERSION.SDK_INT < 31) 
                                                Color.Black.copy(alpha = 0.85f) 
                                            else 
                                                Color.Black.copy(alpha = 0.3f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.History, 
                                        null, 
                                        tint = Color.White.copy(alpha = 0.7f), 
                                        modifier = Modifier.size(32.dp)
                                    )
                                }

                                Surface(
                                    color = Color.Black.copy(0.7f), 
                                    shape = RoundedCornerShape(4.dp), 
                                    modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp)
                                ) {
                                    Text(
                                        text = formatTime(video.duration), 
                                        color = Color.White, 
                                        fontSize = 10.sp, 
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(video.name, color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }

        Text("Folders", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

        val columns = 3
        val rows = (folders.size + columns - 1) / columns
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            for (i in 0 until rows) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    for (j in 0 until columns) {
                        val index = i * columns + j
                        if (index < folders.size) {
                            val (name, count, hasRecent) = folders[index]
                            FolderItem(name, count, hasRecent, Modifier.weight(1f)) { onFolderClick(name) }
                        } else {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FolderItem(name: String, count: Int, hasRecent: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(modifier = modifier.clickable { onClick() }, horizontalAlignment = Alignment.CenterHorizontally) {
        Box {
            Icon(Icons.Default.Folder, null, tint = Color.DarkGray, modifier = Modifier.size(64.dp))
            if (hasRecent) {
                Surface(color = Color.Red, shape = CircleShape, modifier = Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-4).dp).size(18.dp)) {
                    Box(contentAlignment = Alignment.Center) { Text("N", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(name, color = if (name == "Movies") PrimaryAmber else MaterialTheme.colorScheme.onBackground, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
        Text("$count videos", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f), fontSize = 10.sp)
    }
}

@Composable
fun VideoFolderDetail(videos: List<MediaFile>, onVideoClick: (MediaFile) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 160.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(videos) { video -> VideoGridItem(video) { onVideoClick(video) } }
    }
}

@Composable
fun ProVideoPlayer(
    playlist: List<MediaFile>, 
    startIndex: Int, 
    isPipMode: Boolean = false,
    onSavePosition: (String, Long) -> Unit,
    onGetPosition: (String) -> Flow<Long>,
    onClearPosition: (String) -> Unit,
    playerSeekTime: Flow<Int>,
    subtitleFontSizeFlow: Flow<Int>,
    subtitleColorFlow: Flow<String>,
    subtitleOpacityFlow: Flow<Float>,
    subtitleEdgeTypeFlow: Flow<Int>,
    onSetSubtitleFontSize: (Int) -> Unit,
    onSetSubtitleColor: (String) -> Unit,
    onSetSubtitleOpacity: (Float) -> Unit,
    onSetSubtitleEdgeType: (Int) -> Unit,
    nightFilterEnabledFlow: Flow<Boolean>,
    onSetNightFilterEnabled: (Boolean) -> Unit,
    vividModeEnabledFlow: Flow<Boolean>,
    onSetVividModeEnabled: (Boolean) -> Unit,
    hasSeenTutorial: Boolean = true,
    onMarkTutorialSeen: () -> Unit = {},
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    var showTutorialManual by remember { mutableStateOf(false) }
    val showTutorial = !hasSeenTutorial || showTutorialManual

    var currentIndex by remember { mutableIntStateOf(startIndex) }
    val currentFile = remember(currentIndex) { playlist[currentIndex] }
    
    var isLocked by rememberSaveable { mutableStateOf(false) }
    var isRotationLocked by rememberSaveable { mutableStateOf(false) }
    var isPlaying by rememberSaveable { mutableStateOf(false) }
    var showResumeDialog by remember { mutableStateOf(false) }
    var showVideoInfo by remember { mutableStateOf(false) }
    var savedPos by remember { mutableLongStateOf(0L) }
    var videoMetadata by remember { mutableStateOf<VideoMetadata?>(null) }
    var isQuickBoosting by remember { mutableStateOf(false) }
    var isControlNavigation by remember { mutableStateOf(false) }
    var lastPrevClickTime by remember { mutableLongStateOf(0L) }
    var isBackgroundPlayEnabled by rememberSaveable { mutableStateOf(false) }

    var isCapturingGif by remember { mutableStateOf(false) }
    var gifProgress by remember { mutableFloatStateOf(0f) }

    val subFontSize by subtitleFontSizeFlow.collectAsState(initial = 18)
    val subColor by subtitleColorFlow.collectAsState(initial = "White")
    val subOpacity by subtitleOpacityFlow.collectAsState(initial = 0.5f)
    val subEdgeType by subtitleEdgeTypeFlow.collectAsState(initial = 0)
    
    val isNightFilterEnabled by nightFilterEnabledFlow.collectAsState(initial = false)
    val isVividModeEnabled by vividModeEnabledFlow.collectAsState(initial = false)

    var player by remember { mutableStateOf<Player?>(null) }

    var isLooping by rememberSaveable { mutableStateOf(false) }
    var showSettingsDrawer by remember { mutableStateOf(false) }
    var showCastHub by remember { mutableStateOf(false) }
    var isZoomAndBoostEnabled by rememberSaveable { mutableStateOf(true) }

    DisposableEffect(context) {
        val sessionToken = SessionToken(context, ComponentName(context, MediaPlaybackService::class.java))
        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture.addListener(
            {
                val p = controllerFuture.get()
                if (p.mediaItemCount == 0 || p.getMediaItemAt(0).mediaId != playlist[0].uri.toString()) {
                    p.setMediaItems(playlist.map { MediaItem.Builder().setMediaId(it.uri.toString()).setUri(it.uri).build() })
                    p.seekTo(startIndex, 0L)
                    p.prepare()
                }
                player = p
            },
            MoreExecutors.directExecutor(),
        )
        onDispose { MediaController.releaseFuture(controllerFuture); player = null }
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, isBackgroundPlayEnabled) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && !isBackgroundPlayEnabled && !isPipMode) player?.pause()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(currentFile, player) {
        val p = player ?: return@LaunchedEffect
        if (isControlNavigation) { p.play(); isControlNavigation = false } else {
            val pos = onGetPosition(currentFile.uri.toString()).first()
            if (pos > 5000L) { savedPos = pos; showResumeDialog = true; p.pause() } else { p.play() }
        }
        scope.launch { videoMetadata = getVideoMetadata(context, currentFile.uri) }
    }

    LaunchedEffect(Unit) { MainActivity.isVideoPlaying = true }
    DisposableEffect(Unit) { onDispose { MainActivity.isVideoPlaying = false } }

    DisposableEffect(Unit) {
        val window = activity?.window
        if (window != null) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            val controller = WindowCompat.getInsetsController(window, view)
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            val window = activity?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    val seekTimeMillis by playerSeekTime.collectAsState(initial = 10)
    var isControlsVisible by rememberSaveable { mutableStateOf(true) }
    var currentPos by rememberSaveable { mutableLongStateOf(0L) }
    var duration by rememberSaveable { mutableLongStateOf(0L) }
    var resizeMode by rememberSaveable { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }
    var playbackSpeed by rememberSaveable { mutableFloatStateOf(1.0f) }
    var decoderMode by rememberSaveable { mutableStateOf("HW") }
    var sleepTimerMillis by remember { mutableLongStateOf(0L) }
    var volumeBoost by rememberSaveable { mutableFloatStateOf(1.0f) }
    var brightnessHUD by remember { mutableFloatStateOf(-1f) }
    var volumeHUD by remember { mutableFloatStateOf(-1f) }
    var seekHUD by remember { mutableLongStateOf(-1L) }
    var seekDeltaHUD by remember { mutableLongStateOf(0L) }
    var hudHideJob by remember { mutableStateOf<Job?>(null) }
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    var multiTapSide by remember { mutableStateOf<String?>(null) }
    var multiTapCount by remember { mutableIntStateOf(0) }
    var multiTapJob by remember { mutableStateOf<Job?>(null) }
    val zoomScale = remember { Animatable(1.0f) }
    val zoomOffset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }

    var initialSeekPos by remember { mutableLongStateOf(0L) }
    var lastSeekTime by remember { mutableLongStateOf(0L) }
    val setSeekParams: (Boolean) -> Unit = { fast ->
        (player as? MediaController)?.let { p ->
            val args = Bundle().apply { putBoolean("fast", fast) }
            p.sendCustomCommand(SessionCommand("SET_SEEK_PARAMETERS", Bundle.EMPTY), args)
        }
    }

    LaunchedEffect(showSettingsDrawer) { if (showSettingsDrawer) isControlsVisible = false }

    LaunchedEffect(isVividModeEnabled, player) {
        val p = player as? MediaController ?: return@LaunchedEffect
        val args = Bundle().apply { putBoolean("enabled", isVividModeEnabled) }
        p.sendCustomCommand(SessionCommand("SET_VIVID_MODE", Bundle.EMPTY), args)
    }

    LaunchedEffect(isLooping, player) { player?.repeatMode = if (isLooping) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF }

    LaunchedEffect(sleepTimerMillis, player) {
        if (sleepTimerMillis > 0) {
            while (sleepTimerMillis > 0) { delay(1000.milliseconds); sleepTimerMillis -= 1000 }
            player?.pause(); Toast.makeText(context, "Sleep timer expired", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(isLocked, isRotationLocked) {
        if (isLocked || isRotationLocked) {
            val currentOrientation = activity?.resources?.configuration?.orientation
            activity?.requestedOrientation = if (currentOrientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else { activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR }
    }

    LaunchedEffect(isControlsVisible, isPlaying) { if (isControlsVisible && isPlaying && !isLocked) { delay(3000.milliseconds); isControlsVisible = false } }

    LaunchedEffect(isPlaying, currentIndex, player) {
        val p = player ?: return@LaunchedEffect
        if (currentPos > 0 && p.currentPosition == 0L) p.seekTo(currentPos)
        while (true) {
            currentPos = p.currentPosition; duration = p.duration.coerceAtLeast(0L); currentIndex = p.currentMediaItemIndex
            if (currentPos > 2000) onSavePosition(playlist[currentIndex].uri.toString(), currentPos)
            delay(500.milliseconds)
        }
    }

    DisposableEffect(currentFile, player) {
        onDispose { val pos = player?.currentPosition ?: 0L; if (pos > 2000) onSavePosition(currentFile.uri.toString(), pos) }
    }

    DisposableEffect(player) {
        val p = player ?: return@DisposableEffect onDispose {}
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
            override fun onPlaybackStateChanged(state: Int) { if (state == Player.STATE_ENDED) onClearPosition(playlist[p.currentMediaItemIndex].uri.toString()) }
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) { currentIndex = p.currentMediaItemIndex; currentPos = 0L; p.seekTo(0L) }
        }
        p.addListener(listener)
        onDispose { p.removeListener(listener); activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
    }

    BackHandler(enabled = isLocked || showSettingsDrawer || showVideoInfo || showResumeDialog || showCastHub) {
        if (isLocked) Toast.makeText(context, "Unlock screen first", Toast.LENGTH_SHORT).show()
        else if (showSettingsDrawer) showSettingsDrawer = false
        else if (showVideoInfo) showVideoInfo = false
        else if (showResumeDialog) showResumeDialog = false
        else if (showCastHub) showCastHub = false
        else onClose()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(isLocked, isPlaying, playbackSpeed, seekTimeMillis, showSettingsDrawer, isZoomAndBoostEnabled) {
                if (isLocked) return@pointerInput
                coroutineScope {
                    launch {
                        detectTapGestures(
                            onTap = { if (!showSettingsDrawer) isControlsVisible = !isControlsVisible },
                            onDoubleTap = { offset ->
                                if (!showSettingsDrawer) {
                                    if (zoomScale.value > 1.0f && isZoomAndBoostEnabled) {
                                        scope.launch { launch { zoomScale.animateTo(1.0f) }; launch { zoomOffset.animateTo(Offset.Zero) } }
                                    } else {
                                        val isLeft = offset.x < size.width / 2
                                        val side = if (isLeft) "Left" else "Right"
                                        multiTapJob?.cancel()
                                        if (multiTapSide != side) { multiTapCount = 0; multiTapSide = side }
                                        multiTapCount++
                                        val newPos = if (isLeft) ((player?.currentPosition ?: 0L) - 10000L).coerceAtLeast(0) else ((player?.currentPosition ?: 0L) + 10000L).coerceAtMost(player?.duration ?: 0L)
                                        player?.seekTo(newPos)
                                        multiTapJob = scope.launch { delay(600.milliseconds); multiTapSide = null; multiTapCount = 0 }
                                    }
                                }
                            },
                            onLongPress = { if (isPlaying && !showSettingsDrawer && isZoomAndBoostEnabled) { isQuickBoosting = true; player?.setPlaybackSpeed(2.0f) } },
                            onPress = { tryAwaitRelease(); if (isQuickBoosting) { isQuickBoosting = false; player?.setPlaybackSpeed(playbackSpeed) } }
                        )
                    }
                    launch {
                        var accumulatedDragX = 0f
                        var accumulatedDragY = 0f
                        var lastHapticLevel = -1f
                        val handleOneFingerDrag: (Offset, Offset, IntSize) -> Unit = { pan, centroid, size ->
                            if (!isControlsVisible) {
                                val isLeft = centroid.x < size.width.toFloat() / 2
                                val isVertical = abs(pan.y) > abs(pan.x)
                                if (isVertical) {
                                    if (seekHUD == -1L) {
                                        accumulatedDragY += pan.y
                                        if (abs(accumulatedDragY) > 2f) {
                                            if (isLeft) {
                                                val lp = activity?.window?.attributes
                                                val currentBrightness = lp?.screenBrightness ?: -1f
                                                val delta = if (accumulatedDragY < 0) 0.01f else -0.01f
                                                val newBrightness = (if (currentBrightness < 0) 0.5f else currentBrightness + delta).coerceIn(0f, 1f)
                                                lp?.screenBrightness = newBrightness; activity?.window?.attributes = lp; brightnessHUD = newBrightness
                                                val level = (newBrightness * 10).toInt() / 10f
                                                if (level != lastHapticLevel) { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); lastHapticLevel = level }
                                            } else {
                                                var newHUDValue = 0f
                                                val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                                                if (accumulatedDragY < 0) {
                                                    if (maxVol > 0 && audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) < maxVol) {
                                                        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, 0)
                                                        newHUDValue = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxVol
                                                    } else { 
                                                        volumeBoost = (volumeBoost + 0.01f).coerceIn(1.0f, 1.5f)
                                                        newHUDValue = volumeBoost 
                                                    }
                                                } else {
                                                    if (volumeBoost > 1.0f) { 
                                                        volumeBoost = (volumeBoost - 0.01f).coerceAtLeast(1.0f)
                                                        newHUDValue = volumeBoost 
                                                    } else { 
                                                        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, 0)
                                                        if (maxVol > 0) newHUDValue = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxVol 
                                                    }
                                                }
                                                volumeHUD = newHUDValue.coerceIn(0f, 1.5f)
                                                player?.let { p ->
                                                    try {
                                                        // MediaController.setVolume expects 0..1. 
                                                        // For boost, we keep it at 1.0 and just show UI.
                                                        // If we want real boost, we need an AudioProcessor.
                                                        p.volume = if (volumeBoost > 1.0f) 1.0f else newHUDValue.coerceIn(0f, 1f)
                                                    } catch (_: Exception) {}
                                                }
                                                
                                                val level = (newHUDValue * 10).toInt() / 10f
                                                if (level != lastHapticLevel) { 
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    lastHapticLevel = level 
                                                }
                                            }
                                            accumulatedDragY = 0f; hudHideJob?.cancel(); hudHideJob = scope.launch { delay(1000.milliseconds); brightnessHUD = -1f; volumeHUD = -1f }
                                        }
                                    }
                                } else {
                                    if (brightnessHUD == -1f && volumeHUD == -1f) {
                                        if (seekHUD == -1L) {
                                            setSeekParams(true) // Start scrubbing
                                            initialSeekPos = player?.currentPosition ?: 0L
                                            accumulatedDragX = 0f
                                        }
                                        
                                        accumulatedDragX += pan.x
                                        // High sensitivity: 1px = 150ms. (100px = 15s).
                                        val deltaMs = (accumulatedDragX * 150).toLong()
                                        val newPos = (initialSeekPos + deltaMs).coerceIn(0L, duration)
                                        
                                        seekHUD = newPos
                                        seekDeltaHUD = deltaMs
                                        
                                        hudHideJob?.cancel()
                                        hudHideJob = scope.launch { 
                                            delay(150.milliseconds) // Detect gesture end
                                            if (seekHUD != -1L) {
                                                player?.seekTo(seekHUD)
                                                setSeekParams(false) // Restore exact seeking
                                            }
                                            delay(850.milliseconds)
                                            seekHUD = -1L
                                            seekDeltaHUD = 0L 
                                            accumulatedDragX = 0f
                                        }
                                    }
                                }
                            }
                        }
                        detectTransformGestures(
                            onGesture = { centroid, pan, zoom, _ ->
                            if (showSettingsDrawer && centroid.x > size.width / 2) return@detectTransformGestures
                            if (isZoomAndBoostEnabled && (zoom != 1f || pan != Offset.Zero)) {
                                if (zoom != 1f) {
                                    scope.launch {
                                        val newScale = (zoomScale.value * zoom).coerceIn(1.0f, 4.0f)
                                        if (newScale > 1.0f) {
                                            val maxOffsetX = (size.width * (newScale - 1f)) / 2; val maxOffsetY = (size.height * (newScale - 1f)) / 2
                                            zoomOffset.snapTo(Offset(x = (zoomOffset.value.x + pan.x * newScale).coerceIn(-maxOffsetX, maxOffsetX), y = (zoomOffset.value.y + pan.y * newScale).coerceIn(-maxOffsetY, maxOffsetY)))
                                        } else { zoomOffset.snapTo(Offset.Zero) }
                                        zoomScale.snapTo(newScale)
                                    }
                                } else if (zoomScale.value > 1f) {
                                    scope.launch {
                                        val maxOffsetX = (size.width * (zoomScale.value - 1f)) / 2; val maxOffsetY = (size.height * (zoomScale.value - 1f)) / 2
                                        zoomOffset.snapTo(Offset(x = (zoomOffset.value.x + pan.x * zoomScale.value).coerceIn(-maxOffsetX, maxOffsetX), y = (zoomOffset.value.y + pan.y * zoomScale.value).coerceIn(-maxOffsetY, maxOffsetY)))
                                    }
                                } else { handleOneFingerDrag(pan, centroid, size) }
                            } else if (!isZoomAndBoostEnabled) { if (zoomScale.value <= 1f) handleOneFingerDrag(pan, centroid, size) }
                        })
                    }
                }
            }
    ) {
        val p = player
        if (p != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(scaleX = zoomScale.value, scaleY = zoomScale.value, translationX = zoomOffset.value.x, translationY = zoomOffset.value.y)
            ) {
                AndroidView(
                    factory = { ctx -> PlayerView(ctx).apply { this.player = p; this.useController = false } },
                    update = { playerView ->
                        playerView.resizeMode = resizeMode; playerView.player = p
                        val colorMap = mapOf("White" to Color.White, "Yellow" to Color.Yellow, "Cyan" to Color.Cyan, "Green" to Color.Green)
                        val fgColor = colorMap[subColor] ?: Color.White; val bgColor = Color.Black.copy(alpha = subOpacity)
                        val captionStyle = CaptionStyleCompat(fgColor.toArgb(), bgColor.toArgb(), Color.Transparent.toArgb(), subEdgeType, Color.Black.toArgb(), Typeface.DEFAULT_BOLD)
                        playerView.subtitleView?.setStyle(captionStyle); playerView.subtitleView?.setFixedTextSize(TypedValue.COMPLEX_UNIT_SP, subFontSize.toFloat())
                        val paddingDp = if (isControlsVisible) 96.dp else 24.dp; val paddingPx = with(density) { paddingDp.roundToPx() }; playerView.subtitleView?.setPadding(0, 0, 0, paddingPx)
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 600.dp)
                        .align(Alignment.Center)
                )
                if (isNightFilterEnabled) { Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f))) }
            }
            Box(modifier = Modifier.fillMaxSize()) {
                AnimatedVisibility(visible = brightnessHUD != -1f, enter = fadeIn(), exit = fadeOut()) { SideHUDBar(value = brightnessHUD, icon = Icons.Default.BrightnessHigh, isLeft = true) }
                AnimatedVisibility(visible = volumeHUD != -1f, enter = fadeIn(), exit = fadeOut()) { 
                    SideHUDBar(value = volumeHUD, icon = if (volumeHUD > 1.0f) Icons.Default.ElectricBolt else Icons.AutoMirrored.Filled.VolumeUp, isLeft = false) 
                }
                
                // Hearing Safety Warning
                AnimatedVisibility(
                    visible = volumeHUD > 0.85f,
                    enter = fadeIn() + slideInVertically { it },
                    exit = fadeOut() + slideOutVertically { it },
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 120.dp)
                ) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f))
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, null, tint = Color.Red, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (volumeHUD > 1.0f) "Super Boost: High volume may damage speakers!" else "High Volume: May be harmful to your hearing",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                if (seekHUD != -1L) ScrubbingOverlay(time = seekHUD, delta = seekDeltaHUD)
                if (multiTapSide != null) DoubleTapIndicator(side = multiTapSide!!, seconds = multiTapCount * 10, onFinish = { })
                if (isQuickBoosting) QuickBoostBanner()
                if (isCapturingGif) {
                    Box(modifier = Modifier.fillMaxSize().padding(top = 100.dp), contentAlignment = Alignment.TopCenter) {
                        Surface(color = Color.Black.copy(alpha = 0.7f), shape = RoundedCornerShape(24.dp)) {
                            Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = PrimaryAmber, strokeWidth = 2.dp)
                                Spacer(Modifier.width(12.dp)); Text("Generating GIF: ${(gifProgress * 100).toInt()}%", color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
            AnimatedVisibility(visible = isControlsVisible && !isPipMode, enter = fadeIn(), exit = fadeOut()) {
                VideoControlsOverlay(
                    title = currentFile.name, isPlaying = isPlaying, isLocked = isLocked, isRotationLocked = isRotationLocked, 
                    currentPos = currentPos, duration = duration, playbackSpeed = playbackSpeed,
                    onBack = onClose, onPlayPause = { if (isPlaying) p.pause() else p.play() }, 
                    onSeek = { 
                        setSeekParams(true)
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastSeekTime > 50) {
                            p.seekTo(it)
                            lastSeekTime = currentTime
                        }
                    }, 
                    onSeekFinished = { setSeekParams(false) },
                    onLockToggle = { isLocked = !isLocked }, onRotationLockToggle = { isRotationLocked = !isRotationLocked }, 
                    onSpeedClick = { val speedList = listOf(1.0f, 2.0f, 3.0f, 4.0f, 5.0f); val idx = speedList.indexOf(playbackSpeed); val next = speedList[(idx + 1) % speedList.size]; playbackSpeed = next; p.setPlaybackSpeed(next) },
                    onCastClick = { showCastHub = true },
                    onAudioTrack = { showSettingsDrawer = true },
                    onSkipNext = { if (p.hasNextMediaItem()) { isControlNavigation = true; p.seekToNextMediaItem() } },
                    onSkipPrev = { val currentTime = System.currentTimeMillis(); val isDoubleClick = (currentTime - lastPrevClickTime) < 1000L; if (isDoubleClick || p.currentPosition < 3000) { if (p.hasPreviousMediaItem()) { isControlNavigation = true; p.seekToPreviousMediaItem() } else { p.seekTo(0L) } } else { p.seekTo(0L) }; lastPrevClickTime = currentTime },
                    onMoreOptionsToggle = { showSettingsDrawer = true }, 
                    onResizeToggle = { resizeMode = if (resizeMode == AspectRatioFrameLayout.RESIZE_MODE_FIT) AspectRatioFrameLayout.RESIZE_MODE_ZOOM else AspectRatioFrameLayout.RESIZE_MODE_FIT }
                )
            }
            AnimatedVisibility(visible = showSettingsDrawer, enter = slideInHorizontally { it } + fadeIn(), exit = slideOutHorizontally { it } + fadeOut()) {
                Box(modifier = Modifier.fillMaxSize().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { showSettingsDrawer = false }) {
                    VideoSettingsDrawer(
                        player = p, playbackSpeed = playbackSpeed, isLooping = isLooping, sleepTimerMillis = sleepTimerMillis, isVividModeEnabled = isVividModeEnabled, isNightFilterEnabled = isNightFilterEnabled, isZoomAndBoostEnabled = isZoomAndBoostEnabled, decoderMode = decoderMode, resizeMode = resizeMode, subtitleFontSize = subFontSize, subtitleColor = subColor, subtitleOpacity = subOpacity, subtitleEdgeType = subEdgeType,
                        onSpeedChange = { speed -> playbackSpeed = speed; p.setPlaybackSpeed(speed) }, onLoopToggle = { isLooping = !isLooping }, onSleepTimerChange = { sleepTimerMillis = it }, onSetNightFilterEnabled = onSetNightFilterEnabled, onSetVividModeEnabled = onSetVividModeEnabled, onZoomAndBoostToggle = { isZoomAndBoostEnabled = !isZoomAndBoostEnabled }, onDecoderToggle = { decoderMode = if (decoderMode == "HW") "SW" else "HW" }, onResizeToggle = { resizeMode = if (resizeMode == AspectRatioFrameLayout.RESIZE_MODE_FIT) AspectRatioFrameLayout.RESIZE_MODE_ZOOM else AspectRatioFrameLayout.RESIZE_MODE_FIT }, onSetSubtitleFontSize = onSetSubtitleFontSize, onSetSubtitleColor = onSetSubtitleColor, onSetSubtitleOpacity = onSetSubtitleOpacity, onSetSubtitleEdgeType = onSetSubtitleEdgeType,
                        onCastClick = { showCastHub = true },
                        onCapture = {
                            val pos = p.currentPosition
                            scope.launch {
                                val bitmap = withContext(Dispatchers.IO) { val retriever = MediaMetadataRetriever(); try { context.contentResolver.openFileDescriptor(currentFile.uri, "r")?.use { pfd -> retriever.setDataSource(pfd.fileDescriptor); retriever.getFrameAtTime(pos * 1000) } } catch (_: Exception) { null } finally { retriever.release() } }
                                if (bitmap != null) { saveBitmapToGallery(context, bitmap, "Screenshot_${System.currentTimeMillis()}.jpg"); Toast.makeText(context, "Screenshot Saved!", Toast.LENGTH_SHORT).show() }
                            }
                        },
                        onInfoToggle = { showVideoInfo = true }, onPiP = { if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) { activity?.enterPictureInPictureMode(android.app.PictureInPictureParams.Builder().build()) }; showSettingsDrawer = false },
                        onShowTutorial = { showTutorialManual = true; showSettingsDrawer = false }, onDismiss = { showSettingsDrawer = false }, modifier = Modifier.align(Alignment.CenterEnd)
                    )
                }
            }
            if (showVideoInfo && videoMetadata != null) VideoInfoDialog(videoMetadata!!, onDismiss = { showVideoInfo = false })
            if (showCastHub) CastHubDialog(onDismiss = { showCastHub = false })
            if (showTutorial) {
                TutorialOverlay(
                    steps = listOf(
                        TutorialStep("Vertical Swipes", "Swipe up/down on the LEFT for Brightness, and on the RIGHT for Volume.", Icons.Default.SwapVert),
                        TutorialStep("Horizontal Seek", "Swipe LEFT or RIGHT anywhere to seek through the video duration.", Icons.Default.SwapHoriz),
                        TutorialStep("Double Tap", "Double tap on either side to jump forward or backward by the time set in Settings.", Icons.Default.TouchApp),
                        TutorialStep("Volume Boost", "Swipe up past 100% volume to enable 200% Super Boost mode.", Icons.Default.ElectricBolt)
                    ), onDismiss = { onMarkTutorialSeen(); showTutorialManual = false }, onSkip = { onMarkTutorialSeen(); showTutorialManual = false }
                )
            }
            if (showResumeDialog) {
                AlertDialog(onDismissRequest = { showResumeDialog = false },
            modifier = Modifier.widthIn(max = 560.dp),
            title = { Text("Resume Playback?") }, text = { Text("Continue watching from ${formatTime(savedPos)}?") },
                    confirmButton = { Button(onClick = { p.seekTo(savedPos); p.play(); showResumeDialog = false }) { Text("Resume") } },
                    dismissButton = { TextButton(onClick = { onClearPosition(currentFile.uri.toString()); p.seekTo(0L); p.play(); showResumeDialog = false }) { Text("Start Over") } }
                )
            }
        }
    }
}

@Composable
fun QuickBoostBanner() {
    Box(modifier = Modifier.fillMaxSize().padding(top = 80.dp), contentAlignment = Alignment.TopCenter) {
        Surface(color = Color.Black.copy(alpha = 0.6f), shape = CircleShape, border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryAmber.copy(alpha = 0.5f))) {
            Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.FastForward, null, tint = PrimaryAmber, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp)); Text("2.0X BOOST >>", color = Color.White, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun DoubleTapIndicator(side: String, seconds: Int, onFinish: () -> Unit) {
    var visible by remember { mutableStateOf(value = true) }
    val infiniteTransition = rememberInfiniteTransition(label = "arrows")
    val arrowOffset by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 20f, animationSpec = infiniteRepeatable(animation = tween(600), repeatMode = RepeatMode.Restart), label = "arrowOffset")
    LaunchedEffect(side, seconds) { visible = true; delay(800.milliseconds); visible = false; onFinish() }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = if (side == "Left") Alignment.CenterStart else Alignment.CenterEnd) {
        AnimatedVisibility(visible = visible, enter = fadeIn() + expandHorizontally(), exit = fadeOut() + shrinkHorizontally()) {
            Box(modifier = Modifier.fillMaxHeight().width(180.dp).background(Brush.horizontalGradient(colors = if (side == "Left") listOf(Color.White.copy(0.15f), Color.Transparent) else listOf(Color.Transparent, Color.White.copy(0.15f)))), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(modifier = Modifier.offset(x = if (side == "Left") (-arrowOffset).dp else arrowOffset.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (side == "Left") Icon(Icons.Default.KeyboardDoubleArrowLeft, null, tint = Color.White, modifier = Modifier.size(40.dp)) else Icon(Icons.Default.KeyboardDoubleArrowRight, null, tint = Color.White, modifier = Modifier.size(40.dp))
                    }
                    Spacer(Modifier.height(8.dp)); Text(if (side == "Left") "<< ${seconds}s" else "${seconds}s >>", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                }
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    val path = Path(); if (side == "Left") { path.moveTo(0f, 0f); path.quadraticTo(size.width * 0.8f, size.height / 2, 0f, size.height) } else { path.moveTo(size.width, 0f); path.quadraticTo(size.width * 0.2f, size.height / 2, size.width, size.height) }
                    drawPath(path = path, color = Color.White.copy(alpha = 0.1f), style = Fill); path.close()
                }
            }
        }
    }
}

@Composable
fun ScrubbingOverlay(time: Long, delta: Long) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Surface(color = Color.Black.copy(0.7f), shape = RoundedCornerShape(12.dp)) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(formatTime(time), color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                val prefix = if (delta >= 0) "+" else ""; Text("$prefix${formatTime(abs(delta))}", color = if (delta >= 0) Color.Green else Color.Red, fontSize = 18.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun SideHUDBar(value: Float, icon: ImageVector, isLeft: Boolean) {
    val alignment = if (isLeft) Alignment.CenterStart else Alignment.CenterEnd; val paddingValues = if (isLeft) PaddingValues(start = 24.dp) else PaddingValues(end = 24.dp)
    Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = alignment) {
        Surface(color = Color.Black.copy(alpha = 0.5f), shape = RoundedCornerShape(32.dp), modifier = Modifier.width(48.dp).height(200.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))) {
            Column(modifier = Modifier.fillMaxSize().padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
                Box(modifier = Modifier.width(6.dp).weight(1f).clip(CircleShape).background(Color.White.copy(alpha = 0.2f))) {
                    val progress = if (value > 1.0f) (value - 1.0f) else value; val barColor = if (value > 1.0f) Color.Cyan else PrimaryAmber
                    Box(modifier = Modifier.fillMaxWidth().fillMaxHeight(progress.coerceIn(0f, 1f)).align(Alignment.BottomCenter).background(barColor))
                }
                Text(text = "${(value * 100).toInt()}%", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
fun VideoInfoDialog(meta: VideoMetadata, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.widthIn(max = 560.dp),
        title = { Text("Video X-Ray") },
        text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            InfoRow("Resolution", "${meta.width}x${meta.height}"); InfoRow("Codec", meta.codec)
            InfoRow("Size", Formatter.formatShortFileSize(LocalContext.current, meta.size)); InfoRow("Duration", formatTime(meta.duration))
        }
    }, confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } })
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = Color.Gray); Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SettingsCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(), 
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), 
        shape = RoundedCornerShape(16.dp), 
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) { content() }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun VideoControlsOverlay(
    title: String, isPlaying: Boolean, isLocked: Boolean, isRotationLocked: Boolean, 
    currentPos: Long, duration: Long, playbackSpeed: Float,
    onBack: () -> Unit, onPlayPause: () -> Unit, 
    onSeek: (Long) -> Unit, 
    onSeekFinished: () -> Unit,
    onLockToggle: () -> Unit, onRotationLockToggle: () -> Unit,
    onSpeedClick: () -> Unit,
    onCastClick: () -> Unit,
    onAudioTrack: () -> Unit, onSkipNext: () -> Unit, onSkipPrev: () -> Unit,
    onMoreOptionsToggle: () -> Unit, onResizeToggle: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }; val isDragged by interactionSource.collectIsDraggedAsState()
    val thumbSize by animateDpAsState(if (isDragged) 20.dp else 12.dp, label = "thumbSize")
    var unlockTapCount by remember { mutableIntStateOf(0) }; var showUnlockPrompt by remember { mutableStateOf(false) }
    LaunchedEffect(unlockTapCount) { if (unlockTapCount > 0) { showUnlockPrompt = true; delay(2000.milliseconds); unlockTapCount = 0; showUnlockPrompt = false } }
    Box(modifier = Modifier.fillMaxSize()) {
        if (isLocked) {
            Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.TopStart) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(onClick = { unlockTapCount++; if (unlockTapCount >= 2) { onLockToggle(); unlockTapCount = 0 } }, color = Color.Black.copy(alpha = 0.4f), shape = CircleShape, modifier = Modifier.size(50.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))) {
                        Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Lock, null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(24.dp)) }
                    }
                    AnimatedVisibility(visible = showUnlockPrompt) { Text("Tap again to unlock", color = Color.White, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 8.dp)) }
                }
            }
        }
        AnimatedVisibility(
            visible = !isLocked,
            enter = fadeIn(tween(300)) + slideInVertically(animationSpec = tween(300), initialOffsetY = { -it }),
            exit = fadeOut(tween(300)) + slideOutVertically(animationSpec = tween(300), targetOffsetY = { -it }),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = 600.dp)
        ) {
            Column {
                Box {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp).background(Brush.verticalGradient(listOf(Color.Black.copy(0.7f), Color.Transparent))))
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) { Icon(Icons.Default.ExpandMore, null, tint = Color.White) }
                        Text(title, color = Color.White, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        Row(verticalAlignment = Alignment.CenterVertically) { 
                            IconButton(onClick = onCastClick) { Icon(Icons.Default.Cast, null, tint = Color.White) }
                            IconButton(onClick = onAudioTrack) { Icon(Icons.Default.Audiotrack, null, tint = Color.White) }
                            IconButton(onClick = onMoreOptionsToggle) { Icon(Icons.Default.MoreVert, null, tint = Color.White) } 
                        }
                    }
                }
                Row(modifier = Modifier.padding(start = 16.dp, top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuickActionPill(text = "${playbackSpeed.toInt()}X", icon = Icons.Default.Speed, onClick = onSpeedClick)
                    QuickActionPill(text = "Fit", icon = Icons.Default.AspectRatio, onClick = onResizeToggle)
                    if (isRotationLocked) QuickActionPill(text = "Locked", icon = Icons.Default.ScreenLockRotation, onClick = onRotationLockToggle)
                }
            }
        }
        AnimatedVisibility(
            visible = !isLocked, 
            enter = fadeIn(tween(300)) + slideInVertically(animationSpec = tween(300), initialOffsetY = { it }), 
            exit = fadeOut(tween(300)) + slideOutVertically(animationSpec = tween(300), targetOffsetY = { it }), 
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .widthIn(max = 600.dp)
        ) {
            Box(contentAlignment = Alignment.BottomCenter) {
                Box(modifier = Modifier.fillMaxWidth().height(180.dp).background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.7f)))))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp, start = 16.dp, end = 16.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Slider(
                            value = currentPos.toFloat(), 
                            onValueChange = { onSeek(it.toLong()) }, 
                            onValueChangeFinished = { onSeekFinished() },
                            valueRange = 0f..duration.toFloat().coerceAtLeast(1f), 
                            interactionSource = interactionSource, 
                            thumb = { Box(modifier = Modifier.size(thumbSize).shadow(if (isDragged) 12.dp else 0.dp, CircleShape, spotColor = PrimaryAmber).background(PrimaryAmber, CircleShape)) }, 
                            colors = SliderDefaults.colors(thumbColor = PrimaryAmber, activeTrackColor = PrimaryAmber, inactiveTrackColor = Color.White.copy(alpha = 0.3f)), 
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = formatTime(currentPos), color = Color.White, style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium))
                            Text(text = "-${formatTime(duration - currentPos)}", color = Color.White, style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onLockToggle) { Icon(if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen, null, tint = Color.White) }; IconButton(onClick = onRotationLockToggle) { Icon(if (isRotationLocked) Icons.Default.ScreenLockRotation else Icons.Default.ScreenRotation, null, tint = if (isRotationLocked) PrimaryAmber else Color.White) } }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                            IconButton(onClick = onSkipPrev) { Icon(Icons.Default.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(32.dp)) }
                            IconButton(onClick = onPlayPause, modifier = Modifier.size(64.dp)) { Icon(if (isPlaying) Icons.Default.PauseCircle else Icons.Default.PlayCircle, null, tint = Color.White, modifier = Modifier.size(64.dp)) }
                            IconButton(onClick = onSkipNext) { Icon(Icons.Default.SkipNext, null, tint = Color.White, modifier = Modifier.size(32.dp)) }
                        }
                        IconButton(onClick = onResizeToggle) { Icon(Icons.Default.AspectRatio, null, tint = Color.White) }
                    }
                }
            }
        }
    }
}

@Composable
fun QuickActionPill(text: String, icon: ImageVector, onClick: (() -> Unit)? = null) {
    Surface(color = Color.Black.copy(alpha = 0.5f), shape = RoundedCornerShape(16.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)), onClick = { onClick?.invoke() }, enabled = onClick != null) {
        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(14.dp)); Spacer(Modifier.width(4.dp)); 
            Text(
                text, 
                color = Color.White, 
                fontSize = 10.sp, 
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun VideoSettingsDrawer(
    player: Player, playbackSpeed: Float, isLooping: Boolean, sleepTimerMillis: Long, isVividModeEnabled: Boolean, isNightFilterEnabled: Boolean, isZoomAndBoostEnabled: Boolean, decoderMode: String, resizeMode: Int,
    subtitleFontSize: Int, subtitleColor: String, subtitleOpacity: Float, subtitleEdgeType: Int,
    onSpeedChange: (Float) -> Unit, onLoopToggle: () -> Unit, onSleepTimerChange: (Long) -> Unit, onSetNightFilterEnabled: (Boolean) -> Unit, onSetVividModeEnabled: (Boolean) -> Unit, onZoomAndBoostToggle: () -> Unit,
    onCastClick: () -> Unit,
    onDecoderToggle: () -> Unit, onResizeToggle: () -> Unit, onSetSubtitleFontSize: (Int) -> Unit, onSetSubtitleColor: (String) -> Unit, onSetSubtitleOpacity: (Float) -> Unit, onSetSubtitleEdgeType: (Int) -> Unit,
    onCapture: () -> Unit, onInfoToggle: () -> Unit, onPiP: () -> Unit, onShowTutorial: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier
) {
    var currentPanel by remember { mutableStateOf("Main") }
    Box(modifier = modifier.fillMaxHeight().width(320.dp).background(color = Color.Black.copy(alpha = 0.78f), shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)).pointerInput(Unit) { detectTapGestures { } }.pointerInput(Unit) { detectDragGestures { _, _ -> } }) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) { if (currentPanel != "Main") { IconButton(onClick = { currentPanel = "Main" }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) } }; Text(text = when(currentPanel) { "Tracks" -> "Tracks & Sync"; "Subtitles" -> "Subtitle Style"; else -> "Video Settings" }, style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold) }
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null, tint = Color.White) }
            }
            when (currentPanel) {
                "Main" -> {
                    MainSettingsPanel(
                        sleepTimerMillis = sleepTimerMillis, isNightFilterEnabled = isNightFilterEnabled, isVividModeEnabled = isVividModeEnabled, isLooping = isLooping, isZoomAndBoostEnabled = isZoomAndBoostEnabled, decoderMode = decoderMode, resizeMode = resizeMode, playbackSpeed = playbackSpeed,
                        onResizeToggle = onResizeToggle, onSleepTimerChange = onSleepTimerChange, onSetNightFilterEnabled = onSetNightFilterEnabled, onSetVividModeEnabled = onSetVividModeEnabled, onZoomAndBoostToggle = onZoomAndBoostToggle,
                        onCastClick = onCastClick,
                        onSubtitleSettings = { currentPanel = "Subtitles" }, onTracksSettings = { currentPanel = "Tracks" }, onDecoderToggle = onDecoderToggle, onCapture = onCapture, onLoopToggle = onLoopToggle, onShowTutorial = onShowTutorial, onSpeedChange = onSpeedChange, onInfoToggle = onInfoToggle, onPiP = onPiP
                    )
                }
                "Tracks" -> { TracksPanel(player = player, onDismiss = { currentPanel = "Main" }) }
                "Subtitles" -> { SubtitleStylePanel(subtitleFontSize = subtitleFontSize, subtitleColor = subtitleColor, subtitleOpacity = subtitleOpacity, subtitleEdgeType = subtitleEdgeType, onSetSubtitleFontSize = onSetSubtitleFontSize, onSetSubtitleColor = onSetSubtitleColor, onSetSubtitleOpacity = onSetSubtitleOpacity, onSetSubtitleEdgeType = onSetSubtitleEdgeType) }
            }
        }
    }
}

@Composable
fun MainSettingsPanel(
    sleepTimerMillis: Long, isNightFilterEnabled: Boolean, isVividModeEnabled: Boolean, isLooping: Boolean, isZoomAndBoostEnabled: Boolean, decoderMode: String, resizeMode: Int, playbackSpeed: Float,
    onResizeToggle: () -> Unit, onSleepTimerChange: (Long) -> Unit, onSetNightFilterEnabled: (Boolean) -> Unit, onSetVividModeEnabled: (Boolean) -> Unit, onZoomAndBoostToggle: () -> Unit,
    onCastClick: () -> Unit,
    onSubtitleSettings: () -> Unit, onTracksSettings: () -> Unit, onDecoderToggle: () -> Unit, onCapture: () -> Unit, onLoopToggle: () -> Unit, onShowTutorial: () -> Unit, onSpeedChange: (Float) -> Unit, onInfoToggle: () -> Unit, onPiP: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Box(Modifier.weight(1f)) { DrawerAction(icon = Icons.Default.AspectRatio, label = "Aspect", active = resizeMode == AspectRatioFrameLayout.RESIZE_MODE_ZOOM, onClick = onResizeToggle) }
                Box(Modifier.weight(1f)) { DrawerAction(icon = Icons.Default.Timer, label = "Sleep", active = sleepTimerMillis > 0, onClick = { onSleepTimerChange(if(sleepTimerMillis > 0) 0 else 15.minutes.inWholeMilliseconds) }) }
                Box(Modifier.weight(1f)) { DrawerAction(icon = Icons.Default.Bedtime, label = "Night", active = isNightFilterEnabled, onClick = { onSetNightFilterEnabled(!isNightFilterEnabled) }) }
                Box(Modifier.weight(1f)) { DrawerAction(icon = Icons.Default.Flare, label = "Vivid", active = isVividModeEnabled, onClick = { onSetVividModeEnabled(!isVividModeEnabled) }) }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Box(Modifier.weight(1f)) { 
                    val speedList = listOf(1.0f, 2.0f, 3.0f, 4.0f, 5.0f); val speedText = "${playbackSpeed.toInt()}X"
                    DrawerAction(icon = Icons.Default.Speed, label = speedText, active = playbackSpeed > 1f, onClick = { val idx = speedList.indexOf(playbackSpeed); val next = speedList[(idx + 1) % speedList.size]; onSpeedChange(next) }) 
                }
                Box(Modifier.weight(1f)) { DrawerAction(icon = Icons.Default.Cast, label = "Cast", active = false, onClick = onCastClick) }
                Box(Modifier.weight(1f)) { DrawerAction(icon = Icons.Default.ZoomIn, label = "Zoom", active = isZoomAndBoostEnabled, onClick = onZoomAndBoostToggle) }
                Box(Modifier.weight(1f)) { DrawerAction(icon = Icons.Default.PictureInPicture, label = "PiP", active = false, onClick = onPiP) }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Box(Modifier.weight(1f)) { DrawerAction(icon = Icons.Default.ClosedCaption, label = "Subtitles", active = false, onClick = onSubtitleSettings) }
                Box(Modifier.weight(1f)) { DrawerAction(icon = Icons.Default.Audiotrack, label = "Tracks", active = false, onClick = onTracksSettings) }
                Box(Modifier.weight(1f)) { DrawerAction(icon = Icons.Default.Memory, label = "Decoder", active = decoderMode == "HW", onClick = onDecoderToggle) }
                Box(Modifier.weight(1f)) { DrawerAction(icon = Icons.Default.PhotoCamera, label = "Capture", active = false, onClick = onCapture) }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Box(Modifier.weight(1f)) { DrawerAction(icon = Icons.Default.Repeat, label = "Loop", active = isLooping, onClick = onLoopToggle) }
                Box(Modifier.weight(1f)) { DrawerAction(icon = Icons.AutoMirrored.Filled.HelpOutline, label = "Help", active = false, onClick = onShowTutorial) }
                Spacer(Modifier.weight(2f))
            }
        }
        HorizontalDivider(color = Color.White.copy(alpha = 0.1f)); Spacer(Modifier.weight(1f))
        TextButton(onClick = onInfoToggle, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.textButtonColors(contentColor = PrimaryAmber)) { Icon(Icons.Default.Info, null); Spacer(Modifier.width(8.dp)); Text("Video Details (X-Ray)") }
    }
}

@Composable
fun TracksPanel(player: Player, onDismiss: () -> Unit) {
    var trackType by remember { mutableStateOf("Audio") }; val tracks = player.currentTracks; val groups = tracks.groups.filter { if (trackType == "Audio") it.type == C.TRACK_TYPE_AUDIO else it.type == C.TRACK_TYPE_TEXT }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        LazyRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(
                    selected = trackType == "Audio",
                    onClick = { trackType = "Audio" },
                    label = {
                        Text(
                            "Audio",
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    modifier = Modifier.widthIn(min = 80.dp)
                )
            }
            item {
                FilterChip(
                    selected = trackType == "Subtitle",
                    onClick = { trackType = "Subtitle" },
                    label = {
                        Text(
                            "Subtitles",
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    modifier = Modifier.widthIn(min = 80.dp)
                )
            }
        }
        Text("Embedded Streams", style = MaterialTheme.typography.labelLarge, color = Color.Gray)
        SettingsCard {
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp)) {
                item { ListItem(headlineContent = { Text("Default / None") }, colors = ListItemDefaults.colors(containerColor = Color.Transparent, headlineColor = Color.White), modifier = Modifier.clickable { player.trackSelectionParameters = player.trackSelectionParameters.buildUpon().setTrackTypeDisabled(if (trackType == "Audio") C.TRACK_TYPE_AUDIO else C.TRACK_TYPE_TEXT, true).build(); onDismiss() }) }
                groups.forEach { group -> items(group.length) { i -> val format = group.getTrackFormat(i); val label = "${format.language ?: "Unknown"} (${format.label ?: "Stream $i"})"; ListItem(headlineContent = { Text(label) }, trailingContent = { if (group.isTrackSelected(i)) Icon(Icons.Default.Check, null, tint = PrimaryAmber) }, colors = ListItemDefaults.colors(containerColor = Color.Transparent, headlineColor = Color.White), modifier = Modifier.clickable { player.trackSelectionParameters = player.trackSelectionParameters.buildUpon().setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, i)).setTrackTypeDisabled(if (trackType == "Audio") C.TRACK_TYPE_AUDIO else C.TRACK_TYPE_TEXT, false).build(); onDismiss() }) } }
            }
        }
    }
}

@Composable
fun SubtitleStylePanel(subtitleFontSize: Int, subtitleColor: String, subtitleOpacity: Float, subtitleEdgeType: Int, onSetSubtitleFontSize: (Int) -> Unit, onSetSubtitleColor: (String) -> Unit, onSetSubtitleOpacity: (Float) -> Unit, onSetSubtitleEdgeType: (Int) -> Unit) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { _ -> }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Button(onClick = { picker.launch("*/*") }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = PrimaryAmber)) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("Load External Subtitle (.srt)") }
        SettingsCard {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Column { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.FormatSize, null, tint = Color.White, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(12.dp)); Text("Font Size: ${subtitleFontSize}sp", color = Color.White, fontWeight = FontWeight.Bold) }; Slider(value = subtitleFontSize.toFloat(), onValueChange = { onSetSubtitleFontSize(it.toInt()) }, valueRange = 12f..32f, colors = SliderDefaults.colors(activeTrackColor = PrimaryAmber, thumbColor = PrimaryAmber)) }
                Column {
                    Text("Text Color", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val colors = listOf("White", "Yellow", "Cyan", "Green")
                        items(colors) { color ->
                            FilterChip(
                                selected = subtitleColor == color,
                                onClick = { onSetSubtitleColor(color) },
                                label = {
                                    Text(
                                        color,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryAmber.copy(alpha = 0.2f),
                                    selectedLabelColor = PrimaryAmber,
                                    labelColor = Color.White,
                                    containerColor = Color.White.copy(alpha = 0.05f)
                                )
                            )
                        }
                    }
                }
                Column { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Opacity, null, tint = Color.White, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(12.dp)); Text("Background Opacity: ${(subtitleOpacity * 100).toInt()}%", color = Color.White, fontWeight = FontWeight.Bold) }; Slider(value = subtitleOpacity, onValueChange = { onSetSubtitleOpacity(it) }, valueRange = 0f..1f, colors = SliderDefaults.colors(activeTrackColor = PrimaryAmber, thumbColor = PrimaryAmber)) }
                Column {
                    Text("Text Edge Style", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val edgeOptions = listOf("None" to CaptionStyleCompat.EDGE_TYPE_NONE, "Outline" to CaptionStyleCompat.EDGE_TYPE_OUTLINE, "Shadow" to CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW)
                        items(edgeOptions) { (label, edgeType) ->
                            FilterChip(
                                selected = subtitleEdgeType == edgeType,
                                onClick = { onSetSubtitleEdgeType(edgeType) },
                                label = {
                                    Text(
                                        label,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryAmber.copy(alpha = 0.2f),
                                    selectedLabelColor = PrimaryAmber,
                                    labelColor = Color.White,
                                    containerColor = Color.White.copy(alpha = 0.05f)
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DrawerAction(icon: ImageVector, label: String, active: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }) {
        Surface(color = if (active) PrimaryAmber.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f), shape = CircleShape, modifier = Modifier.size(48.dp), border = if (active) androidx.compose.foundation.BorderStroke(1.dp, PrimaryAmber) else null) {
            Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = if (active) PrimaryAmber else Color.White, modifier = Modifier.size(24.dp)) }
        }
        Spacer(Modifier.height(4.dp)); Text(label, color = Color.White, fontSize = 10.sp)
    }
}


@Composable
fun VideoGridItem(file: MediaFile, onClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { onClick() }) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(16/9f)) {
            MediaThumbnail(uri = file.uri, isVideo = true)
            Surface(color = Color.Black.copy(alpha = 0.7f), shape = RoundedCornerShape(4.dp), modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp)) {
                Text(text = formatTime(file.duration), color = Color.White, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp), fontSize = 10.sp)
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Text(text = file.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Icon(Icons.Default.MoreVert, null, modifier = Modifier.size(16.dp).padding(top = 2.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

