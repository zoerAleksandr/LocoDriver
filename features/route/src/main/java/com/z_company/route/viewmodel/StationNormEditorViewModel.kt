@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.z_company.route.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.domain.entities.norma_time.StationNorm
import com.z_company.domain.repositories.StationNormRepository
import com.z_company.domain.util.generateId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

enum class StationNormField {
    APPEARANCE_TO_START, END_TO_BARRIER, BARRIER_TO_START, END_TO_WORK_END
}

data class StationEditorState(
    val stationId: String? = null,
    val name: String = "",
    val appearanceToStartMin: Int? = null,
    val endToBarrierMin: Int? = null,
    val barrierToStartMin: Int? = null,
    val endToWorkEndMin: Int? = null,
    val saved: Boolean = false,
    val deleted: Boolean = false,
)

class StationNormEditorViewModel(private val stationId: String?) : ViewModel(), KoinComponent {
    private val repository: StationNormRepository by inject()

    private val _state = MutableStateFlow(StationEditorState(stationId = stationId))
    val state = _state.asStateFlow()

    init {
        if (stationId != null) {
            viewModelScope.launch {
                val found = repository.getAll().find { it.stationId == stationId }
                found?.let { s ->
                    _state.update {
                        it.copy(
                            stationId = s.stationId,
                            name = s.name,
                            appearanceToStartMin = s.appearanceToStartMin,
                            endToBarrierMin = s.endToBarrierMin,
                            barrierToStartMin = s.barrierToStartMin,
                            endToWorkEndMin = s.endToWorkEndMin,
                        )
                    }
                }
            }
        }
    }

    fun setName(value: String) = _state.update { it.copy(name = value) }

    fun increment(field: StationNormField) = _state.update { s ->
        when (field) {
            StationNormField.APPEARANCE_TO_START ->
                s.copy(appearanceToStartMin = ((s.appearanceToStartMin ?: 0) + 1).coerceAtMost(120))
            StationNormField.END_TO_BARRIER ->
                s.copy(endToBarrierMin = ((s.endToBarrierMin ?: 0) + 1).coerceAtMost(120))
            StationNormField.BARRIER_TO_START ->
                s.copy(barrierToStartMin = ((s.barrierToStartMin ?: 0) + 1).coerceAtMost(120))
            StationNormField.END_TO_WORK_END ->
                s.copy(endToWorkEndMin = ((s.endToWorkEndMin ?: 0) + 1).coerceAtMost(120))
        }
    }

    fun decrement(field: StationNormField) = _state.update { s ->
        when (field) {
            StationNormField.APPEARANCE_TO_START -> {
                val cur = s.appearanceToStartMin
                if (cur == null || cur <= 0) s else s.copy(appearanceToStartMin = cur - 1)
            }
            StationNormField.END_TO_BARRIER -> {
                val cur = s.endToBarrierMin
                if (cur == null || cur <= 0) s else s.copy(endToBarrierMin = cur - 1)
            }
            StationNormField.BARRIER_TO_START -> {
                val cur = s.barrierToStartMin
                if (cur == null || cur <= 0) s else s.copy(barrierToStartMin = cur - 1)
            }
            StationNormField.END_TO_WORK_END -> {
                val cur = s.endToWorkEndMin
                if (cur == null || cur <= 0) s else s.copy(endToWorkEndMin = cur - 1)
            }
        }
    }

    fun setField(field: StationNormField, value: Int) = _state.update { s ->
        val v = value.coerceIn(0, 120)
        when (field) {
            StationNormField.APPEARANCE_TO_START -> s.copy(appearanceToStartMin = v)
            StationNormField.END_TO_BARRIER -> s.copy(endToBarrierMin = v)
            StationNormField.BARRIER_TO_START -> s.copy(barrierToStartMin = v)
            StationNormField.END_TO_WORK_END -> s.copy(endToWorkEndMin = v)
        }
    }

    /** Autosave — silent, does NOT set saved = true (no navigation side-effect). */
    fun save() {
        val s = _state.value
        if (s.name.isBlank()) return
        viewModelScope.launch {
            val all = repository.getAll().toMutableList()
            val updated = StationNorm(
                stationId = s.stationId ?: generateId(),
                name = s.name.trim(),
                appearanceToStartMin = s.appearanceToStartMin,
                endToBarrierMin = s.endToBarrierMin,
                barrierToStartMin = s.barrierToStartMin,
                endToWorkEndMin = s.endToWorkEndMin,
            )
            val idx = all.indexOfFirst { it.stationId == updated.stationId }
            if (idx >= 0) all[idx] = updated else all.add(updated)
            repository.replaceAll(all).collect {}
        }
    }

    fun delete() {
        val sid = _state.value.stationId ?: return
        viewModelScope.launch {
            val updated = repository.getAll().filter { it.stationId != sid }
            repository.replaceAll(updated).collect {}
            _state.update { it.copy(deleted = true) }
        }
    }
}
