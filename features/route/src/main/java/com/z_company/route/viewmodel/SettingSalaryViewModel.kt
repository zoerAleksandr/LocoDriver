package com.z_company.route.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.core.ResultState
import com.z_company.domain.entities.SalarySetting
import com.z_company.domain.use_cases.SalarySettingUseCase
import com.z_company.domain.util.str
import com.z_company.domain.util.toDoubleOrZero
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SettingSalaryViewModel : ViewModel(), KoinComponent {
    private val salarySettingUseCase: SalarySettingUseCase by inject()
    private val _uiState = MutableStateFlow(SettingSalaryUIState())
    val uiState = _uiState.asStateFlow()

    private var currentSalarySetting: SalarySetting?
        get() {
            return _uiState.value.settingSalaryState.let {
                if (it is ResultState.Success) it.data else null
            }
        }
        private set(value) {
            _uiState.update {
                it.copy(
                    settingSalaryState = ResultState.Success(value),
                    tariffRate = ResultState.Success(value?.tariffRate.str()),
                    zonalSurcharge = ResultState.Success(value?.zonalSurcharge.str()),
                    surchargeQualificationClass = ResultState.Success(value?.surchargeQualificationClass.str()),
                    surchargeExtendedServicePhase = ResultState.Success(value?.surchargeExtendedServicePhase.str()),
                    surchargeHeavyLongDistanceTrains = ResultState.Success(value?.surchargeHeavyLongDistanceTrains.str()),
                    otherSurchargeState = ResultState.Success(value?.otherSurcharge.str()),
                    ndfl = ResultState.Success(value?.ndfl.str()),
                    unionistsRetentionState = ResultState.Success(value?.unionistsRetention.str()),
                    otherRetention = ResultState.Success(value?.otherRetention.str())
                )
            }
        }

    init {
        loadSalarySetting()
    }

    fun resetSaveState() {
        _uiState.update {
            it.copy(saveSettingState = null)
        }
    }

    fun saveSetting() {
        viewModelScope.launch {
            val state = uiState.value.settingSalaryState
            if (state is ResultState.Success) {
                state.data?.let { salarySetting ->
                    salarySettingUseCase.saveSalarySetting(salarySetting).collect { saveResult ->
                        _uiState.update {
                            it.copy(
                                saveSettingState = saveResult
                            )
                        }
                    }
                }
            }
        }
    }

    private fun loadSalarySetting() {
        viewModelScope.launch {
            salarySettingUseCase.getSalarySetting().collect { result ->
                if (result is ResultState.Success) {
                    currentSalarySetting = if (result.data != null) {
                        result.data
                    } else {
                        SalarySetting()
                    }
                }
            }
        }
    }

    private fun isErrorInputDouble(value: String): Boolean {
        if (value.isBlank()) return false
        return value.toDoubleOrNull() == null
    }

    fun setTariffRate(value: String) {
        currentSalarySetting = currentSalarySetting?.copy(
            tariffRate = value.toDoubleOrZero()
        )
        _uiState.update {
            it.copy(
                tariffRate = ResultState.Success(value),
                isErrorInputTariffRate = isErrorInputDouble(value)
            )
        }
    }

    fun setZonalSurcharge(value: String) {
        currentSalarySetting = currentSalarySetting?.copy(
            zonalSurcharge = value.toDoubleOrZero()
        )
        _uiState.update {
            it.copy(
                zonalSurcharge = ResultState.Success(value),
                isErrorInputZonalSurcharge = isErrorInputDouble(value)
            )
        }
    }

    fun setSurchargeQualificationClass(value: String) {
        currentSalarySetting = currentSalarySetting?.copy(
            surchargeQualificationClass = value.toDoubleOrZero()
        )
        _uiState.update {
            it.copy(
                surchargeQualificationClass = ResultState.Success(value),
                isErrorInputSurchargeQualificationClass = isErrorInputDouble(value)
            )
        }
    }

    fun setSurchargeExtendedServicePhase(value: String) {
        currentSalarySetting = currentSalarySetting?.copy(
            surchargeExtendedServicePhase = value.toDoubleOrZero()
        )
        _uiState.update {
            it.copy(
                surchargeExtendedServicePhase = ResultState.Success(value),
                isErrorInputSurchargeExtendedServicePhase = isErrorInputDouble(value)
            )
        }
    }

    fun setSurchargeHeavyLongDistanceTrains(value: String) {
        currentSalarySetting = currentSalarySetting?.copy(
            surchargeHeavyLongDistanceTrains = value.toDoubleOrZero()
        )
        _uiState.update {
            it.copy(
                surchargeHeavyLongDistanceTrains = ResultState.Success(value),
                isErrorInputSurchargeHeavyLongDistanceTrains = isErrorInputDouble(value)
            )
        }
    }

    fun setOtherSurcharge(value: String) {
        currentSalarySetting = currentSalarySetting?.copy(
            otherSurcharge = value.toDoubleOrZero()
        )
        _uiState.update {
            it.copy(
                otherSurchargeState = ResultState.Success(value),
                isErrorInputOtherSurcharge = isErrorInputDouble(value)
            )
        }
    }

    fun setNDFL(value: String) {
        currentSalarySetting = currentSalarySetting?.copy(
            ndfl = value.toDoubleOrZero()
        )
        _uiState.update {
            it.copy(
                ndfl = ResultState.Success(value),
                isErrorInputNdfl = isErrorInputDouble(value)
            )
        }
    }

    fun setUnionistsRetention(value: String) {
        currentSalarySetting = currentSalarySetting?.copy(
            unionistsRetention = value.toDoubleOrZero()
        )
        _uiState.update {
            it.copy(
                unionistsRetentionState = ResultState.Success(value),
                isErrorInputUnionistsRetention = isErrorInputDouble(value)
            )
        }
    }

    fun setOtherRetention(value: String) {
        currentSalarySetting = currentSalarySetting?.copy(
            otherRetention = value.toDoubleOrZero()
        )
        _uiState.update {
            it.copy(
                otherRetention = ResultState.Success(value),
                isErrorInputOtherRetention = isErrorInputDouble(value)
            )
        }
    }
}