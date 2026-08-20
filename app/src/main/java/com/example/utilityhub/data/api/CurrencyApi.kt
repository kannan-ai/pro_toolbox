package com.example.utilityhub.data.api

import retrofit2.http.GET
import retrofit2.http.Path
import com.google.gson.annotations.SerializedName

interface CurrencyApi {
    @GET("latest/{base}")
    suspend fun getLatestRates(
        @Path("base") base: String
    ): CurrencyResponse
}

data class CurrencyResponse(
    val result: String,
    @SerializedName("base_code") val baseCode: String,
    val rates: Map<String, Double>
)
