package com.example.assignment3.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object NetworkClient {
    private const val STEAM_BASE_URL = "https://store.steampowered.com/"

    val steamApiService: SteamApiService by lazy {
        Retrofit.Builder()
            .baseUrl(STEAM_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SteamApiService::class.java)
    }
}
