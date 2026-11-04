@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.z_company.iosapp.viewmodel

import com.z_company.domain.brake.LocoSeriesDefaults
import com.z_company.domain.brake.SeriesPassportField
import com.z_company.domain.entities.norma_time.LocomotiveSeries
import com.z_company.domain.entities.norma_time.SectionNumberingType
import com.z_company.domain.entities.norma_time.StationNorm
import com.z_company.domain.entities.route.LocoType
import com.z_company.domain.repositories.LocomotiveSeriesRepository
import com.z_company.domain.repositories.StationNormRepository
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.domain.use_cases.StationDirectoryUseCase
import com.z_company.domain.util.ServicePhaseStationsMerge
import com.z_company.domain.util.generateId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock

/*
 * Справочники норм на iOS (SCREEN_SPECS §17.4, §2.10): списки и редакторы серий и станций.
 * Один и тот же редактор используется в Настройках и в пикере шторки времени локомотива.
 * Правила — по Android `SeriesEditorViewModel` / `StationNormEditorViewModel` (0 нормы = «не задано»,
 * запись создаётся только когда есть данные, имя регистрируется в справочнике автодополнения
 * только в `commit()`).
 */

// ═══ Списки ═══════════════════════════════════════════════════════════════════

data class NormsSeriesRowUi(
    val id: String,
    val name: String,
    val isElectric: Boolean,
    /** «Приёмка A мин · Сдача D мин» / «Нормы не заданы». */
    val normsText: String,
    val hasNorms: Boolean,
)

data class NormsStationRowUi(
    val id: String,
    val name: String,
    /** «Приёмка A/B · Сдача C/D» / «Норма не задана». */
    val normsText: String,
    /** Заданы все четыре нормы (как в Android-списке): раздел «С нормами». */
    val hasNorms: Boolean,
    val hasCoordinates: Boolean,
)

class SeriesListIosViewModel(
    private val repository: LocomotiveSeriesRepository,
    private val settingsUseCase: SettingsUseCase,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val rows = MutableStateFlow<List<NormsSeriesRowUi>?>(null)

    init {
        scope.launch {
            repository.getAllFlow().collect { list -> rows.value = list.map { it.toRow() } }
        }
    }

    /** `null` — ещё не загружено. */
    fun watchRows(callback: (List<NormsSeriesRowUi>?) -> Unit) {
        scope.launch { rows.collect { callback(it) } }
    }

    fun delete(id: String) {
        scope.launch {
            val all = repository.getAll()
            val name = all.find { it.seriesId == id }?.name?.trim()
            repository.replaceAll(all.filter { it.seriesId != id }).collect {}
            // Серия в справочнике = запись норм ∪ имя в locomotiveSeriesList (как в редакторе).
            if (!name.isNullOrBlank()) launch { settingsUseCase.removeLocomotiveSeries(name) }
        }
    }

    fun dispose() {
        scope.cancel()
    }

    private fun LocomotiveSeries.toRow(): NormsSeriesRowUi {
        val acc = acceptanceDurationMin ?: acceptanceHandToHandMin
        val del = deliveryDurationMin ?: deliveryHandToHandMin
        val parts = listOfNotNull(acc?.let { "Приёмка $it мин" }, del?.let { "Сдача $it мин" })
        return NormsSeriesRowUi(
            id = seriesId,
            name = name,
            isElectric = type == LocoType.ELECTRIC,
            normsText = if (parts.isEmpty()) "Нормы не заданы" else parts.joinToString(" · "),
            hasNorms = parts.isNotEmpty(),
        )
    }
}

class StationListIosViewModel(
    private val repository: StationNormRepository,
    private val settingsUseCase: SettingsUseCase,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val rows = MutableStateFlow<List<NormsStationRowUi>?>(null)

    init {
        scope.launch {
            repository.getAllFlow().collect { list -> rows.value = list.map { it.toRow() } }
        }
    }

    fun watchRows(callback: (List<NormsStationRowUi>?) -> Unit) {
        scope.launch { rows.collect { callback(it) } }
    }

    fun delete(id: String) {
        scope.launch {
            val all = repository.getAll()
            val name = all.find { it.stationId == id }?.name?.trim()
            repository.replaceAll(all.filter { it.stationId != id }).collect {}
            if (!name.isNullOrBlank()) launch { settingsUseCase.removeStation(name) }
        }
    }

    fun dispose() {
        scope.cancel()
    }

    private fun StationNorm.toRow(): NormsStationRowUi {
        val any = hasNorms
        return NormsStationRowUi(
            id = stationId,
            name = name,
            normsText = if (any) {
                "Приёмка ${appearanceToStartMin ?: 0}/${endToBarrierMin ?: 0} · " +
                    "Сдача ${barrierToStartMin ?: 0}/${endToWorkEndMin ?: 0}"
            } else "Норма не задана",
            hasNorms = appearanceToStartMin != null && endToBarrierMin != null &&
                barrierToStartMin != null && endToWorkEndMin != null,
            hasCoordinates = hasCoordinates,
        )
    }
}

// ═══ Редактор серии ═══════════════════════════════════════════════════════════

/** Снимок редактора серии для SwiftUI. Числа — строки, пустая строка = «не задано». */
data class SeriesEditorStateUi(
    val id: String,
    val name: String,
    val isElectric: Boolean,
    val numberingLetters: Boolean,
    /** «После отстоя»: приёмка / сдача. */
    val acceptanceParking: String,
    val deliveryParking: String,
    /** «Из рук в руки»: приёмка / сдача. */
    val acceptanceHand: String,
    val deliveryHand: String,
    val sectionCount: String,
    val weight: String,
    val axleCount: String,
    val axleLoad: String,
    val brakeVolume: String,
    /** Серия ещё не сохранена в справочнике (новая или «старая» — только имя). */
    val isNewRecord: Boolean,
    val loaded: Boolean,
    val deleted: Boolean,
)

class SeriesEditorIosViewModel(
    private val repository: LocomotiveSeriesRepository,
    private val settingsUseCase: SettingsUseCase,
    seriesId: String?,
    initialName: String?,
) {
    companion object {
        /** Коды норм для [setNorm]. */
        const val NORM_ACCEPTANCE_PARKING = 0
        const val NORM_DELIVERY_PARKING = 1
        const val NORM_ACCEPTANCE_HAND = 2
        const val NORM_DELIVERY_HAND = 3

        /** Коды данных локомотива для [setPassport] (порядок `SeriesPassportField`). */
        const val PASSPORT_SECTIONS = 0
        const val PASSPORT_WEIGHT = 1
        const val PASSPORT_AXLES = 2
        const val PASSPORT_AXLE_LOAD = 3
        const val PASSPORT_VOLUME = 4
    }

    private data class S(
        val id: String,
        val name: String = "",
        val type: LocoType = LocoType.ELECTRIC,
        val numbering: SectionNumberingType = SectionNumberingType.NUMERIC,
        val accP: Int? = null,
        val delP: Int? = null,
        val accH: Int? = null,
        val delH: Int? = null,
        val sectionCount: Int? = null,
        val weightT: Double? = null,
        val axleCount: Int? = null,
        val axleLoadTf: Double? = null,
        val brakeVolumeL: Int? = null,
        val brakeEdited: Boolean = false,
        val isNewRecord: Boolean = true,
        val loaded: Boolean = false,
        val deleted: Boolean = false,
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // ID генерируем один раз — иначе автосейв новой серии создавал бы дубли.
    private val persistentId: String = seriesId ?: generateId()
    private val _state = MutableStateFlow(
        S(id = persistentId, name = initialName?.trim().orEmpty(), loaded = seriesId == null).withBrakeDefaults()
    )

    /** Имя, под которым запись лежит в справочнике (для синхронизации переименования, §2.12). */
    private var savedName: String? = null
    private var onRenamed: (String, String) -> Unit = { _, _ -> }
    private var onDeleted: (String) -> Unit = {}

    init {
        if (seriesId != null) {
            scope.launch {
                val found = repository.getAll().find { it.seriesId == seriesId }
                if (found == null) {
                    _state.update { it.copy(loaded = true) }
                } else {
                    // Запись без секций (от старого клиента) показываем со значениями по умолчанию.
                    val s = if (LocoSeriesDefaults.needsDefaults(found)) with(LocoSeriesDefaults) { found.withDefaults() } else found
                    savedName = found.name
                    _state.update {
                        it.copy(
                            id = s.seriesId,
                            name = s.name,
                            type = s.type,
                            numbering = s.sectionNumberingType,
                            sectionCount = s.sectionCount,
                            weightT = s.weightT,
                            axleCount = s.axleCount,
                            axleLoadTf = s.axleLoadTf,
                            brakeVolumeL = s.brakeVolumeL,
                            brakeEdited = !LocoSeriesDefaults.needsDefaults(found),
                            isNewRecord = false,
                            // 0 из старых данных = «не задано».
                            accP = s.acceptanceDurationMin?.takeIf { v -> v > 0 },
                            delP = s.deliveryDurationMin?.takeIf { v -> v > 0 },
                            accH = s.acceptanceHandToHandMin?.takeIf { v -> v > 0 },
                            delH = s.deliveryHandToHandMin?.takeIf { v -> v > 0 },
                            loaded = true,
                        )
                    }
                }
            }
        }
    }

    fun watchState(callback: (SeriesEditorStateUi) -> Unit) {
        scope.launch { _state.collect { callback(it.toUi()) } }
    }

    /** `onRenamed(старое, новое)` — запись уже лежала в справочнике под другим именем. */
    fun setCallbacks(onRenamed: (String, String) -> Unit, onDeleted: (String) -> Unit) {
        this.onRenamed = onRenamed
        this.onDeleted = onDeleted
    }

    fun dispose() {
        scope.cancel()
    }

    /** Имя — без немедленного сохранения (Swift вызывает [save] с debounce 500 мс). */
    fun setName(value: String) = _state.update {
        val next = it.copy(name = value)
        if (it.isNewRecord && !it.brakeEdited) next.withBrakeDefaults() else next
    }

    fun setElectric(value: Boolean) {
        _state.update { it.copy(type = if (value) LocoType.ELECTRIC else LocoType.DIESEL) }
        save()
    }

    fun setNumberingLetters(value: Boolean) {
        _state.update {
            it.copy(numbering = if (value) SectionNumberingType.LETTERS else SectionNumberingType.NUMERIC)
        }
        save()
    }

    /** Норма в минутах: пусто/0/нечисло = «не задано» (null), максимум 240. */
    fun setNorm(field: Int, text: String) {
        val v = text.trim().toIntOrNull()?.coerceIn(0, 240)?.takeIf { it > 0 }
        _state.update {
            when (field) {
                NORM_ACCEPTANCE_PARKING -> it.copy(accP = v)
                NORM_DELIVERY_PARKING -> it.copy(delP = v)
                NORM_ACCEPTANCE_HAND -> it.copy(accH = v)
                else -> it.copy(delH = v)
            }
        }
        save()
    }

    /** Данные локомотива: пусто — «не задано» (для секций — значение по умолчанию). */
    fun setPassport(field: Int, text: String) {
        val number = text.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() && it > 0 }
        _state.update {
            val edited = it.copy(brakeEdited = true)
            when (SeriesPassportField.entries[field]) {
                SeriesPassportField.SECTIONS ->
                    edited.copy(sectionCount = number?.toInt()?.coerceIn(1, 9) ?: LocoSeriesDefaults.sectionCountFor(it.name))
                SeriesPassportField.WEIGHT -> edited.copy(weightT = number)
                SeriesPassportField.AXLES -> edited.copy(axleCount = number?.toInt()?.coerceAtMost(99))
                SeriesPassportField.AXLE_LOAD -> edited.copy(axleLoadTf = number)
                SeriesPassportField.VOLUME -> edited.copy(brakeVolumeL = number?.toInt()?.coerceAtMost(9999))
            }
        }
        save()
    }

    /** Тихое автосохранение (без регистрации имени в справочнике автодополнения). */
    fun save() {
        scope.launch { persistRecord() }
    }

    /** Финальное сохранение («Готово»/уход с экрана): запись + имя в справочнике автодополнения. */
    fun commit() {
        val name = _state.value.name.trim()
        if (name.isBlank() || _state.value.deleted) return
        scope.launch {
            persistRecord()
            // Отдельная корутина: setLocomotiveSeries в конце отменяет собственный scope.
            launch { settingsUseCase.setLocomotiveSeries(name) }
        }
    }

    fun delete() {
        val s = _state.value
        val name = s.name.trim()
        scope.launch {
            val updated = repository.getAll().filter { it.seriesId != s.id }
            repository.replaceAll(updated).collect {}
            // Флаг ставим ДО removeLocomotiveSeries (тот отменяет свой scope).
            _state.update { it.copy(deleted = true) }
            onDeleted(savedName ?: name)
            if (name.isNotBlank()) launch { settingsUseCase.removeLocomotiveSeries(name) }
        }
    }

    private suspend fun persistRecord() {
        val s = _state.value
        if (s.name.isBlank() || s.deleted) return
        val hasNorm = s.accP != null || s.delP != null || s.accH != null || s.delH != null ||
            s.numbering != SectionNumberingType.NUMERIC || s.brakeEdited
        val all = repository.getAll().toMutableList()
        val idx = all.indexOfFirst { it.seriesId == persistentId }
        // Новая запись без данных не создаётся (серия «только имя» не должна переезжать из «без норм»).
        if (idx < 0 && !hasNorm) return
        val trimmed = s.name.trim()
        val updated = LocomotiveSeries(
            seriesId = persistentId,
            name = trimmed,
            type = s.type,
            acceptanceDurationMin = s.accP,
            deliveryDurationMin = s.delP,
            acceptanceHandToHandMin = s.accH,
            deliveryHandToHandMin = s.delH,
            sectionNumberingType = s.numbering,
            brakeVolumeL = s.brakeVolumeL,
            axleCount = s.axleCount,
            axleLoadTf = s.axleLoadTf,
            weightT = s.weightT,
            sectionCount = s.sectionCount,
            updatedAt = Clock.System.now().toEpochMilliseconds(),
        )
        // Без изменений — не пишем и не двигаем updatedAt (иначе просто открыть/закрыть = ложная синхронизация).
        if (idx >= 0 && all[idx].copy(updatedAt = updated.updatedAt) == updated) {
            savedName = trimmed
            return
        }
        if (idx >= 0) all[idx] = updated else all.add(updated)
        repository.replaceAll(all).collect {}
        _state.update { it.copy(isNewRecord = false) }
        val old = savedName
        savedName = trimmed
        if (old != null && old != trimmed) onRenamed(old, trimmed)
    }

    private fun S.withBrakeDefaults(): S {
        val d = LocoSeriesDefaults.forSeries(name)
        return copy(
            sectionCount = d.sectionCount,
            weightT = d.weightT,
            axleCount = d.axleCount,
            axleLoadTf = d.axleLoadTf,
            brakeVolumeL = d.brakeVolumeL,
        )
    }

    private fun S.toUi() = SeriesEditorStateUi(
        id = id,
        name = name,
        isElectric = type == LocoType.ELECTRIC,
        numberingLetters = numbering == SectionNumberingType.LETTERS,
        acceptanceParking = accP?.toString().orEmpty(),
        deliveryParking = delP?.toString().orEmpty(),
        acceptanceHand = accH?.toString().orEmpty(),
        deliveryHand = delH?.toString().orEmpty(),
        sectionCount = sectionCount?.toString().orEmpty(),
        weight = weightT?.let { formatNumber(it) }.orEmpty(),
        axleCount = axleCount?.toString().orEmpty(),
        axleLoad = axleLoadTf?.let { formatNumber(it) }.orEmpty(),
        brakeVolume = brakeVolumeL?.toString().orEmpty(),
        isNewRecord = isNewRecord,
        loaded = loaded,
        deleted = deleted,
    )
}

/** 14.0 → «14», 14.5 → «14,5». */
private fun formatNumber(v: Double): String {
    val asLong = v.toLong()
    return if (v == asLong.toDouble()) asLong.toString() else v.toString().replace('.', ',')
}

// ═══ Редактор станции ═════════════════════════════════════════════════════════

data class StationEditorStateUi(
    val id: String,
    val name: String,
    val appearanceToStart: String,
    val endToBarrier: String,
    val barrierToStart: String,
    val endToWorkEnd: String,
    val hasCoordinates: Boolean,
    /** «lat, lon · ЕСР …» (моно) или пусто. */
    val coordinatesText: String,
    val axisKm: String,
    val axisPicket: String,
    val loaded: Boolean,
    val deleted: Boolean,
)

class StationEditorIosViewModel(
    private val repository: StationNormRepository,
    private val settingsUseCase: SettingsUseCase,
    private val stationDirectory: StationDirectoryUseCase,
    stationId: String?,
    initialName: String?,
) {
    companion object {
        const val NORM_APPEARANCE_TO_START = 0
        const val NORM_END_TO_BARRIER = 1
        const val NORM_BARRIER_TO_START = 2
        const val NORM_END_TO_WORK_END = 3
    }

    private data class S(
        val id: String,
        val name: String = "",
        val a: Int? = null,
        val b: Int? = null,
        val c: Int? = null,
        val d: Int? = null,
        val latitude: Double? = null,
        val longitude: Double? = null,
        val esr: String? = null,
        val axisKm: Int? = null,
        val axisPicket: Int? = null,
        val loaded: Boolean = false,
        val deleted: Boolean = false,
    ) {
        val hasCoordinates get() = latitude != null && longitude != null
        val hasData get() = a != null || b != null || c != null || d != null || hasCoordinates || esr != null || axisKm != null
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var persistentId: String = stationId ?: generateId()
    private val _state = MutableStateFlow(
        S(id = persistentId, name = initialName?.trim().orEmpty(), loaded = stationId == null && initialName.isNullOrBlank())
    )
    private var onDeleted: (String) -> Unit = {}

    init {
        scope.launch {
            // Координаты из плеч, сохранённых до справочника, — сначала в справочник (как на Android).
            runCatching { settingsUseCase.getUserSetting() }.getOrNull()
                ?.let { runCatching { stationDirectory.backfill(it.servicePhases) } }
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
                found?.let { s ->
                    st.copy(
                        id = s.stationId,
                        name = s.name,
                        // 0 из старых данных = «не задано».
                        a = s.appearanceToStartMin?.takeIf { it > 0 },
                        b = s.endToBarrierMin?.takeIf { it > 0 },
                        c = s.barrierToStartMin?.takeIf { it > 0 },
                        d = s.endToWorkEndMin?.takeIf { it > 0 },
                        latitude = s.latitude,
                        longitude = s.longitude,
                        esr = s.esr,
                        axisKm = s.axisKm,
                        axisPicket = s.axisPicket,
                        loaded = true,
                    )
                } ?: st.copy(loaded = true)
            }
        }
    }

    fun watchState(callback: (StationEditorStateUi) -> Unit) {
        scope.launch { _state.collect { callback(it.toUi()) } }
    }

    fun setCallbacks(onDeleted: (String) -> Unit) {
        this.onDeleted = onDeleted
    }

    fun dispose() {
        scope.cancel()
    }

    fun setName(value: String) = _state.update { it.copy(name = value) }

    fun setNorm(field: Int, text: String) {
        val v = text.trim().toIntOrNull()?.coerceIn(0, 120)?.takeIf { it > 0 }
        _state.update {
            when (field) {
                NORM_APPEARANCE_TO_START -> it.copy(a = v)
                NORM_END_TO_BARRIER -> it.copy(b = v)
                NORM_BARRIER_TO_START -> it.copy(c = v)
                else -> it.copy(d = v)
            }
        }
        save()
    }

    /** Км оси: целое ≥ 0, пусто = стереть (вместе с пикетом). */
    fun setAxisKm(text: String) {
        val km = text.trim().toIntOrNull()?.takeIf { it >= 0 }
        _state.update { it.copy(axisKm = km, axisPicket = it.axisPicket?.takeIf { _ -> km != null }) }
        save()
    }

    /** Пикет: 1–10 (больше 10 → 10), доступен только при заданном км; пусто/0/нечисло = стереть. */
    fun setAxisPicket(text: String) {
        val p = text.trim().toIntOrNull()?.takeIf { it >= 1 }?.coerceAtMost(10)
        _state.update { it.copy(axisPicket = p?.takeIf { _ -> it.axisKm != null }) }
        save()
    }

    fun save() {
        scope.launch { persistRecord() }
    }

    fun commit() {
        val name = _state.value.name.trim()
        if (name.isBlank() || _state.value.deleted) return
        scope.launch {
            persistRecord()
            launch { settingsUseCase.setStations(listOf(name)) }
        }
    }

    fun delete() {
        val s = _state.value
        val name = s.name.trim()
        scope.launch {
            val updated = repository.getAll().filter { it.stationId != s.id }
            repository.replaceAll(updated).collect {}
            _state.update { it.copy(deleted = true) }
            onDeleted(s.id)
            // Удаляем и имя из stationList, иначе станция вернётся «старой» записью без норм.
            if (name.isNotBlank()) launch { settingsUseCase.removeStation(name) }
        }
    }

    private suspend fun persistRecord() {
        val s = _state.value
        if (s.name.isBlank() || s.deleted) return
        val all = repository.getAll().toMutableList()
        val idx = all.indexOfFirst { it.stationId == persistentId }
        // Новая запись без данных не создаётся.
        if (idx < 0 && !s.hasData) return
        val base = all.getOrNull(idx) ?: StationNorm(stationId = persistentId, name = s.name.trim())
        // Координаты/ЕСР/osmId/kind на iOS не редактируются — сохраняются как в записи.
        val updated = base.copy(
            name = s.name.trim(),
            appearanceToStartMin = s.a,
            endToBarrierMin = s.b,
            barrierToStartMin = s.c,
            endToWorkEndMin = s.d,
            axisKm = s.axisKm,
            axisPicket = s.axisPicket,
        )
        if (updated == base && idx >= 0) return
        val stamped = updated.copy(updatedAt = Clock.System.now().toEpochMilliseconds())
        if (idx >= 0) all[idx] = stamped else all.add(stamped)
        repository.replaceAll(all).collect {}
    }

    private fun S.toUi() = StationEditorStateUi(
        id = id,
        name = name,
        appearanceToStart = a?.toString().orEmpty(),
        endToBarrier = b?.toString().orEmpty(),
        barrierToStart = c?.toString().orEmpty(),
        endToWorkEnd = d?.toString().orEmpty(),
        hasCoordinates = hasCoordinates,
        coordinatesText = if (hasCoordinates) {
            "${roundCoord(latitude!!)}, ${roundCoord(longitude!!)}" + (esr?.let { " · ЕСР $it" } ?: "")
        } else "",
        axisKm = axisKm?.toString().orEmpty(),
        axisPicket = axisPicket?.toString().orEmpty(),
        loaded = loaded,
        deleted = deleted,
    )

    private fun roundCoord(v: Double): String {
        val scaled = kotlin.math.round(v * 10000).toLong()
        val neg = scaled < 0
        val abs = kotlin.math.abs(scaled)
        return (if (neg) "-" else "") + (abs / 10000) + "." + (abs % 10000).toString().padStart(4, '0')
    }
}
