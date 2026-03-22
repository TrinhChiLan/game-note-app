package com.example.assignment3.network

import com.google.gson.annotations.SerializedName

data class RawgResponse(
    val results: List<RawgGame>
)

data class RawgGame(
    val id: Int,
    val name: String,
    @SerializedName("background_image") val backgroundImage: String?,
    val released: String?,
    val genres: List<RawgGenre>?
)

data class RawgGenre(
    val name: String
)
