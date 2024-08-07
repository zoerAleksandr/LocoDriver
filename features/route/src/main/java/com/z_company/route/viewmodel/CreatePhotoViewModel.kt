package com.z_company.route.viewmodel

import androidx.lifecycle.ViewModel
import com.z_company.domain.use_cases.PhotoUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


class CreatePhotoViewModel(
    private val basicId: String
) : ViewModel(), KoinComponent {

    private val photoUseCase: PhotoUseCase by inject()
    private val _uiState = MutableStateFlow(CreatePhotoScreenUiState())
    val uiState = _uiState.asStateFlow()

    private var savePhotoJob: Job? = null
}