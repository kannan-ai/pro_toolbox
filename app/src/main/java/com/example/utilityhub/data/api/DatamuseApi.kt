package com.example.utilityhub.data.api

import retrofit2.http.GET
import retrofit2.http.Query

interface DatamuseApi {
    @GET("words")
    suspend fun getSynonyms(
        @Query("rel_syn") word: String,
        @Query("max") max: Int = 6
    ): List<SynonymResponse>
}

data class SynonymResponse(
    val word: String,
    val score: Int
)
