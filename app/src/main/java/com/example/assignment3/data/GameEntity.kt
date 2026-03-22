package com.example.assignment3.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "games")
data class GameEntity(
    @PrimaryKey val id: Int, // From RAWG API
    val name: String,
    val imageUrl: String,
    val releaseDate: String?,
    val genres: String?,
    val status: String, // "PLAYING", "WANT_TO_PLAY", "FINISHED", "NONE"
    val isFavorite: Boolean = false
)
