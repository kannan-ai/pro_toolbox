package com.example.utilityhub.data.db

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(tableName = "swara_knowledge")
data class SwaraKnowledge(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "keyword") val keyword: String,
    @ColumnInfo(name = "content") val content: String,
    @ColumnInfo(name = "category") val category: String, // "APP", "GENERAL"
    @ColumnInfo(name = "audio_action") val audioAction: String = "NONE"
)

@Fts4(contentEntity = SwaraKnowledge::class)
@Entity(tableName = "swara_knowledge_fts")
data class SwaraKnowledgeFts(
    @ColumnInfo(name = "keyword") val keyword: String,
    @ColumnInfo(name = "content") val content: String
)

@Dao
interface SwaraDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<SwaraKnowledge>)

    @Query("""
        SELECT * FROM swara_knowledge 
        JOIN swara_knowledge_fts ON swara_knowledge.keyword = swara_knowledge_fts.keyword
        WHERE swara_knowledge_fts MATCH :query
    """)
    suspend fun search(query: String): List<SwaraKnowledge>

    @Query("SELECT COUNT(*) FROM swara_knowledge")
    suspend fun getCount(): Int

    @Query("DELETE FROM swara_knowledge")
    suspend fun clear()
}
