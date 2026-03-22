package com.example.assignment3.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.assignment3.data.AppDatabase
import com.example.assignment3.data.NoteEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class NoteEditorState {
    object Loading : NoteEditorState()
    data class Success(val note: NoteEntity?) : NoteEditorState()
    object Saved : NoteEditorState()
    data class Error(val message: String) : NoteEditorState()
}

class NoteEditorViewModel(application: Application) : AndroidViewModel(application) {

    private val gameDao = AppDatabase.getDatabase(application).gameDao()

    private val _noteEditorState = MutableStateFlow<NoteEditorState>(NoteEditorState.Loading)
    val noteEditorState: StateFlow<NoteEditorState> = _noteEditorState.asStateFlow()

    private var currentNote: NoteEntity? = null

    fun loadNote(noteId: Int) {
        if (noteId == -1) {
            // New note, no loading needed
            _noteEditorState.value = NoteEditorState.Success(null)
            currentNote = null
            return
        }

        viewModelScope.launch {
            _noteEditorState.value = NoteEditorState.Loading
            try {
                val note = gameDao.getNoteById(noteId)
                currentNote = note
                _noteEditorState.value = NoteEditorState.Success(note)
            } catch (e: Exception) {
                _noteEditorState.value = NoteEditorState.Error(e.message ?: "Failed to load note")
            }
        }
    }

    fun saveNote(gameId: Int, title: String, content: String, type: String) {
        viewModelScope.launch {
            try {
                if (currentNote == null) {
                    // Insert new note
                    val newNote = NoteEntity(
                        gameId = gameId,
                        title = title,
                        content = content,
                        type = type,
                        isChecked = false // Default for new notes, will be ignored for text notes
                    )
                    gameDao.insertNote(newNote)
                } else {
                    // Update existing note
                    val updatedNote = currentNote!!.copy(
                        title = title,
                        content = content,
                        type = type
                    )
                    gameDao.updateNote(updatedNote)
                }
                _noteEditorState.value = NoteEditorState.Saved
            } catch (e: Exception) {
                _noteEditorState.value = NoteEditorState.Error(e.message ?: "Failed to save note")
            }
        }
    }
}
