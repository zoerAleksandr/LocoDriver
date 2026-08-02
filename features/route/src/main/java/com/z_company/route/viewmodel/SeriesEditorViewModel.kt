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

// Четыре нормы на серию: приёмка/сдача × «После отстоя»/«Из рук в руки».
enum class SeriesNormField {
    ACCEPTANCE_PARKING, DELIVERY_PARKING, ACCEPTANCE_HAND, DELIVERY_HAND
}

data class SeriesEditorState(
    val seriesId: String? = null,
    val name: String = "",
    val type: LocoType = LocoType.ELECTRIC,
    // «После отстоя без бригады» (вариант по умолчанию)
    val acceptanceDurationMin: Int? = null,
    val deliveryDurationMin: Int? = null,
    // «Из рук в руки»
    val acceptanceHandToHandMin: Int? = null,
    val deliveryHandToHandMin: Int? = null,
    val saved: Boolean = false,
    val deleted: Boolean = false,
) {
    fun valueOf(field: SeriesNormField): Int? = when (field) {
        SeriesNormField.ACCEPTANCE_PARKING -> acceptanceDurationMin
        SeriesNormField.DELIVERY_PARKING -> deliveryDurationMin
        SeriesNormField.ACCEPTANCE_HAND -> acceptanceHandToHandMin
        SeriesNormField.DELIVERY_HAND -> deliveryHandToHandMin
    }
}

class SeriesEditorViewModel(
    private val seriesId: String?,
    initialName: String? = null,
) : ViewModel(), KoinComponent {
    private val repository: LocomotiveSeriesRepository by inject()

    // Generate ID once — prevents duplicates on autosave for new series
    private val persistentId: String = seriesId ?: generateId()

    private val _state = MutableStateFlow(
        // Для «старой» серии (только имя в locomotiveSeriesList, без норм) открываем
        // редактор с заполненным названием — пользователь дозадаёт приёмку/сдачу.
        SeriesEditorState(seriesId = persistentId, name = initialName?.trim().orEmpty())
    )
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
                            // 0 из старых данных трактуем как «не задано» (прочерк).
                            acceptanceDurationMin = s.acceptanceDurationMin?.takeIf { it > 0 },
                            deliveryDurationMin = s.deliveryDurationMin?.takeIf { it > 0 },
                            acceptanceHandToHandMin = s.acceptanceHandToHandMin?.takeIf { it > 0 },
                            deliveryHandToHandMin = s.deliveryHandToHandMin?.takeIf { it > 0 },
                        )
                    }
                }
            }
        }
    }

    fun setName(value: String) = _state.update { it.copy(name = value) }
    fun setType(value: LocoType) = _state.update { it.copy(type = value) }

    private fun SeriesEditorState.withValue(field: SeriesNormField, value: Int?): SeriesEditorState =
        when (field) {
            SeriesNormField.ACCEPTANCE_PARKING -> copy(acceptanceDurationMin = value)
            SeriesNormField.DELIVERY_PARKING -> copy(deliveryDurationMin = value)
            SeriesNormField.ACCEPTANCE_HAND -> copy(acceptanceHandToHandMin = value)
            SeriesNormField.DELIVERY_HAND -> copy(deliveryHandToHandMin = value)
        }

    fun increment(field: SeriesNormField) {
        _state.update {
            it.withValue(field, ((it.valueOf(field) ?: 0) + 1).coerceAtMost(240))
        }
        save()
    }

    fun decrement(field: SeriesNormField) {
        _state.update {
            val cur = it.valueOf(field)
            // 0 = «не задано» (прочерк, не считается нормой) — храним null, а не 0.
            if (cur == null || cur <= 0) it else it.withValue(field, (cur - 1).takeIf { v -> v > 0 })
        }
        save()
    }

    fun setField(field: SeriesNormField, value: Int) {
        _state.update {
            // 0 трактуем как «не задано» → null (иначе серия попадёт в список «с нормами»).
            it.withValue(field, value.coerceIn(0, 240).takeIf { v -> v > 0 })
        }
        save()
    }

    /** Autosave — silent, does NOT set saved = true (no navigation side-effect). */
    fun save() {
        val s = _state.value
        if (s.name.isBlank()) return
        val hasNorm = s.acceptanceDurationMin != null || s.deliveryDurationMin != null ||
            s.acceptanceHandToHandMin != null || s.deliveryHandToHandMin != null
        viewModelScope.launch {
            val all = repository.getAll().toMutableList()
            val idx = all.indexOfFirst { it.seriesId == persistentId }
            // Не создаём новую запись без норм: иначе «старая» серия (только имя в
            // locomotiveSeriesList), открытая и закрытая без ввода, сохранилась бы
            // как серия и «переехала» бы из раздела «без норм» наверх.
            if (idx < 0 && !hasNorm) return@launch
            val updated = LocomotiveSeries(
                seriesId = persistentId,
                name = s.name.trim(),
                type = s.type,
                acceptanceDurationMin = s.acceptanceDurationMin,
                deliveryDurationMin = s.deliveryDurationMin,
                acceptanceHandToHandMin = s.acceptanceHandToHandMin,
                deliveryHandToHandMin = s.deliveryHandToHandMin,
            )
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
