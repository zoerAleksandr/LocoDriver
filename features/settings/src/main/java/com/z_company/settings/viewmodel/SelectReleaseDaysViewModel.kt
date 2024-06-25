package com.z_company.settings.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.core.ResultState
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.ReleasePeriod
import com.z_company.domain.use_cases.CalendarUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.Calendar

class SelectReleaseDaysViewModel : ViewModel(), KoinComponent {
    private val calendarUseCase: CalendarUseCase by inject()

    private val _uiState = MutableStateFlow(SelectReleaseDaysUIState())
    val uiState = _uiState.asStateFlow()

    private var releasePeriodListState: SnapshotStateList<ReleasePeriod>
        get() {
            return uiState.value.releaseDaysPeriodState ?: mutableStateListOf()
        }
        set(value) {
            _uiState.update {
                it.copy(
                    releaseDaysPeriodState = value
                )
            }
        }

    var currentMonthOfYear: MonthOfYear?
        get() {
            return _uiState.value.calendarState.let {
                if (it is ResultState.Success) it.data else null
            }
        }
        private set(value) {
            _uiState.update {
                it.copy(calendarState = ResultState.Success(value))
            }
        }

    fun addReleasePeriod(period: ReleasePeriod) {
        releasePeriodListState.add(period)
    }

    fun deleteReleasePeriod(period: ReleasePeriod) {
        releasePeriodListState.remove(period)
    }

    init {
        viewModelScope.launch {
            calendarUseCase.loadMonthOfYearList().collect { result ->
                if (result is ResultState.Success) {
                    val currentDate = Calendar.getInstance()
                    val currentMonth = currentDate.get(Calendar.MONTH)
                    val currentYear = currentDate.get(Calendar.YEAR)
                    val monthList = result.data
                    val monthOfYear = monthList.find {
                        it.month == currentMonth && it.year == currentYear
                    }
                    _uiState.update {
                        it.copy(
                            calendarState = ResultState.Success(monthOfYear)
                        )
                    }
                }
            }
        }
    }
}