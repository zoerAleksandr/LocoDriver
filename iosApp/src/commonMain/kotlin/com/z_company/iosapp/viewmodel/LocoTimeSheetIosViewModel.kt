@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.z_company.iosapp.viewmodel

import com.z_company.domain.entities.norma_time.LocomotiveSeries
import com.z_company.domain.entities.norma_time.SectionNumberingType
import com.z_company.domain.entities.norma_time.StationNorm
import com.z_company.domain.entities.route.LocoType
import com.z_company.domain.repositories.LocomotiveSeriesRepository
import com.z_company.domain.repositories.SharedPreferencesRepositories
import com.z_company.domain.repositories.StationNormRepository
import com.z_company.domain.use_cases.SettingsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.time.Clock

/** Результат шторки (`TimeSheetResult`, §2.1). */
data class TimeSheetResultUi(
    val startTime: Long?,
    val endTime: Long?,
    val barrierOut: Long?,
    val barrierIn: Long?,
    val routeEndWork: Long?,
    val stationId: String?,
)

/**
 * iOS-аналог логики Android `TimeBottomSheet` (SCREEN_SPECS §2): расчёт времени по нормам серии
 * и станции, якоря, валидация последовательности, предупреждения, сохранение норм, пикеры
 * серии/станции. Swift только показывает [TimeSheetState] и пробрасывает действия.
 *
 * Koin `factory` — по экземпляру на открытие шторки.
 */
class LocoTimeSheetIosViewModel(
    private val seriesRepository: LocomotiveSeriesRepository,
    private val stationRepository: StationNormRepository,
    private val sharedPrefs: SharedPreferencesRepositories,
    private val settingsUseCase: SettingsUseCase,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _state = MutableStateFlow(TimeSheetState())

    private enum class Anchor { NONE, ASKING, FROM_SELECTED }
    private enum class AccField { START_WORK, START_TIME, END_TIME, BARRIER_OUT }
    private enum class DelField { BARRIER_IN, START_TIME, END_TIME, WORK_END }

    private var started = false
    private var isAcceptance = true
    private var locoType = LocoType.ELECTRIC
    private var offsetMs = 3 * 3_600_000L

    private var initialStart: Long? = null
    private var initialEnd: Long? = null
    private var initialBarrierOut: Long? = null
    private var initialBarrierIn: Long? = null
    private var initialStationId: String? = null
    private var routeStartWork: Long? = null
    private var routeEndWork: Long? = null

    private var startTime: Long? = null
    private var endTime: Long? = null
    private var barrierOut: Long? = null
    private var barrierIn: Long? = null
    private var workEnd: Long? = null

    private var allSeries: List<LocomotiveSeries> = emptyList()
    private var allStations: List<StationNorm> = emptyList()
    private var selectedStation: StationNorm? = null
    private var selectedSeriesName: String? = null

    private var normHandToHand = false
    private var appliedHandToHand = false
    private var anchorState = Anchor.NONE
    private var accAnchor: AccField? = null
    private var delAnchor: DelField? = null
    private var pendingEndWorkUpdate: Long? = null
    private var workEndAccepted = false
    private var showRecalcDialog = false
    private var dismissedWarnings = setOf<String>()
    private var catalogVersion = 0

    private var onSave: (TimeSheetResultUi) -> Unit = {}
    private var onSeriesChanged: (String) -> Unit = {}
    private var onTimeEndWork: (Long?) -> Unit = {}

    fun watchState(callback: (TimeSheetState) -> Unit) {
        scope.launch { _state.collect { callback(it) } }
    }

    fun setCallbacks(
        onSave: (TimeSheetResultUi) -> Unit,
        onSeriesChanged: (String) -> Unit,
        onTimeEndWorkChanged: (Long?) -> Unit,
    ) {
        this.onSave = onSave
        this.onSeriesChanged = onSeriesChanged
        this.onTimeEndWork = onTimeEndWorkChanged
    }

    fun dispose() {
        scope.cancel()
    }

    /** Параметры входа (§2.1). `kind`: "acceptance" | "delivery". */
    fun start(
        kind: String,
        seriesName: String?,
        isElectric: Boolean,
        initialStartTime: Long?,
        initialEndTime: Long?,
        initialBarrierOut: Long?,
        initialBarrierIn: Long?,
        routeStartWork: Long?,
        routeEndWork: Long?,
        initialStationId: String?,
        displayOffsetSeconds: Int,
    ) {
        if (started) return
        started = true
        isAcceptance = kind == "acceptance"
        locoType = if (isElectric) LocoType.ELECTRIC else LocoType.DIESEL
        offsetMs = displayOffsetSeconds * 1000L
        this.initialStart = initialStartTime
        this.initialEnd = initialEndTime
        this.initialBarrierOut = initialBarrierOut
        this.initialBarrierIn = initialBarrierIn
        this.initialStationId = initialStationId
        this.routeStartWork = routeStartWork
        this.routeEndWork = routeEndWork
        startTime = initialStartTime
        endTime = initialEndTime
        this.barrierOut = initialBarrierOut
        this.barrierIn = initialBarrierIn
        workEnd = routeEndWork
        selectedSeriesName = seriesName?.takeIf { it.isNotBlank() }
        normHandToHand = sharedPrefs.isLocoNormHandToHand()
        appliedHandToHand = normHandToHand
        // Справочник обычно уже прогрет — инициализируем синхронно (§2.1).
        allSeries = seriesRepository.getAll()
        allStations = stationRepository.getAll()
        selectedStation = allStations.find { it.stationId == initialStationId }
        publish(ready = true)
        scope.launch {
            seriesRepository.getAllFlow().collect {
                allSeries = it
                catalogVersion++
                publish()
            }
        }
        scope.launch {
            stationRepository.getAllFlow().collect { list ->
                allStations = list
                // Восстановление выбранной станции — только пока выбор пуст (§2.1);
                // правки норм в редакторе подхватываются сразу (§2.8).
                val current = selectedStation
                selectedStation = if (current == null) {
                    initialStationId?.let { id -> list.find { it.stationId == id } }
                } else {
                    list.find { it.stationId == current.stationId } ?: current
                }
                catalogVersion++
                publish()
            }
        }
    }

    // ═══ Нормы и вычисляемые значения ══════════════════════════════════════════

    private val selectedSeries: LocomotiveSeries? get() = allSeries.find { it.name == selectedSeriesName }

    private val seriesAcceptanceMin: Int?
        get() = if (normHandToHand) selectedSeries?.acceptanceHandToHandMin else selectedSeries?.acceptanceDurationMin
    private val seriesDeliveryMin: Int?
        get() = if (normHandToHand) selectedSeries?.deliveryHandToHandMin else selectedSeries?.deliveryDurationMin
    private val deltaAcceptanceMin: Int?
        get() = if (appliedHandToHand) selectedSeries?.acceptanceHandToHandMin else selectedSeries?.acceptanceDurationMin
    private val deltaDeliveryMin: Int?
        get() = if (appliedHandToHand) selectedSeries?.deliveryHandToHandMin else selectedSeries?.deliveryDurationMin

    private fun canFromAcceptance(f: AccField): Boolean = when (f) {
        AccField.START_WORK -> routeStartWork != null && selectedStation?.appearanceToStartMin != null
        AccField.START_TIME -> startTime != null && seriesAcceptanceMin != null
        AccField.END_TIME -> endTime != null && (seriesAcceptanceMin != null || selectedStation?.endToBarrierMin != null)
        AccField.BARRIER_OUT -> barrierOut != null && selectedStation?.endToBarrierMin != null
    }

    private fun canFromDelivery(f: DelField): Boolean = when (f) {
        DelField.BARRIER_IN -> barrierIn != null && selectedStation?.barrierToStartMin != null
        DelField.START_TIME -> startTime != null && seriesDeliveryMin != null
        DelField.END_TIME -> endTime != null && (seriesDeliveryMin != null || selectedStation?.endToWorkEndMin != null)
        DelField.WORK_END -> workEnd != null && selectedStation?.endToWorkEndMin != null
    }

    private fun canApplyNorms(): Boolean =
        if (isAcceptance) AccField.entries.any { canFromAcceptance(it) } else DelField.entries.any { canFromDelivery(it) }

    private fun handleCalculatedWorkEnd(calculated: Long) {
        val route = routeEndWork
        if (route != null && calculated != route) {
            pendingEndWorkUpdate = calculated
            workEnd = route
        } else {
            workEnd = calculated
            if (route == null) workEndAccepted = true
        }
    }

    /** Расчёт сдачи от якоря вперёд и назад (§2.5). */
    private fun applyFromDelivery(field: DelField, deliveryMin: Int? = seriesDeliveryMin) {
        pendingEndWorkUpdate = null
        workEndAccepted = false
        delAnchor = field
        appliedHandToHand = normHandToHand
        val barrierMin = selectedStation?.barrierToStartMin
        val barrierNormSet = barrierMin != null
        val workEndMin = selectedStation?.endToWorkEndMin
        val durMin = deliveryMin
        when (field) {
            DelField.BARRIER_IN -> {
                val anchor = barrierIn ?: return
                val newStart = barrierMin?.let { anchor + it * 60_000L } ?: return
                startTime = newStart
                val newEnd = durMin?.let { newStart + it * 60_000L }
                if (newEnd != null) {
                    endTime = newEnd
                    workEndMin?.let { handleCalculatedWorkEnd(newEnd + it * 60_000L) }
                }
            }
            DelField.START_TIME -> {
                val anchor = startTime ?: return
                durMin?.let { duration ->
                    val newEnd = anchor + duration * 60_000L
                    endTime = newEnd
                    workEndMin?.let { handleCalculatedWorkEnd(newEnd + it * 60_000L) }
                }
                barrierIn = if (barrierNormSet) anchor - barrierMin!! * 60_000L else null
            }
            DelField.END_TIME -> {
                val anchor = endTime ?: return
                workEndMin?.let { handleCalculatedWorkEnd(anchor + it * 60_000L) }
                durMin?.let { duration ->
                    val newStart = anchor - duration * 60_000L
                    startTime = newStart
                    barrierIn = if (barrierNormSet) newStart - barrierMin!! * 60_000L else null
                }
            }
            DelField.WORK_END -> {
                val ew = workEnd ?: routeEndWork ?: return
                val newEnd = workEndMin?.let { ew - it * 60_000L } ?: return
                endTime = newEnd
                durMin?.let { duration ->
                    val newStart = newEnd - duration * 60_000L
                    startTime = newStart
                    barrierIn = if (barrierNormSet) newStart - barrierMin!! * 60_000L else null
                }
                workEnd = ew
                workEndAccepted = false // источник — в маршрут не синхронизируем
            }
        }
    }

    /** Расчёт приёмки от якоря; явка из маршрута не меняется (§2.5). */
    private fun applyFromAcceptance(field: AccField, acceptanceMin: Int? = seriesAcceptanceMin) {
        accAnchor = field
        appliedHandToHand = normHandToHand
        val appearMin = selectedStation?.appearanceToStartMin
        val durMin = acceptanceMin
        val barrierNormSet = selectedStation?.endToBarrierMin != null
        val barrierMin = selectedStation?.endToBarrierMin ?: 0
        when (field) {
            AccField.START_WORK -> {
                val base = routeStartWork ?: return
                val newStart = appearMin?.let { base + it * 60_000L } ?: return
                startTime = newStart
                durMin?.let { duration ->
                    val newEnd = newStart + duration * 60_000L
                    endTime = newEnd
                    barrierOut = if (barrierNormSet) newEnd + barrierMin * 60_000L else null
                }
            }
            AccField.START_TIME -> {
                val anchor = startTime ?: return
                durMin?.let { duration ->
                    val newEnd = anchor + duration * 60_000L
                    endTime = newEnd
                    barrierOut = if (barrierNormSet) newEnd + barrierMin * 60_000L else null
                }
            }
            AccField.END_TIME -> {
                val anchor = endTime ?: return
                barrierOut = if (barrierNormSet) anchor + barrierMin * 60_000L else null
                durMin?.let { startTime = anchor - it * 60_000L }
            }
            AccField.BARRIER_OUT -> {
                val anchor = barrierOut ?: return
                if (!barrierNormSet) return
                val newEnd = anchor - barrierMin * 60_000L
                endTime = newEnd
                durMin?.let { startTime = newEnd - it * 60_000L }
            }
        }
    }

    // ═══ Действия шторки ═══════════════════════════════════════════════════════

    /** Кнопка «Рассчитать время»: 0 якорей — ничего, 1 — сразу, ≥2 — режим ASKING (§2.5). */
    fun applyNorms() {
        if (isAcceptance) {
            val anchors = AccField.entries.filter { canFromAcceptance(it) }
            when (anchors.size) {
                0 -> Unit
                1 -> {
                    applyFromAcceptance(anchors[0])
                    anchorState = Anchor.FROM_SELECTED
                }
                else -> anchorState = Anchor.ASKING
            }
        } else {
            val anchors = DelField.entries.filter { canFromDelivery(it) }
            when (anchors.size) {
                0 -> Unit
                1 -> {
                    applyFromDelivery(anchors[0])
                    anchorState = Anchor.FROM_SELECTED
                }
                else -> anchorState = Anchor.ASKING
            }
        }
        changed()
    }

    /**
     * Тап по строке. Возвращает true — нужно открыть пикер даты/времени этой строки;
     * false — тап обработан (выбор якоря в ASKING) или ничего не делает.
     */
    fun tapRow(id: String): Boolean {
        val asking = anchorState == Anchor.ASKING
        if (isAcceptance) {
            val field = accField(id) ?: return false
            if (asking) {
                if (canFromAcceptance(field)) {
                    applyFromAcceptance(field)
                    anchorState = Anchor.FROM_SELECTED
                    changed()
                }
                return false
            }
            return field != AccField.START_WORK
        } else {
            val field = delField(id) ?: return false
            if (asking) {
                if (canFromDelivery(field)) {
                    applyFromDelivery(field)
                    anchorState = Anchor.FROM_SELECTED
                    changed()
                }
                return false
            }
            // Пока висит «Обновить …», тап по «Окончание работы» пикер не открывает.
            return !(field == DelField.WORK_END && pendingEndWorkUpdate != null)
        }
    }

    private fun accField(id: String) = when (id) {
        "routeStart" -> AccField.START_WORK
        "start" -> AccField.START_TIME
        "end" -> AccField.END_TIME
        "barrierOut" -> AccField.BARRIER_OUT
        else -> null
    }

    private fun delField(id: String) = when (id) {
        "barrierIn" -> DelField.BARRIER_IN
        "start" -> DelField.START_TIME
        "end" -> DelField.END_TIME
        "workEnd" -> DelField.WORK_END
        else -> null
    }

    /** Результат пикера строки (секунды уже обнулены на стороне пикера). */
    fun setRowTime(id: String, ms: Long) {
        when (id) {
            "start" -> startTime = ms
            "end" -> endTime = ms
            "barrierOut" -> barrierOut = ms
            "barrierIn" -> barrierIn = ms
            "workEnd" -> {
                workEnd = ms
                workEndAccepted = true
                // Сразу синхронизируем окончание работы в маршрут (не ждём «Готово»).
                onTimeEndWork(ms)
            }
        }
        changed()
    }

    /** «Удалить значение» (§2.9). Явка не удаляется. */
    fun clearRow(id: String) {
        when (id) {
            "start" -> startTime = null
            "end" -> endTime = null
            "barrierOut" -> barrierOut = null
            "barrierIn" -> barrierIn = null
            "workEnd" -> {
                workEnd = null
                pendingEndWorkUpdate = null
                onTimeEndWork(null)
            }
            else -> return
        }
        changed()
    }

    /** Кнопка «Обновить ЧЧ:ММ» у вычисленного окончания работы. */
    fun acceptPendingWorkEnd() {
        val v = pendingEndWorkUpdate ?: return
        workEnd = v
        workEndAccepted = true
        pendingEndWorkUpdate = null
        onTimeEndWork(v)
        changed()
    }

    /** Переключатель варианта нормы (§2.4): сразу не пересчитываем; при отличии — диалог. */
    fun setHandToHand(value: Boolean) {
        normHandToHand = value
        sharedPrefs.setLocoNormHandToHand(value)
        val s = selectedSeries
        val newDur = if (isAcceptance) {
            if (value) s?.acceptanceHandToHandMin else s?.acceptanceDurationMin
        } else {
            if (value) s?.deliveryHandToHandMin else s?.deliveryDurationMin
        }
        val appliedDur = if (isAcceptance) deltaAcceptanceMin else deltaDeliveryMin
        if (startTime != null && endTime != null && newDur != null && newDur != appliedDur) {
            showRecalcDialog = true
        }
        publish()
    }

    fun answerRecalc(recalculate: Boolean) {
        showRecalcDialog = false
        if (recalculate) {
            if (isAcceptance) applyFromAcceptance(accAnchor ?: AccField.START_TIME)
            else applyFromDelivery(delAnchor ?: DelField.START_TIME)
        }
        changed()
    }

    fun dismissWarning(text: String) {
        dismissedWarnings = dismissedWarnings + text
        publish()
    }

    /** «Готово»: для сдачи с заданным workEnd — синхронизация в маршрут; false при ошибке последовательности. */
    fun finish(): Boolean {
        if (hasSequenceError()) return false
        if (!isAcceptance && workEnd != null) onTimeEndWork(workEnd)
        emitSave()
        return true
    }

    // ── Серия / станция ──────────────────────────────────────────────────────

    fun selectSeries(id: String) {
        val s = allSeries.find { it.seriesId == id } ?: return
        if (s.name == selectedSeriesName) clearSeries() else {
            selectedSeriesName = s.name
            onSeriesChanged(s.name)
            publish()
        }
    }

    fun clearSeries() {
        selectedSeriesName = null
        onSeriesChanged("")
        publish()
    }

    fun selectStation(id: String) {
        val st = allStations.find { it.stationId == id } ?: return
        if (selectedStation?.stationId == st.stationId) clearStation() else {
            selectedStation = st
            changed()
        }
    }

    /** Снятие станции — немедленный `onSave` с null (§2.10). */
    fun clearStation() {
        selectedStation = null
        emitSave()
        publish()
    }

    // ── Сохранение норм (§2.8) ───────────────────────────────────────────────

    fun saveSeriesNorm() {
        val name = selectedSeriesName
        val s = startTime
        val e = endTime
        if (name.isNullOrBlank() || s == null || e == null) return
        val dur = ((e - s) / 60_000).toInt()
        val current = selectedSeries
        scope.launch {
            val all = seriesRepository.getAll().toMutableList()
            if (current != null) {
                val updated = when {
                    isAcceptance && normHandToHand -> current.copy(acceptanceHandToHandMin = dur)
                    isAcceptance -> current.copy(acceptanceDurationMin = dur)
                    normHandToHand -> current.copy(deliveryHandToHandMin = dur)
                    else -> current.copy(deliveryDurationMin = dur)
                }
                val idx = all.indexOfFirst { it.seriesId == updated.seriesId }
                if (idx >= 0) all[idx] = updated else all.add(updated)
            } else {
                all.add(
                    LocomotiveSeries(
                        name = name.trim(),
                        type = locoType,
                        acceptanceDurationMin = if (isAcceptance && !normHandToHand) dur else null,
                        deliveryDurationMin = if (!isAcceptance && !normHandToHand) dur else null,
                        acceptanceHandToHandMin = if (isAcceptance && normHandToHand) dur else null,
                        deliveryHandToHandMin = if (!isAcceptance && normHandToHand) dur else null,
                        updatedAt = Clock.System.now().toEpochMilliseconds(),
                    )
                )
            }
            seriesRepository.replaceAll(all).collect {}
        }
    }

    fun saveStationNorm() {
        val st = selectedStation ?: return
        val updated = if (isAcceptance) {
            val a = routeStartWork
            val s = startTime
            val e = endTime
            val b = barrierOut
            st.copy(
                appearanceToStartMin = if (a != null && s != null) ((s - a) / 60_000).toInt() else st.appearanceToStartMin,
                endToBarrierMin = if (e != null && b != null) ((b - e) / 60_000).toInt() else st.endToBarrierMin,
            )
        } else {
            val bi = barrierIn
            val s = startTime
            val e = endTime
            val w = workEnd
            st.copy(
                barrierToStartMin = if (bi != null && s != null) ((s - bi) / 60_000).toInt() else st.barrierToStartMin,
                endToWorkEndMin = if (e != null && w != null) ((w - e) / 60_000).toInt() else st.endToWorkEndMin,
            )
        }
        selectedStation = updated
        publish()
        scope.launch {
            val all = stationRepository.getAll().toMutableList()
            val idx = all.indexOfFirst { it.stationId == updated.stationId }
            if (idx >= 0) all[idx] = updated else all.add(updated)
            stationRepository.replaceAll(all).collect {}
        }
    }

    // ═══ Пикеры серии/станции (§2.10) ══════════════════════════════════════════

    fun seriesRows(query: String): List<SeriesRowUi> {
        val q = query.trim()
        return allSeries
            .filter { q.isEmpty() || it.name.contains(q, ignoreCase = true) }
            .map { s ->
                val acc = s.acceptanceDurationMin ?: s.acceptanceHandToHandMin
                val del = s.deliveryDurationMin ?: s.deliveryHandToHandMin
                val parts = listOfNotNull(acc?.let { "Приёмка $it мин" }, del?.let { "Сдача $it мин" })
                SeriesRowUi(
                    id = s.seriesId,
                    name = s.name,
                    isElectric = s.type == LocoType.ELECTRIC,
                    normsText = if (parts.isEmpty()) "Нормы не заданы" else parts.joinToString(" · "),
                    isSelected = s.name == selectedSeriesName,
                )
            }
    }

    fun stationRows(query: String): List<StationRowUi> {
        val q = query.trim()
        return allStations
            .filter { q.isEmpty() || it.name.contains(q, ignoreCase = true) }
            .map { s ->
                val has = s.appearanceToStartMin != null || s.endToBarrierMin != null ||
                    s.barrierToStartMin != null || s.endToWorkEndMin != null
                StationRowUi(
                    id = s.stationId,
                    name = s.name,
                    normsText = if (has) {
                        "Приёмка ${s.appearanceToStartMin ?: 0}/${s.endToBarrierMin ?: 0} · " +
                            "Сдача ${s.barrierToStartMin ?: 0}/${s.endToWorkEndMin ?: 0}"
                    } else "Норма не задана",
                    hasNorms = has,
                    isSelected = s.stationId == selectedStation?.stationId,
                )
            }
    }

    fun seriesEditor(id: String?): SeriesEditorUi {
        val s = id?.let { sid -> allSeries.find { it.seriesId == sid } }
        return SeriesEditorUi(
            id = s?.seriesId,
            name = s?.name ?: "",
            isElectric = (s?.type ?: locoType) == LocoType.ELECTRIC,
            acceptanceDuration = s?.acceptanceDurationMin?.toString() ?: "",
            deliveryDuration = s?.deliveryDurationMin?.toString() ?: "",
            acceptanceHandToHand = s?.acceptanceHandToHandMin?.toString() ?: "",
            deliveryHandToHand = s?.deliveryHandToHandMin?.toString() ?: "",
            numberingLetters = s?.sectionNumberingType == SectionNumberingType.LETTERS,
        )
    }

    /**
     * Автосохранение редактора серии (§2.10). Новая серия без единой нормы не создаётся.
     * Возвращает id сохранённой серии или пустую строку.
     */
    fun saveSeries(
        id: String?,
        name: String,
        isElectric: Boolean,
        acceptanceDuration: String,
        deliveryDuration: String,
        acceptanceHandToHand: String,
        deliveryHandToHand: String,
        numberingLetters: Boolean,
    ): String {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return ""
        val acc = acceptanceDuration.trim().toIntOrNull()
        val del = deliveryDuration.trim().toIntOrNull()
        val accH = acceptanceHandToHand.trim().toIntOrNull()
        val delH = deliveryHandToHand.trim().toIntOrNull()
        val existing = id?.let { sid -> allSeries.find { it.seriesId == sid } }
        if (existing == null && acc == null && del == null && accH == null && delH == null) return ""
        val numbering = if (numberingLetters) SectionNumberingType.LETTERS else SectionNumberingType.NUMERIC
        val type = if (isElectric) LocoType.ELECTRIC else LocoType.DIESEL
        val saved = existing?.copy(
            name = trimmed, type = type,
            acceptanceDurationMin = acc, deliveryDurationMin = del,
            acceptanceHandToHandMin = accH, deliveryHandToHandMin = delH,
            sectionNumberingType = numbering,
            updatedAt = Clock.System.now().toEpochMilliseconds(),
        ) ?: LocomotiveSeries(
            name = trimmed, type = type,
            acceptanceDurationMin = acc, deliveryDurationMin = del,
            acceptanceHandToHandMin = accH, deliveryHandToHandMin = delH,
            sectionNumberingType = numbering,
        )
        if (existing != null && existing.name == selectedSeriesName && existing.name != trimmed) {
            selectedSeriesName = trimmed
            onSeriesChanged(trimmed)
        }
        val all = allSeries.toMutableList()
        val idx = all.indexOfFirst { it.seriesId == saved.seriesId }
        if (idx >= 0) all[idx] = saved else all.add(saved)
        allSeries = all
        scope.launch {
            seriesRepository.replaceAll(all).collect {}
            // Имя регистрируется в общем справочнике автодополнения.
            settingsUseCase.setLocomotiveSeries(trimmed)
        }
        return saved.seriesId
    }

    fun stationEditor(id: String?): StationEditorUi {
        val s = id?.let { sid -> allStations.find { it.stationId == sid } }
        return StationEditorUi(
            id = s?.stationId,
            name = s?.name ?: "",
            appearanceToStart = s?.appearanceToStartMin?.toString() ?: "",
            endToBarrier = s?.endToBarrierMin?.toString() ?: "",
            barrierToStart = s?.barrierToStartMin?.toString() ?: "",
            endToWorkEnd = s?.endToWorkEndMin?.toString() ?: "",
        )
    }

    /** Автосохранение редактора станции; пустое имя не сохраняется. Возвращает id или пустую строку. */
    fun saveStation(
        id: String?,
        name: String,
        appearanceToStart: String,
        endToBarrier: String,
        barrierToStart: String,
        endToWorkEnd: String,
    ): String {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return ""
        // Редактор трактует 0 как «не задано» (§2.8).
        fun norm(t: String): Int? = t.trim().toIntOrNull()?.takeIf { it > 0 }
        val existing = id?.let { sid -> allStations.find { it.stationId == sid } }
        val saved = existing?.copy(
            name = trimmed,
            appearanceToStartMin = norm(appearanceToStart), endToBarrierMin = norm(endToBarrier),
            barrierToStartMin = norm(barrierToStart), endToWorkEndMin = norm(endToWorkEnd),
            updatedAt = Clock.System.now().toEpochMilliseconds(),
        ) ?: StationNorm(
            name = trimmed,
            appearanceToStartMin = norm(appearanceToStart), endToBarrierMin = norm(endToBarrier),
            barrierToStartMin = norm(barrierToStart), endToWorkEndMin = norm(endToWorkEnd),
        )
        val all = allStations.toMutableList()
        val idx = all.indexOfFirst { it.stationId == saved.stationId }
        if (idx >= 0) all[idx] = saved else all.add(saved)
        allStations = all
        if (selectedStation?.stationId == saved.stationId) selectedStation = saved
        scope.launch { stationRepository.replaceAll(all).collect {} }
        publish()
        return saved.stationId
    }

    // ═══ Сборка состояния ══════════════════════════════════════════════════════

    private fun hasSequenceError(): Boolean = rowErrors().values.any { it }

    private fun rowErrors(): Map<String, Boolean> {
        val s = startTime
        val e = endTime
        return if (isAcceptance) {
            val rs = routeStartWork
            val bo = barrierOut
            mapOf(
                "start" to (s != null && rs != null && s <= rs),
                "end" to (e != null && s != null && e <= s),
                // Равенство допустимо — интервал может быть нулевым.
                "barrierOut" to (bo != null && e != null && bo < e),
            )
        } else {
            val bi = barrierIn
            val w = workEnd
            mapOf(
                "start" to (s != null && bi != null && s < bi),
                "end" to (e != null && s != null && e <= s),
                "workEnd" to (w != null && e != null && w <= e),
            )
        }
    }

    private fun fmtTime(ms: Long?): String? {
        ms ?: return null
        val total = ((ms + offsetMs) / 60_000L)
        val minutesOfDay = ((total % 1440) + 1440) % 1440
        val h = (minutesOfDay / 60).toString().padStart(2, '0')
        val m = (minutesOfDay % 60).toString().padStart(2, '0')
        return "$h:$m"
    }

    private data class Delta(val text: String, val note: String, val tone: Int)

    private fun buildDelta(from: Long?, to: Long?, norm: Int?): Delta? {
        if (from == null || to == null) return null
        val actual = ((to - from) / 60_000).toInt()
        val actualStr = if (actual >= 0) "+$actual мин" else "$actual мин"
        if (norm == null) return Delta(actualStr, "", 0)
        val diff = actual - norm
        return when {
            diff == 0 -> Delta(actualStr, "норма", 0)
            diff > 0 -> Delta(actualStr, "на $diff мин больше", 1)
            else -> Delta(actualStr, "на ${abs(diff)} мин меньше", 2)
        }
    }

    private fun warnings(): List<TimeWarnUi> {
        val name = selectedSeriesName
        val station = selectedStation
        val list = mutableListOf<TimeWarnUi>()
        val noSeriesNorm = selectedSeries != null &&
            (if (isAcceptance) seriesAcceptanceMin == null else seriesDeliveryMin == null)
        val hasSeriesNorm = if (isAcceptance) seriesAcceptanceMin != null else seriesDeliveryMin != null
        when {
            name == null -> list += TimeWarnUi("Серия не выбрана", "Выберите серию, чтобы применить нормы", null, true, false)
            noSeriesNorm -> list += TimeWarnUi("Нет нормы для серии $name", null, "Настроить серию", true, false)
        }
        val stationMissing: List<String> = if (station == null) emptyList() else if (isAcceptance) {
            buildList { if (station.appearanceToStartMin == null) add("от явки до начала приёмки") }
        } else {
            buildList { if (station.endToWorkEndMin == null) add("от окончания сдачи до окончания работы") }
        }
        val noStationNorm = station != null && stationMissing.isNotEmpty() &&
            (if (isAcceptance) station.endToBarrierMin == null else station.barrierToStartMin == null)
        if (station == null && name != null) {
            if (isAcceptance && startTime != null && hasSeriesNorm) {
                list += TimeWarnUi("Будет рассчитано только время приёмки", "Чтобы применить нормы станции, выберите станцию", null, false, false)
            } else if (isAcceptance) {
                list += TimeWarnUi(
                    "Укажите время начала / окончания приёмки или выберите станцию",
                    "Начало нужно для расчёта времени приёмки по норме локомотива", null, false, false,
                )
            } else {
                list += TimeWarnUi(
                    "Станция сдачи не выбрана",
                    "Выберите станцию или укажите начало / окончание сдачи локомотива", null, false, false,
                )
            }
        } else if (noStationNorm) {
            list += TimeWarnUi("Нет нормы для станции ${station!!.name}", null, "Настроить станцию", false, true)
        } else if (stationMissing.isNotEmpty()) {
            list += TimeWarnUi(
                "Нет нормы времени: ${stationMissing.joinToString(", ")}",
                "Расчёт выполнится без этой нормы", "Настроить станцию", false, true,
            )
        }
        val hasAnchorForDelivery = barrierIn != null || startTime != null || endTime != null || workEnd != null
        if (!isAcceptance && !hasAnchorForDelivery) {
            list += TimeWarnUi(
                "Укажите время захода на КП или время окончания работы",
                "От него будет выполнен расчёт по нормам", null, false, false,
            )
        }
        return list.filter { it.text !in dismissedWarnings }
    }

    private fun row(
        id: String, label: String, ms: Long?, delta: Delta?,
        isLocked: Boolean = false, canHighlight: Boolean, canTapWhenAsking: Boolean = canHighlight,
        errors: Map<String, Boolean>, pending: Long? = null,
    ): TimeRowUi {
        val asking = anchorState == Anchor.ASKING
        return TimeRowUi(
            id = id, label = label, timeText = fmtTime(ms), timeMs = ms,
            deltaText = delta?.text ?: "", deltaNote = delta?.note ?: "", deltaTone = delta?.tone ?: 0,
            isLocked = isLocked,
            hasError = errors[id] == true,
            isHighlighted = asking && canHighlight,
            pendingText = pending?.let { fmtTime(it) },
            canTap = if (asking) canTapWhenAsking else !isLocked,
            canLongPress = !isLocked && ms != null,
        )
    }

    private fun rows(errors: Map<String, Boolean>): List<TimeRowUi> {
        val st = selectedStation
        return if (isAcceptance) {
            listOf(
                row("routeStart", "Явка", routeStartWork, null, isLocked = true,
                    canHighlight = canFromAcceptance(AccField.START_WORK), errors = errors),
                row("start", "Начало приёмки", startTime, buildDelta(routeStartWork, startTime, st?.appearanceToStartMin),
                    canHighlight = canFromAcceptance(AccField.START_TIME), errors = errors),
                row("end", "Окончание приёмки", endTime, buildDelta(startTime, endTime, deltaAcceptanceMin),
                    canHighlight = canFromAcceptance(AccField.END_TIME), errors = errors),
                row("barrierOut", "Выход на КП", barrierOut, buildDelta(endTime, barrierOut, st?.endToBarrierMin),
                    canHighlight = canFromAcceptance(AccField.BARRIER_OUT), errors = errors),
            )
        } else {
            listOf(
                row("barrierIn", "Заход на КП", barrierIn, null,
                    canHighlight = canFromDelivery(DelField.BARRIER_IN), errors = errors),
                row("start", "Начало сдачи", startTime, buildDelta(barrierIn, startTime, st?.barrierToStartMin),
                    canHighlight = canFromDelivery(DelField.START_TIME), errors = errors),
                row("end", "Окончание сдачи", endTime, buildDelta(startTime, endTime, deltaDeliveryMin),
                    canHighlight = canFromDelivery(DelField.END_TIME), errors = errors),
                row("workEnd", "Окончание работы", workEnd, buildDelta(endTime, workEnd, st?.endToWorkEndMin),
                    canHighlight = canFromDelivery(DelField.WORK_END), errors = errors, pending = pendingEndWorkUpdate),
            )
        }
    }

    private fun saveNormButtons(): Pair<SaveNormUi?, SaveNormUi?> {
        val name = selectedSeriesName
        val s = startTime
        val e = endTime
        val series = selectedSeries
        val variantText = if (normHandToHand) "из рук в руки" else "после отстоя"
        val canSaveSeries = !name.isNullOrBlank() && s != null && e != null && run {
            if (series != null) {
                val dur = ((e - s) / 60_000).toInt()
                val saved = if (isAcceptance) seriesAcceptanceMin else seriesDeliveryMin
                saved == null || saved != dur
            } else true
        }
        val seriesUi = if (canSaveSeries) SaveNormUi(
            title = "Сохранить норму серии $name",
            subtitle = if (series == null && !name.isNullOrBlank()) "Добавить в справочник · $variantText"
            else (if (isAcceptance) "Длительность приёмки · $variantText" else "Длительность сдачи · $variantText"),
        ) else null

        val st = selectedStation
        val canSaveStation = st != null && run {
            fun norm(from: Long?, to: Long?): Int? =
                if (from == null || to == null) null else ((to - from) / 60_000).toInt().takeIf { it > 0 }
            fun stored(v: Int?): Int? = v?.takeIf { it > 0 }
            if (isAcceptance) {
                routeStartWork != null && s != null && e != null &&
                    (norm(routeStartWork, s) != stored(st.appearanceToStartMin) ||
                        (barrierOut != null && norm(e, barrierOut) != stored(st.endToBarrierMin)))
            } else {
                barrierIn != null && s != null && e != null && workEnd != null &&
                    (norm(barrierIn, s) != stored(st.barrierToStartMin) ||
                        norm(e, workEnd) != stored(st.endToWorkEndMin))
            }
        }
        val missingBoth = st != null && if (isAcceptance) {
            st.appearanceToStartMin == null && st.endToBarrierMin == null
        } else {
            st.endToWorkEndMin == null && st.barrierToStartMin == null
        }
        val stationUi = if (canSaveStation && st != null) SaveNormUi(
            title = "Сохранить норму станции ${st.name}",
            subtitle = if (missingBoth) "Добавить в справочник"
            else if (isAcceptance) "явка -> приемка / приемка -> выход на КП" else "КП -> сдача / сдача -> окончание работы",
        ) else null
        return seriesUi to stationUi
    }

    /** Любое изменение значений: немедленное сохранение в форму + перерисовка (§2.11). */
    private var lastKey: List<Any?>? = null

    private fun changed() {
        emitIfChanged()
        publish()
    }

    private fun currentKey() = listOf(startTime, endTime, barrierOut, barrierIn, selectedStation?.stationId)

    private fun emitIfChanged() {
        val key = currentKey()
        if (key == lastKey) return
        lastKey = key
        val stationChanged = selectedStation != null && selectedStation?.stationId != initialStationId
        val changedFromInitial = startTime != initialStart || endTime != initialEnd ||
            barrierOut != initialBarrierOut || barrierIn != initialBarrierIn || stationChanged
        if (changedFromInitial) emitSave()
    }

    private fun emitSave() {
        onSave(TimeSheetResultUi(startTime, endTime, barrierOut, barrierIn, workEnd, selectedStation?.stationId))
    }

    private fun publish(ready: Boolean = _state.value.ready) {
        val errors = rowErrors()
        val (saveSeries, saveStation) = saveNormButtons()
        _state.value = TimeSheetState(
            ready = ready,
            isAcceptance = isAcceptance,
            title = if (isAcceptance) "Приёмка" else "Сдача",
            seriesName = selectedSeriesName,
            seriesId = selectedSeries?.seriesId,
            stationName = selectedStation?.name,
            stationId = selectedStation?.stationId,
            stationLabel = if (isAcceptance) "Станция приёмки" else "Станция сдачи",
            showNormMode = selectedSeriesName != null,
            handToHand = normHandToHand,
            warnings = warnings(),
            isAsking = anchorState == Anchor.ASKING,
            rows = rows(errors),
            saveSeries = saveSeries,
            saveStation = saveStation,
            canCalculate = canApplyNorms(),
            hasSequenceError = errors.values.any { it },
            showRecalcDialog = showRecalcDialog,
            recalcVariantText = if (normHandToHand) "Из рук в руки" else "После отстоя",
            catalogVersion = catalogVersion,
        )
    }
}
