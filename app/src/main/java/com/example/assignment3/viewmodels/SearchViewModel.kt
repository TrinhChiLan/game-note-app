package com.example.assignment3.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.assignment3.data.AppDatabase
import com.example.assignment3.data.GameEntity
import com.example.assignment3.network.NetworkClient
import com.example.assignment3.network.RawgGame
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class SearchState {
    object Idle : SearchState()
    object Loading : SearchState()
    data class Success(val games: List<RawgGame>) : SearchState()
    data class Error(val message: String) : SearchState()
}

class SearchViewModel(application: Application) : AndroidViewModel(application) {

    private val _searchState = MutableStateFlow<SearchState>(SearchState.Idle)
    val searchState: StateFlow<SearchState> = _searchState

    private val gameDao = AppDatabase.getDatabase(application).gameDao()

    // Since we won't modify build.gradle, you can manually paste your API key here
    private val API_KEY = "a86a6853a1eb4ce6aa8fd1b4e0ff4b44"

    fun searchGames(query: String) {
        if (query.isEmpty()) {
            _searchState.value = SearchState.Idle
            return
        }

        viewModelScope.launch {
            _searchState.value = SearchState.Loading
            try {
                val response = NetworkClient.apiService.searchGames(API_KEY, query)
                _searchState.value = SearchState.Success(response.results)
            } catch (e: Exception) {
                _searchState.value = SearchState.Error(e.message ?: "Unknown error occurred")
            }
        }
    }

    fun saveGame(rawgGame: RawgGame) {
        viewModelScope.launch {
            val genre = rawgGame.genres?.firstOrNull()?.name ?: "Unknown"
            val year = rawgGame.released?.take(4) ?: "xxxx"

            val gameEntity = GameEntity(
                id = rawgGame.id,
                name = rawgGame.name,
                imageUrl = rawgGame.backgroundImage ?: "",
                releaseDate = rawgGame.released,
                genres = "$genre - $year",
                status = "NONE",
                isFavorite = false
            )
            gameDao.insertGame(gameEntity)
        }
    }
}
