package com.example.utilityhub.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history")
data class HistoryEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // "EMI", "Translator", "Currency", etc.
    val input: String,
    val result: String,
    val timestamp: Long = System.currentTimeMillis()
)
