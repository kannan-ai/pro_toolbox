package com.example.utilityhub.features.media

import android.Manifest
import android.content.ComponentName
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.format.Formatter
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import androidx.media3.ui.PlayerView
import com.example.utilityhub.data.db.Playlist
import com.example.utilityhub.data.db.PlaylistDao
import com.example.utilityhub.data.db.PlaylistSongCrossRef
import com.example.utilityhub.ui.components.TutorialOverlay
import com.example.utilityhub.ui.components.TutorialStep
import com.example.utilityhub.ui.theme.PrimaryAmber
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

val EliteGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFFFFD700).copy(alpha = 0.2f), // Gold
        Color(0xFFFF8C00).copy(alpha = 0.1f)  // Dark Orange
    )
)

fun Modifier.glassmorphism(radius: Dp = 15.dp): Modifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    this.blur(radius)
} else {
    this.background(Color.White.copy(alpha = 0.05f))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicPlayerScreen(
    playlistDao: PlaylistDao,
    hasSeenTutorial: Boolean = true,
    onMarkTutorialSeen: () -> Unit = {},
    onNavigateBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var musicList by remember { mutableStateOf<List<MediaFile>>(emptyList()) }
    var isLoading by remember { mutableStateOf(value = true) }
    var activeMediaUri by remember { mutableStateOf<Uri?>(null) }
    var isPlayerExpanded by remember { mutableStateOf(value = false) }
    var showTutorialManual by remember { mutableStateOf(false) }

    // Navigation & Tabs
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Songs, 1: Artists, 2: Albums, 3: Folders
    var currentArtistDetail by remember { mutableStateOf<Artist?>(null) }
    var currentAlbumDetail by remember { mutableStateOf<Album?>(null) }
    var currentFolderDetail by remember { mutableStateOf<String?>(null) }

    val showTutorial = !hasSeenTutorial || showTutorialManual
    
    // Search & Sort states
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var sortOrder by remember { mutableStateOf("Date") } // "Name", "Date", "Size"
    var isLibraryOverflowExpanded by remember { mutableStateOf(false) }

    // Data lists
    var artists by remember { mutableStateOf<List<Artist>>(emptyList()) }
    var albums by remember { mutableStateOf<List<Album>>(emptyList()) }

    // UI states
    var isVideoMode by remember { mutableStateOf(false) }
    var showXRay by remember { mutableStateOf(false) }
    var showQueueSheet by remember { mutableStateOf(false) }
    var isLiked by remember { mutableStateOf(false) }
    var isFavorite by remember { mutableStateOf(false) }
    var isScanningVisuals by remember { mutableStateOf(false) }
    var showSettingsDrawer by remember { mutableStateOf(false) }
    var decoderMode by rememberSaveable { mutableStateOf("HW") }
    var isVividModeEnabled by rememberSaveable { mutableStateOf(false) }
    var showCastHub by remember { mutableStateOf(false) }
    
    var menuTrack by remember { mutableStateOf<MediaFile?>(null) }
    
    // Playlist states
    var showPlaylistDialog by remember { mutableStateOf(false) }
    var showAddToPlaylistDialog by remember { mutableStateOf<MediaFile?>(null) }
    val playlists by playlistDao.getAllPlaylists().collectAsState(initial = emptyList())

    // Permission Handling
    val requiredPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, requiredPermission) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        if (!isGranted) {
            Toast.makeText(context, "Audio permission is required to browse music.", Toast.LENGTH_SHORT).show()
        }
    }

    var player by remember { mutableStateOf<Player?>(null) }
    
    DisposableEffect(context) {
        val sessionToken = SessionToken(context, ComponentName(context, MediaPlaybackService::class.java))
        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        
        controllerFuture.addListener(
            {
                try {
                    player = controllerFuture.get()
                } catch (e: Exception) {
                    android.util.Log.e("MusicPlayer", "Failed to connect to MediaController", e)
                }
            },
            MoreExecutors.directExecutor(),
        )

        onDispose {
            MediaController.releaseFuture(controllerFuture)
            player = null
        }
    }

    BackHandler(enabled = (isPlayerExpanded || showQueueSheet || showXRay || isSearching || currentArtistDetail != null || currentAlbumDetail != null || currentFolderDetail != null || showCastHub)) {
        if (showCastHub) {
            showCastHub = false
        } else if (showQueueSheet) {
            showQueueSheet = false
        } else if (showXRay) {
            showXRay = false
        } else if (isPlayerExpanded) {
            isPlayerExpanded = false
        } else if (isSearching) {
            isSearching = false
            searchQuery = ""
        } else if (currentArtistDetail != null) {
            currentArtistDetail = null
        } else if (currentAlbumDetail != null) {
            currentAlbumDetail = null
        } else if (currentFolderDetail != null) {
            currentFolderDetail = null
        }
    }

    var isPlaying by remember { mutableStateOf(false) }
    var currentPos by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var isShuffleEnabled by remember { mutableStateOf(false) }
    var repeatMode by remember { mutableIntStateOf(Player.REPEAT_MODE_ALL) }
    var playbackState by remember { mutableIntStateOf(Player.STATE_IDLE) }
    var audioSessionId by remember { mutableIntStateOf(0) }

    val activeFile = remember(activeMediaUri, musicList) {
        musicList.find { it.uri == activeMediaUri }
    }

    val filteredMusic = remember(musicList, searchQuery, sortOrder) {
        musicList.asSequence()
            .filter { it.name.contains(searchQuery, ignoreCase = true) }
            .sortedWith { a, b ->
                when (sortOrder) {
                    "Name" -> a.name.compareTo(b.name, ignoreCase = true)
                    "Size" -> b.size.compareTo(a.size)
                    else -> b.date.compareTo(a.date) // Date
                }
            }
            .toList()
    }

    // Load music list
    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            isLoading = true
            withContext(Dispatchers.IO) {
                try {
                    val fetchedMusic = fetchMedia(context, isVideo = false)
                    val fetchedArtists = fetchArtists(context)
                    val fetchedAlbums = fetchAlbums(context)
                    
                    withContext(Dispatchers.Main) {
                        musicList = fetchedMusic
                        artists = fetchedArtists
                        albums = fetchedAlbums
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MusicPlayer", "Error loading audio library", e)
                } finally {
                    withContext(Dispatchers.Main) {
                        isLoading = false
                    }
                }
            }
        } else {
            isLoading = false
        }
    }

    // Sync position & state
    LaunchedEffect(player) {
        val p = player ?: return@LaunchedEffect
        while (true) {
            try {
                currentPos = p.currentPosition
                duration = p.duration.coerceAtLeast(0L)
                isPlaying = p.isPlaying
                isShuffleEnabled = p.shuffleModeEnabled
                repeatMode = p.repeatMode
                playbackState = p.playbackState
                
                // Get audio session ID from extras
                val extras = (p as? MediaController)?.sessionExtras
                val sid = extras?.getInt("audio_session_id", 0) ?: 0
                if (sid != 0) audioSessionId = sid
                
                val currentItem = p.currentMediaItem
                currentItem?.let {
                    if (it.mediaId.isNotBlank()) {
                        activeMediaUri = it.mediaId.toUri()
                    }
                }
            } catch (_: Exception) {
                // Controller might have disconnected
                break
            }
            
            delay(1000.milliseconds)
        }
    }

    // Explicit Play on new track
    LaunchedEffect(activeMediaUri) {
        currentPos = 0L // Reset position immediately on track change
        val p = player
        if (activeMediaUri != null && p != null && !p.isPlaying) {
            p.play()
        }
    }


    LaunchedEffect(isVividModeEnabled, player) {
        val p = player as? MediaController ?: return@LaunchedEffect
        val args = Bundle().apply { putBoolean("enabled", isVividModeEnabled) }
        p.sendCustomCommand(SessionCommand("SET_VIVID_MODE", Bundle.EMPTY), args)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Music Library View (Base Layer)
        Scaffold(
            topBar = {
                if (isSearching) {
                    TopAppBar(
                        title = {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Search songs...", color = Color.Gray) },
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
                                    unfocusedBorderColor = Color.Transparent
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
                    Column {
                        TopAppBar(
                            title = { 
                                val title = when {
                                    currentArtistDetail != null -> currentArtistDetail?.name ?: "Artist"
                                    currentAlbumDetail != null -> currentAlbumDetail?.name ?: "Album"
                                    currentFolderDetail != null -> currentFolderDetail ?: "Folder"
                                    else -> "Pro Music Hub"
                                }
                                Text(title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            },
                            navigationIcon = {
                                IconButton(onClick = {
                                    when {
                                        currentArtistDetail != null -> currentArtistDetail = null
                                        currentAlbumDetail != null -> currentAlbumDetail = null
                                        currentFolderDetail != null -> currentFolderDetail = null
                                        else -> onNavigateBack()
                                    }
                                }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                }
                            },
                            actions = {
                                IconButton(onClick = { isSearching = true }) { Icon(Icons.Default.Search, null) }
                                IconButton(onClick = { showPlaylistDialog = true }) { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, null) }
                                Box {
                                    IconButton(onClick = { showSortMenu = true }) { Icon(Icons.AutoMirrored.Filled.Sort, null) }
                                    DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                                        DropdownMenuItem(text = { Text("Sort by Name") }, onClick = { sortOrder = "Name"; showSortMenu = false })
                                        DropdownMenuItem(text = { Text("Sort by Date") }, onClick = { sortOrder = "Date"; showSortMenu = false })
                                        DropdownMenuItem(text = { Text("Sort by Size") }, onClick = { sortOrder = "Size"; showSortMenu = false })
                                    }
                                }
                                Box {
                                    IconButton(onClick = { isLibraryOverflowExpanded = true }) {
                                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                                    }
                                    DropdownMenu(
                                        expanded = isLibraryOverflowExpanded,
                                        onDismissRequest = { isLibraryOverflowExpanded = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Home") },
                                            leadingIcon = { Icon(Icons.Default.Home, null) },
                                            onClick = { onNavigateBack(); isLibraryOverflowExpanded = false }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Help") },
                                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.HelpOutline, null) },
                                            onClick = { showTutorialManual = true; isLibraryOverflowExpanded = false }
                                        )
                                    }
                                }
                            }
                        )
                        if (currentArtistDetail == null && currentAlbumDetail == null && currentFolderDetail == null) {
                            PrimaryTabRow(
                                selectedTabIndex = selectedTab,
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.primary,
                                indicator = { 
                                    TabRowDefaults.PrimaryIndicator(
                                        Modifier.tabIndicatorOffset(selectedTab),
                                        color = PrimaryAmber
                                    )
                                }
                            ) {
                                val tabs = listOf("Songs", "Artists", "Albums", "Folders")
                                tabs.forEachIndexed { index, title ->
                                    Tab(
                                        selected = selectedTab == index,
                                        onClick = { selectedTab = index },
                                        text = { 
                                            Text(
                                                title, 
                                                style = MaterialTheme.typography.labelMedium,
                                                maxLines = 1,
                                                softWrap = false,
                                                overflow = TextOverflow.Ellipsis
                                            ) 
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding).fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                if (!hasPermission) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Music library access needed", color = Color.Gray)
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = { permissionLauncher.launch(requiredPermission) }) {
                                Text("Grant Permission")
                            }
                        }
                    }
                } else if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = PrimaryAmber) }
                } else if (musicList.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No music found on device.", color = Color.Gray) }
                } else {
                    val detailActive = currentArtistDetail != null || currentAlbumDetail != null || currentFolderDetail != null
                    
                    if (detailActive) {
                        val filteredTracks = remember(musicList, currentArtistDetail, currentAlbumDetail, currentFolderDetail) {
                            musicList.filter { track ->
                                when {
                                    currentArtistDetail != null -> {
                                        val artistName = currentArtistDetail?.name ?: ""
                                        track.artist.equals(artistName, ignoreCase = true)
                                    }
                                    currentAlbumDetail != null -> {
                                        val albumName = currentAlbumDetail?.name ?: ""
                                        track.album.equals(albumName, ignoreCase = true)
                                    }
                                    currentFolderDetail != null -> {
                                        track.bucketName.equals(currentFolderDetail, ignoreCase = true)
                                    }
                                    else -> true
                                }
                            }
                        }
                        
                        TrackListDetail(
                            tracks = filteredTracks,
                            activeMediaUri = activeMediaUri,
                            onTrackClick = { index, tracks ->
                                player?.clearMediaItems()
                                val tracksToPlay = tracks
                                if (tracksToPlay.isNotEmpty()) {
                                    tracksToPlay.forEach { song ->
                                        player?.addMediaItem(
                                            MediaItem.Builder()
                                                .setMediaId(song.uri.toString())
                                                .setUri(song.uri)
                                                .setMediaMetadata(
                                                    androidx.media3.common.MediaMetadata.Builder()
                                                        .setTitle(song.name)
                                                        .setArtist(song.artist)
                                                        .build()
                                                )
                                                .build()
                                        )
                                    }
                                    val safeIndex = index.coerceIn(0, tracksToPlay.size - 1)
                                    player?.seekTo(safeIndex, 0L)
                                    player?.prepare()
                                    player?.play()
                                    activeMediaUri = tracksToPlay.getOrNull(safeIndex)?.uri
                                    isPlayerExpanded = true
                                }
                            },
                            onAddToPlaylist = { showAddToPlaylistDialog = it }
                        )
                    } else {
                        when (selectedTab) {
                            0 -> { // Songs
                                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                    if (playlists.isNotEmpty()) {
                                        item { Text("Your Playlists", style = MaterialTheme.typography.titleMedium, color = PrimaryAmber) }
                                        item {
                                            LazyRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                                items(playlists) { playlist ->
                                                    PlaylistCard(playlist) {
                                                        scope.launch {
                                                            val songUris = playlistDao.getSongsInPlaylist(playlist.id).first()
                                                            val playlistSongs = withContext(Dispatchers.Default) {
                                                                musicList.filter { songUris.contains(it.uri.toString()) }
                                                            }
                                                            if (playlistSongs.isNotEmpty()) {
                                                                player?.clearMediaItems()
                                                                playlistSongs.forEach { song ->
                                                                    player?.addMediaItem(
                                                                        MediaItem.Builder()
                                                                            .setMediaId(song.uri.toString())
                                                                            .setUri(song.uri)
                                                                            .setMediaMetadata(
                                                                                androidx.media3.common.MediaMetadata.Builder()
                                                                                    .setTitle(song.name)
                                                                                    .setArtist(song.artist)
                                                                                    .build()
                                                                            )
                                                                            .build()
                                                                    )
                                                                }
                                                                player?.prepare()
                                                                player?.play()
                                                                activeMediaUri = playlistSongs.firstOrNull()?.uri
                                                                isPlayerExpanded = true
                                                            } else {
                                                                Toast.makeText(context, "Playlist is empty", Toast.LENGTH_SHORT).show()
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    item { Text("All Songs (${filteredMusic.size})", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary) }
                                    itemsIndexed(filteredMusic) { index, file ->
                                        Box {
                                            MediaListItem(
                                                file = file,
                                                isPlaying = (file.uri == activeMediaUri),
                                                onMenuClick = { menuTrack = file }
                                            ) {
                                                player?.clearMediaItems()
                                                val tracksToPlay = filteredMusic
                                                tracksToPlay.forEach { song ->
                                                    player?.addMediaItem(
                                                        MediaItem.Builder()
                                                            .setMediaId(song.uri.toString())
                                                            .setUri(song.uri)
                                                            .setMediaMetadata(
                                                                androidx.media3.common.MediaMetadata.Builder()
                                                                    .setTitle(song.name)
                                                                    .setArtist(song.artist)
                                                                    .build()
                                                            )
                                                            .build()
                                                    )
                                                }
                                                if (index >= 0 && index < tracksToPlay.size) {
                                                    player?.seekTo(index, 0L)
                                                    player?.prepare()
                                                    player?.play()
                                                    activeMediaUri = file.uri
                                                    isPlayerExpanded = true
                                                }
                                            }
                                            
                                            DropdownMenu(
                                                expanded = menuTrack == file,
                                                onDismissRequest = { menuTrack = null }
                                            ) {
                                                DropdownMenuItem(
                                                    text = { Text("Add to Playlist") },
                                                    onClick = { 
                                                        showAddToPlaylistDialog = file
                                                        menuTrack = null
                                                    },
                                                    leadingIcon = { Icon(Icons.Default.PlaylistAdd, null) }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("Share") },
                                                    onClick = { 
                                                        shareFile(context, file)
                                                        menuTrack = null
                                                    },
                                                    leadingIcon = { Icon(Icons.Default.Share, null) }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("Details") },
                                                    onClick = { 
                                                        activeMediaUri = file.uri
                                                        isPlayerExpanded = true
                                                        showXRay = true
                                                        menuTrack = null
                                                    },
                                                    leadingIcon = { Icon(Icons.Default.Info, null) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            1 -> { // Artists
                                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(artists) { artist ->
                                        ListItem(
                                            headlineContent = { Text(artist.name, fontWeight = FontWeight.Bold) },
                                            supportingContent = { Text("${artist.albumCount} Albums • ${artist.songCount} Songs") },
                                            leadingContent = { 
                                                Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = CircleShape, modifier = Modifier.size(48.dp)) { 
                                                    Box(contentAlignment = Alignment.Center) { 
                                                        if (artist.representativeUri != null) {
                                                            MediaThumbnail(uri = artist.representativeUri, isVideo = false)
                                                        } else {
                                                            Icon(Icons.Default.Person, null) 
                                                        }
                                                    } 
                                                } 
                                            },
                                            modifier = Modifier.clickable { currentArtistDetail = artist }
                                        )
                                    }
                                }
                            }
                            2 -> { // Albums
                                LazyVerticalGrid(
                                    columns = GridCells.Adaptive(minSize = 160.dp),
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    items(albums) { album ->
                                        AlbumGridItem(album) { currentAlbumDetail = album }
                                    }
                                }
                            }
                            3 -> { // Folders
                                val folders = remember(musicList) { musicList.map { it.bucketName }.distinct().sorted() }
                                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(folders) { folder ->
                                        ListItem(
                                            headlineContent = { Text(folder, fontWeight = FontWeight.Bold) },
                                            supportingContent = { Text("${musicList.count { it.bucketName == folder }} Songs") },
                                            leadingContent = { Icon(Icons.Default.Folder, null, tint = PrimaryAmber) },
                                            modifier = Modifier.clickable { currentFolderDetail = folder }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(80.dp)) // Extra space for mini player
            }
        }

        // 2. Persistent Mini Player (Bottom Docked)
        if (activeMediaUri != null && activeFile != null && !isPlayerExpanded) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp, start = 8.dp, end = 8.dp)
            ) {
                MiniPlayer(
                    file = activeFile,
                    isPlaying = isPlaying,
                    currentPos = currentPos,
                    duration = duration,
                    onPlayPause = { if (isPlaying) player?.pause() else player?.play() },
                    onNext = { player?.seekToNext() },
                    onCastClick = { showCastHub = true },
                    onClick = { isPlayerExpanded = true }
                )
            }
        }

        // 3. Immersive Full Player (Overlay Layer)
        AnimatedVisibility(
            visible = (activeMediaUri != null) && (activeFile != null) && isPlayerExpanded,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut()
        ) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                Box(
                    modifier = Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            colors = listOf(PrimaryAmber.copy(alpha = 0.4f), Color.Black)
                        )
                    )
                )

                Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                    // Header Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { isPlayerExpanded = false }) {
                            Icon(Icons.Default.ExpandMore, null, tint = Color.White, modifier = Modifier.size(32.dp))
                        }
                        
                        Surface(
                            color = Color.White.copy(alpha = 0.1f),
                            shape = CircleShape,
                            modifier = Modifier.height(36.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
                                Surface(
                                    color = if (!isVideoMode) Color.White.copy(alpha = 0.2f) else Color.Transparent,
                                    shape = CircleShape,
                                    onClick = { isVideoMode = false }
                                ) {
                                    Text("Song", color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
                                }
                                Surface(
                                    color = if (isVideoMode) Color.White.copy(alpha = 0.2f) else Color.Transparent,
                                    shape = CircleShape,
                                    onClick = { 
                                        if (!isVideoMode) {
                                            isVideoMode = true 
                                            isScanningVisuals = true
                                            scope.launch {
                                                delay(2500.milliseconds)
                                                isScanningVisuals = false
                                            }
                                        }
                                    }
                                ) {
                                    Text("Video", color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
                                }
                            }
                        }

                        IconButton(onClick = { showTutorialManual = true }) {
                            Icon(Icons.AutoMirrored.Filled.HelpOutline, null, tint = Color.White)
                        }

                        IconButton(onClick = { showXRay = !showXRay }) {
                            Icon(Icons.Default.Info, null, tint = Color.White)
                        }

                        IconButton(onClick = { showSettingsDrawer = true }) {
                            Icon(Icons.Default.MoreVert, null, tint = Color.White)
                        }
                    }

                    Spacer(Modifier.weight(0.4f))

                    // Media Canvas
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 600.dp)
                            .align(Alignment.CenterHorizontally)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.DarkGray)
                    ) {
                        if (isVideoMode) {
                            AndroidView(
                                factory = { ctx -> PlayerView(ctx).apply { this.player = player; useController = false } },
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            activeFile?.let {
                                MediaThumbnail(uri = it.uri, isVideo = false, highRes = true)
                            }
                        }
                    }

                    Spacer(Modifier.weight(0.4f))


                    // Metadata Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            val displayName = remember(activeFile?.name) {
                                activeFile?.name?.substringBeforeLast(".")
                                    ?.replace("_", " - ")
                                    ?.replace("  ", " ")
                                    ?.trim() ?: "Unknown Track"
                            }
            
            Text(
                text = displayName,
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text("Local Pro Media", color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.bodyMedium)
        }
        IconButton(onClick = { isFavorite = !isFavorite }) {
            Icon(if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null, tint = if (isFavorite) Color.Red else Color.White, modifier = Modifier.size(32.dp))
        }
    }

                    Spacer(Modifier.height(24.dp))

                    // Timeline
                    Column {
                        val isSeekable = playbackState == Player.STATE_READY && duration > 0
                        Slider(
                            value = currentPos.toFloat(),
                            onValueChange = { player?.seekTo(it.toLong()) },
                            valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                            enabled = isSeekable,
                            colors = SliderDefaults.colors(
                                thumbColor = if (isSeekable) Color.White else Color.Transparent,
                                activeTrackColor = if (isSeekable) Color.White else Color.White.copy(0.2f),
                                inactiveTrackColor = Color.White.copy(alpha = 0.2f),
                                disabledThumbColor = Color.Transparent,
                                disabledActiveTrackColor = Color.White.copy(0.2f),
                                disabledInactiveTrackColor = Color.White.copy(0.1f)
                            ),
                            track = { sliderState ->
                                SliderDefaults.Track(
                                    sliderState = sliderState,
                                    colors = SliderDefaults.colors(
                                        activeTrackColor = Color.White,
                                        inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                                    ),
                                    thumbTrackGapSize = 0.dp,
                                    trackInsideCornerSize = 0.dp,
                                    drawStopIndicator = null
                                )
                            }
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = formatTime(currentPos),
                                color = Color.White.copy(0.6f),
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = formatTime(duration),
                                color = Color.White.copy(0.6f),
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // Premium Controls
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 600.dp)
                            .align(Alignment.CenterHorizontally), 
                        horizontalArrangement = Arrangement.SpaceBetween, 
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { player?.shuffleModeEnabled = !isShuffleEnabled }) { 
                            Icon(Icons.Default.Shuffle, null, tint = if (isShuffleEnabled) PrimaryAmber else Color.White.copy(0.7f)) 
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                            IconButton(onClick = { player?.seekToPrevious() }) { Icon(Icons.Default.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(42.dp)) }
                            Surface(
                                color = Color.White,
                                shape = CircleShape,
                                modifier = Modifier.size(72.dp).clickable { if (isPlaying) player?.pause() else player?.play() }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(42.dp)
                                    )
                                }
                            }
                            IconButton(onClick = { player?.seekToNext() }) { Icon(Icons.Default.SkipNext, null, tint = Color.White, modifier = Modifier.size(42.dp)) }
                        }
                        IconButton(onClick = { 
                            val nextMode = when(repeatMode) {
                                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                                else -> Player.REPEAT_MODE_OFF
                            }
                            player?.repeatMode = nextMode
                        }) { 
                            Icon(
                                if (repeatMode == Player.REPEAT_MODE_ONE) Icons.Default.RepeatOne else Icons.Default.Repeat, 
                                null, 
                                tint = if (repeatMode != Player.REPEAT_MODE_OFF) PrimaryAmber else Color.White.copy(0.7f)
                            ) 
                        }
                    }

                    Spacer(Modifier.height(32.dp))

                    // Interaction Ribbon
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        androidx.compose.animation.AnimatedVisibility(
                            visible = !isScanningVisuals,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    IconButton(onClick = { isLiked = !isLiked }) {
                                        Icon(if (isLiked) Icons.Default.ThumbUp else Icons.Default.ThumbUpOffAlt, null, tint = Color.White)
                                    }
                                    Text("1.2k", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
                                }
                                IconButton(onClick = { }) { Icon(Icons.AutoMirrored.Filled.Comment, null, tint = Color.White) }
                                IconButton(onClick = { showAddToPlaylistDialog = activeFile }) { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, null, tint = Color.White) }
                                IconButton(onClick = { showCastHub = true }) { Icon(Icons.Default.Cast, null, tint = Color.White) }
                                IconButton(onClick = { }) { Icon(Icons.Default.Share, null, tint = Color.White) }
                            }
                        }

                        androidx.compose.animation.AnimatedVisibility(
                            visible = isScanningVisuals,
                            enter = fadeIn() + slideInVertically { h -> -h },
                            exit = fadeOut() + slideOutVertically { h -> -h }
                        ) {
                            Surface(
                                color = PrimaryAmber,
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier.height(40.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = Color.Black,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        "Scanning for visuals...",
                                        color = Color.Black,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.weight(0.2f))

                    // Bottom Panel Trigger
                    Box(modifier = Modifier.fillMaxWidth().clickable { showQueueSheet = true }, contentAlignment = Alignment.Center) {
                        Surface(color = Color.White.copy(0.1f), shape = RoundedCornerShape(24.dp)) {
                            Text("Lyrics & Queue", color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp))
                        }
                    }
                }

                // HW/SW Settings Drawer
                AnimatedVisibility(
                    visible = showSettingsDrawer,
                    enter = slideInHorizontally { it } + fadeIn(),
                    exit = slideOutHorizontally { it } + fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { showSettingsDrawer = false }
                    ) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .fillMaxHeight()
                                .width(280.dp),
                            color = Color.Black.copy(alpha = 0.85f),
                            shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                        ) {
                            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Audio/Video HW", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                    IconButton(onClick = { showSettingsDrawer = false }) {
                                        Icon(Icons.Default.Close, null, tint = Color.White)
                                    }
                                }

                                Text("Hardware Acceleration", color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                                
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Box(Modifier.weight(1f)) {
                                        DrawerAction(
                                            icon = Icons.Default.Memory,
                                            label = "Decoder",
                                            active = decoderMode == "HW",
                                            onClick = { 
                                                decoderMode = if (decoderMode == "HW") "SW" else "HW"
                                                Toast.makeText(context, "Audio Decoder: $decoderMode", Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    }
                                    Box(Modifier.weight(1f)) {
                                        DrawerAction(
                                            icon = Icons.Default.Flare,
                                            label = "Vivid",
                                            active = isVividModeEnabled,
                                            onClick = { isVividModeEnabled = !isVividModeEnabled }
                                        )
                                    }
                                    Spacer(Modifier.weight(1f))
                                }
                                
                                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                                
                                Text("Pro Features Enabled", color = PrimaryAmber, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                // X-Ray Information
                if (showXRay && activeFile != null) {
                    Surface(modifier = Modifier.fillMaxSize(), color = Color.Black.copy(0.95f)) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { showXRay = false }) { Icon(Icons.Default.Close, null, tint = Color.White) }
                                Text("Pro X-Ray: Audio Details", color = Color.White, style = MaterialTheme.typography.titleLarge)
                            }
                            Spacer(Modifier.height(32.dp))
                            XRayItem("Format", activeFile.mimeType)
                            XRayItem("Sample Rate", "44.1 kHz (Studio Quality)")
                            XRayItem("Bitrate", "320 kbps")
                            XRayItem("Channel", "Stereo")
                            XRayItem("File Size", Formatter.formatShortFileSize(context, activeFile.size))
                        }
                    }
                }

                // Queue & Lyrics Sheet
                if (showQueueSheet) {
                    ModalBottomSheet(
                        onDismissRequest = { showQueueSheet = false },
                        modifier = Modifier.widthIn(max = 560.dp),
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().height(400.dp).padding(16.dp)) {
                            Text("Coming Up Next", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(16.dp))
                            LazyColumn(modifier = Modifier.weight(1f)) {
                                itemsIndexed(musicList) { index, song ->
                                    ListItem(
                                        headlineContent = { Text(song.name, color = if (song.uri == activeMediaUri) PrimaryAmber else MaterialTheme.colorScheme.onSurface) },
                                        leadingContent = { Box(Modifier.size(40.dp).clip(RoundedCornerShape(4.dp))) { MediaThumbnail(song.uri, false) } },
                                        trailingContent = { Icon(Icons.Default.Menu, null) },
                                        modifier = Modifier.clickable {
                                            player?.seekTo(index, 0L)
                                            player?.play()
                                        }
                                    )
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                            Text("Lyrics coming soon!", color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }

    if (showCastHub) CastHubDialog(onDismiss = { showCastHub = false })

    // Add to Playlist Dialog
    if (showAddToPlaylistDialog != null) {
        AlertDialog(
            onDismissRequest = { showAddToPlaylistDialog = null },
            modifier = Modifier.widthIn(max = 560.dp),
            title = { Text("Add to Playlist") },
            text = {
                LazyColumn {
                    items(playlists) { playlist ->
                        ListItem(
                            headlineContent = { Text(playlist.name) },
                            modifier = Modifier.clickable {
                                val trackToAdd = showAddToPlaylistDialog
                                if (trackToAdd != null) {
                                    scope.launch {
                                        playlistDao.addSongToPlaylist(PlaylistSongCrossRef(playlist.id, trackToAdd.uri.toString()))
                                        showAddToPlaylistDialog = null
                                        Toast.makeText(context, "Added to ${playlist.name}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
                    }
                    item {
                        ListItem(
                            headlineContent = { Text("+ Create New Playlist", color = PrimaryAmber) },
                            modifier = Modifier.clickable {
                                showPlaylistDialog = true
                                // Handled in the next dialog
                            }
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showAddToPlaylistDialog = null }) { Text("Cancel") } }
        )
    }

    // Create Playlist Dialog
    if (showPlaylistDialog) {
        var playlistName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPlaylistDialog = false },
            modifier = Modifier.widthIn(max = 560.dp),
            title = { Text("New Playlist") },
            text = {
                OutlinedTextField(
                    value = playlistName,
                    onValueChange = { playlistName = it },
                    label = { Text("Playlist Name") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (playlistName.isNotBlank()) {
                        scope.launch {
                            playlistDao.insertPlaylist(Playlist(name = playlistName))
                            showPlaylistDialog = false
                        }
                    }
                }) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showPlaylistDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showTutorial) {
        TutorialOverlay(
            steps = listOf(
                TutorialStep(
                    "Studio Playback",
                    "High-fidelity local playback with automatic visual scanning.",
                    Icons.Default.MusicNote
                ),
                TutorialStep(
                    "Custom Playlists",
                    "Organize your local tracks into custom collections easily.",
                    Icons.AutoMirrored.Filled.QueueMusic
                ),
                TutorialStep(
                    "Pro X-Ray",
                    "Check sample rates, bitrates, and file details in the info menu.",
                    Icons.Default.Info
                ),
                TutorialStep(
                    "Shuffle & Repeat",
                    "Use advanced playback modes to enjoy your music your way.",
                    Icons.Default.Shuffle
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
fun TrackListDetail(
    tracks: List<MediaFile>,
    activeMediaUri: Uri?,
    onTrackClick: (Int, List<MediaFile>) -> Unit,
    onAddToPlaylist: (MediaFile) -> Unit
) {
    var menuTrack by remember { mutableStateOf<MediaFile?>(null) }
    val context = LocalContext.current
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { if (tracks.isNotEmpty()) onTrackClick(0, tracks) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryAmber, contentColor = Color.Black)
                ) {
                    Icon(Icons.Default.PlayArrow, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Play All")
                }
                OutlinedButton(
                    onClick = { 
                        if (tracks.isNotEmpty()) {
                            val shuffled = tracks.shuffled()
                            onTrackClick(0, shuffled)
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Shuffle, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Shuffle")
                }
            }
        }
        
        itemsIndexed(tracks) { index, track ->
            Box {
                MediaListItem(
                    file = track,
                    isPlaying = track.uri == activeMediaUri,
                    onMenuClick = { menuTrack = track },
                    onClick = { onTrackClick(index, tracks) }
                )
                
                DropdownMenu(
                    expanded = menuTrack == track,
                    onDismissRequest = { menuTrack = null }
                ) {
                    DropdownMenuItem(
                        text = { Text("Add to Playlist") },
                        onClick = { 
                            onAddToPlaylist(track)
                            menuTrack = null
                        },
                        leadingIcon = { Icon(Icons.Default.PlaylistAdd, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Share") },
                        onClick = { 
                            shareFile(context, track)
                            menuTrack = null
                        },
                        leadingIcon = { Icon(Icons.Default.Share, null) }
                    )
                }
            }
        }
    }
}

@Composable
fun AlbumGridItem(album: Album, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (album.representativeUri != null) {
                MediaThumbnail(uri = album.representativeUri, isVideo = false)
            } else {
                Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Album, null, tint = PrimaryAmber, modifier = Modifier.size(48.dp))
                }
            }
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.7f))))
            )
            
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            ) {
                Text(album.name, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(album.artist, color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp, maxLines = 1)
            }
        }
    }
}

@Composable
private fun DrawerAction(icon: ImageVector, label: String, active: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }) {
        Surface(
            color = if (active) PrimaryAmber.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f),
            shape = CircleShape,
            modifier = Modifier.size(48.dp),
            border = if (active) androidx.compose.foundation.BorderStroke(1.dp, PrimaryAmber) else null
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = if (active) PrimaryAmber else Color.White, modifier = Modifier.size(24.dp))
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(label, color = Color.White, fontSize = 10.sp)
    }
}

@Composable
fun MiniPlayer(
    file: MediaFile,
    isPlaying: Boolean,
    currentPos: Long,
    duration: Long,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onCastClick: () -> Unit,
    onClick: () -> Unit
) {
    val progress = if (duration > 0) currentPos.toFloat() / duration.toFloat() else 0f
    
    val displayName = remember(file.name) {
        file.name.substringBeforeLast(".")
            .replace("_", " ")
            .replace("-", " - ")
            .replace("  ", " ")
            .trim()
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clickable { onClick() }
            .background(EliteGradient)
            .glassmorphism(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Top Progress Indicator
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .align(Alignment.TopCenter),
                color = PrimaryAmber,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp))) {
                    MediaThumbnail(uri = file.uri, isVideo = false)
                }
                
                Spacer(Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Local Pro Media",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onCastClick) {
                        Icon(Icons.Default.Cast, null, tint = MaterialTheme.colorScheme.onSurface)
                    }
                    IconButton(onClick = onPlayPause) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            null,
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = onNext) {
                        Icon(Icons.Default.SkipNext, null, tint = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}

@Composable
fun PlaylistCard(playlist: Playlist, onClick: () -> Unit) {
    Card(
        modifier = Modifier.size(120.dp).clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.AutoMirrored.Filled.QueueMusic, null, tint = PrimaryAmber, modifier = Modifier.size(40.dp))
                Spacer(Modifier.height(8.dp))
                Text(playlist.name, style = MaterialTheme.typography.labelMedium, maxLines = 1)
            }
        }
    }
}

@Composable
fun XRayItem(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text(label, color = PrimaryAmber, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(value, color = Color.White, fontSize = 16.sp)
    }
}
