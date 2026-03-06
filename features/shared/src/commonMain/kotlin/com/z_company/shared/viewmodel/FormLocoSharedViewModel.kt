package com.z_company.shared.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.core.ResultState
import com.z_company.domain.entities.route.LocoType
import com.z_company.domain.entities.route.Locomotive
import com.z_company.domain.entities.route.SectionDiesel
import com.z_company.domain.entities.route.SectionElectric
import com.z_company.domain.use_cases.LocomotiveUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Shared KMP ViewModel for the locomotive create/edit form.
 *
 * Based on Android LocoFormViewModel with constructor injection.
 * Supports both Electric and Diesel sections, series list from settings.
 */
class FormLocoSharedViewModel(
    private val locoUseCase: LocomotiveUseCase,
    private val settingsUseCase: SettingsUseCase,
) : ViewModel() {

    private val _locomotive = MutableStateFlow<Locomotive?>(null)
    val locomotive: StateFlow<Locomotive?> = _locomotive.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSaved = MutableStateFlow(false)
    val isSaved: StateFlow<Boolean> = _isSaved.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _seriesList = MutableStateFlow<List<String>>(emptyList())
    val seriesList: StateFlow<List<String>> = _seriesList.asStateFlow()

    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            settingsUseCase.getUserSettingFlow().collect { settings ->
                _seriesList.value = settings.locomotiveSeriesList
            }
        }
    }

    /**
     * Loads locomotive by [locoId]. If null, creates a new loco for [basicId].
     */
    fun loadLoco(locoId: String?, basicId: String) {
        loadJob?.cancel()
        _isSaved.value = false
        _errorMessage.value = null

        if (locoId == null) {
            _locomotive.value = Locomotive(basicId = basicId)
            _isLoading.value = false
            return
        }

        loadJob = viewModelScope.launch {
            _isLoading.value = true
            locoUseCase.getLocoById(locoId).collect { result ->
                when (result) {
                    is ResultState.Success -> {
                        _locomotive.value = result.data ?: Locomotive(basicId = basicId)
                        _isLoading.value = false
                    }
                    is ResultState.Error -> {
                        _errorMessage.value = result.entity.message ?: "Ошибка загрузки локомотива"
                        _isLoading.value = false
                    }
                    is ResultState.Loading -> _isLoading.value = true
                }
            }
        }
    }

    // --- Basic fields ---

    fun updateSeries(value: String) = updateLoco { copy(series = value.ifBlank { null }) }
    fun updateNumber(value: String) = updateLoco { copy(number = value.ifBlank { null }) }

    fun setType(type: LocoType) = updateLoco { copy(type = type) }

    fun setTimeStartAcceptance(millis: Long?) = updateLoco { copy(timeStartOfAcceptance = millis) }
    fun setTimeEndAcceptance(millis: Long?) = updateLoco { copy(timeEndOfAcceptance = millis) }
    fun setTimeStartDelivery(millis: Long?) = updateLoco { copy(timeStartOfDelivery = millis) }
    fun setTimeEndDelivery(millis: Long?) = updateLoco { copy(timeEndOfDelivery = millis) }

    // --- Heating counters ---

    fun updateHeatingAccepted(value: Double?) = updateLoco { copy(heatingCounterAccepted = value) }
    fun updateHeatingDelivery(value: Double?) = updateLoco { copy(heatingCounterDelivery = value) }

    // --- Electric sections ---

    fun addElectricSection() {
        val current = _locomotive.value ?: return
        val section = SectionElectric(locoId = current.locoId)
        _locomotive.value = current.copy(
            electricSectionList = (current.electricSectionList + section).toMutableList()
        )
    }

    fun removeElectricSection(index: Int) {
        val current = _locomotive.value ?: return
        val list = current.electricSectionList.toMutableList()
        if (index in list.indices) list.removeAt(index)
        _locomotive.value = current.copy(electricSectionList = list)
    }

    fun updateElectricAccepted(index: Int, value: Double?) =
        updateElectricSection(index) { it.copy(acceptedEnergy = value) }

    fun updateElectricDelivery(index: Int, value: Double?) =
        updateElectricSection(index) { it.copy(deliveryEnergy = value) }

    fun updateElectricAcceptedRecovery(index: Int, value: Double?) =
        updateElectricSection(index) { it.copy(acceptedRecovery = value) }

    fun updateElectricDeliveryRecovery(index: Int, value: Double?) =
        updateElectricSection(index) { it.copy(deliveryRecovery = value) }

    fun updateElectricAcceptedOther(index: Int, value: Double?) =
        updateElectricSection(index) { it.copy(acceptedEnergyOtherCurrent = value) }

    fun updateElectricDeliveryOther(index: Int, value: Double?) =
        updateElectricSection(index) { it.copy(deliveryEnergyOtherCurrent = value) }

    fun updateElectricAcceptedRecoveryOther(index: Int, value: Double?) =
        updateElectricSection(index) { it.copy(acceptedRecoveryOtherCurrent = value) }

    fun updateElectricDeliveryRecoveryOther(index: Int, value: Double?) =
        updateElectricSection(index) { it.copy(deliveryRecoveryOtherCurrent = value) }

    // --- Diesel sections ---

    fun addDieselSection() {
        val current = _locomotive.value ?: return
        val section = SectionDiesel(locoId = current.locoId)
        _locomotive.value = current.copy(
            dieselSectionList = (current.dieselSectionList + section).toMutableList()
        )
    }

    fun removeDieselSection(index: Int) {
        val current = _locomotive.value ?: return
        val list = current.dieselSectionList.toMutableList()
        if (index in list.indices) list.removeAt(index)
        _locomotive.value = current.copy(dieselSectionList = list)
    }

    fun updateDieselAccepted(index: Int, value: Double?) =
        updateDieselSection(index) { it.copy(acceptedFuel = value) }

    fun updateDieselDelivery(index: Int, value: Double?) =
        updateDieselSection(index) { it.copy(deliveryFuel = value) }

    fun updateDieselCoefficient(index: Int, value: Double?) =
        updateDieselSection(index) { it.copy(coefficient = value) }

    fun updateDieselFuelSupply(index: Int, value: Double?) =
        updateDieselSection(index) { it.copy(fuelSupply = value) }

    // --- Save ---

    fun saveLoco() {
        val current = _locomotive.value ?: return
        viewModelScope.launch {
            locoUseCase.saveLocomotive(current).collect { result ->
                when (result) {
                    is ResultState.Success -> _isSaved.value = true
                    is ResultState.Error -> _errorMessage.value =
                        result.entity.message ?: "Ошибка сохранения"
                    is ResultState.Loading -> { /* waiting */ }
                }
            }
        }
    }

    fun clearError() { _errorMessage.value = null }

    // --- Helpers ---

    private inline fun updateLoco(block: Locomotive.() -> Locomotive) {
        _locomotive.value = _locomotive.value?.block()
    }

    private inline fun updateElectricSection(index: Int, transform: (SectionElectric) -> SectionElectric) {
        val current = _locomotive.value ?: return
        val list = current.electricSectionList.toMutableList()
        if (index in list.indices) list[index] = transform(list[index])
        _locomotive.value = current.copy(electricSectionList = list)
    }

    private inline fun updateDieselSection(index: Int, transform: (SectionDiesel) -> SectionDiesel) {
        val current = _locomotive.value ?: return
        val list = current.dieselSectionList.toMutableList()
        if (index in list.indices) list[index] = transform(list[index])
        _locomotive.value = current.copy(dieselSectionList = list)
    }
}
