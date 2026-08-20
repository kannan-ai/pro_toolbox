package com.example.utilityhub.features.media

import android.net.Uri

data class MediaFile(
    val id: Long,
    val name: String,
    val size: Long,
    val date: Long,
    val uri: Uri,
    val mimeType: String,
    val duration: Long = 0L,
    val bucketName: String = "",
    val artist: String = "Unknown Artist",
    val album: String = "Unknown Album",
    val albumId: Long = -1L
)

data class Artist(
    val id: Long,
    val name: String,
    val albumCount: Int,
    val songCount: Int,
    val representativeUri: Uri? = null
)

data class Album(
    val id: Long,
    val name: String,
    val artist: String,
    val firstYear: Int,
    val songCount: Int,
    val representativeUri: Uri? = null
)

data class VideoMetadata(
    val width: Int,
    val height: Int,
    val bitrate: Long,
    val size: Long,
    val duration: Long,
    val frameRate: Float = 0f,
    val codec: String = "Unknown"
)

data class ProcessingResult(
    val isSuccess: Boolean,
    val errorMessage: String? = null,
    val outputUri: Uri? = null
)

data class ProductCompare(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val price: Double,
    val quantity: Double,
    val unit: String,
    val specs: String = "",
    val isBestValue: Boolean = false,
    val unitValue: Double = 0.0
)

