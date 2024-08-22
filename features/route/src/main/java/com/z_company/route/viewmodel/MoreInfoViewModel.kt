package com.z_company.route.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.core.ResultState
import com.z_company.domain.use_cases.CalendarUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class MoreInfoViewModel(private val monthOfYearId: String) : ViewModel(), KoinComponent {
    private val calendarUseCase: CalendarUseCase by inject()

    private val _uiState = MutableStateFlow(MoreInfoUiState())
    val uiState = _uiState.asStateFlow()

    init {
        getMonthOfYear()
    }

    private fun getMonthOfYear() {
        _uiState.update {
            it.copy(currentMonthOfYearState = ResultState.Loading)
        }
        viewModelScope.launch {
            calendarUseCase.loadMonthOfYearById(monthOfYearId).collect { result ->
                _uiState.update {
                    it.copy(currentMonthOfYearState = result)
                }
            }
        }
    }
}
