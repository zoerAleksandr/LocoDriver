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

class StationNormEditorViewModel(
    private val stationId: String?,
    initialName: String? = null,
) : ViewModel(), KoinComponent {
    private val repository: StationNormRepository by inject()

    // Generate the ID once — prevents duplicate rows on each autosave for new stations.
    private val persistentId: String = stationId ?: generateId()

    // Для «старой» станции (только имя в stationList, без норм) открываем редактор
    // с заполненным названием — пользователь дозадаёт нормы интервалов.
    private val _state = MutableStateFlow(
        StationEditorState(stationId = persistentId, name = initialName?.trim().orEmpty())
    )
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
                            // 0 из старых данных трактуем как «не задано» (прочерк).
                            appearanceToStartMin = s.appearanceToStartMin?.takeIf { it > 0 },
                            endToBarrierMin = s.endToBarrierMin?.takeIf { it > 0 },
                            barrierToStartMin = s.barrierToStartMin?.takeIf { it > 0 },
                            endToWorkEndMin = s.endToWorkEndMin?.takeIf { it > 0 },
                        )
                    }
                }
            }
        }
    }

    fun setName(value: String) = _state.update { it.copy(name = value) }

    fun increment(field: StationNormField) {
        _state.update { s ->
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
        save()
    }

    fun decrement(field: StationNormField) {
        _state.update { s ->
            // 0 = «не задано» (прочерк, не считается нормой) — храним null, а не 0.
            when (field) {
                StationNormField.APPEARANCE_TO_START -> {
                    val cur = s.appearanceToStartMin
                    if (cur == null || cur <= 0) s else s.copy(appearanceToStartMin = (cur - 1).takeIf { it > 0 })
                }
                StationNormField.END_TO_BARRIER -> {
                    val cur = s.endToBarrierMin
                    if (cur == null || cur <= 0) s else s.copy(endToBarrierMin = (cur - 1).takeIf { it > 0 })
                }
                StationNormField.BARRIER_TO_START -> {
                    val cur = s.barrierToStartMin
                    if (cur == null || cur <= 0) s else s.copy(barrierToStartMin = (cur - 1).takeIf { it > 0 })
                }
                StationNormField.END_TO_WORK_END -> {
                    val cur = s.endToWorkEndMin
                    if (cur == null || cur <= 0) s else s.copy(endToWorkEndMin = (cur - 1).takeIf { it > 0 })
                }
            }
        }
        save()
    }

    fun setField(field: StationNormField, value: Int) {
        _state.update { s ->
            // 0 трактуем как «не задано» → null (иначе станция попадёт в список «с нормами»).
            val v = value.coerceIn(0, 120).takeIf { it > 0 }
            when (field) {
                StationNormField.APPEARANCE_TO_START -> s.copy(appearanceToStartMin = v)
                StationNormField.END_TO_BARRIER -> s.copy(endToBarrierMin = v)
                StationNormField.BARRIER_TO_START -> s.copy(barrierToStartMin = v)
                StationNormField.END_TO_WORK_END -> s.copy(endToWorkEndMin = v)
            }
        }
        save()
    }

    /** Autosave — silent, does NOT set saved = true (no navigation side-effect). */
    fun save() {
        val s = _state.value
        if (s.name.isBlank()) return
        val hasAnyNorm = s.appearanceToStartMin != null || s.endToBarrierMin != null ||
            s.barrierToStartMin != null || s.endToWorkEndMin != null
        viewModelScope.launch {
            val all = repository.getAll().toMutableList()
            val idx = all.indexOfFirst { it.stationId == persistentId }
            // Не создаём новую запись без норм: иначе «старая» станция (только имя в
            // stationList), открытая и закрытая без ввода, сохранилась бы записью и
            // «переехала» бы из раздела «без норм» наверх.
            if (idx < 0 && !hasAnyNorm) return@launch
            val updated = StationNorm(
                stationId = persistentId,
                name = s.name.trim(),
                appearanceToStartMin = s.appearanceToStartMin,
                endToBarrierMin = s.endToBarrierMin,
                barrierToStartMin = s.barrierToStartMin,
                endToWorkEndMin = s.endToWorkEndMin,
            )
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
