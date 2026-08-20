package com.example.utilityhub.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)

@Entity(
    tableName = "playlist_songs",
    primaryKeys = ["playlistId", "songUri"],
    indices = [Index("playlistId"), Index("songUri")]
)
data class PlaylistSongCrossRef(
    val playlistId: Long,
    val songUri: String
)
