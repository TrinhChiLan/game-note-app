package com.example.assignment3.viewmodels

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.assignment3.adapters.HomeItem
import com.example.assignment3.data.AppDatabase
import com.example.assignment3.data.GameEntity
import com.example.assignment3.data.ImageStorageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val gameDao = AppDatabase.getDatabase(application).gameDao()
    private val searchQuery = MutableStateFlow("")

    val homeItems: StateFlow<List<HomeItem>> = combine(
        gameDao.getFavoriteGames(),
        gameDao.getOtherGames(),
        searchQuery
    ) { favorites, others, query ->
        val items = mutableListOf<HomeItem>()
        
        items.add(HomeItem.SearchBar)

        val filteredFavorites = if (query.isEmpty()) favorites else favorites.filter { it.name.contains(query, ignoreCase = true) }
        val filteredOthers = if (query.isEmpty()) others else others.filter { it.name.contains(query, ignoreCase = true) }

        if (filteredFavorites.isNotEmpty()) {
            items.add(HomeItem.Header("Favorite"))
            items.addAll(filteredFavorites.map { HomeItem.Game(it) })
        }

        if (filteredOthers.isNotEmpty()) {
            items.add(HomeItem.Header("Other"))
            items.addAll(filteredOthers.map { HomeItem.Game(it) })
        }

        items
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = listOf(HomeItem.SearchBar)
    )

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun deleteGame(game: GameEntity) {
        viewModelScope.launch {
            if (game.imageUrl.startsWith("/")) {
                ImageStorageManager.deleteImageFromInternalStorage(game.imageUrl)
            }
            if (game.imageUrlAdditional?.startsWith("/") == true) {
                ImageStorageManager.deleteImageFromInternalStorage(game.imageUrlAdditional)
            }
            gameDao.deleteGame(game)
        }
    }

    fun toggleFavorite(game: GameEntity) {
        viewModelScope.launch {
            gameDao.updateGame(game.copy(isFavorite = !game.isFavorite))
        }
    }

    fun updateGameImage(game: GameEntity, uriString: String, isHeader: Boolean) {
        viewModelScope.launch {
            val context = getApplication<Application>().applicationContext
            val savedPath = ImageStorageManager.saveImageToInternalStorage(context, Uri.parse(uriString))
            
            if (savedPath != null) {
                val updatedGame = if (isHeader) {
                    if (game.imageUrlAdditional?.startsWith("/") == true) {
                        ImageStorageManager.deleteImageFromInternalStorage(game.imageUrlAdditional)
                    }
                    game.copy(imageUrlAdditional = savedPath)
                } else {
                    if (game.imageUrl.startsWith("/")) {
                        ImageStorageManager.deleteImageFromInternalStorage(game.imageUrl)
                    }
                    game.copy(imageUrl = savedPath)
                }
                gameDao.updateGame(updatedGame)
            }
        }
    }
}
