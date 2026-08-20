package com.example.utilityhub.data.api

import retrofit2.http.GET
import retrofit2.http.Query
import com.google.gson.JsonArray

interface TranslationApi {
    @GET("translate_a/single")
    suspend fun translate(
        @Query("client") client: String = "gtx",
        @Query("sl") sl: String,
        @Query("tl") tl: String,
        @Query("dt") dt: List<String> = listOf("t", "rm"),
        @Query("q") q: String
    ): JsonArray
}
