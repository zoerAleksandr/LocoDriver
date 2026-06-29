package com.z_company.route.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.domain.entities.norma_time.StationNorm
import com.z_company.domain.repositories.StationNormRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class StationNormListViewModel : ViewModel(), KoinComponent {
    private val repository: StationNormRepository by inject()

    private val _stations = MutableStateFlow<List<StationNorm>>(emptyList())
    val stationsFlow = _stations.asStateFlow()

    val withNorms
        get() = _stations.value.filter {
            it.appearanceToStartMin != null && it.endToBarrierMin != null &&
                it.barrierToStartMin != null && it.endToWorkEndMin != null
        }
    val withoutNorms
        get() = _stations.value.filter {
            it.appearanceToStartMin == null || it.endToBarrierMin == null ||
                it.barrierToStartMin == null || it.endToWorkEndMin == null
        }

    init {
        repository.getAllFlow()
            .onEach { _stations.value = it }
            .launchIn(viewModelScope)
    }

    fun delete(station: StationNorm) {
        viewModelScope.launch {
            val updated = _stations.value.filter { it.stationId != station.stationId }
            repository.replaceAll(updated).collect {}
        }
    }
}
