package com.example.assignment3.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.assignment3.data.AppDatabase
import com.example.assignment3.data.GameEntity
import com.example.assignment3.network.NetworkClient
import com.example.assignment3.network.RawgGame
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import retrofit2.HttpException

sealed class SearchState {
    object Idle : SearchState()
    object Loading : SearchState()
    data class Success(val games: List<Pair<RawgGame, Boolean>>) : SearchState()
    data class Error(val message: String) : SearchState()
}

class SearchViewModel(application: Application) : AndroidViewModel(application) {

    private val gameDao = AppDatabase.getDatabase(application).gameDao()
    
    private val _rawResults = MutableStateFlow<List<RawgGame>>(emptyList())
    private val _isLoading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    private val API_KEY = "a86a6853a1eb4ce6aa8fd1b4e0ff4b44"
    private var searchJob: Job? = null

    // Reactively derive the search state from all inputs
    val searchState: StateFlow<SearchState> = combine(
        _rawResults,
        gameDao.getAllGames(),
        _isLoading,
        _error
    ) { rawGames, localGames, loading, error ->
        when {
            error != null -> SearchState.Error(error)
            loading -> SearchState.Loading
            rawGames.isEmpty() -> SearchState.Idle
            else -> {
                val localIds = localGames.map { it.id }.toSet()
                val gamesWithLibraryStatus = rawGames.map { it to localIds.contains(it.id) }
                SearchState.Success(gamesWithLibraryStatus)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SearchState.Idle)

    fun onQueryChanged(query: String) {
        searchJob?.cancel()
        _error.value = null
        if (query.isBlank()) {
            _rawResults.value = emptyList()
            _isLoading.value = false
            return
        }

        searchJob = viewModelScope.launch {
            delay(500)
            performSearch(query)
        }
    }

    fun searchGames(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            performSearch(query)
        }
    }

    private suspend fun performSearch(query: String) {
        _isLoading.value = true
        _error.value = null
        try {
            val response = NetworkClient.apiService.searchGames(API_KEY, query)
            _rawResults.value = response.results
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            val message = when (e.code()) {
                502 -> "Server is busy. Please try again in a moment."
                429 -> "Too many requests. Please slow down."
                else -> "Server error: ${e.code()}"
            }
            _error.value = message
        } catch (e: Exception) {
            _error.value = e.message ?: "Unknown error occurred"
        } finally {
            _isLoading.value = false
        }
    }

    fun saveGame(rawgGame: RawgGame) {
        viewModelScope.launch {
            val existing = gameDao.getGameById(rawgGame.id)
            if (existing != null) return@launch

            val genre = rawgGame.genres?.firstOrNull()?.name ?: "Unknown"
            val year = rawgGame.released?.take(4) ?: "xxxx"

            val gameEntity = GameEntity(
                id = rawgGame.id,
                name = rawgGame.name,
                imageUrl = rawgGame.backgroundImage ?: "",
                apiImageUrl = rawgGame.backgroundImage ?: "",
                releaseDate = rawgGame.released,
                genres = "$genre - $year",
                status = "NONE",
                isFavorite = false
            )
            gameDao.insertGame(gameEntity)
        }
    }
}
