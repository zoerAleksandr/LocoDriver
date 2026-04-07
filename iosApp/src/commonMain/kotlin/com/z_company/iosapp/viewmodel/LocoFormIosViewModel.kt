package com.z_company.iosapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.domain.entities.route.Locomotive
import com.z_company.domain.entities.route.LocoType
import com.z_company.domain.entities.route.SectionElectric
import com.z_company.domain.entities.route.SectionDiesel
import com.z_company.domain.use_cases.RouteUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LocoFormIosViewModel(
    private val routeUseCase: RouteUseCase,
) : ViewModel() {

    private val _loco = MutableStateFlow<Locomotive?>(null)
    val loco: StateFlow<Locomotive?> = _loco.asStateFlow()

    private val _isSaved = MutableStateFlow(false)
    val isSaved: StateFlow<Boolean> = _isSaved.asStateFlow()

    fun loadLoco(routeId: String, locoId: String?) {
        viewModelScope.launch {
            routeUseCase.routeDetails(routeId).collect { result ->
                val route = (result as? com.z_company.core.ResultState.Success)?.data ?: return@collect
                val found = route.locomotives.find { it.locoId == locoId }
                _loco.value = found ?: Locomotive(basicId = routeId)
            }
        }
    }

    fun setSeries(value: String) {
        _loco.update { it?.copy(series = value.ifBlank { null }) }
    }

    fun setNumber(value: String) {
        _loco.update { it?.copy(number = value.ifBlank { null }) }
    }

    fun setType(type: LocoType) {
        _loco.update { it?.copy(type = type) }
    }

    fun setTimeStartAcceptance(ms: Long?) {
        _loco.update { it?.copy(timeStartOfAcceptance = ms) }
    }

    fun setTimeEndAcceptance(ms: Long?) {
        _loco.update { it?.copy(timeEndOfAcceptance = ms) }
    }

    fun setTimeStartDelivery(ms: Long?) {
        _loco.update { it?.copy(timeStartOfDelivery = ms) }
    }

    fun setTimeEndDelivery(ms: Long?) {
        _loco.update { it?.copy(timeEndOfDelivery = ms) }
    }

    fun watchLoco(callback: (Locomotive?) -> Unit) {
        viewModelScope.launch { loco.collect { callback(it) } }
    }

    fun watchIsSaved(callback: (Boolean) -> Unit) {
        viewModelScope.launch { isSaved.collect { callback(it) } }
    }

    private fun MutableStateFlow<Locomotive?>.update(transform: (Locomotive?) -> Locomotive?) {
        value = transform(value)
    }
}
