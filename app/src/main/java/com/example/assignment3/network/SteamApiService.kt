package com.example.assignment3.network

import retrofit2.http.GET
import retrofit2.http.Query

interface SteamApiService {
    @GET("api/storesearch/")
    suspend fun searchGames(
        @Query("term") query: String,
        @Query("l") language: String = "english",
        @Query("cc") countryCode: String = "US"
    ): SteamSearchResponse

    @GET("api/appdetails")
    suspend fun getAppDetails(
        @Query("appids") appId: Int,
        @Query("l") language: String = "english"
    ): Map<String, SteamAppDetailsResponse>
}
