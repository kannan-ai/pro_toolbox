package com.example.utilityhub.data.db

import androidx.room.*

@Entity(tableName = "cached_currency")
data class CachedCurrency(
    @PrimaryKey val baseCode: String,
    val ratesJson: String, // Storing as JSON string for simplicity
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface CurrencyCacheDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCache(cache: CachedCurrency)

    @Query("SELECT * FROM cached_currency WHERE baseCode = :baseCode LIMIT 1")
    suspend fun getCache(baseCode: String): CachedCurrency?
}
