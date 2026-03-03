package com.z_company.shared.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.core.ResultState
import com.z_company.domain.entities.setting.SalarySetting
import com.z_company.domain.use_cases.SalarySettingUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Shared KMP ViewModel for the salary settings screen.
 *
 * Loads [SalarySetting] from [SalarySettingUseCase] and exposes
 * per-field update helpers so the UI can bind directly to individual fields.
 * Call [saveSalarySetting] to persist the current state.
 *
 * Used by both Android and iOS.
 */
class SettingSalarySharedViewModel(
    private val salarySettingUseCase: SalarySettingUseCase,
) : ViewModel() {

    private val _setting = MutableStateFlow<SalarySetting?>(null)
    val setting: StateFlow<SalarySetting?> = _setting.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSaved = MutableStateFlow(false)
    val isSaved: StateFlow<Boolean> = _isSaved.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        viewModelScope.launch {
            salarySettingUseCase.salarySettingFlow().collect { s ->
                _setting.value = s
                _isLoading.value = false
            }
        }
    }

    // -----------------------------------------------------------------------
    // Field updaters — each creates a copy of the current SalarySetting
    // -----------------------------------------------------------------------

    fun updateAveragePaymentHour(value: Double) =
        update { copy(averagePaymentHour = value) }

    fun updateNightTimePercent(value: Double) =
        update { copy(nightTimePercent = value) }

    fun updateZonalSurcharge(value: Double) =
        update { copy(zonalSurcharge = value) }

    fun updateSurchargeQualificationClass(value: Double) =
        update { copy(surchargeQualificationClass = value) }

    fun updateHarmfulnessPercent(value: Double) =
        update { copy(harmfulnessPercent = value) }

    fun updateDistrictCoefficient(value: Double) =
        update { copy(districtCoefficient = value) }

    fun updateNordicPercent(value: Double) =
        update { copy(nordicPercent = value) }

    fun updateOnePersonOperationPercent(value: Double) =
        update { copy(onePersonOperationPercent = value) }

    fun updateOnePersonOperationPassengerTrainPercent(value: Double) =
        update { copy(onePersonOperationPassengerTrainPercent = value) }

    fun updatePercentLongDistanceTrain(value: Double) =
        update { copy(percentLongDistanceTrain = value) }

    fun updateOtherSurcharge(value: Double) =
        update { copy(otherSurcharge = value) }

    fun updateNdfl(value: Double) =
        update { copy(ndfl = value) }

    fun updateUnionistsRetention(value: Double) =
        update { copy(unionistsRetention = value) }

    fun updateOtherRetention(value: Double) =
        update { copy(otherRetention = value) }

    /** Persists current salary setting to the local DB. */
    fun saveSalarySetting() {
        val current = _setting.value ?: return
        _isSaved.value = false
        _errorMessage.value = null
        viewModelScope.launch {
            salarySettingUseCase.saveSalarySetting(current).collect { result ->
                when (result) {
                    is ResultState.Success -> _isSaved.value = true
                    is ResultState.Error -> _errorMessage.value =
                        result.entity.message ?: "Ошибка сохранения настроек"
                    is ResultState.Loading -> { /* waiting */ }
                }
            }
        }
    }

    /** Dismisses the saved confirmation flag (called after UI acknowledges save). */
    fun onSaveAcknowledged() {
        _isSaved.value = false
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private inline fun update(block: SalarySetting.() -> SalarySetting) {
        _setting.value = _setting.value?.block()
    }
}
