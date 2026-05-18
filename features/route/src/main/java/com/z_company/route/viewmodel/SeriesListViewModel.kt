package com.z_company.route.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.domain.entities.norma_time.LocomotiveSeries
import com.z_company.domain.entities.route.LocoType
import com.z_company.domain.repositories.LocomotiveSeriesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SeriesListViewModel : ViewModel(), KoinComponent {
    private val repository: LocomotiveSeriesRepository by inject()

    private val _series = MutableStateFlow<List<LocomotiveSeries>>(emptyList())
    val seriesFlow = _series.asStateFlow()

    val electricSeries get() = _series.value.filter { it.type == LocoType.ELECTRIC }
    val dieselSeries get() = _series.value.filter { it.type == LocoType.DIESEL }

    init {
        repository.getAllFlow()
            .onEach { _series.value = it }
            .launchIn(viewModelScope)
    }

    fun delete(series: LocomotiveSeries) {
        viewModelScope.launch {
            val updated = _series.value.filter { it.seriesId != series.seriesId }
            repository.replaceAll(updated).collect {}
        }
    }
}
