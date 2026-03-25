package com.example.assignment3.network

import com.google.gson.annotations.SerializedName

data class SteamSearchResponse(
    val total: Int,
    val items: List<SteamGame>
)

data class SteamGame(
    val id: Int,
    val name: String,
    @SerializedName("tiny_image") val tinyImage: String?
) {
    val headerImage: String
        get() = "https://cdn.akamai.steamstatic.com/steam/apps/$id/header.jpg"

    val wideCapsuleImage: String
        get() = "https://cdn.akamai.steamstatic.com/steam/apps/$id/capsule_616x353.jpg"

    val libraryHeroImage: String
        get() = "https://cdn.akamai.steamstatic.com/steam/apps/$id/library_hero.jpg"
}

data class SteamAppDetailsResponse(
    val success: Boolean,
    val data: SteamAppDetails?
)

data class SteamAppDetails(
    val name: String,
    @SerializedName("header_image") val headerImage: String,
    val screenshots: List<SteamScreenshot>?,
    val genres: List<SteamGenre>?,
    @SerializedName("release_date") val releaseDate: SteamReleaseDate?
)

data class SteamScreenshot(
    val id: Int,
    @SerializedName("path_full") val pathFull: String
)

data class SteamGenre(
    val description: String
)

data class SteamReleaseDate(
    val date: String
)
