package com.z_company.route.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.core.ResultState
import com.z_company.domain.entities.route.Photo
import com.z_company.domain.use_cases.PhotoUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ViewingImageViewModel(
    private val imageId: String
) : ViewModel(), KoinComponent {
    private val photoUseCase: PhotoUseCase by inject()

    private var loadImageJob: Job? = null
    private var removeImageJob: Job? = null

    private val _uiState = MutableStateFlow(ViewingImageUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadImage()
    }

    private fun loadImage() {
        loadImageJob?.cancel()
        loadImageJob = photoUseCase.getPhotoById(imageId).onEach { result ->
            _uiState.update {
                it.copy(imageState = result)
            }
        }.launchIn(viewModelScope)
    }

    fun removePhoto(photo: Photo) {
        removeImageJob?.cancel()
        removeImageJob = photoUseCase.removePhoto(photo).onEach { result ->
            if (result is ResultState.Success) {
                _uiState.update {
                    it.copy(
                        removeImageState = result
                    )
                }
            }
        }.launchIn(viewModelScope)
    }
}