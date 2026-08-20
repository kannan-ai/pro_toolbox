package com.example.utilityhub.features.media

import android.view.WindowManager
import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.text.format.Formatter
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.utilityhub.ui.theme.PrimaryAmber
import java.text.SimpleDateFormat
import java.util.*
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CreationsScreen(
    isAccessEnabled: Boolean,
    isVaultLocked: Boolean = false,
    onToggleAccess: (Boolean) -> Unit,
) {
    val context = LocalContext.current

    // Stealth Screen Protection for The Vault
    DisposableEffect(Unit) {
        val activity = context.findActivity()
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    var isAuthenticated by remember { mutableStateOf(!isVaultLocked) }
    
    if (!isAuthenticated && isVaultLocked) {
        VaultAuthScreen { isAuthenticated = true }
        return
    }

    var files by remember { mutableStateOf<List<MediaFile>>(emptyList()) }
    var isLoading by remember { mutableStateOf(value = true) }
    var viewMode by remember { mutableStateOf(GalleryViewMode.LIST) }
    var renameFile by remember { mutableStateOf<MediaFile?>(null) }
    var filterMode by remember { mutableStateOf("All") }
    var sortOrder by remember { mutableStateOf("Date") } // "Name", "Date", "Size"
    val scope = rememberCoroutineScope()

    // Permission State
    val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.READ_MEDIA_VIDEO)
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
    
    var hasPermission by remember {
        mutableStateOf(
            requiredPermissions.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasPermission = results.values.all { it }
        if (!hasPermission) {
            Toast.makeText(context, "Permission needed to access storage.", Toast.LENGTH_SHORT).show()
        }
    }

    // Playback State
    val player = remember { ExoPlayer.Builder(context).build() }
    var currentlyPlayingUri by remember { mutableStateOf<Uri?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var totalDuration by remember { mutableLongStateOf(0L) }

    LaunchedEffect(isPlaying, currentlyPlayingUri) {
        if (isPlaying) {
            while (true) {
                currentPosition = player.currentPosition
                totalDuration = player.duration.coerceAtLeast(0L)
                delay(500.milliseconds)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { player.release() }
    }

    fun loadFiles() {
        if (!hasPermission || !isAccessEnabled) return
        scope.launch {
            isLoading = true
            files = fetchCreations(context)
            isLoading = false
        }
    }

    LaunchedEffect(hasPermission, isAccessEnabled) { 
        if (hasPermission && isAccessEnabled) loadFiles() 
        else {
            files = emptyList()
            isLoading = false
        }
    }

    val filteredFiles = remember(files, filterMode, sortOrder) {
        val baseList = when (filterMode) {
            "Videos" -> files.filter { it.mimeType.startsWith("video") }
            "Audio" -> files.filter { it.mimeType.startsWith("audio") }
            "Images" -> files.filter { it.mimeType.startsWith("image") }
            "PDFs" -> files.filter { it.mimeType == "application/pdf" }
            else -> files
        }
        
        baseList.sortedWith { a, b ->
            when (sortOrder) {
                "Name" -> a.name.compareTo(b.name, ignoreCase = true)
                "Size" -> b.size.compareTo(a.size)
                else -> b.date.compareTo(a.date) // Date (Newest first)
            }
        }
    }

    if (renameFile != null) {
        RenameDialog(
            file = renameFile!!,
            onDismiss = { renameFile = null },
            onRename = { 
                scope.launch {
                    val extension = renameFile!!.name.substringAfterLast(".", "")
                    val fullName = if (extension.isNotEmpty()) "$it.$extension" else it
                    if (performRename(context, renameFile!!, fullName)) loadFiles()
                    renameFile = null
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        if (!isAccessEnabled || !hasPermission) {
            StorageAccessNeededCard(
                onGrant = {
                    if (!isAccessEnabled) onToggleAccess(true)
                    if (!hasPermission) permissionLauncher.launch(requiredPermissions)
                }
            )
        } else {
            // Header: Metadata and Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Storage Summary Tag
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    val count = filteredFiles.size
                    val sizeSum = filteredFiles.sumOf { it.size }
                    val sizeStr = Formatter.formatShortFileSize(context, sizeSum)
                    Text(
                        text = "$count ${if (count == 1) "File" else "Files"} • $sizeStr",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    var showSortMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort")
                        }
                        DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Sort by Date") }, 
                                onClick = { sortOrder = "Date"; showSortMenu = false },
                                leadingIcon = { if (sortOrder == "Date") Icon(Icons.Default.Check, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Sort by Name") }, 
                                onClick = { sortOrder = "Name"; showSortMenu = false },
                                leadingIcon = { if (sortOrder == "Name") Icon(Icons.Default.Check, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Sort by Size") }, 
                                onClick = { sortOrder = "Size"; showSortMenu = false },
                                leadingIcon = { if (sortOrder == "Size") Icon(Icons.Default.Check, null) }
                            )
                        }
                    }

                    IconButton(onClick = { viewMode = if (viewMode == GalleryViewMode.LIST) GalleryViewMode.GRID else GalleryViewMode.LIST }) {
                        Icon(imageVector = if (viewMode == GalleryViewMode.LIST) Icons.Default.GridView else Icons.AutoMirrored.Filled.ViewList, contentDescription = null)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Filter Chips
            LazyRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val modes = listOf("All", "Videos", "Audio", "Images", "PDFs")
                items(modes) { mode ->
                    FilterChip(
                        selected = filterMode == mode,
                        onClick = { filterMode = mode },
                        label = {
                            Text(
                                mode,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryAmber.copy(alpha = 0.1f),
                            selectedLabelColor = PrimaryAmber
                        ),
                        border = if (filterMode == mode) FilterChipDefaults.filterChipBorder(enabled = true, selected = true, borderColor = PrimaryAmber.copy(alpha = 0.5f)) else FilterChipDefaults.filterChipBorder(enabled = true, selected = false)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = PrimaryAmber) }
            } else if (filteredFiles.isEmpty()) {
                EmptyStateView()
            } else {
                val listModifier = Modifier.weight(1f)
                if (viewMode == GalleryViewMode.LIST) {
                    LazyColumn(modifier = listModifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(filteredFiles) { file ->
                            val isThisPlaying = currentlyPlayingUri == file.uri && isPlaying
                            CreationItem(
                                file = file, 
                                isPlaying = isThisPlaying,
                                currentPosition = if (isThisPlaying) currentPosition else 0L,
                                totalDuration = if (isThisPlaying) totalDuration else 0L,
                                player = player,
                                onOpen = { openFile(context, file) }, 
                                onShare = { shareFile(context, file) }, 
                                onDelete = { if (deleteFile(context, file)) loadFiles() }, 
                                onRename = { renameFile = file },
                                onPlayPause = {
                                    if (isThisPlaying) {
                                        player.pause()
                                        isPlaying = false
                                    } else {
                                        if (currentlyPlayingUri != file.uri) {
                                            player.setMediaItem(MediaItem.fromUri(file.uri))
                                            player.prepare()
                                        }
                                        player.play()
                                        currentlyPlayingUri = file.uri
                                        isPlaying = true
                                    }
                                },
                                onSeek = { pos -> player.seekTo(pos) }
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 160.dp),
                        modifier = listModifier,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredFiles) { file ->
                            val isThisPlaying = currentlyPlayingUri == file.uri && isPlaying
                            CreationGridItem(
                                file = file, 
                                isPlaying = isThisPlaying,
                                player = player,
                                onOpen = { openFile(context, file) }, 
                                onDelete = { if (deleteFile(context, file)) loadFiles() }, 
                                onRename = { renameFile = file },
                                onPlayPause = {
                                    if (isThisPlaying) {
                                        player.pause()
                                        isPlaying = false
                                    } else {
                                        if (currentlyPlayingUri != file.uri) {
                                            player.setMediaItem(MediaItem.fromUri(file.uri))
                                            player.prepare()
                                        }
                                        player.play()
                                        currentlyPlayingUri = file.uri
                                        isPlaying = true
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Audio Mini Player Overlay
    if (currentlyPlayingUri != null) {
        val playingFile = files.find { it.uri == currentlyPlayingUri }
        if (playingFile != null && playingFile.mimeType.startsWith("audio")) {
            Box(modifier = Modifier.fillMaxSize()) {
                Card(
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(8.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, PrimaryAmber.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(color = PrimaryAmber.copy(alpha = 0.1f), shape = CircleShape, modifier = Modifier.size(40.dp)) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.MusicNote, null, tint = PrimaryAmber, modifier = Modifier.size(20.dp))
                                }
                            }
                            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                                Text(playingFile.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                                Text("${formatTime(currentPosition)} / ${formatTime(totalDuration)}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            }
                            IconButton(onClick = { if (player.isPlaying) player.pause() else player.play(); isPlaying = !isPlaying }) {
                                Icon(if (player.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = PrimaryAmber)
                            }
                            IconButton(onClick = { player.stop(); currentlyPlayingUri = null; isPlaying = false }) {
                                Icon(Icons.Default.Close, null, tint = Color.Gray)
                            }
                        }
                        Slider(
                            value = if (totalDuration > 0) currentPosition.toFloat() else 0f,
                            onValueChange = { player.seekTo(it.toLong()); currentPosition = it.toLong() },
                            valueRange = 0f..(if (totalDuration > 0) totalDuration.toFloat() else 1f),
                            colors = SliderDefaults.colors(thumbColor = PrimaryAmber, activeTrackColor = PrimaryAmber),
                            modifier = Modifier.height(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VaultAuthScreen(onSuccess: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Surface(
                color = PrimaryAmber.copy(alpha = 0.1f),
                shape = CircleShape,
                modifier = Modifier.size(100.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Lock, null, tint = PrimaryAmber, modifier = Modifier.size(48.dp))
                }
            }
            Spacer(Modifier.height(24.dp))
            Text("Vault Locked", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Authentication required to access creations", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = {
                    if (activity != null) {
                        val executor = ContextCompat.getMainExecutor(activity)
                        val biometricPrompt = BiometricPrompt(activity, executor,
                            object : BiometricPrompt.AuthenticationCallback() {
                                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                    super.onAuthenticationSucceeded(result)
                                    onSuccess()
                                }
                            })

                        val promptInfo = BiometricPrompt.PromptInfo.Builder()
                            .setTitle("Vault Authentication")
                            .setSubtitle("Unlock your professional creations")
                            .setAllowedAuthenticators(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG or androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                            .build()

                        biometricPrompt.authenticate(promptInfo)
                    } else {
                        onSuccess() // Fallback if no activity
                    }
                },
                modifier = Modifier.height(56.dp).width(200.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryAmber)
            ) {
                Icon(Icons.Default.Fingerprint, null)
                Spacer(Modifier.width(12.dp))
                Text("Unlock Vault")
            }
        }
    }
    
    // Auto-trigger on mount
    LaunchedEffect(Unit) {
        // We can auto trigger here if desired, but user tapping is often better for UX stability
    }
}

@Composable
fun StorageAccessNeededCard(onGrant: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Surface(color = PrimaryAmber.copy(alpha = 0.1f), shape = CircleShape, modifier = Modifier.size(64.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Storage, null, tint = PrimaryAmber, modifier = Modifier.size(32.dp))
                }
            }
            Text(
                text = "Storage Access Needed",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "To view and manage your exported media, please grant storage permissions and enable gallery access.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = Color.Gray
            )
            Button(
                onClick = onGrant,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryAmber)
            ) {
                Text("Grant Access", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun EmptyStateView() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(
                Icons.Default.CloudQueue, 
                null, 
                modifier = Modifier.size(80.dp).alpha(0.1f),
                tint = MaterialTheme.colorScheme.onSurface
            )
            Text(
                "Your exported media and documents\nwill appear here",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = Color.Gray.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun CreationItem(file: MediaFile, isPlaying: Boolean, currentPosition: Long, totalDuration: Long, player: ExoPlayer, onOpen: () -> Unit, onShare: () -> Unit, onDelete: () -> Unit, onRename: () -> Unit, onPlayPause: () -> Unit, onSeek: (Long) -> Unit) {
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()) }
    val isPdf = file.mimeType == "application/pdf"
    val isAudio = file.mimeType.startsWith("audio")
    val isVideo = file.mimeType.startsWith("video")
    val isImage = file.mimeType.startsWith("image")
    
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onOpen() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                // Circular Type Icon / Thumbnail
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    shape = CircleShape,
                    modifier = Modifier.size(44.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isImage) {
                            MediaThumbnail(uri = file.uri, isVideo = false)
                        } else {
                            Icon(
                                imageVector = when {
                                    isPdf -> Icons.Default.PictureAsPdf
                                    isAudio -> Icons.Default.Audiotrack
                                    isVideo -> Icons.Default.Movie
                                    else -> Icons.AutoMirrored.Filled.InsertDriveFile
                                },
                                null,
                                tint = when {
                                    isPdf -> Color(0xFFEF5350)
                                    isAudio -> Color(0xFF42A5F5)
                                    isVideo -> Color(0xFFFFA726)
                                    else -> Color.Gray
                                },
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                
                Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(file.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        text = "${Formatter.formatShortFileSize(context, file.size)} • ${dateFormat.format(Date(file.date * 1000))}", 
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isAudio || isVideo) {
                        IconButton(onClick = onPlayPause) {
                            Icon(
                                if (isPlaying) Icons.Default.PauseCircle else Icons.Default.PlayCircle, 
                                null, 
                                tint = PrimaryAmber, 
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    
                    IconButton(onClick = onShare) { Icon(Icons.Default.Share, null, modifier = Modifier.size(20.dp), tint = Color.Gray) }
                    
                    Box {
                        IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, null, modifier = Modifier.size(20.dp), tint = Color.Gray) }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(text = { Text("Rename") }, onClick = { onRename(); showMenu = false }, leadingIcon = { Icon(Icons.Default.Edit, null) })
                            DropdownMenuItem(
                                text = { Text("Delete", color = Color(0xFFE57373)) }, 
                                onClick = { onDelete(); showMenu = false }, 
                                leadingIcon = { Icon(Icons.Default.Delete, null, tint = Color(0xFFE57373)) }
                            )
                        }
                    }
                }
            }
            
            if (isPlaying && isVideo) {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp).background(Color.Black)) {
                    AndroidView(
                        factory = { ctx -> PlayerView(ctx).apply { this.player = player; useController = true } },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            if (isPlaying && (isAudio || isVideo) && totalDuration > 0) {
                Slider(
                    value = currentPosition.toFloat(), 
                    onValueChange = { onSeek(it.toLong()) }, 
                    valueRange = 0f..totalDuration.toFloat(), 
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(20.dp),
                    colors = SliderDefaults.colors(thumbColor = PrimaryAmber, activeTrackColor = PrimaryAmber)
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun CreationGridItem(file: MediaFile, isPlaying: Boolean, player: ExoPlayer, onOpen: () -> Unit, onDelete: () -> Unit, onRename: () -> Unit, onPlayPause: () -> Unit) {
    val context = LocalContext.current
    val isPdf = file.mimeType == "application/pdf"
    val isAudio = file.mimeType.startsWith("audio")
    val isVideo = file.mimeType.startsWith("video")
    val isImage = file.mimeType.startsWith("image")
    
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onOpen() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.fillMaxWidth().height(80.dp)) {
                if (isPlaying && isVideo) {
                    AndroidView(
                        factory = { ctx -> PlayerView(ctx).apply { this.player = player; useController = true } },
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp))
                    )
                } else if (isImage) {
                    Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp))) {
                        MediaThumbnail(uri = file.uri, isVideo = false)
                    }
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                        shape = CircleShape,
                        modifier = Modifier.size(52.dp).align(Alignment.Center),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = when {
                                    isPdf -> Icons.Default.PictureAsPdf
                                    isAudio -> Icons.Default.Audiotrack
                                    isVideo -> Icons.Default.Movie
                                    else -> Icons.AutoMirrored.Filled.InsertDriveFile
                                },
                                null,
                                tint = PrimaryAmber.copy(alpha = 0.5f),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
                
                // Grid Menu Anchor
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.align(Alignment.TopEnd).size(24.dp)
                ) {
                    Icon(Icons.Default.MoreVert, null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(text = { Text("Rename") }, onClick = { onRename(); showMenu = false })
                        DropdownMenuItem(text = { Text("Delete", color = Color(0xFFE57373)) }, onClick = { onDelete(); showMenu = false })
                    }
                }
            }
            
            Spacer(Modifier.height(8.dp))
            
            Text(file.name, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, maxLines = 1, textAlign = TextAlign.Center, overflow = TextOverflow.Ellipsis)
            
            if (isPlaying && (isAudio || isVideo)) {
                 IconButton(onClick = onPlayPause, modifier = Modifier.size(32.dp)) {
                    Icon(if (isPlaying) Icons.Default.PauseCircle else Icons.Default.PlayCircle, null, tint = PrimaryAmber)
                }
            } else {
                Text(Formatter.formatShortFileSize(context, file.size), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
        }
    }
}

@Composable
fun RenameDialog(file: MediaFile, onDismiss: () -> Unit, onRename: (String) -> Unit) {
    var newName by remember { mutableStateOf(file.name.substringBeforeLast(".")) }
    AlertDialog(
        onDismissRequest = onDismiss, 
        modifier = Modifier.widthIn(max = 560.dp),
        title = { Text("Rename File") }, 
        text = { 
            OutlinedTextField(
                value = newName, 
                onValueChange = { newName = it }, 
                label = { Text("New Name") }, 
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            ) 
        }, 
        confirmButton = { 
            Button(onClick = { if (newName.isNotBlank()) onRename(newName) }, colors = ButtonDefaults.buttonColors(containerColor = PrimaryAmber)) { 
                Text("Rename") 
            } 
        }, 
        dismissButton = { 
            TextButton(onClick = onDismiss) { Text("Cancel") } 
        }
    )
}
