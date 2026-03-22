package com.example.assignment3.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "games")
data class GameEntity(
    @PrimaryKey val id: Int, // From RAWG API
    val name: String,
    val imageUrl: String, // Current thumbnail (API or Custom)
    val apiImageUrl: String, // Original API thumbnail for fallback
    val imageUrlAdditional: String? = null, // Custom header image
    val releaseDate: String?,
    val genres: String?,
    val status: String, // "PLAYING", "WANT_TO_PLAY", "FINISHED", "NONE"
    val isFavorite: Boolean = false
)
