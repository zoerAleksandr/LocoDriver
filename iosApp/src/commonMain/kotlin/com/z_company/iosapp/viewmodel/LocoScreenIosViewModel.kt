package com.z_company.iosapp.viewmodel

import com.z_company.core.ResultState
import com.z_company.domain.entities.norma_time.SectionNumberingType
import com.z_company.domain.entities.route.LocoType
import com.z_company.domain.entities.route.Locomotive
import com.z_company.domain.entities.route.SectionDiesel
import com.z_company.domain.entities.route.SectionElectric
import com.z_company.domain.entities.setting.UserSettings
import com.z_company.domain.repositories.LocomotiveSeriesRepository
import com.z_company.domain.repositories.SharedPreferencesRepositories
import com.z_company.domain.use_cases.LocomotiveUseCase
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.domain.util.CalculationEnergy
import com.z_company.domain.util.CalculationEnergy.rounding
import com.z_company.domain.util.displayTimeZone
import com.z_company.domain.util.minus
import com.z_company.domain.util.plus
import com.z_company.domain.util.sanitizeNumericInput
import com.z_company.domain.util.str
import com.z_company.domain.util.times
import com.z_company.domain.util.toDoubleOrZero
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * iOS-аналог Android `LocoFormViewModel` (SCREEN_SPECS §1): экран «Локомотив».
 *
 * Регистрируется как Koin `factory` — по экземпляру на открытый экран. Swift вызывает
 * [dispose] в `deinit` обёртки (аналог `onCleared`: финальное сохранение + guard пустого
 * нового локомотива, §1.2).
 *
 * Поля секций хранятся **текстом** (как в Android), в модель переводятся при сохранении.
 */
class LocoScreenIosViewModel(
    private val locomotiveUseCase: LocomotiveUseCase,
    private val routeUseCase: RouteUseCase,
    private val settingsUseCase: SettingsUseCase,
    private val sharedPrefs: SharedPreferencesRepositories,
    private val seriesRepository: LocomotiveSeriesRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _state = MutableStateFlow(LocoScreenState())

    private data class DieselForm(
        val id: String,
        val accepted: String = "",
        val delivery: String = "",
        val coefficient: String = "",
        val refuel: String = "",
        val refuelKilo: String = "",
        val refuelCoefficient: String = "",
    )

    private data class ElectricForm(
        val id: String,
        val accepted: String = "",
        val delivery: String = "",
        val recoveryAccepted: String = "",
        val recoveryDelivery: String = "",
        val accepted2: String = "",
        val delivery2: String = "",
        val recoveryAccepted2: String = "",
        val recoveryDelivery2: String = "",
    )

    private var started = false
    private var basicId = ""
    private var isNewLoco = true
    private var loco: Locomotive? = null
    private var diesel: List<DieselForm> = emptyList()
    private var electric: List<ElectricForm> = emptyList()
    private var numbering = SectionNumberingType.NUMERIC
    private var shouldSaveNumberingForSeries = false
    private var settings: UserSettings? = null
    private val seriesNames = mutableListOf<String>()

    private var isKiloMode = false
    private var showOtherCurrent = false
    private var heatingAccepted = ""
    private var heatingDelivery = ""
    private var auxiliaryAccepted = ""
    private var auxiliaryDelivery = ""
    private var norma1 = ""
    private var norma2 = ""
    private var saveError: String? = null
    private var routeStartWork: Long? = null
    private var routeEndWork: Long? = null

    // Снимок кг на момент открытия шторки коэффициента (режим «кг», §1.3 п.4).
    private var kgSnapshotAccepted: Double? = null
    private var kgSnapshotDelivery: Double? = null

    private var autoSaveJob: Job? = null

    // ═══ Подписки для Swift ═══════════════════════════════════════════════════

    fun watchState(callback: (LocoScreenState) -> Unit) {
        scope.launch { _state.collect { callback(it) } }
    }

    // ═══ Старт / загрузка (§1.1) ══════════════════════════════════════════════

    fun start(basicId: String, locoId: String?) {
        if (started) return
        started = true
        this.basicId = basicId
        scope.launch {
            numbering = sharedPrefs.getDefaultLocoSectionNumberingType()
            isKiloMode = sharedPrefs.isInputDieselInKilo()
            val db = settingsUseCase.getUserSettingFlow().first()
            val sett = withPrefsFlags(db)
            settings = sett
            seriesNames.addAll(db.locomotiveSeriesList.map { it.trim() }.filter { it.isNotBlank() })
            showOtherCurrent = sett.isShowOtherCurrent
            // Справочник серий — источник автодополнения (синхронизированные серии тоже).
            scope.launch {
                seriesRepository.getAllFlow().collect { records ->
                    val names = records.map { it.name.trim() }.filter { it.isNotBlank() }
                    val known = seriesNames.map { it.lowercase() }.toSet()
                    val missing = names.filter { it.lowercase() !in known }.distinct()
                    if (missing.isNotEmpty()) {
                        seriesNames.addAll(missing)
                        settingsUseCase.setLocomotiveSeriesList(missing)
                    }
                }
            }
            try {
                val route = routeUseCase.routeDetails(basicId)
                    .first { it is ResultState.Success || it is ResultState.Error }
                (route as? ResultState.Success)?.data?.basicData?.let {
                    routeStartWork = it.timeStartWork
                    routeEndWork = it.timeEndWork
                }
            } catch (e: Exception) {
                // Не критично: шторка покажет пустую явку
            }
            if (locoId == null) {
                isNewLoco = true
                loco = Locomotive(basicId = basicId, type = sett.defaultLocoType)
                electric = listOf(ElectricForm(id = SectionElectric().sectionId))
                diesel = listOf(newDieselForm())
            } else {
                isNewLoco = false
                loadLoco(locoId)
            }
            publish(isLoading = false)
        }
    }

    private suspend fun loadLoco(locoId: String) {
        val result = locomotiveUseCase.getLocoById(locoId)
            .first { it is ResultState.Success || it is ResultState.Error }
        val data = (result as? ResultState.Success)?.data
        if (data == null) {
            // Не найден — как в Android экран остаётся пустым; создаём пустую форму, чтобы не зависнуть.
            loco = Locomotive(basicId = basicId, type = settings?.defaultLocoType ?: LocoType.ELECTRIC)
            electric = listOf(ElectricForm(id = SectionElectric().sectionId))
            diesel = listOf(newDieselForm())
            return
        }
        loco = data
        seriesRepository.getAll()
            .find { it.name.equals(data.series?.trim(), ignoreCase = true) }
            ?.sectionNumberingType
            ?.let { numbering = it }
        applySections(data)
    }

    private fun applySections(l: Locomotive) {
        var otherCurrent = (l.normaElectricCurrent2 ?: 0.0) != 0.0
        if (l.type == LocoType.DIESEL) {
            diesel = l.dieselSectionList.map {
                DieselForm(
                    id = it.sectionId,
                    accepted = it.acceptedFuel?.str() ?: "",
                    delivery = it.deliveryFuel?.str() ?: "",
                    coefficient = it.coefficient?.str() ?: "",
                    refuel = it.fuelSupply?.str() ?: "",
                    refuelKilo = it.fuelSupplyInKilo?.str() ?: "",
                    refuelCoefficient = it.coefficientSupply?.str() ?: "",
                )
            }
            electric = emptyList()
        } else {
            electric = l.electricSectionList.map {
                if (it.acceptedEnergyOtherCurrent != null || it.deliveryEnergyOtherCurrent != null ||
                    it.acceptedRecoveryOtherCurrent != null || it.deliveryRecoveryOtherCurrent != null
                ) otherCurrent = true
                ElectricForm(
                    id = it.sectionId,
                    accepted = it.acceptedEnergy?.str() ?: "",
                    delivery = it.deliveryEnergy?.str() ?: "",
                    recoveryAccepted = it.acceptedRecovery?.str() ?: "",
                    recoveryDelivery = it.deliveryRecovery?.str() ?: "",
                    accepted2 = it.acceptedEnergyOtherCurrent?.str() ?: "",
                    delivery2 = it.deliveryEnergyOtherCurrent?.str() ?: "",
                    recoveryAccepted2 = it.acceptedRecoveryOtherCurrent?.str() ?: "",
                    recoveryDelivery2 = it.deliveryRecoveryOtherCurrent?.str() ?: "",
                )
            }
            diesel = emptyList()
        }
        showOtherCurrent = otherCurrent || (settings?.isShowOtherCurrent == true)
        // Значение 0.0 показывается пустым полем (§1.1).
        heatingAccepted = l.heatingCounterAccepted?.takeIf { it != 0.0 }?.str() ?: ""
        heatingDelivery = l.heatingCounterDelivery?.takeIf { it != 0.0 }?.str() ?: ""
        auxiliaryAccepted = l.auxiliaryCounterAccepted?.takeIf { it != 0.0 }?.str() ?: ""
        auxiliaryDelivery = l.auxiliaryCounterDelivery?.takeIf { it != 0.0 }?.str() ?: ""
        norma1 = l.normaElectricCurrent1?.takeIf { it != 0.0 }?.str() ?: ""
        norma2 = l.normaElectricCurrent2?.takeIf { it != 0.0 }?.str() ?: ""
    }

    private fun newDieselForm() = DieselForm(
        id = SectionDiesel().sectionId,
        coefficient = settings?.lastEnteredDieselCoefficient?.str() ?: "",
        refuelCoefficient = sharedPrefs.getLastRefuelCoefficient(),
    )

    private fun withPrefsFlags(s: UserSettings): UserSettings = s.copy(
        isShowLocoHeating = sharedPrefs.isShowLocoHeating(),
        isShowLocoAuxiliary = sharedPrefs.isShowLocoAuxiliary(),
        isShowLocoStatistics = sharedPrefs.isShowLocoStatistics(),
        isShowLocoNorma = sharedPrefs.isShowLocoNorma(),
        isShowOtherCurrent = sharedPrefs.isShowOtherCurrent(),
    )

    /** При возврате на экран (ON_RESUME) флаги видимости перечитываются из настроек (§1.1). */
    fun reloadSettingsFromPrefs() {
        val current = settings ?: return
        settings = withPrefsFlags(current)
        val hasOtherData = electric.any {
            it.accepted2.isNotBlank() || it.delivery2.isNotBlank() ||
                it.recoveryAccepted2.isNotBlank() || it.recoveryDelivery2.isNotBlank()
        }
        showOtherCurrent = sharedPrefs.isShowOtherCurrent() || hasOtherData
        publish()
    }

    // ═══ Основные данные (§1.3 п.2) ════════════════════════════════════════════

    fun setSeries(value: String) {
        loco = loco?.copy(series = value)
        changesHave()
    }

    fun setNumber(value: String) {
        loco = loco?.copy(number = value)
        changesHave()
    }

    /** Подсказки серии: фильтр `startsWith(ввод, ignoreCase)`, точное совпадение скрыто (§1.3 п.2). */
    fun suggestSeries(query: String): List<String> {
        if (query.isEmpty()) return seriesNames.toList()
        return seriesNames.filter { it.startsWith(query, ignoreCase = true) }.filterNot { it == query }
    }

    /** Выбор готовой серии применяет вид тяги и тип нумерации записи справочника. */
    fun selectSeries(name: String) {
        val selected = seriesRepository.getAll().find { it.name.equals(name.trim(), ignoreCase = true) }
        selected?.sectionNumberingType?.let { numbering = it }
        shouldSaveNumberingForSeries = false
        loco = loco?.let { it.copy(series = name, type = selected?.type ?: it.type) }
        ensureOneSection()
        changesHave()
    }

    /** Удаляет только подсказку из `locomotiveSeriesList`; запись справочника остаётся. */
    fun removeSeriesSuggestion(name: String) {
        seriesNames.removeAll { it.equals(name, ignoreCase = true) }
        scope.launch { settingsUseCase.removeLocomotiveSeries(name) }
        changesHave()
    }

    fun changeType(isElectric: Boolean) {
        loco = loco?.copy(type = if (isElectric) LocoType.ELECTRIC else LocoType.DIESEL)
        ensureOneSection()
        changesHave()
    }

    /** Гарантирует минимум одну секцию текущего вида; секции прежнего вида не удаляются. */
    private fun ensureOneSection() {
        when (loco?.type) {
            LocoType.DIESEL -> if (diesel.isEmpty()) diesel = listOf(newDieselForm())
            LocoType.ELECTRIC -> if (electric.isEmpty()) electric = listOf(ElectricForm(id = SectionElectric().sectionId))
            null -> Unit
        }
    }

    // ═══ Запись времени из шторки (§1.4) ═══════════════════════════════════════

    fun saveAcceptanceFromSheet(startTime: Long?, endTime: Long?, barrierOut: Long?, stationId: String?) {
        loco = loco?.copy(
            timeStartOfAcceptance = startTime,
            timeEndOfAcceptance = endTime,
            timeBarrierOut = barrierOut,
            acceptanceStationId = stationId,
        )
        changesHave()
    }

    fun saveDeliveryFromSheet(barrierIn: Long?, startTime: Long?, endTime: Long?, stationId: String?) {
        loco = loco?.copy(
            timeBarrierIn = barrierIn,
            timeStartOfDelivery = startTime,
            timeEndOfDelivery = endTime,
            deliveryStationId = stationId,
        )
        changesHave()
    }

    /** Окончание работы (сдача) пишется не в локомотив, а в маршрут (§1.4). */
    fun setTimeEndWork(value: Long?) {
        scope.launch {
            try {
                val result = routeUseCase.routeDetails(basicId)
                    .first { it is ResultState.Success || it is ResultState.Error }
                val route = (result as? ResultState.Success)?.data ?: return@launch
                routeUseCase.saveRoute(route.copy(basicData = route.basicData.copy(timeEndWork = value))).collect {}
                routeEndWork = value
                publish()
            } catch (e: Exception) {
                // Не критично
            }
        }
    }

    // ═══ Секции (§1.3 п.4) ═════════════════════════════════════════════════════

    fun addSection() {
        if (loco?.type == LocoType.DIESEL) {
            diesel = diesel + newDieselForm()
        } else {
            electric = electric + ElectricForm(id = SectionElectric().sectionId)
        }
        changesHave()
    }

    /** true — секция пуста и удалена сразу; false — нужно подтверждение «Удалить секцию?». */
    fun requestDeleteSection(id: String): Boolean {
        val empty = if (loco?.type == LocoType.DIESEL) {
            diesel.find { it.id == id }?.let { isDieselEmpty(it) } ?: true
        } else {
            electric.find { it.id == id }?.let { isElectricEmpty(it) } ?: true
        }
        if (empty) deleteSection(id)
        return empty
    }

    fun deleteSection(id: String) {
        diesel = diesel.filterNot { it.id == id }
        electric = electric.filterNot { it.id == id }
        changesHave()
    }

    /** Перетаскивание карточки секции: меняет порядок (считается изменением формы). */
    fun moveSection(from: Int, to: Int) {
        if (loco?.type == LocoType.DIESEL) {
            if (from !in diesel.indices || to !in diesel.indices || from == to) return
            diesel = diesel.toMutableList().also { it.add(to, it.removeAt(from)) }
        } else {
            if (from !in electric.indices || to !in electric.indices || from == to) return
            electric = electric.toMutableList().also { it.add(to, it.removeAt(from)) }
        }
        changesHave()
    }

    /** Подписи всех секций переключаются; выбор запоминается и пишется в запись серии. */
    fun toggleNumbering() {
        val seriesName = loco?.series?.trim().orEmpty()
        val next = if (numbering == SectionNumberingType.NUMERIC) SectionNumberingType.LETTERS else SectionNumberingType.NUMERIC
        numbering = next
        sharedPrefs.setDefaultLocoSectionNumberingType(next)
        shouldSaveNumberingForSeries = true
        publish()
        scope.launch {
            val all = seriesRepository.getAll().toMutableList()
            val index = all.indexOfFirst { it.name.equals(seriesName, ignoreCase = true) }
            if (index >= 0) {
                all[index] = all[index].copy(sectionNumberingType = next)
                seriesRepository.replaceAll(all).collect {}
                shouldSaveNumberingForSeries = false
                changesHave()
            }
        }
    }

    fun toggleKiloMode() {
        isKiloMode = !isKiloMode
        sharedPrefs.toggleInputDieselInKilo(isKiloMode)
        publish()
    }

    // ── Тепловоз ──────────────────────────────────────────────────────────────

    private fun updateDiesel(index: Int, block: DieselForm.() -> DieselForm) {
        if (index !in diesel.indices) return
        diesel = diesel.toMutableList().also { it[index] = it[index].block() }
    }

    /** `field`: «accepted» | «delivery» — ввод в выбранных единицах Л/КГ, в модель всегда литры. */
    fun setFuel(index: Int, field: String, text: String) {
        val section = diesel.getOrNull(index) ?: return
        val filtered = text.filter { it.isDigit() || it == '.' }.take(6)
        if (filtered.count { it == '.' } > 1) return
        val liters: String = if (filtered.isEmpty()) {
            ""
        } else {
            val input = filtered.toDoubleOrNull() ?: return
            val coeff = section.coefficient.toDoubleOrNull() ?: 1.0
            if (isKiloMode) {
                if (coeff != 0.0) (input / coeff).str() else ""
            } else {
                rounding(input, 2).str()
            }
        }
        updateDiesel(index) { if (field == "accepted") copy(accepted = liters) else copy(delivery = liters) }
        changesHave()
    }

    /** Экипировка, литры: кг = round2(л × k); литры пусты → кг тоже пусты; k нет — кг не трогаем. */
    fun setRefuel(index: Int, text: String) {
        val value = text.take(7)
        updateDiesel(index) {
            val coeff = refuelCoefficient.toDoubleOrNull()
            val kilo = if (coeff != null) {
                val liters = value.toDoubleOrNull()
                if (liters != null) rounding(liters * coeff, 2)?.str() ?: "" else ""
            } else refuelKilo
            copy(refuel = value, refuelKilo = kilo)
        }
        changesHave()
    }

    /** Экипировка, кг: k задан и ≠ 0 → л = round2(кг ÷ k); иначе литры не трогаем. */
    fun setRefuelKilo(index: Int, text: String) {
        val value = text.take(7)
        updateDiesel(index) {
            val coeff = refuelCoefficient.toDoubleOrNull()
            val liters = if (coeff != null && coeff != 0.0) {
                value.toDoubleOrNull()?.let { rounding(it / coeff, 2)?.str() ?: "" }
            } else refuel
            copy(refuelKilo = value, refuel = liters ?: refuel)
        }
        changesHave()
    }

    /** Открытие шторки коэффициента: снимок кг для режима «кг». */
    fun openCoefficient(index: Int) {
        val s = diesel.getOrNull(index) ?: return
        if (isKiloMode) {
            val coeff = s.coefficient.toDoubleOrNull() ?: 1.0
            kgSnapshotAccepted = s.accepted.toDoubleOrNull()?.times(coeff)
            kgSnapshotDelivery = s.delivery.toDoubleOrNull()?.times(coeff)
        } else {
            kgSnapshotAccepted = null
            kgSnapshotDelivery = null
        }
    }

    private fun filterCoefficient(text: String): String? {
        val filtered = text.filter { it.isDigit() || it == '.' }.take(5)
        return if (filtered.count { it == '.' } <= 1) filtered else null
    }

    private fun applyKgPreserve(index: Int, newValue: String) {
        if (!isKiloMode) return
        val newCoeff = newValue.toDoubleOrNull()
        if (newCoeff != null && newCoeff != 0.0) {
            kgSnapshotAccepted?.let { kg -> updateDiesel(index) { copy(accepted = (kg / newCoeff).str()) } }
            kgSnapshotDelivery?.let { kg -> updateDiesel(index) { copy(delivery = (kg / newCoeff).str()) } }
        }
    }

    /** «k секции»: степпер/поле шторки. В режиме «кг» введённые кг сохраняются, литры пересчитываются. */
    fun setCoefficient(index: Int, text: String) {
        val value = filterCoefficient(text) ?: return
        updateDiesel(index) { copy(coefficient = value) }
        applyKgPreserve(index, value)
        changesHave()
    }

    /** Закрытие шторки «k секции»: история + последний коэффициент для новых секций (§1.3 п.4). */
    fun closeCoefficient(index: Int) {
        val value = diesel.getOrNull(index)?.coefficient ?: return
        value.toDoubleOrNull()?.let { d ->
            sharedPrefs.addRecentCoefficient(value)
            scope.launch { settingsUseCase.setDieselCoefficient(d).collect {} }
        }
    }

    /**
     * «k экипировки»; [applyToSection] (чекбокс «Применить ко всем значениям этой секции») копирует
     * значение и в «k секции». При заданных литрах кг пересчитываются.
     */
    fun setRefuelCoefficient(index: Int, text: String, applyToSection: Boolean) {
        val value = filterCoefficient(text) ?: return
        updateDiesel(index) {
            val coeff = value.toDoubleOrNull()
            val liters = refuel.toDoubleOrNull()
            val kilo = if (coeff != null && liters != null) rounding(liters * coeff, 2)?.str() ?: "" else refuelKilo
            copy(refuelCoefficient = value, refuelKilo = kilo)
        }
        if (applyToSection) {
            updateDiesel(index) { copy(coefficient = value) }
            applyKgPreserve(index, value)
        }
        changesHave()
    }

    /** Включение чекбокса «Применить…» сразу копирует k экипировки в k секции. */
    fun applyRefuelCoefficientToSection(index: Int) {
        val v = diesel.getOrNull(index)?.refuelCoefficient ?: return
        updateDiesel(index) { copy(coefficient = v) }
        applyKgPreserve(index, v)
        changesHave()
    }

    fun closeRefuelCoefficient(index: Int) {
        val value = diesel.getOrNull(index)?.refuelCoefficient ?: return
        value.toDoubleOrNull()?.let { d ->
            sharedPrefs.addRecentCoefficient(value)
            sharedPrefs.setLastRefuelCoefficient(value)
            scope.launch { settingsUseCase.setDieselCoefficient(d).collect {} }
        }
    }

    fun recentCoefficients(): List<String> = sharedPrefs.getRecentCoefficients()

    /** Шаг степпера: ±0.01 (±0.001 при ≥ 3 знаках), минимум 0, пустое шагает от 1.0. */
    fun stepCoefficient(value: String, direction: Int): String {
        val current = value.toDoubleOrNull()
        val decimals = value.substringAfter('.', "").length
        val precision = if (decimals >= 3) 3 else 2
        val step = if (decimals >= 3) 0.001 else 0.01
        val base = current ?: 1.0
        val next = if (direction < 0) (base - step).coerceAtLeast(0.0) else base + step
        return rounding(next, precision)?.str() ?: ""
    }

    // ── Электровоз ────────────────────────────────────────────────────────────

    /** `field`: accepted | delivery | recoveryAccepted | recoveryDelivery | accepted2 | delivery2 | recoveryAccepted2 | recoveryDelivery2. */
    fun setEnergy(index: Int, field: String, text: String) {
        if (index !in electric.indices) return
        val value = text.take(10)
        val s = electric[index]
        electric = electric.toMutableList().also {
            it[index] = when (field) {
                "accepted" -> s.copy(accepted = value)
                "delivery" -> s.copy(delivery = value)
                "recoveryAccepted" -> s.copy(recoveryAccepted = value)
                "recoveryDelivery" -> s.copy(recoveryDelivery = value)
                "accepted2" -> s.copy(accepted2 = value)
                "delivery2" -> s.copy(delivery2 = value)
                "recoveryAccepted2" -> s.copy(recoveryAccepted2 = value)
                "recoveryDelivery2" -> s.copy(recoveryDelivery2 = value)
                else -> s
            }
        }
        changesHave()
    }

    // ═══ ДРУГОЕ и ИТОГО ═══════════════════════════════════════════════════════

    // Отклонение от Android (BUG-03): счётчики и нормы тоже автосохраняются (по требованию владельца).
    fun setCounter(field: String, text: String) {
        when (field) {
            "heatingAccepted" -> {
                heatingAccepted = text
                loco = loco?.copy(heatingCounterAccepted = text.toDoubleOrNull())
            }
            "heatingDelivery" -> {
                heatingDelivery = text
                loco = loco?.copy(heatingCounterDelivery = text.toDoubleOrNull())
            }
            "auxiliaryAccepted" -> {
                auxiliaryAccepted = text
                loco = loco?.copy(auxiliaryCounterAccepted = text.toDoubleOrNull())
            }
            "auxiliaryDelivery" -> {
                auxiliaryDelivery = text
                loco = loco?.copy(auxiliaryCounterDelivery = text.toDoubleOrNull())
            }
        }
        changesHave()
    }

    fun setNorma1(text: String) {
        norma1 = text.sanitizeNumericInput(allowDecimal = true)
        loco = loco?.copy(normaElectricCurrent1 = norma1.toDoubleOrNull())
        changesHave()
    }

    fun setNorma2(text: String) {
        norma2 = text.sanitizeNumericInput(allowDecimal = true)
        loco = loco?.copy(normaElectricCurrent2 = norma2.toDoubleOrNull())
        changesHave()
    }

    /** Строка уходит на сервер как есть — только с точкой (§1.3 п.6). */
    fun setNormaDiesel(text: String) {
        val clean = text.sanitizeNumericInput(allowDecimal = true)
        loco = loco?.copy(normaDiesel = clean.ifBlank { null })
        changesHave()
    }

    // ═══ Сохранение (§1.2) ═════════════════════════════════════════════════════

    private fun changesHave() {
        publish()
        autoSaveJob?.cancel()
        autoSaveJob = scope.launch {
            delay(500)
            persist(buildLoco())
        }
    }

    private fun buildLoco(): Locomotive? {
        val l = loco ?: return null
        return l.copy(
            dieselSectionList = diesel.map {
                SectionDiesel(
                    sectionId = it.id,
                    acceptedFuel = it.accepted.toDoubleOrNull(),
                    deliveryFuel = it.delivery.toDoubleOrNull(),
                    coefficient = it.coefficient.toDoubleOrNull(),
                    fuelSupply = it.refuel.toDoubleOrNull(),
                    fuelSupplyInKilo = it.refuelKilo.toDoubleOrNull(),
                    coefficientSupply = it.refuelCoefficient.toDoubleOrNull(),
                )
            }.toMutableList(),
            electricSectionList = electric.map {
                SectionElectric(
                    sectionId = it.id,
                    acceptedEnergy = it.accepted.toDoubleOrNull(),
                    deliveryEnergy = it.delivery.toDoubleOrNull(),
                    acceptedRecovery = it.recoveryAccepted.toDoubleOrNull(),
                    deliveryRecovery = it.recoveryDelivery.toDoubleOrNull(),
                    acceptedEnergyOtherCurrent = it.accepted2.toDoubleOrNull(),
                    deliveryEnergyOtherCurrent = it.delivery2.toDoubleOrNull(),
                    acceptedRecoveryOtherCurrent = it.recoveryAccepted2.toDoubleOrNull(),
                    deliveryRecoveryOtherCurrent = it.recoveryDelivery2.toDoubleOrNull(),
                )
            }.toMutableList(),
        )
    }

    private suspend fun persist(updated: Locomotive?) {
        updated ?: return
        updated.series?.takeIf { it.isNotBlank() }?.let { settingsUseCase.setLocomotiveSeries(it) }
        saveNumberingForExistingSeries(updated.series)
        locomotiveUseCase.saveLocomotive(updated).collect { result ->
            if (result is ResultState.Error) {
                saveError = "Ошибка: ${result.entity.message ?: result.entity.throwable?.message ?: ""}"
                publish()
            }
        }
    }

    private suspend fun saveNumberingForExistingSeries(seriesName: String?) {
        if (!shouldSaveNumberingForSeries) return
        val normalized = seriesName?.trim().orEmpty()
        if (normalized.isNotBlank()) {
            val all = seriesRepository.getAll().toMutableList()
            val index = all.indexOfFirst { it.name.equals(normalized, ignoreCase = true) }
            if (index >= 0) {
                all[index] = all[index].copy(sectionNumberingType = numbering)
                seriesRepository.replaceAll(all).collect {}
            }
        }
        // Незнакомую серию в справочник не добавляем.
        shouldSaveNumberingForSeries = false
    }

    fun clearSaveError() {
        saveError = null
        publish()
    }

    /** Guard пустого локомотива (§1.2): серия, номер, времена, станции, нормы, счётчики и секции пусты. */
    private fun isLocoEmpty(l: Locomotive): Boolean =
        l.series.isNullOrBlank() && l.number.isNullOrBlank() &&
            l.timeStartOfAcceptance == null && l.timeEndOfAcceptance == null &&
            l.timeStartOfDelivery == null && l.timeEndOfDelivery == null &&
            l.timeBarrierOut == null && l.timeBarrierIn == null &&
            l.acceptanceStationId == null && l.deliveryStationId == null &&
            (l.normaElectricCurrent1 ?: 0.0) == 0.0 && (l.normaElectricCurrent2 ?: 0.0) == 0.0 &&
            l.normaDiesel.isNullOrBlank() &&
            (l.heatingCounterAccepted ?: 0.0) == 0.0 && (l.heatingCounterDelivery ?: 0.0) == 0.0 &&
            (l.auxiliaryCounterAccepted ?: 0.0) == 0.0 && (l.auxiliaryCounterDelivery ?: 0.0) == 0.0 &&
            diesel.all { isDieselEmpty(it) } && electric.all { isElectricEmpty(it) }

    /** Немедленное сохранение (сворачивание приложения, «Готово»): без ожидания дебаунса. */
    fun flush() {
        autoSaveJob?.cancel()
        val updated = buildLoco() ?: return
        if (isNewLoco && isLocoEmpty(updated)) return
        scope.launch { persist(updated) }
    }

    /** Финальное сохранение при уходе с экрана (аналог `onCleared`). */
    fun dispose() {
        autoSaveJob?.cancel()
        val updated = buildLoco()
        if (updated != null && !(isNewLoco && isLocoEmpty(updated))) {
            CoroutineScope(NonCancellable + Dispatchers.Default).launch { persist(updated) }
        }
        scope.cancel()
    }

    private fun blankOrZero(s: String) = s.isBlank() || s.toDoubleOrNull() == 0.0

    private fun isDieselEmpty(s: DieselForm) =
        blankOrZero(s.accepted) && blankOrZero(s.delivery) && blankOrZero(s.refuel) && blankOrZero(s.refuelKilo)

    private fun isElectricEmpty(s: ElectricForm) =
        blankOrZero(s.accepted) && blankOrZero(s.delivery) && blankOrZero(s.recoveryAccepted) &&
            blankOrZero(s.recoveryDelivery) && blankOrZero(s.accepted2) && blankOrZero(s.delivery2) &&
            blankOrZero(s.recoveryAccepted2) && blankOrZero(s.recoveryDelivery2)

    // ═══ Сборка состояния для Swift ═══════════════════════════════════════════

    private fun publish(isLoading: Boolean = _state.value.isLoading) {
        _state.value = buildState(isLoading)
    }

    private fun displayOffsetSeconds(): Int {
        // BUG-07 (паритет с Android): «GMT+05:30» не разбирается → GMT+3.
        val tz = settings?.displayTimeZone() ?: "GMT+3"
        val hours = tz.removePrefix("GMT").removePrefix("UTC").toIntOrNull() ?: 3
        return hours * 3600
    }

    private fun sectionLabel(index: Int): String {
        val letters = listOf("А", "Б", "В", "Г", "Д", "Е", "Ж", "З")
        return if (numbering == SectionNumberingType.LETTERS) {
            "Секция ${letters.getOrNull(index) ?: (index + 1)}"
        } else {
            "Секция ${index + 1}"
        }
    }

    private fun decimalPlaces(vararg texts: String): Int = texts.maxOf { s ->
        val dot = s.indexOf('.')
        if (dot < 0) 0 else s.length - dot - 1
    }

    private fun groupThousands(s: String?): String {
        if (s.isNullOrBlank()) return "0"
        val neg = s.startsWith("-") || s.startsWith("−")
        val body = s.trimStart('-', '−')
        val dot = body.indexOf('.')
        val intPart = if (dot < 0) body else body.substring(0, dot)
        val frac = if (dot < 0) "" else body.substring(dot)
        val grouped = intPart.reversed().chunked(3).joinToString(" ").reversed()
        return (if (neg) "−" else "") + grouped + frac.replace('.', ',')
    }

    private fun resultUi(norma: Double, consumed: Double?): LocoResultUi {
        val result = norma - (consumed ?: 0.0)
        val pct = if (norma != 0.0) abs(result / norma * 100).toInt() else 0
        return LocoResultUi(isOver = result < 0, magnitude = groupThousands(rounding(abs(result), 2).str()), percent = pct)
    }

    private fun dieselUi(index: Int, s: DieselForm): LocoDieselSectionUi {
        val acc = s.accepted.toDoubleOrNull()
        val del = s.delivery.toDoubleOrNull()
        val k = s.coefficient.toDoubleOrNull()
        val coeff = k ?: 1.0
        val accKilo = acc.times(k)
        val delKilo = del.times(k)
        val result = CalculationEnergy.getTotalFuelConsumption(acc, del, s.refuel.toDoubleOrNull())
        val resultKilo = CalculationEnergy.getTotalFuelInKiloConsumption(accKilo, delKilo, s.refuelKilo.toDoubleOrNull())

        fun shown(raw: String, liters: Double?): String = when {
            raw.isBlank() -> ""
            isKiloMode -> liters?.let { rounding(it * coeff, 2)?.str() ?: "" } ?: ""
            else -> rounding(raw.toDoubleOrZero(), 2).str()
        }

        fun hint(raw: String, kilo: Double?): String {
            if (raw.isBlank() || kilo == null) return ""
            return if (isKiloMode) "${rounding(raw.toDoubleOrZero(), 2).str()} л" else "${rounding(kilo, 2)?.str() ?: ""} кг"
        }

        val kiloText = "${rounding(resultKilo ?: 0.0, 2)?.str() ?: ""} кг"
        val literText = "${rounding(result ?: 0.0, 2).str()} л"
        return LocoDieselSectionUi(
            id = s.id,
            label = sectionLabel(index),
            acceptedText = shown(s.accepted, acc),
            deliveryText = shown(s.delivery, del),
            acceptedHint = hint(s.accepted, accKilo),
            deliveryHint = hint(s.delivery, delKilo),
            coefficientText = s.coefficient,
            coefficientPill = s.coefficient.ifBlank { "1.0" },
            refuelText = s.refuel,
            refuelKiloText = s.refuelKilo,
            refuelCoefficientText = s.refuelCoefficient,
            refuelCoefficientPill = s.refuelCoefficient.ifBlank { "1.0" },
            hasSupplyData = s.refuel.isNotBlank() || s.refuelKilo.isNotBlank(),
            resultText = "$kiloText / $literText",
            isEmpty = isDieselEmpty(s),
        )
    }

    private fun electricUi(index: Int, s: ElectricForm): LocoElectricSectionUi {
        val energyPrecision = decimalPlaces(s.accepted, s.delivery)
        val recoveryPrecision = decimalPlaces(s.recoveryAccepted, s.recoveryDelivery)
        val energyPrecision2 = decimalPlaces(s.accepted2, s.delivery2)
        val recoveryPrecision2 = decimalPlaces(s.recoveryAccepted2, s.recoveryDelivery2)
        val result = rounding(CalculationEnergy.getTotalEnergyConsumption(s.accepted.toDoubleOrNull(), s.delivery.toDoubleOrNull()), energyPrecision)
        val recovery = rounding(CalculationEnergy.getTotalEnergyConsumption(s.recoveryAccepted.toDoubleOrNull(), s.recoveryDelivery.toDoubleOrNull()), recoveryPrecision)
        val result2 = rounding(CalculationEnergy.getTotalEnergyConsumption(s.accepted2.toDoubleOrNull(), s.delivery2.toDoubleOrNull()), energyPrecision2)
        val recovery2 = rounding(CalculationEnergy.getTotalEnergyConsumption(s.recoveryAccepted2.toDoubleOrNull(), s.recoveryDelivery2.toDoubleOrNull()), recoveryPrecision2)
        return LocoElectricSectionUi(
            id = s.id,
            label = sectionLabel(index),
            accepted = s.accepted,
            delivery = s.delivery,
            recoveryAccepted = s.recoveryAccepted,
            recoveryDelivery = s.recoveryDelivery,
            accepted2 = s.accepted2,
            delivery2 = s.delivery2,
            recoveryAccepted2 = s.recoveryAccepted2,
            recoveryDelivery2 = s.recoveryDelivery2,
            consumption = (result ?: 0.0).str(),
            recovery = recovery?.str(),
            consumption2 = result2?.str(),
            recovery2 = recovery2?.str(),
            hasRecoveryData = s.recoveryAccepted.isNotBlank() || s.recoveryDelivery.isNotBlank() ||
                s.recoveryAccepted2.isNotBlank() || s.recoveryDelivery2.isNotBlank(),
            isEmpty = isElectricEmpty(s),
        )
    }

    private fun counterResult(accepted: String, delivery: String): String? {
        val a = accepted.toDoubleOrNull()
        val d = delivery.toDoubleOrNull()
        if (a == null || d == null) return null
        return rounding(d - a, 2)?.str()
    }

    private fun electricTotals(l: Locomotive): List<LocoElectricTotalUi> {
        var over: Double? = null
        var overRec: Double? = null
        var over2: Double? = null
        var overRec2: Double? = null
        electric.forEach {
            over += (it.delivery.toDoubleOrNull() - it.accepted.toDoubleOrNull())
            overRec += (it.recoveryDelivery.toDoubleOrNull() - it.recoveryAccepted.toDoubleOrNull())
            over2 += (it.delivery2.toDoubleOrNull() - it.accepted2.toDoubleOrNull())
            overRec2 += (it.recoveryDelivery2.toDoubleOrNull() - it.recoveryAccepted2.toDoubleOrNull())
        }
        over = CalculationEnergy.getTotalEnergyConsumption(
            sectionConsumption = over,
            heatingAccepted = l.heatingCounterAccepted,
            heatingDelivery = l.heatingCounterDelivery,
            auxiliaryAccepted = l.auxiliaryCounterAccepted,
            auxiliaryDelivery = l.auxiliaryCounterDelivery,
            considerHeating = settings?.isConsiderLocoHeatingInTotal ?: true,
            considerAuxiliary = settings?.isConsiderLocoAuxiliaryInTotal ?: true,
        )

        fun block(label: String?, consumed: Double?, recovery: Double?, norma: String): LocoElectricTotalUi {
            val net = (consumed ?: 0.0) - (recovery ?: 0.0)
            return LocoElectricTotalUi(
                label = label,
                consumed = groupThousands(rounding(consumed, 2)?.str()),
                recovery = recovery?.let { groupThousands(rounding(it, 2)?.str()) },
                net = recovery?.let { groupThousands(rounding(net, 2).str()) },
                result = if (norma.isNotBlank()) resultUi(norma.toDoubleOrZero(), consumed) else null,
            )
        }
        val list = mutableListOf(block(if (showOtherCurrent) "ТОК 1" else null, over, overRec, norma1))
        if (showOtherCurrent) list += block("ТОК 2", over2, overRec2, norma2)
        return list
    }

    private fun dieselTotal(l: Locomotive): LocoDieselTotalUi {
        var liters: Double? = null
        var kilo: Double? = null
        var refuelLiters: Double? = null
        var refuelKilo: Double? = null
        diesel.forEach {
            val acc = it.accepted.toDoubleOrNull()
            val del = it.delivery.toDoubleOrNull()
            val k = it.coefficient.toDoubleOrNull()
            liters += CalculationEnergy.getTotalFuelConsumption(acc, del, it.refuel.toDoubleOrNull())
            // Кг-итог — тем же способом, что в карточке секции (не умножать суммарные литры на k).
            kilo += CalculationEnergy.getTotalFuelInKiloConsumption(acc.times(k), del.times(k), it.refuelKilo.toDoubleOrNull())
            refuelLiters += it.refuel.toDoubleOrNull()
            refuelKilo += it.refuelKilo.toDoubleOrNull()
        }
        fun big(lit: Double?, kg: Double?): Triple<String, String, String?> {
            val litersText = rounding(lit, 2)?.str() ?: "—"
            val kiloText = rounding(kg, 2)?.str()
            return if (kiloText != null) Triple(kiloText, "кг", "$litersText л") else Triple(litersText, "л", null)
        }
        val fuel = big(liters, kilo)
        val supply = big(refuelLiters ?: 0.0, refuelKilo)
        val normaText = l.normaDiesel
        val result = if (!normaText.isNullOrBlank()) {
            val norma = normaText.toDoubleOrZero()
            val over = (norma - kilo) ?: 0.0
            val pct = if (norma != 0.0) abs(over / norma * 100).toInt() else 0
            LocoResultUi(isOver = over < 0, magnitude = rounding(abs(over), 2).str(), percent = pct)
        } else null
        return LocoDieselTotalUi(
            fuelBig = fuel.first, fuelUnit = fuel.second, fuelPill = fuel.third,
            hasSupply = refuelLiters != null,
            supplyBig = supply.first, supplyUnit = supply.second, supplyPill = supply.third,
            result = result,
        )
    }

    private fun buildState(isLoading: Boolean): LocoScreenState {
        val l = loco
        if (l == null) return LocoScreenState(isLoading = isLoading, displayOffsetSeconds = displayOffsetSeconds())
        val isElectric = l.type == LocoType.ELECTRIC
        val built = buildLoco() ?: l
        return LocoScreenState(
            isLoading = isLoading,
            isNew = isNewLoco,
            series = l.series ?: "",
            number = l.number ?: "",
            isElectric = isElectric,
            timeStartOfAcceptance = l.timeStartOfAcceptance,
            timeEndOfAcceptance = l.timeEndOfAcceptance,
            timeBarrierOut = l.timeBarrierOut,
            timeBarrierIn = l.timeBarrierIn,
            timeStartOfDelivery = l.timeStartOfDelivery,
            timeEndOfDelivery = l.timeEndOfDelivery,
            acceptanceStationId = l.acceptanceStationId,
            deliveryStationId = l.deliveryStationId,
            displayOffsetSeconds = displayOffsetSeconds(),
            routeStartWork = routeStartWork,
            routeEndWork = routeEndWork,
            showHeating = settings?.isShowLocoHeating == true,
            showAuxiliary = settings?.isShowLocoAuxiliary == true,
            showOtherCurrent = showOtherCurrent,
            isKiloMode = isKiloMode,
            settingsSeriesName = l.series?.trim().orEmpty(),
            heatingAccepted = heatingAccepted,
            heatingDelivery = heatingDelivery,
            heatingResult = counterResult(heatingAccepted, heatingDelivery),
            auxiliaryAccepted = auxiliaryAccepted,
            auxiliaryDelivery = auxiliaryDelivery,
            auxiliaryResult = counterResult(auxiliaryAccepted, auxiliaryDelivery),
            sectionCount = if (isElectric) electric.size else diesel.size,
            dieselSections = if (isElectric) emptyList() else diesel.mapIndexed { i, s -> dieselUi(i, s) },
            electricSections = if (isElectric) electric.mapIndexed { i, s -> electricUi(i, s) } else emptyList(),
            electricTotals = if (isElectric) electricTotals(built) else emptyList(),
            dieselTotal = if (isElectric) null else dieselTotal(built),
            norma1 = norma1,
            norma2 = norma2,
            normaDiesel = l.normaDiesel ?: "",
            saveError = saveError,
        )
    }
}
