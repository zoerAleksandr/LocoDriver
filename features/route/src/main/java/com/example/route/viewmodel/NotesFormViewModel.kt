package com.example.route.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.ResultState
import com.example.domain.entities.route.Notes
import com.example.domain.use_cases.NotesUseCase
import com.example.domain.util.addOrReplace
import com.example.route.Const.NULLABLE_ID
import com.example.route.extention.EMPTY_IMAGE_URI
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class NotesFormViewModel constructor(
    notesId: String?,
    basicId: String
) : ViewModel(), KoinComponent {
    private val _uiState = MutableStateFlow(NotesFormUiState())
    val uiState = _uiState.asStateFlow()

    private val notesUseCase: NotesUseCase by inject()
    private var loadNotesJob: Job? = null
    private var saveNotesJob: Job? = null
    var currentNotes: Notes?
        get() {
            return _uiState.value.notesDetailState.let {
                if (it is ResultState.Success) it.data else null
            }
        }
        set(value) {
            _uiState.update {
                it.copy(
                    notesDetailState = ResultState.Success(value)
                )
            }
        }

    var photosListState: SnapshotStateList<String>
        get() {
            return _uiState.value.photosListState ?: mutableStateListOf(EMPTY_IMAGE_URI)
        }
        set(value) {
            _uiState.update {
                it.copy(
                    photosListState = value
                )
            }
        }

    init {
        if (notesId == NULLABLE_ID) {
            currentNotes = Notes(basicId = basicId)
        } else {
            loadNotes(notesId!!)
        }
    }

    private fun loadNotes(notesId: String) {
        loadNotesJob?.cancel()
        loadNotesJob = notesUseCase.loadNotes(notesId).onEach { resultState ->
            _uiState.update {
                it.copy(
                    notesDetailState = resultState
                )
            }
            if (resultState is ResultState.Success) {
                currentNotes = resultState.data
                resultState.data?.let { notes ->
                    setPhotosList(notes.photos)
                }
            }
        }.launchIn(viewModelScope)
    }

    private fun setPhotosList(list: MutableList<String>) {
        photosListState.add(EMPTY_IMAGE_URI)
        list.forEach { photo ->
            photosListState.addOrReplace(photo)
        }
    }

    fun saveNotes() {
        val state = _uiState.value.notesDetailState
        if (state is ResultState.Success) {
            state.data?.let { notes ->
                val photoList = photosListState
                photoList.remove(EMPTY_IMAGE_URI)
                notes.photos = photoList
                saveNotesJob?.cancel()
                saveNotesJob = notesUseCase.saveNotes(notes).onEach { resultState ->
                    _uiState.update {
                        it.copy(
                            saveNotesState = resultState
                        )
                    }
                }.launchIn(viewModelScope)
            }
        }
    }

    fun resetSaveState() {
        _uiState.update {
            it.copy(saveNotesState = null)
        }
    }

    fun clearAllField() {
        currentNotes = currentNotes?.copy(
            text = null
        )
        photosListState = mutableStateListOf(EMPTY_IMAGE_URI)
    }

    fun setNoteText(text: String) {
        currentNotes = currentNotes?.copy(
            text = text
        )
    }

    fun addingPhoto(url: String) {
        photosListState.add(url)
    }

    fun deletePhoto(url: String) {
        photosListState.remove(url)
    }
}