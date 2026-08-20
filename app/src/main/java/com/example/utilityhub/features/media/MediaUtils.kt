@file:Suppress("SpellCheckingInspection")
package com.example.utilityhub.features.media

import android.app.Activity
import android.content.ContextWrapper
import android.content.ContentValues
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.format.Formatter
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.items
import kotlin.coroutines.resume
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayCircle

fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

enum class GalleryViewMode { LIST, GRID }

@Composable
fun MediaListItem(
    file: MediaFile, 
    isPlaying: Boolean = false,
    onMenuClick: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val isVideo = file.mimeType.startsWith("video")
    val sizeStr = Formatter.formatShortFileSize(LocalContext.current, file.size)
    
    val displayName = remember(file.name) {
        file.name.substringBeforeLast(".")
            .replace("_", " ")
            .replace("-", " - ")
            .replace("  ", " ")
            .trim()
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isPlaying) 
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) 
            else 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(50.dp).clip(RoundedCornerShape(8.dp))) {
                MediaThumbnail(uri = file.uri, isVideo = isVideo)
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayName, 
                    style = MaterialTheme.typography.bodyMedium, 
                    fontWeight = if (isPlaying) FontWeight.ExtraBold else FontWeight.Bold,
                    color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(sizeStr, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (file.duration > 0) {
                        Text(" • ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(formatTime(file.duration), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isPlaying) {
                    AnimatedEqualizer()
                    Spacer(Modifier.width(8.dp))
                }
                if (onMenuClick != null) {
                    IconButton(onClick = onMenuClick) {
                        Icon(
                            Icons.Default.MoreVert, 
                            contentDescription = "Options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else if (!isPlaying) {
                    Icon(
                        if (isVideo) Icons.Default.PlayCircle else Icons.AutoMirrored.Filled.VolumeUp, 
                        null, 
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
fun AnimatedEqualizer() {
    val infiniteTransition = rememberInfiniteTransition(label = "eq")
    
    Row(
        modifier = Modifier.height(24.dp).padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        repeat(3) { index ->
            val heightScale by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(400 + (index * 150), easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bar_$index"
            )
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight(heightScale)
                    .background(Color(0xFF4CAF50), RoundedCornerShape(2.dp))
            )
        }
    }
}

@Composable
fun MediaThumbnail(
    uri: Uri, 
    isVideo: Boolean, 
    modifier: Modifier = Modifier,
    highRes: Boolean = false
) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    LaunchedEffect(uri) {
        bitmap = null
        withContext(Dispatchers.IO) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val size = if (highRes) android.util.Size(512, 512) else android.util.Size(128, 128)
                    bitmap = try {
                        context.contentResolver.loadThumbnail(uri, size, null)
                    } catch (_: Exception) {
                        null
                    }
                }
                
                if (bitmap == null) {
                    val retriever = MediaMetadataRetriever()
                    try {
                        val fileMime = try { context.contentResolver.getType(uri) } catch (_: Exception) { null }
                        context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                            retriever.setDataSource(pfd.fileDescriptor)
                            if (isVideo) {
                                bitmap = retriever.getFrameAtTime(1000000)
                            } else if (fileMime?.startsWith("audio") == true) {
                                val art = retriever.embeddedPicture
                                if (art != null) {
                                    val options = BitmapFactory.Options().apply { if (!highRes) inSampleSize = 4 }
                                    bitmap = BitmapFactory.decodeByteArray(art, 0, art.size, options)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("MediaThumbnail", "Retriever failed", e)
                    } finally {
                        try { retriever.release() } catch (_: Exception) {}
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("MediaThumbnail", "Fatal thumbnail error", e)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                filterQuality = androidx.compose.ui.graphics.FilterQuality.High
            )
        } ?: Icon(
            imageVector = if (isVideo) Icons.Default.PlayCircle else Icons.Default.MusicNote,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            modifier = Modifier.size(24.dp)
        )
    }
}

fun formatTime(ms: Long): String {
    if (ms < 0) return "00:00"
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}

fun saveToGallery(context: Context, file: File, name: String, mime: String, folder: String): Uri? {
    val values = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, name)
        put(MediaStore.MediaColumns.MIME_TYPE, mime)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.MediaColumns.RELATIVE_PATH, "$folder/ProToolbox")
        }
    }
    val uri = context.contentResolver.insert(
        if (mime.startsWith("video")) MediaStore.Video.Media.EXTERNAL_CONTENT_URI 
        else if (mime.startsWith("audio")) MediaStore.Audio.Media.EXTERNAL_CONTENT_URI 
        else MediaStore.Files.getContentUri("external"), 
        values
    )
    uri?.let { destUri ->
        context.contentResolver.openOutputStream(destUri)?.use { os ->
            file.inputStream().use { it.copyTo(os) }
        }
    }
    return uri
}

fun saveBitmapToGallery(context: Context, bitmap: Bitmap, name: String): Uri? {
    val values = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, name)
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/ProHub_Captures")
        }
    }
    val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
    uri?.let { destUri ->
        context.contentResolver.openOutputStream(destUri)?.use { os ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os)
        }
    }
    return uri
}

suspend fun createGifFromVideo(context: Context, uri: Uri, startMs: Long, onProgress: (Float) -> Unit): Uri? = withContext(Dispatchers.IO) {
    val tempI = File(context.cacheDir, "v_gif_in")
    val outName = "Capture_${System.currentTimeMillis()}.gif"
    val tempO = File(context.cacheDir, outName)
    
    try {
        context.contentResolver.openInputStream(uri)?.use { it.copyTo(tempI.outputStream()) }
        val startSec = startMs / 1000f
        val command = "-ss $startSec -t 5 -i ${tempI.absolutePath} -vf \"fps=12,scale=480:-1:flags=lanczos\" ${tempO.absolutePath}"
        val success = executeFFmpegWithProgress(command, 5000L, onProgress)
        if (success) saveToGallery(context, tempO, outName, "image/gif", Environment.DIRECTORY_PICTURES) else null
    } catch (_: Exception) { null } finally { tempI.delete(); tempO.delete() }
}

suspend fun fetchMedia(context: Context, isVideo: Boolean): List<MediaFile> = withContext(Dispatchers.IO) {
    val fileList = mutableListOf<MediaFile>()
    val uri = if (isVideo) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    
    // More inclusive selection to avoid missing songs on some devices
    val baseSelection = if (isVideo) null else {
        "${MediaStore.Audio.Media.DURATION} >= 5000" // At least 5 seconds
    }

    val projection = mutableListOf(
        MediaStore.MediaColumns._ID,
        MediaStore.MediaColumns.DISPLAY_NAME,
        MediaStore.MediaColumns.SIZE,
        MediaStore.MediaColumns.DATE_ADDED,
        MediaStore.MediaColumns.MIME_TYPE,
    )
    
    // Add BUCKET column safely
    if (isVideo) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            projection.add(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
        } else {
            projection.add(MediaStore.Video.Media.DATA)
        }
    } else {
        // For Audio, BUCKET_DISPLAY_NAME is often not supported, use DATA
        projection.add(MediaStore.Audio.Media.DATA)
    }

    if (isVideo) {
        projection.add(MediaStore.Video.Media.DURATION)
    } else {
        projection.add(MediaStore.Audio.Media.DURATION)
        projection.add(MediaStore.Audio.Media.ARTIST)
        projection.add(MediaStore.Audio.Media.ALBUM)
        projection.add(MediaStore.Audio.Media.ALBUM_ID)
    }

    try {
        context.contentResolver.query(uri, projection.toTypedArray(), baseSelection, null, "${MediaStore.MediaColumns.DATE_ADDED} DESC")?.use { cursor ->
            val idCol = cursor.getColumnIndex(MediaStore.MediaColumns._ID)
            val nameCol = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
            val sizeCol = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
            val dateCol = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_ADDED)
            val mimeCol = cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)
            val durationCol = cursor.getColumnIndex(if (isVideo) MediaStore.Video.Media.DURATION else MediaStore.Audio.Media.DURATION)
            
            val bucketCol = if (isVideo) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) 
                    cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME) 
                else 
                    cursor.getColumnIndex(MediaStore.Video.Media.DATA)
            } else {
                cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
            }
            
            val artistCol = if (!isVideo) cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST) else -1
            val albumCol = if (!isVideo) cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM) else -1
            val albumIdCol = if (!isVideo) cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID) else -1

            val excludedFolders = listOf("WhatsApp", "Telegram", "Android/data", "Ringtones", "Notifications")

            while (cursor.moveToNext()) {
                val id = if (idCol != -1) cursor.getLong(idCol) else continue
                val name = if (nameCol != -1) cursor.getString(nameCol) ?: "Unknown" else "Unknown"
                
                val bucketRaw = if (bucketCol != -1) cursor.getString(bucketCol) else null
                if (excludedFolders.any { bucketRaw?.contains(it, ignoreCase = true) == true }) continue

                val contentUri = ContentUris.withAppendedId(uri, id)
                val bucketName = if (isVideo && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    bucketRaw ?: "Internal"
                } else {
                    val file = bucketRaw?.let { File(it) }
                    if (isVideo && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                        file?.parentFile?.name ?: "Internal"
                    } else {
                        // For Audio, extract parent name from path
                        file?.parentFile?.name ?: "Music"
                    }
                }

                fileList.add(
                    MediaFile(
                        id = id,
                        name = name,
                        size = if (sizeCol != -1) cursor.getLong(sizeCol) else 0L,
                        date = if (dateCol != -1) cursor.getLong(dateCol) else 0L,
                        uri = contentUri,
                        mimeType = if (mimeCol != -1) cursor.getString(mimeCol) ?: (if (isVideo) "video/*" else "audio/*") else (if (isVideo) "video/*" else "audio/*"),
                        duration = if (durationCol != -1) cursor.getLong(durationCol) else 0L,
                        bucketName = bucketName,
                        artist = if (artistCol != -1) cursor.getString(artistCol) ?: "Unknown Artist" else "Unknown Artist",
                        album = if (albumCol != -1) cursor.getString(albumCol) ?: "Unknown Album" else "Unknown Album",
                        albumId = if (albumIdCol != -1) cursor.getLong(albumIdCol) else -1L
                    )
                )
            }
        }
    } catch (e: Exception) { 
        android.util.Log.e("MediaUtils", "Error fetching media", e)
    }
    fileList
}

suspend fun fetchArtists(context: Context): List<Artist> = withContext(Dispatchers.IO) {
    val artistList = mutableListOf<Artist>()
    val uri = MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI
    val projection = arrayOf(
        MediaStore.Audio.Artists._ID,
        MediaStore.Audio.Artists.ARTIST,
        MediaStore.Audio.Artists.NUMBER_OF_ALBUMS,
        MediaStore.Audio.Artists.NUMBER_OF_TRACKS
    )
    try {
        context.contentResolver.query(uri, projection, null, null, "${MediaStore.Audio.Artists.ARTIST} ASC")?.use { cursor ->
            val idCol = cursor.getColumnIndex(MediaStore.Audio.Artists._ID)
            val nameCol = cursor.getColumnIndex(MediaStore.Audio.Artists.ARTIST)
            val albumsCol = cursor.getColumnIndex(MediaStore.Audio.Artists.NUMBER_OF_ALBUMS)
            val tracksCol = cursor.getColumnIndex(MediaStore.Audio.Artists.NUMBER_OF_TRACKS)
            while (cursor.moveToNext()) {
                val artistId = if (idCol != -1) cursor.getLong(idCol) else continue
                val artistName = if (nameCol != -1) cursor.getString(nameCol) ?: "Unknown Artist" else "Unknown Artist"
                
                // Fetch representative URI safely
                val songUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                val songProjection = arrayOf(MediaStore.Audio.Media._ID)
                val songSelection = "${MediaStore.Audio.Media.ARTIST_ID} = ?"
                val representativeUri = context.contentResolver.query(songUri, songProjection, songSelection, arrayOf(artistId.toString()), null)?.use { songCursor ->
                    if (songCursor.moveToFirst()) {
                        val songId = songCursor.getLong(0)
                        ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, songId)
                    } else null
                }

                artistList.add(
                    Artist(
                        artistId,
                        artistName,
                        if (albumsCol != -1) cursor.getInt(albumsCol) else 0,
                        if (tracksCol != -1) cursor.getInt(tracksCol) else 0,
                        representativeUri
                    )
                )
            }
        }
    } catch (e: Exception) { 
        android.util.Log.e("MediaUtils", "Error fetching artists", e)
    }
    artistList
}

suspend fun fetchAlbums(context: Context): List<Album> = withContext(Dispatchers.IO) {
    val albumList = mutableListOf<Album>()
    val uri = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI
    val projection = arrayOf(
        MediaStore.Audio.Albums._ID,
        MediaStore.Audio.Albums.ALBUM,
        MediaStore.Audio.Albums.ARTIST,
        MediaStore.Audio.Albums.FIRST_YEAR,
        MediaStore.Audio.Albums.NUMBER_OF_SONGS
    )
    try {
        context.contentResolver.query(uri, projection, null, null, "${MediaStore.Audio.Albums.ALBUM} ASC")?.use { cursor ->
            val idCol = cursor.getColumnIndex(MediaStore.Audio.Albums._ID)
            val nameCol = cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM)
            val artistCol = cursor.getColumnIndex(MediaStore.Audio.Albums.ARTIST)
            val yearCol = cursor.getColumnIndex(MediaStore.Audio.Albums.FIRST_YEAR)
            val tracksCol = cursor.getColumnIndex(MediaStore.Audio.Albums.NUMBER_OF_SONGS)
            while (cursor.moveToNext()) {
                val albumId = if (idCol != -1) cursor.getLong(idCol) else continue
                
                // Fetch representative URI safely
                val songUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                val songProjection = arrayOf(MediaStore.Audio.Media._ID)
                val songSelection = "${MediaStore.Audio.Media.ALBUM_ID} = ?"
                val representativeUri = context.contentResolver.query(songUri, songProjection, songSelection, arrayOf(albumId.toString()), null)?.use { songCursor ->
                    if (songCursor.moveToFirst()) {
                        val songId = songCursor.getLong(0)
                        ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, songId)
                    } else null
                }

                albumList.add(
                    Album(
                        albumId,
                        if (nameCol != -1) cursor.getString(nameCol) ?: "Unknown Album" else "Unknown Album",
                        if (artistCol != -1) cursor.getString(artistCol) ?: "Unknown Artist" else "Unknown Artist",
                        if (yearCol != -1) cursor.getInt(yearCol) else 0,
                        if (tracksCol != -1) cursor.getInt(tracksCol) else 0,
                        representativeUri
                    )
                )
            }
        }
    } catch (e: Exception) { 
        android.util.Log.e("MediaUtils", "Error fetching albums", e)
    }
    albumList
}

suspend fun fetchImages(context: Context): List<MediaFile> = withContext(Dispatchers.IO) {
    val fileList = mutableListOf<MediaFile>()
    val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    val projection = arrayOf(
        MediaStore.Files.FileColumns._ID,
        MediaStore.Files.FileColumns.DISPLAY_NAME,
        MediaStore.Files.FileColumns.SIZE,
        MediaStore.Files.FileColumns.DATE_ADDED,
        MediaStore.Files.FileColumns.MIME_TYPE
    )

    try {
        context.contentResolver.query(uri, projection, null, null, "${MediaStore.Files.FileColumns.DATE_ADDED} DESC")?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
            
            while (cursor.moveToNext()) {
                val contentUri = Uri.withAppendedPath(uri, cursor.getString(idCol))
                fileList.add(
                    MediaFile(
                        cursor.getLong(idCol),
                        cursor.getString(nameCol),
                        cursor.getLong(sizeCol),
                        cursor.getLong(dateCol),
                        contentUri,
                        cursor.getString(mimeCol)
                    )
                )
            }
        }
    } catch (e: Exception) { e.printStackTrace() }
    fileList
}

suspend fun fetchCreations(context: Context): List<MediaFile> = withContext(Dispatchers.IO) {
    val fileList = mutableListOf<MediaFile>()
    val projection = arrayOf(
        MediaStore.Files.FileColumns._ID, 
        MediaStore.Files.FileColumns.DISPLAY_NAME, 
        MediaStore.Files.FileColumns.SIZE, 
        MediaStore.Files.FileColumns.DATE_ADDED, 
        MediaStore.Files.FileColumns.MIME_TYPE
    )
    val selection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        "${MediaStore.Files.FileColumns.RELATIVE_PATH} LIKE ?"
    } else {
        "${MediaStore.Files.FileColumns.DATA} LIKE ?"
    }
    val selectionArgs = arrayOf("%ProToolbox%")
    
    val externalUri = MediaStore.Files.getContentUri("external")
    
    try {
        context.contentResolver.query(externalUri, projection, selection, selectionArgs, "${MediaStore.Files.FileColumns.DATE_ADDED} DESC")?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
            
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val mimeType = cursor.getString(mimeCol) ?: ""
                
                val contentUri = when {
                    mimeType.startsWith("video") -> ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                    mimeType.startsWith("audio") -> ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                    mimeType.startsWith("image") -> ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                    else -> ContentUris.withAppendedId(externalUri, id)
                }
                
                fileList.add(
                    MediaFile(
                        id = id, 
                        name = cursor.getString(nameCol), 
                        size = cursor.getLong(sizeCol), 
                        date = cursor.getLong(dateCol), 
                        uri = contentUri, 
                        mimeType = mimeType
                    )
                )
            }
        }
    } catch (e: Exception) { e.printStackTrace() }
    fileList
}

fun performRename(context: Context, file: MediaFile, newName: String): Boolean {
    val values = ContentValues().apply { put(MediaStore.Files.FileColumns.DISPLAY_NAME, newName) }
    return try { context.contentResolver.update(file.uri, values, null, null) > 0 } catch (_: Exception) { false }
}

fun openFile(context: Context, file: MediaFile) {
    val intent = Intent(Intent.ACTION_VIEW).apply { setDataAndType(file.uri, file.mimeType); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }
    try { context.startActivity(intent) } catch (_: Exception) { }
}

fun shareFile(context: Context, file: MediaFile) {
    val intent = Intent(Intent.ACTION_SEND).apply { type = file.mimeType; putExtra(Intent.EXTRA_STREAM, file.uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }
    context.startActivity(Intent.createChooser(intent, "Share"))
}

fun deleteFile(context: Context, file: MediaFile): Boolean {
    return try { context.contentResolver.delete(file.uri, null, null) > 0 } catch (_: Exception) { false }
}

suspend fun mergeAudioFiles(context: Context, u1: Uri, u2: Uri, onProgress: (Float) -> Unit = {}): Uri? = withContext(Dispatchers.IO) {
    val t1 = File(context.cacheDir, "a1"); val t2 = File(context.cacheDir, "a2"); val outName = "Merged_${System.currentTimeMillis()}.mp3"; val tempO = File(context.cacheDir, outName)
    val d1 = getMediaDuration(context, u1)
    val d2 = getMediaDuration(context, u2)
    val totalDuration = maxOf(d1, d2)
    try {
        context.contentResolver.openInputStream(u1)?.use { it.copyTo(t1.outputStream()) }
        context.contentResolver.openInputStream(u2)?.use { it.copyTo(t2.outputStream()) }
        val command = "-i ${t1.absolutePath} -i ${t2.absolutePath} -filter_complex amerge=inputs=2 -ac 2 ${tempO.absolutePath}"
        val success = executeFFmpegWithProgress(command, totalDuration, onProgress)
        if (success) { saveToGallery(context, tempO, outName, "audio/mpeg", Environment.DIRECTORY_MUSIC) } else null
    } catch (_: Exception) { null } finally { t1.delete(); t2.delete(); tempO.delete() }
}

@Composable
fun AsyncImagePlaceholder(uri: Uri) {
    MediaThumbnail(uri = uri, isVideo = true)
}

suspend fun reduceVideoSize(context: Context, uri: Uri, onProgress: (Float) -> Unit): Uri? = withContext(Dispatchers.IO) {
    val tempI = File(context.cacheDir, "v_in"); val outName = "Reduced_${System.currentTimeMillis()}.mp4"; val tempO = File(context.cacheDir, outName)
    val duration = getMediaDuration(context, uri)
    try {
        context.contentResolver.openInputStream(uri)?.use { it.copyTo(t1@tempI.outputStream()) }
        val command = "-i ${tempI.absolutePath} -vcodec libx264 -crf 28 -preset fast ${tempO.absolutePath}"
        val success = executeFFmpegWithProgress(command, duration, onProgress)
        if (success) saveToGallery(context, tempO, outName, "video/mp4", Environment.DIRECTORY_MOVIES) else null
    } catch (_: Exception) { null } finally { tempI.delete(); tempO.delete() }
}

suspend fun convertVideoToGif(context: Context, uri: Uri, onProgress: (Float) -> Unit): Uri? = withContext(Dispatchers.IO) {
    val tempI = File(context.cacheDir, "v_gif_in"); val outName = "GIF_${System.currentTimeMillis()}.gif"; val tempO = File(context.cacheDir, outName)
    val duration = getMediaDuration(context, uri)
    try {
        context.contentResolver.openInputStream(uri)?.use { it.copyTo(tempI.outputStream()) }
        val command = "-i ${tempI.absolutePath} -vf \"fps=10,scale=320:-1:flags=lanczos\" ${tempO.absolutePath}"
        val success = executeFFmpegWithProgress(command, duration, onProgress)
        if (success) saveToGallery(context, tempO, outName, "image/gif", Environment.DIRECTORY_PICTURES) else null
    } catch (_: Exception) { null } finally { tempI.delete(); tempO.delete() }
}

suspend fun mergeAudioVideo(context: Context, videoUri: Uri, audioUri: Uri, onProgress: (Float) -> Unit): Uri? = withContext(Dispatchers.IO) {
    val tV = File(context.cacheDir, "v_m"); val tA = File(context.cacheDir, "a_m"); val outName = "MergedVA_${System.currentTimeMillis()}.mp4"; val tempO = File(context.cacheDir, outName)
    val duration = getMediaDuration(context, videoUri)
    try {
        context.contentResolver.openInputStream(videoUri)?.use { it.copyTo(tV.outputStream()) }
        context.contentResolver.openInputStream(audioUri)?.use { it.copyTo(tA.outputStream()) }
        val command = "-i ${tV.absolutePath} -i ${tA.absolutePath} -c:v copy -c:a aac -map 0:v:0 -map 1:a:0 -shortest ${tempO.absolutePath}"
        val success = executeFFmpegWithProgress(command, duration, onProgress)
        if (success) saveToGallery(context, tempO, outName, "video/mp4", Environment.DIRECTORY_MOVIES) else null
    } catch (_: Exception) { null } finally { tV.delete(); tA.delete(); tempO.delete() }
}

suspend fun muteVideoAudio(context: Context, uri: Uri, onProgress: (Float) -> Unit): Uri? = withContext(Dispatchers.IO) {
    val tempI = File(context.cacheDir, "v_mute"); val outName = "Muted_${System.currentTimeMillis()}.mp4"; val tempO = File(context.cacheDir, outName)
    val duration = getMediaDuration(context, uri)
    try {
        context.contentResolver.openInputStream(uri)?.use { it.copyTo(tempI.outputStream()) }
        val command = "-i ${tempI.absolutePath} -c copy -an ${tempO.absolutePath}"
        val success = executeFFmpegWithProgress(command, duration, onProgress)
        if (success) saveToGallery(context, tempO, outName, "video/mp4", Environment.DIRECTORY_MOVIES) else null
    } catch (_: Exception) { null } finally { tempI.delete(); tempO.delete() }
}

suspend fun extractAudioFromVideo(context: Context, uri: Uri, onProgress: (Float) -> Unit): Uri? = withContext(Dispatchers.IO) {
    val tempI = File(context.cacheDir, "v_ext"); val outName = "Extracted_${System.currentTimeMillis()}.mp3"; val tempO = File(context.cacheDir, outName)
    val duration = getMediaDuration(context, uri)
    try {
        context.contentResolver.openInputStream(uri)?.use { it.copyTo(tempI.outputStream()) }
        val command = "-i ${tempI.absolutePath} -vn -acodec libmp3lame -q:a 2 ${tempO.absolutePath}"
        val success = executeFFmpegWithProgress(command, duration, onProgress)
        if (success) saveToGallery(context, tempO, outName, "audio/mpeg", Environment.DIRECTORY_MUSIC) else null
    } catch (_: Exception) { null } finally { tempI.delete(); tempO.delete() }
}

suspend fun getVideoMetadata(context: Context, uri: Uri): VideoMetadata? = withContext(Dispatchers.IO) {
    val retriever = MediaMetadataRetriever()
    try {
        context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd -> retriever.setDataSource(pfd.fileDescriptor) }
        val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toInt() ?: 0
        val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toInt() ?: 0
        val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L
        val size = context.contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize } ?: 0L
        val fps = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)?.toFloat() ?: 0f
        VideoMetadata(width, height, 0L, size, duration, fps, "H.264")
    } catch (e: Exception) { e.printStackTrace(); null } finally { retriever.release() }
}

internal suspend fun getMediaDuration(context: Context, uri: Uri): Long = withContext(Dispatchers.IO) {
    val retriever = MediaMetadataRetriever()
    try {
        context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
            retriever.setDataSource(pfd.fileDescriptor)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L
        } ?: 0L
    } catch (_: Exception) { 0L } finally { retriever.release() }
}

internal suspend fun executeFFmpegWithProgress(
    command: String,
    durationMs: Long,
    onProgress: (Float) -> Unit
): Boolean = suspendCancellableCoroutine { cont ->
    val session = FFmpegKit.executeAsync(
        command,
        { completeSession ->
            val success = ReturnCode.isSuccess(completeSession.returnCode)
            if (!success) {
                android.util.Log.e("FFMPEG", "FFmpeg failed. Logs: ${completeSession.allLogsAsString}")
            }
            if (cont.isActive) cont.resume(success)
        },
        { log ->
            android.util.Log.d("FFMPEG", log.message)
        },
        { statistics ->
            if (durationMs > 0) {
                val progress = (statistics.time.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                onProgress(progress)
            }
        }
    )

    cont.invokeOnCancellation {
        FFmpegKit.cancel(session.sessionId)
    }
}

