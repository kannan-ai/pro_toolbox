package com.example.utilityhub.data.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {
    private const val TRANSLATE_BASE_URL = "https://translate.googleapis.com/"
    private const val DATAMUSE_BASE_URL = "https://api.datamuse.com/"
    private const val CURRENCY_BASE_URL = "https://open.er-api.com/v6/"

    val translationApi: TranslationApi by lazy {
        Retrofit.Builder()
            .baseUrl(TRANSLATE_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TranslationApi::class.java)
    }

    val datamuseApi: DatamuseApi by lazy {
        Retrofit.Builder()
            .baseUrl(DATAMUSE_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DatamuseApi::class.java)
    }

    val currencyApi: CurrencyApi by lazy {
        Retrofit.Builder()
            .baseUrl(CURRENCY_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CurrencyApi::class.java)
    }
}
