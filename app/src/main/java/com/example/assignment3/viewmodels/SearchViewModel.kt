package com.example.assignment3.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.assignment3.data.AppDatabase
import com.example.assignment3.data.GameEntity
import com.example.assignment3.network.NetworkClient
import com.example.assignment3.network.SteamGame
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import retrofit2.HttpException

sealed class SearchState {
    object Idle : SearchState()
    object Loading : SearchState()
    data class Success(val games: List<Pair<SteamGame, Boolean>>) : SearchState()
    data class Error(val message: String) : SearchState()
}

class SearchViewModel(application: Application) : AndroidViewModel(application) {

    private val gameDao = AppDatabase.getDatabase(application).gameDao()
    
    private val _rawResults = MutableStateFlow<List<SteamGame>>(emptyList())
    private val _isLoading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

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

        _isLoading.value = true
        searchJob = viewModelScope.launch {
            delay(500)
            performSearch(query)
        }
    }

    fun searchGames(query: String) {
        searchJob?.cancel()
        _isLoading.value = true
        searchJob = viewModelScope.launch {
            performSearch(query)
        }
    }

    private suspend fun performSearch(query: String) {
        _isLoading.value = true
        _error.value = null
        try {
            val response = NetworkClient.steamApiService.searchGames(query)
            _rawResults.value = response.items
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

    fun saveGame(steamGame: SteamGame) {
        viewModelScope.launch {
            val existing = gameDao.getGameById(steamGame.id)
            if (existing != null) return@launch

            try {
                Log.d("Fetch", "Success.")
                // fetch more details including genres, release date, and official images
                val detailsMap = NetworkClient.steamApiService.getAppDetails(steamGame.id)
                val appDetails = detailsMap[steamGame.id.toString()]?.takeIf { it.success }?.data

                val genres = appDetails?.genres?.joinToString(", ") { it.description } ?: "Steam Game"
                val releaseDate = appDetails?.releaseDate?.date
                
                // we can use the first screenshot as a fallback header
                val headerImageUrl = appDetails?.screenshots?.firstOrNull()?.pathFull ?: steamGame.libraryHeroImage

                val gameEntity = GameEntity(
                    id = steamGame.id,
                    name = appDetails?.name ?: steamGame.name,
                    imageUrl = appDetails?.headerImage ?: steamGame.headerImage,
                    apiImageUrl = appDetails?.headerImage ?: steamGame.headerImage,
                    imageUrlAdditional = headerImageUrl,
                    releaseDate = releaseDate,
                    genres = genres,
                    status = "NONE",
                    isFavorite = false
                )
                gameDao.insertGame(gameEntity)
            } catch (e: Exception) {
                // fallback to basic info if details fetch fails
                Log.d("Fetch", "Fall back to basic.")
                val gameEntity = GameEntity(
                    id = steamGame.id,
                    name = steamGame.name,
                    imageUrl = steamGame.headerImage,
                    apiImageUrl = steamGame.headerImage,
                    imageUrlAdditional = steamGame.libraryHeroImage,
                    releaseDate = null,
                    genres = "Steam Game",
                    status = "NONE",
                    isFavorite = false
                )
                gameDao.insertGame(gameEntity)
            }
        }
    }
}
