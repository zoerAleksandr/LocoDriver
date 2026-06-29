@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.z_company.route.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.domain.entities.norma_time.LocomotiveSeries
import com.z_company.domain.entities.route.LocoType
import com.z_company.domain.repositories.LocomotiveSeriesRepository
import com.z_company.domain.util.generateId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

data class SeriesEditorState(
    val seriesId: String? = null,
    val name: String = "",
    val type: LocoType = LocoType.ELECTRIC,
    val acceptanceDurationMin: Int? = null,
    val deliveryDurationMin: Int? = null,
    val saved: Boolean = false,
    val deleted: Boolean = false,
)

class SeriesEditorViewModel(private val seriesId: String?) : ViewModel(), KoinComponent {
    private val repository: LocomotiveSeriesRepository by inject()

    // Generate ID once — prevents duplicates on autosave for new series
    private val persistentId: String = seriesId ?: generateId()

    private val _state = MutableStateFlow(SeriesEditorState(seriesId = persistentId))
    val state = _state.asStateFlow()

    init {
        if (seriesId != null) {
            viewModelScope.launch {
                val found = repository.getAll().find { it.seriesId == seriesId }
                found?.let { s ->
                    _state.update {
                        it.copy(
                            seriesId = s.seriesId,
                            name = s.name,
                            type = s.type,
                            acceptanceDurationMin = s.acceptanceDurationMin,
                            deliveryDurationMin = s.deliveryDurationMin,
                        )
                    }
                }
            }
        }
    }

    fun setName(value: String) = _state.update { it.copy(name = value) }
    fun setType(value: LocoType) = _state.update { it.copy(type = value) }

    fun incrementAcceptance() = _state.update {
        it.copy(acceptanceDurationMin = ((it.acceptanceDurationMin ?: 0) + 5).coerceAtMost(240))
    }

    fun decrementAcceptance() = _state.update {
        val cur = it.acceptanceDurationMin
        if (cur == null || cur <= 0) it
        else it.copy(acceptanceDurationMin = (cur - 5).coerceAtLeast(0))
    }

    fun setAcceptanceDurationMin(value: Int) = _state.update {
        it.copy(acceptanceDurationMin = value.coerceIn(0, 240))
    }

    fun incrementDelivery() = _state.update {
        it.copy(deliveryDurationMin = ((it.deliveryDurationMin ?: 0) + 5).coerceAtMost(240))
    }

    fun decrementDelivery() = _state.update {
        val cur = it.deliveryDurationMin
        if (cur == null || cur <= 0) it
        else it.copy(deliveryDurationMin = (cur - 5).coerceAtLeast(0))
    }

    fun setDeliveryDurationMin(value: Int) = _state.update {
        it.copy(deliveryDurationMin = value.coerceIn(0, 240))
    }

    /** Autosave — silent, does NOT set saved = true (no navigation side-effect). */
    fun save() {
        val s = _state.value
        if (s.name.isBlank()) return
        viewModelScope.launch {
            val all = repository.getAll().toMutableList()
            val updated = LocomotiveSeries(
                seriesId = persistentId,
                name = s.name.trim(),
                type = s.type,
                acceptanceDurationMin = s.acceptanceDurationMin,
                deliveryDurationMin = s.deliveryDurationMin,
            )
            val idx = all.indexOfFirst { it.seriesId == updated.seriesId }
            if (idx >= 0) all[idx] = updated else all.add(updated)
            repository.replaceAll(all).collect {}
        }
    }

    fun delete() {
        val sid = _state.value.seriesId ?: return
        viewModelScope.launch {
            val updated = repository.getAll().filter { it.seriesId != sid }
            repository.replaceAll(updated).collect {}
            _state.update { it.copy(deleted = true) }
        }
    }
}
