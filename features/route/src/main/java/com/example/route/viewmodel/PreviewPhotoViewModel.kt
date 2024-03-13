package com.example.route.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.entities.route.Photo
import com.example.domain.use_cases.PhotosUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class PreviewPhotoViewModel(
    private val photoUrl: String,
    private val basicId: String
): ViewModel(), KoinComponent {

    private var savePhotoJob: Job? = null
    private val photosUseCase: PhotosUseCase by inject()

    private val _uiState = MutableStateFlow(PreviewPhotoUiState())
    val uiState = _uiState.asStateFlow()
    fun savePhoto(){
        val photo = Photo(
            basicId = basicId,
            urlPhoto = photoUrl
        )
        savePhotoJob?.cancel()
        savePhotoJob = photosUseCase.addingPhoto(photo).onEach { result ->
            _uiState.update {
                it.copy(
                    savePhotoState = result
                )
            }
        }.launchIn(viewModelScope)
    }

    fun resetSaveState() {
        _uiState.update {
            it.copy(savePhotoState = null)
        }
    }
}