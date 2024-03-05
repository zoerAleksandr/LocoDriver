package com.example.route.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.ResultState
import com.example.domain.use_cases.PhotosUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class NotesFormViewModel constructor(
    private val basicId: String,
) : ViewModel(), KoinComponent {
    private val _uiState = MutableStateFlow(NotesFormUiState())
    val uiState = _uiState.asStateFlow()

    private val photosUseCase: PhotosUseCase by inject()
    private var loadPhotosJob: Job? = null

    private var isSaving by mutableStateOf(false)

    var currentNotes: String?
        get() = _uiState.value.notesText

        private set(value) {
            _uiState.update {
                it.copy(
                    notesText = value
                )
            }
        }

//    var photosListState: MutableList<Photo>?
//        get() {
//            return if (_uiState.value.photosListState is ResultState.Success) {
//                _uiState.value.photosListState.data.toMutableList()
//            } else {
//                mutableListOf()
//            }
//        }
//        set(value) {
//            _uiState.update {
//                it.copy(
//                    photosListState = value
//                )
//            }
//        }

    init {
        loadPhoto(basicId)
    }

    private fun loadPhoto(basicId: String) {
        loadPhotosJob?.cancel()
        loadPhotosJob = photosUseCase.getPhotoByRoute(basicId).onEach { resultState ->
            _uiState.update {
                it.copy(
                    photosListState = resultState
                )
            }
            if (resultState is ResultState.Success) {
                isSaving = true
            }
        }.launchIn(viewModelScope)
    }

    fun clearAllField() {

    }

    fun setNotesText(text: String) {
        currentNotes = text
    }
//
//    fun addingPhoto(url: String) {
//        photosListState.add(url)
//    }
//
//    fun deletePhoto(url: String) {
//        photosListState.remove(url)
//    }
}