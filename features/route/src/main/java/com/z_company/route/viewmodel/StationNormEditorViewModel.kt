@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.z_company.route.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.domain.entities.norma_time.StationNorm
import com.z_company.domain.repositories.StationNormRepository
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.domain.entities.setting.ServicePhaseStationKind
import com.z_company.domain.use_cases.StationDirectoryUseCase
import com.z_company.domain.util.ServicePhaseStationsMerge
import com.z_company.domain.util.StationDirectory
import com.z_company.domain.util.generateId
import kotlin.time.Clock
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
    val latitude: Double? = null,
    val longitude: Double? = null,
    val esr: String? = null,
    val osmId: Long? = null,
    val kind: ServicePhaseStationKind? = null,
    val axisKm: Int? = null,
    val axisPicket: Int? = null,
    /** Страна пользователя — для поиска координат. */
    val country: String = "RU",
    /** Ориентиры для поиска и карты — соседние станции по плечам. */
    val neighbours: List<Pair<Double, Double>> = emptyList(),
    val saved: Boolean = false,
    val deleted: Boolean = false,
) {
    val hasCoordinates: Boolean get() = latitude != null && longitude != null
    val hasData: Boolean
        get() = appearanceToStartMin != null || endToBarrierMin != null || barrierToStartMin != null ||
            endToWorkEndMin != null || hasCoordinates || esr != null || axisKm != null
}

class StationNormEditorViewModel(
    private val stationId: String?,
    initialName: String? = null,
) : ViewModel(), KoinComponent {
    private val repository: StationNormRepository by inject()
    private val settingsUseCase: SettingsUseCase by inject()
    private val stationDirectory: StationDirectoryUseCase by inject()

    // Generate the ID once — prevents duplicate rows on each autosave for new stations.
    // Для «старой» станции по имени — id её записи, если она уже есть (например, создана
    // переносом координат из плеч), иначе получился бы дубль.
    private var persistentId: String = stationId ?: generateId()

    // Для «старой» станции (только имя в stationList, без норм) открываем редактор
    // с заполненным названием — пользователь дозадаёт нормы интервалов.
    private val _state = MutableStateFlow(
        StationEditorState(stationId = persistentId, name = initialName?.trim().orEmpty())
    )
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val settings = runCatching { settingsUseCase.getUserSetting() }.getOrNull()
            // Координаты из плеч, сохранённых до справочника, — сначала в справочник.
            settings?.let { stationDirectory.backfill(it.servicePhases) }
            val all = repository.getAll()
            val found = if (stationId != null) {
                all.find { it.stationId == stationId }
            } else {
                initialName?.trim()?.takeIf { it.isNotBlank() }?.let { name ->
                    all.find { ServicePhaseStationsMerge.normalizeName(it.name) == ServicePhaseStationsMerge.normalizeName(name) }
                }
            }
            found?.let { persistentId = it.stationId }
            _state.update { st ->
                val base = st.copy(country = settings?.country ?: st.country)
                found?.let { s ->
                    base.copy(
                        stationId = s.stationId,
                        name = s.name,
                        // 0 из старых данных трактуем как «не задано» (прочерк).
                        appearanceToStartMin = s.appearanceToStartMin?.takeIf { it > 0 },
                        endToBarrierMin = s.endToBarrierMin?.takeIf { it > 0 },
                        barrierToStartMin = s.barrierToStartMin?.takeIf { it > 0 },
                        endToWorkEndMin = s.endToWorkEndMin?.takeIf { it > 0 },
                        latitude = s.latitude,
                        longitude = s.longitude,
                        esr = s.esr,
                        osmId = s.osmId,
                        kind = s.kind,
                        axisKm = s.axisKm,
                        axisPicket = s.axisPicket,
                    )
                } ?: base
            }
            refreshNeighbours(settings?.servicePhases.orEmpty())
        }
    }

    private fun refreshNeighbours(phases: List<com.z_company.domain.entities.setting.ServicePhase>) {
        val name = _state.value.name
        if (name.isBlank()) return
        _state.update { it.copy(neighbours = StationDirectory.neighbours(name, phases, repository.getAll())) }
    }

    /** Координаты станции: найдены автоматически или указаны на карте. */
    fun setCoordinates(
        latitude: Double,
        longitude: Double,
        esr: String? = null,
        osmId: Long? = null,
        kind: ServicePhaseStationKind? = null,
        pickedOnMap: Boolean = false,
    ) {
        _state.update {
            it.copy(
                latitude = latitude,
                longitude = longitude,
                // Точка с карты — не та станция OSM, ЕСР/OSM от прежней находки не переносим.
                esr = if (pickedOnMap) it.esr else esr ?: it.esr,
                osmId = if (pickedOnMap) null else osmId,
                kind = kind ?: it.kind,
            )
        }
        save()
    }

    /** Км и пикет оси станции по ж/д километражу (null — не задано). */
    fun setAxis(km: Int?, picket: Int?) {
        _state.update {
            it.copy(
                axisKm = km?.takeIf { v -> v >= 0 },
                axisPicket = picket?.takeIf { v -> v in 1..10 }?.takeIf { _ -> km != null },
            )
        }
        save()
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
        viewModelScope.launch { persistRecord() }
    }

    /**
     * Финальное сохранение при выходе («Готово»/назад): дописывает запись норм
     * и регистрирует имя в справочнике настроек (stationList), чтобы станция
     * появилась в выпадающих списках при заполнении полей.
     *
     * Регистрацию имени делаем ТОЛЬКО здесь (не в autosave), иначе debounce во
     * время набора успел бы добавить в справочник частичные имена-мусор.
     */
    fun commit() {
        val name = _state.value.name.trim()
        if (name.isBlank()) return
        viewModelScope.launch { persistRecord() }
        // Отдельная корутина: setStations в конце вызывает this.cancel(),
        // и эта отмена не должна прерывать сохранение записи выше.
        viewModelScope.launch { settingsUseCase.setStations(listOf(name)) }
    }

    private suspend fun persistRecord() {
        val s = _state.value
        if (s.name.isBlank()) return
        val all = repository.getAll().toMutableList()
        val idx = all.indexOfFirst { it.stationId == persistentId }
        // Не создаём новую запись без данных: иначе «старая» станция (только имя в
        // stationList), открытая и закрытая без ввода, сохранилась бы записью и
        // «переехала» бы из раздела «без норм» наверх.
        if (idx < 0 && !s.hasData) return
        val base = all.getOrNull(idx) ?: StationNorm(stationId = persistentId, name = s.name.trim())
        val updated = base.copy(
            name = s.name.trim(),
            appearanceToStartMin = s.appearanceToStartMin,
            endToBarrierMin = s.endToBarrierMin,
            barrierToStartMin = s.barrierToStartMin,
            endToWorkEndMin = s.endToWorkEndMin,
            latitude = s.latitude,
            longitude = s.longitude,
            esr = s.esr,
            osmId = s.osmId,
            kind = s.kind,
            axisKm = s.axisKm,
            axisPicket = s.axisPicket,
        )
        if (updated == base && idx >= 0) return
        val stamped = updated.copy(updatedAt = Clock.System.now().toEpochMilliseconds())
        if (idx >= 0) all[idx] = stamped else all.add(stamped)
        repository.replaceAll(all).collect {}
    }

    fun delete() {
        val sid = _state.value.stationId ?: return
        val name = _state.value.name.trim()
        viewModelScope.launch {
            val updated = repository.getAll().filter { it.stationId != sid }
            repository.replaceAll(updated).collect {}
            // Ставим deleted ДО removeStation: тот в конце вызывает this.cancel(),
            // и отмена прервала бы корутину раньше, чем выставится флаг (из-за
            // чего экран не перенаправлялся к списку).
            _state.update { it.copy(deleted = true) }
        }
        // Станция в справочнике = запись норм (StationNorm) ∪ имя в
        // settings.stationList. Удаляем и имя, иначе станция «возвращается»
        // как legacy-запись без норм. Отдельная корутина — из-за this.cancel().
        if (name.isNotBlank()) {
            viewModelScope.launch { settingsUseCase.removeStation(name) }
        }
    }
}
