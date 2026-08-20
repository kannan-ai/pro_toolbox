package com.example.utilityhub.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [HistoryEntry::class, Playlist::class, PlaylistSongCrossRef::class, SwaraKnowledge::class, SwaraKnowledgeFts::class, CachedCurrency::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun swaraDao(): SwaraDao
    abstract fun currencyCacheDao(): CurrencyCacheDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                try {
                    val instance = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "utility_hub_db"
                    )
                        .fallbackToDestructiveMigration() // Automatic "Clear Data" only for DB if schema breaks
                        .build()
                    INSTANCE = instance
                    instance
                } catch (_: Exception) {
                    // Critical Recovery: Recreate DB from scratch if even fallback fails
                    context.deleteDatabase("utility_hub_db")
                    val instance = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "utility_hub_db"
                    ).build()
                    INSTANCE = instance
                    instance
                }
            }
        }
    }
}
