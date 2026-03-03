package com.z_company.iosapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.core.ResultState
import com.z_company.domain.entities.route.Locomotive
import com.z_company.domain.entities.route.LocoType
import com.z_company.domain.entities.route.SectionDiesel
import com.z_company.domain.entities.route.SectionElectric
import com.z_company.domain.use_cases.LocomotiveUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * KMP ViewModel for the locomotive create/edit form.
 *
 * Registered as Koin single. State resets on [loadLoco] call.
 *
 * @param locoUseCase UseCase from domain module (KMP-compatible).
 */
class LocoFormIosViewModel(
    private val locoUseCase: LocomotiveUseCase,
) : ViewModel() {

    private val _locomotive = MutableStateFlow<Locomotive?>(null)
    val locomotive: StateFlow<Locomotive?> = _locomotive.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSaved = MutableStateFlow(false)
    val isSaved: StateFlow<Boolean> = _isSaved.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var loadJob: Job? = null

    /**
     * Loads locomotive by [locoId]. If null, creates a new empty locomotive for [basicId].
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
                    is ResultState.Loading -> {
                        _isLoading.value = true
                    }
                }
            }
        }
    }

    /** Updates locomotive series. */
    fun updateSeries(value: String) {
        val current = _locomotive.value ?: return
        _locomotive.value = current.copy(series = value.ifBlank { null })
    }

    /** Updates locomotive number. */
    fun updateNumber(value: String) {
        val current = _locomotive.value ?: return
        _locomotive.value = current.copy(number = value.ifBlank { null })
    }

    /** Sets locomotive traction type (electric or diesel). */
    fun setType(type: LocoType) {
        val current = _locomotive.value ?: return
        _locomotive.value = current.copy(type = type)
    }

    // --- Acceptance times ---

    fun setTimeStartAcceptance(millis: Long?) {
        val current = _locomotive.value ?: return
        _locomotive.value = current.copy(timeStartOfAcceptance = millis)
    }

    fun setTimeEndAcceptance(millis: Long?) {
        val current = _locomotive.value ?: return
        _locomotive.value = current.copy(timeEndOfAcceptance = millis)
    }

    // --- Delivery times ---

    fun setTimeStartDelivery(millis: Long?) {
        val current = _locomotive.value ?: return
        _locomotive.value = current.copy(timeStartOfDelivery = millis)
    }

    fun setTimeEndDelivery(millis: Long?) {
        val current = _locomotive.value ?: return
        _locomotive.value = current.copy(timeEndOfDelivery = millis)
    }

    // --- Electric sections ---

    /** Adds a new empty electric section. */
    fun addElectricSection() {
        val current = _locomotive.value ?: return
        val newSection = SectionElectric(locoId = current.locoId)
        _locomotive.value = current.copy(
            electricSectionList = (current.electricSectionList + newSection).toMutableList()
        )
    }

    /** Removes the electric section at [index]. */
    fun removeElectricSection(index: Int) {
        val current = _locomotive.value ?: return
        val updated = current.electricSectionList.toMutableList()
        if (index in updated.indices) updated.removeAt(index)
        _locomotive.value = current.copy(electricSectionList = updated)
    }

    /** Updates accepted energy for electric section at [index]. */
    fun updateElectricSectionAccepted(index: Int, value: String) {
        updateElectricSection(index) { it.copy(acceptedEnergy = value.toDoubleOrNull()) }
    }

    /** Updates delivery energy for electric section at [index]. */
    fun updateElectricSectionDelivery(index: Int, value: String) {
        updateElectricSection(index) { it.copy(deliveryEnergy = value.toDoubleOrNull()) }
    }

    /** Updates accepted recovery for electric section at [index]. */
    fun updateElectricSectionAcceptedRecovery(index: Int, value: String) {
        updateElectricSection(index) { it.copy(acceptedRecovery = value.toDoubleOrNull()) }
    }

    /** Updates delivery recovery for electric section at [index]. */
    fun updateElectricSectionDeliveryRecovery(index: Int, value: String) {
        updateElectricSection(index) { it.copy(deliveryRecovery = value.toDoubleOrNull()) }
    }

    private fun updateElectricSection(index: Int, transform: (SectionElectric) -> SectionElectric) {
        val current = _locomotive.value ?: return
        val updated = current.electricSectionList.toMutableList()
        if (index in updated.indices) updated[index] = transform(updated[index])
        _locomotive.value = current.copy(electricSectionList = updated)
    }

    // --- Diesel sections ---

    /** Adds a new empty diesel section. */
    fun addDieselSection() {
        val current = _locomotive.value ?: return
        val newSection = SectionDiesel(locoId = current.locoId)
        _locomotive.value = current.copy(
            dieselSectionList = (current.dieselSectionList + newSection).toMutableList()
        )
    }

    /** Removes the diesel section at [index]. */
    fun removeDieselSection(index: Int) {
        val current = _locomotive.value ?: return
        val updated = current.dieselSectionList.toMutableList()
        if (index in updated.indices) updated.removeAt(index)
        _locomotive.value = current.copy(dieselSectionList = updated)
    }

    /** Updates accepted fuel for diesel section at [index]. */
    fun updateDieselSectionAccepted(index: Int, value: String) {
        updateDieselSection(index) { it.copy(acceptedFuel = value.toDoubleOrNull()) }
    }

    /** Updates delivery fuel for diesel section at [index]. */
    fun updateDieselSectionDelivery(index: Int, value: String) {
        updateDieselSection(index) { it.copy(deliveryFuel = value.toDoubleOrNull()) }
    }

    /** Updates coefficient for diesel section at [index]. */
    fun updateDieselSectionCoefficient(index: Int, value: String) {
        updateDieselSection(index) { it.copy(coefficient = value.toDoubleOrNull()) }
    }

    /** Updates fuel supply for diesel section at [index]. */
    fun updateDieselSectionFuelSupply(index: Int, value: String) {
        updateDieselSection(index) { it.copy(fuelSupply = value.toDoubleOrNull()) }
    }

    private fun updateDieselSection(index: Int, transform: (SectionDiesel) -> SectionDiesel) {
        val current = _locomotive.value ?: return
        val updated = current.dieselSectionList.toMutableList()
        if (index in updated.indices) updated[index] = transform(updated[index])
        _locomotive.value = current.copy(dieselSectionList = updated)
    }

    /** Saves the current locomotive to the DB. */
    fun saveLoco() {
        val current = _locomotive.value ?: return
        viewModelScope.launch {
            locoUseCase.saveLocomotive(current).collect { result ->
                when (result) {
                    is ResultState.Success -> _isSaved.value = true
                    is ResultState.Error -> _errorMessage.value =
                        result.entity.message ?: "Ошибка сохранения"
                    is ResultState.Loading -> {}
                }
            }
        }
    }

    /** Clears the error message after it has been shown. */
    fun clearError() {
        _errorMessage.value = null
    }
}
