package com.z_company.route.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.core.util.ConverterUrlBase64
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
import java.util.Calendar

class PreviewPhotoViewModel(
    private val basicId: String,
) : ViewModel(), KoinComponent {

    private var savePhotoJob: Job? = null
    private val photoUseCase: PhotoUseCase by inject()

    private val _uiState = MutableStateFlow(PreviewPhotoUiState())
    val uiState = _uiState.asStateFlow()
    fun savePhoto(bitmap: Bitmap) {
        val base64 = ConverterUrlBase64.bitmapToBase64(bitmap)
        val currentDate = Calendar.getInstance().timeInMillis
        val photo = Photo(
            basicId = basicId,
            url = base64,
            dateOfCreate = currentDate
        )
        savePhotoJob?.cancel()
        savePhotoJob = photoUseCase.addingPhoto(photo).onEach { result ->
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