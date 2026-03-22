package com.example.assignment3.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.assignment3.data.AppDatabase
import com.example.assignment3.data.GameEntity
import com.example.assignment3.data.NoteEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

sealed class DetailState {
    object Loading : DetailState()
    data class Success(val game: GameEntity, val notes: List<NoteEntity>) : DetailState()
    data class Error(val message: String) : DetailState()
}

class DetailViewModel(application: Application) : AndroidViewModel(application) {

    private val gameDao = AppDatabase.getDatabase(application).gameDao()
    private val _detailState = MutableStateFlow<DetailState>(DetailState.Loading)
    val detailState: StateFlow<DetailState> = _detailState.asStateFlow()

    fun loadGameDetails(gameId: Int) {
        viewModelScope.launch {
            combine(
                gameDao.getGameByIdFlow(gameId),
                gameDao.getNotesForGame(gameId)
            ) { game, notes ->
                if (game != null) {
                    DetailState.Success(game, notes)
                } else {
                    DetailState.Error("Game not found")
                }
            }.collect { state ->
                _detailState.value = state
            }
        }
    }

    fun updateGameStatus(game: GameEntity, newStatus: String) {
        viewModelScope.launch {
            gameDao.updateGame(game.copy(status = newStatus))
        }
    }

    fun toggleFavorite(game: GameEntity) {
        viewModelScope.launch {
            gameDao.updateGame(game.copy(isFavorite = !game.isFavorite))
        }
    }

    fun deleteGame(game: GameEntity) {
        viewModelScope.launch {
            gameDao.deleteGame(game)
        }
    }

    fun copyNote(note: NoteEntity) {
        viewModelScope.launch {
            val copy = note.copy(id = 0, title = "${note.title} (Copy)")
            gameDao.insertNote(copy)
        }
    }

    fun deleteNote(note: NoteEntity) {
        viewModelScope.launch {
            gameDao.deleteNote(note)
        }
    }
}
