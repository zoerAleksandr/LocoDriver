package com.z_company.iosapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.setting.SalarySetting
import com.z_company.domain.entities.setting.ServicePhase
import com.z_company.domain.entities.setting.SurchargeExtendedServicePhase
import com.z_company.domain.entities.setting.SurchargeHeavyTrains
import com.z_company.domain.entities.setting.SurchargeLongTrains
import com.z_company.domain.repositories.SharedPreferencesRepositories
import com.z_company.domain.use_cases.CalendarUseCase
import com.z_company.domain.use_cases.SalarySettingUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.domain.util.str
import com.z_company.domain.util.toNonNegativeFiniteDoubleOrNull
import com.z_company.repository.remote_rest.SyncManager
import com.z_company.repository.SecureTokenStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ── UI-модели (§12.1) ─────────────────────────────────────────────────────

/** Поле ввода: строка + флаг ошибки «Некорректные данные» (§12.2). */
data class SettingFieldUi(val value: String = "", val isError: Boolean = false)

/** Строка пороговой доплаты: [порог][%] (§12.1 п.4). */
data class SurchargeRowUi(val id: String = "", val threshold: String = "", val percent: String = "")

/** Плечо с собственными доплатами («По плечам», §12.1). */
data class PhaseSurchargesUi(
    val id: String = "",
    val name: String = "",
    val heavyRows: List<SurchargeRowUi> = emptyList(),
    val longRows: List<SurchargeRowUi> = emptyList(),
)

data class SettingSalaryState(
    val monthLabel: String = "",
    val tariffRate: SettingFieldUi = SettingFieldUi(),
    val oldTariffRate: SettingFieldUi = SettingFieldUi(),
    /** «на {день} {Месяц} {год}» — день = dateNewRate либо 1. */
    val dateLabel: String = "",
    /** «до …» у старой ставки; пусто, если смены тарифа нет. */
    val oldDateLabel: String = "",
    val hasDateSetTariffRate: Boolean = false,
    val averagePaymentHour: SettingFieldUi = SettingFieldUi(),
    val districtCoefficient: SettingFieldUi = SettingFieldUi(),
    val nordicCoefficient: SettingFieldUi = SettingFieldUi(),
    val zonalSurcharge: SettingFieldUi = SettingFieldUi(),
    val surchargeQualificationClass: SettingFieldUi = SettingFieldUi(),
    val onePersonOperationPercent: SettingFieldUi = SettingFieldUi(),
    val onePersonOperationPassengerTrainPercent: SettingFieldUi = SettingFieldUi(),
    val harmfulnessPercent: SettingFieldUi = SettingFieldUi(),
    val surchargeHeavyLongDistanceTrains: SettingFieldUi = SettingFieldUi(),
    val otherSurcharge: SettingFieldUi = SettingFieldUi(),
    val ndfl: SettingFieldUi = SettingFieldUi(),
    val unionistsRetention: SettingFieldUi = SettingFieldUi(),
    val otherRetention: SettingFieldUi = SettingFieldUi(),
    val welfarePercent: SettingFieldUi = SettingFieldUi(),
    val alimonyPercent: SettingFieldUi = SettingFieldUi(),
    val heavyRows: List<SurchargeRowUi> = emptyList(),
    val longRows: List<SurchargeRowUi> = emptyList(),
    val extendedRows: List<SurchargeRowUi> = emptyList(),
    val phases: List<PhaseSurchargesUi> = emptyList(),
    val showUnderworkPayments: Boolean = true,
    val isTariffDialogShown: Boolean = false,
    val saveErrorText: String? = null,
)

// ── Валидация списков порогов (§12.2, атомарное сохранение) ───────────────

internal fun validatedHeavyTrainSettings(values: List<SurchargeHeavyTrains>): List<SurchargeHeavyTrains>? =
    values.takeIf { list ->
        list.all { item ->
            item.weight.toNonNegativeFiniteDoubleOrNull()?.let { it > 0.0 } == true &&
                item.percentSurcharge.toNonNegativeFiniteDoubleOrNull() != null
        }
    }?.map { it.copy() }

internal fun validatedLongTrainSettings(values: List<SurchargeLongTrains>): List<SurchargeLongTrains>? =
    values.takeIf { list ->
        list.all { item ->
            item.conditionalLength.toNonNegativeFiniteDoubleOrNull()?.let { it > 0.0 } == true &&
                item.percentSurcharge.toNonNegativeFiniteDoubleOrNull() != null
        }
    }?.map { it.copy() }

internal fun validatedExtendedServiceSettings(values: List<SurchargeExtendedServicePhase>): List<SurchargeExtendedServicePhase>? =
    values.takeIf { list ->
        list.all { item ->
            item.distance.toNonNegativeFiniteDoubleOrNull()?.let { it > 0.0 } == true &&
                item.percentSurcharge.toNonNegativeFiniteDoubleOrNull() != null
        }
    }?.map { it.copy() }

private val MONTH_NAMES = listOf(
    "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
    "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь",
)

/**
 * ViewModel экрана «Настройки зарплаты» (§12).
 *
 * Тарифная ставка живёт в `MonthOfYear` (`selectMonthOfYear`), остальные поля —
 * в `SalarySetting`. Числовые поля сохраняются автоматически с debounce 500 мс;
 * изменение ставки сразу взводит `settingsSyncPending` (§12 «Синхронизация»).
 */
class SettingSalaryIosViewModel(
    private val salarySettingUseCase: SalarySettingUseCase,
    private val settingsUseCase: SettingsUseCase,
    private val calendarUseCase: CalendarUseCase,
    private val sharedPrefs: SharedPreferencesRepositories,
    private val syncManager: SyncManager,
    private val secureTokenStorage: SecureTokenStorage,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingSalaryState())
    val state: StateFlow<SettingSalaryState> = _state.asStateFlow()

    private var currentSalarySetting: SalarySetting = SalarySetting()
    private var currentMonthOfYear: MonthOfYear? = null
    private var initialValueTariffRate: Double? = null
    private var initialDateSetTariffRate: com.z_company.domain.entities.DateSetTariffRate? = null

    private var autoSaveJob: Job? = null
    private var phaseSaveJob: Job? = null
    private var phaseSavePending = false

    // Черновые списки порогов: в БД попадает только последняя полностью
    // корректная версия всего списка (§12 «Списки порогов»).
    private var heavyDraft: List<SurchargeHeavyTrains> = emptyList()
    private var longDraft: List<SurchargeLongTrains> = emptyList()
    private var extendedDraft: List<SurchargeExtendedServicePhase> = emptyList()
    private var phaseDrafts: Map<String, Pair<List<SurchargeHeavyTrains>, List<SurchargeLongTrains>>> = emptyMap()
    // ВАЖНО: объявлять ДО init-блока — иначе корутина init (запускается синхронно)
    // обращается к неинициализированному полю (null) и падает на итерации (SEGV).
    private var servicePhases: List<ServicePhase> = emptyList()

    init {
        viewModelScope.launch {
            val result = salarySettingUseCase.getFlowSalarySetting().first()
            currentSalarySetting = result.let { (it as? com.z_company.core.ResultState.Success)?.data } ?: SalarySetting()
            heavyDraft = currentSalarySetting.surchargeHeavyTrainsList
            longDraft = currentSalarySetting.surchargeLongTrainsList
            extendedDraft = currentSalarySetting.surchargeExtendedServicePhaseList
            pushState()
            loadUserSetting()
        }
    }

    private suspend fun loadUserSetting() {
        val userSettings = settingsUseCase.getUserSetting()
        val selectedMonth = userSettings.selectMonthOfYear
        val allMonths = calendarUseCase.loadFlowMonthOfYearListState().first()
        val actualMonth = allMonths.find { it.year == selectedMonth.year && it.month == selectedMonth.month }
            ?: selectedMonth
        currentMonthOfYear = actualMonth
        initialValueTariffRate = actualMonth.tariffRate
        initialDateSetTariffRate = actualMonth.dateSetTariffRate
        servicePhases = userSettings.servicePhases
        phaseDrafts = userSettings.servicePhases.associate {
            it.id to (it.surchargeHeavyTrainsList to it.surchargeLongTrainsList)
        }
        pushState()
    }

    // ── Снимок состояния ─────────────────────────────────────────────────────

    private fun pushState() {
        val setting = currentSalarySetting
        val month = currentMonthOfYear
        val dateDay = month?.dateSetTariffRate?.dateNewRate ?: 1
        val monthName = MONTH_NAMES.getOrElse(month?.month ?: 0) { "?" }
        _state.value = _state.value.copy(
            monthLabel = "$monthName ${month?.year ?: ""}",
            tariffRate = field(month?.tariffRate.str()),
            oldTariffRate = field(month?.dateSetTariffRate?.oldRate.str()),
            dateLabel = "на $dateDay $monthName ${month?.year ?: ""}",
            oldDateLabel = month?.dateSetTariffRate?.let { "до ${it.dateNewRate} $monthName ${month.year}" } ?: "",
            hasDateSetTariffRate = month?.dateSetTariffRate != null,
            averagePaymentHour = field(setting.averagePaymentHour.str()),
            districtCoefficient = field(setting.districtCoefficient.str()),
            nordicCoefficient = field(setting.nordicPercent.str()),
            zonalSurcharge = field(setting.zonalSurcharge.str()),
            surchargeQualificationClass = field(setting.surchargeQualificationClass.str()),
            onePersonOperationPercent = field(setting.onePersonOperationPercent.str()),
            onePersonOperationPassengerTrainPercent = field(setting.onePersonOperationPassengerTrainPercent.str()),
            harmfulnessPercent = field(setting.harmfulnessPercent.str()),
            surchargeHeavyLongDistanceTrains = field(setting.surchargeHeavyLongDistanceTrains.str()),
            otherSurcharge = field(setting.otherSurcharge.str()),
            ndfl = field(setting.ndfl.str()),
            unionistsRetention = field(setting.unionistsRetention.str()),
            otherRetention = field(setting.otherRetention.str()),
            welfarePercent = field(setting.welfarePercent.str()),
            alimonyPercent = field(setting.alimonyPercent.str()),
            heavyRows = heavyDraft.map { SurchargeRowUi(it.id, it.weight, it.percentSurcharge) },
            longRows = longDraft.map { SurchargeRowUi(it.id, it.conditionalLength, it.percentSurcharge) },
            extendedRows = extendedDraft.map { SurchargeRowUi(it.id, it.distance, it.percentSurcharge) },
            showUnderworkPayments = setting.showUnderworkPayments,
        )
        rebuildPhaseState()
    }

    private fun rebuildPhaseState() {
        _state.value = _state.value.copy(
            phases = servicePhases.map { phase ->
                val (heavy, long) = phaseDrafts[phase.id]
                    ?: (emptyList<SurchargeHeavyTrains>() to emptyList<SurchargeLongTrains>())
                PhaseSurchargesUi(
                    id = phase.id,
                    name = phaseName(phase),
                    heavyRows = heavy.map { SurchargeRowUi(it.id, it.weight, it.percentSurcharge) },
                    longRows = long.map { SurchargeRowUi(it.id, it.conditionalLength, it.percentSurcharge) },
                )
            }
        )
    }

    private fun phaseName(phase: ServicePhase): String {
        val from = phase.departureStation.ifBlank { "?" }
        val to = phase.arrivalStation.ifBlank { "?" }
        return "$from — $to"
    }

    private fun field(value: String) = SettingFieldUi(value, value.toNonNegativeFiniteDoubleOrNull() == null)

    // ── Поля настроек (§12.2) ────────────────────────────────────────────────

    private fun editField(value: String): SettingFieldUi =
        SettingFieldUi(value, value.toNonNegativeFiniteDoubleOrNull() == null)

    private fun applyNumeric(value: String, update: (Double) -> Unit) {
        val parsed = value.toNonNegativeFiniteDoubleOrNull()
        if (parsed != null) {
            update(parsed)
            scheduleAutoSave()
        }
    }

    fun setAveragePaymentHour(value: String) {
        applyNumeric(value) { currentSalarySetting = currentSalarySetting.copy(averagePaymentHour = it) }
        _state.value = _state.value.copy(averagePaymentHour = editField(value))
    }

    fun setDistrictCoefficient(value: String) {
        applyNumeric(value) { currentSalarySetting = currentSalarySetting.copy(districtCoefficient = it) }
        _state.value = _state.value.copy(districtCoefficient = editField(value))
    }

    fun setNordicCoefficient(value: String) {
        applyNumeric(value) { currentSalarySetting = currentSalarySetting.copy(nordicPercent = it) }
        _state.value = _state.value.copy(nordicCoefficient = editField(value))
    }

    fun setZonalSurcharge(value: String) {
        applyNumeric(value) { currentSalarySetting = currentSalarySetting.copy(zonalSurcharge = it) }
        _state.value = _state.value.copy(zonalSurcharge = editField(value))
    }

    fun setSurchargeQualificationClass(value: String) {
        applyNumeric(value) { currentSalarySetting = currentSalarySetting.copy(surchargeQualificationClass = it) }
        _state.value = _state.value.copy(surchargeQualificationClass = editField(value))
    }

    fun setOnePersonOperationPercent(value: String) {
        applyNumeric(value) { currentSalarySetting = currentSalarySetting.copy(onePersonOperationPercent = it) }
        _state.value = _state.value.copy(onePersonOperationPercent = editField(value))
    }

    fun setOnePersonOperationPassengerTrainPercent(value: String) {
        applyNumeric(value) {
            currentSalarySetting = currentSalarySetting.copy(onePersonOperationPassengerTrainPercent = it)
        }
        _state.value = _state.value.copy(onePersonOperationPassengerTrainPercent = editField(value))
    }

    fun setHarmfulnessPercent(value: String) {
        applyNumeric(value) { currentSalarySetting = currentSalarySetting.copy(harmfulnessPercent = it) }
        _state.value = _state.value.copy(harmfulnessPercent = editField(value))
    }

    fun setSurchargeHeavyLongDistanceTrains(value: String) {
        applyNumeric(value) { currentSalarySetting = currentSalarySetting.copy(surchargeHeavyLongDistanceTrains = it) }
        _state.value = _state.value.copy(surchargeHeavyLongDistanceTrains = editField(value))
    }

    fun setOtherSurcharge(value: String) {
        applyNumeric(value) { currentSalarySetting = currentSalarySetting.copy(otherSurcharge = it) }
        _state.value = _state.value.copy(otherSurcharge = editField(value))
    }

    fun setNDFL(value: String) {
        applyNumeric(value) { currentSalarySetting = currentSalarySetting.copy(ndfl = it) }
        _state.value = _state.value.copy(ndfl = editField(value))
    }

    fun setUnionistsRetention(value: String) {
        applyNumeric(value) { currentSalarySetting = currentSalarySetting.copy(unionistsRetention = it) }
        _state.value = _state.value.copy(unionistsRetention = editField(value))
    }

    fun setOtherRetention(value: String) {
        applyNumeric(value) { currentSalarySetting = currentSalarySetting.copy(otherRetention = it) }
        _state.value = _state.value.copy(otherRetention = editField(value))
    }

    fun setWelfarePercent(value: String) {
        applyNumeric(value) { currentSalarySetting = currentSalarySetting.copy(welfarePercent = it) }
        _state.value = _state.value.copy(welfarePercent = editField(value))
    }

    fun setAlimonyPercent(value: String) {
        applyNumeric(value) { currentSalarySetting = currentSalarySetting.copy(alimonyPercent = it) }
        _state.value = _state.value.copy(alimonyPercent = editField(value))
    }

    fun setShowUnderworkPayments(value: Boolean) {
        currentSalarySetting = currentSalarySetting.copy(showUnderworkPayments = value)
        _state.value = _state.value.copy(showUnderworkPayments = value)
        scheduleAutoSave()
    }

    // ── Тарифная ставка (§12 «Тарифная ставка») ─────────────────────────────

    fun setTariffRate(value: String) {
        val parsed = value.toNonNegativeFiniteDoubleOrNull()
        if (parsed != null) {
            currentMonthOfYear = currentMonthOfYear?.copy(tariffRate = parsed)
            // Помечаем настройки несинхронизированными сразу (§12 «Синхронизация»).
            sharedPrefs.setSettingsSyncPending(true)
        }
        _state.value = _state.value.copy(tariffRate = editField(value))
    }

    fun setOldTariffRate(value: String) {
        val parsed = value.toNonNegativeFiniteDoubleOrNull()
        if (parsed != null) {
            currentMonthOfYear = currentMonthOfYear?.copy(
                dateSetTariffRate = currentMonthOfYear?.dateSetTariffRate?.copy(oldRate = parsed)
            )
            sharedPrefs.setSettingsSyncPending(true)
        }
        // BUG-42 (паритет с Android): поле старой ставки подсвечивает ошибку по
        // флагу новой ставки (`isErrorInputTariffRate`), собственный флаг на
        // экран не передаётся.
        _state.value = _state.value.copy(
            oldTariffRate = editField(value),
            tariffRate = _state.value.tariffRate.copy(isError = value.toNonNegativeFiniteDoubleOrNull() == null),
        )
    }

    /** Выбор даты смены тарифа (§12.3): день 1 — «нового тарифа нет». */
    fun setDateSetTariffRate(year: Int, month: Int, day: Int) {
        sharedPrefs.setSettingsSyncPending(true)
        viewModelScope.launch {
            if (currentMonthOfYear?.month != month || currentMonthOfYear?.year != year) {
                val monthList = calendarUseCase.loadFlowMonthOfYearListState().first()
                val newMonthOfYear = monthList.find { it.month == month && it.year == year }
                currentMonthOfYear = newMonthOfYear ?: MonthOfYear(month = month, year = year)
                initialValueTariffRate = currentMonthOfYear?.tariffRate
                initialDateSetTariffRate = currentMonthOfYear?.dateSetTariffRate
            }
            if (day == 1) {
                currentMonthOfYear = currentMonthOfYear?.copy(dateSetTariffRate = null)
            } else {
                val existing = currentMonthOfYear?.dateSetTariffRate
                currentMonthOfYear = if (existing == null) {
                    currentMonthOfYear?.copy(
                        dateSetTariffRate = com.z_company.domain.entities.DateSetTariffRate(
                            dateNewRate = day,
                            oldRate = initialValueTariffRate ?: 0.0,
                        )
                    )
                } else {
                    currentMonthOfYear?.copy(dateSetTariffRate = existing.copy(dateNewRate = day))
                }
            }
            pushState()
            // BUG-42 (паритет с Android): после выбора даты старая ставка
            // показывается через toString() («1234.0»), а не str().
            val oldRateText = currentMonthOfYear?.dateSetTariffRate?.oldRate.toString()
            if (currentMonthOfYear?.dateSetTariffRate != null) {
                _state.value = _state.value.copy(oldTariffRate = _state.value.oldTariffRate.copy(value = oldRateText))
            }
        }
    }

    fun showDialogTariffRate() {
        _state.value = _state.value.copy(isTariffDialogShown = true)
    }

    fun hideDialogTariffRate() {
        _state.value = _state.value.copy(isTariffDialogShown = false)
    }

    fun isTariffRateChanged(): Boolean {
        val month = currentMonthOfYear ?: return false
        val initialRate = initialValueTariffRate ?: return false
        return month.tariffRate != initialRate
    }

    /**
     * Выход с экрана: если ставка изменилась — вместо закрытия показать шторку
     * «Изменилась тарифная ставка» (§12.3).
     */
    fun onBackPressed(onExit: () -> Unit) {
        if (isTariffRateChanged()) {
            showDialogTariffRate()
        } else {
            flushPendingSaves()
            onExit()
        }
    }

    /** «Только для этого месяца» (§12.3). Прошлые месяцы не изменяются. */
    fun saveSettingAndOnlyMonthTariffRate(onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            currentMonthOfYear?.let { month ->
                salarySettingUseCase.updateTariffRateOnlyInOneMonthOfYear(month)
                    .first { it is com.z_company.core.ResultState.Success || it is com.z_company.core.ResultState.Error }
            }
            finishTariffRateSave(onSaved)
        }
    }

    /** «Для этого и следующих» (§12.3). */
    fun saveSettingAndTariffRateCurrentAndNextMonth(onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            currentMonthOfYear?.let { month ->
                salarySettingUseCase.updateTariffRateCurrentAndNextMonths(month)
                    .first { it is com.z_company.core.ResultState.Success || it is com.z_company.core.ResultState.Error }
            }
            finishTariffRateSave(onSaved)
        }
    }

    private suspend fun finishTariffRateSave(onSaved: () -> Unit) {
        initialValueTariffRate = currentMonthOfYear?.tariffRate
        initialDateSetTariffRate = currentMonthOfYear?.dateSetTariffRate
        _state.value = _state.value.copy(isTariffDialogShown = false)
        persistSetting(updateMonthOfYear = true)
        onSaved()
    }

    // ── Пороговые доплаты (§12.1 п.4) ───────────────────────────────────────

    fun addSurchargeHeavyTrain() {
        heavyDraft = heavyDraft + SurchargeHeavyTrains()
        pushState()
        scheduleAutoSave()
    }

    fun setSurchargeHeavyTrainWeight(id: String, value: String) {
        heavyDraft = heavyDraft.map { if (it.id == id) it.copy(weight = value) else it }
        pushState()
        scheduleAutoSave()
    }

    fun setSurchargeHeavyTrainPercent(id: String, value: String) {
        heavyDraft = heavyDraft.map { if (it.id == id) it.copy(percentSurcharge = value) else it }
        pushState()
        scheduleAutoSave()
    }

    fun deleteSurchargeHeavyTrain(id: String) {
        heavyDraft = heavyDraft.filterNot { it.id == id }
        pushState()
        scheduleAutoSave()
    }

    fun addSurchargeLongTrain() {
        longDraft = longDraft + SurchargeLongTrains()
        pushState()
        scheduleAutoSave()
    }

    fun setSurchargeLongTrainLength(id: String, value: String) {
        longDraft = longDraft.map { if (it.id == id) it.copy(conditionalLength = value) else it }
        pushState()
        scheduleAutoSave()
    }

    fun setSurchargeLongTrainPercent(id: String, value: String) {
        longDraft = longDraft.map { if (it.id == id) it.copy(percentSurcharge = value) else it }
        pushState()
        scheduleAutoSave()
    }

    fun deleteSurchargeLongTrain(id: String) {
        longDraft = longDraft.filterNot { it.id == id }
        pushState()
        scheduleAutoSave()
    }

    fun addSurchargeExtendedServicePhase() {
        extendedDraft = extendedDraft + SurchargeExtendedServicePhase()
        pushState()
        scheduleAutoSave()
    }

    fun setSurchargeExtendedServicePhaseDistance(id: String, value: String) {
        extendedDraft = extendedDraft.map { if (it.id == id) it.copy(distance = value) else it }
        pushState()
        scheduleAutoSave()
    }

    fun setSurchargeExtendedServicePhasePercent(id: String, value: String) {
        extendedDraft = extendedDraft.map { if (it.id == id) it.copy(percentSurcharge = value) else it }
        pushState()
        scheduleAutoSave()
    }

    fun deleteSurchargeExtendedServicePhase(id: String) {
        extendedDraft = extendedDraft.filterNot { it.id == id }
        pushState()
        scheduleAutoSave()
    }

    // ── Доплаты по плечам (§12.1 «По плечам») ────────────────────────────────

    fun addPhaseHeavyRow(phaseId: String) {
        updatePhaseDrafts(phaseId) { heavy, long -> heavy + SurchargeHeavyTrains() to long }
    }

    fun setPhaseHeavyRowThreshold(phaseId: String, rowId: String, value: String) {
        updatePhaseDrafts(phaseId) { heavy, long ->
            heavy.map { if (it.id == rowId) it.copy(weight = value) else it } to long
        }
    }

    fun setPhaseHeavyRowPercent(phaseId: String, rowId: String, value: String) {
        updatePhaseDrafts(phaseId) { heavy, long ->
            heavy.map { if (it.id == rowId) it.copy(percentSurcharge = value) else it } to long
        }
    }

    fun deletePhaseHeavyRow(phaseId: String, rowId: String) {
        updatePhaseDrafts(phaseId) { heavy, long -> heavy.filterNot { it.id == rowId } to long }
    }

    fun addPhaseLongRow(phaseId: String) {
        updatePhaseDrafts(phaseId) { heavy, long -> heavy to long + SurchargeLongTrains() }
    }

    fun setPhaseLongRowThreshold(phaseId: String, rowId: String, value: String) {
        updatePhaseDrafts(phaseId) { heavy, long ->
            heavy to long.map { if (it.id == rowId) it.copy(conditionalLength = value) else it }
        }
    }

    fun setPhaseLongRowPercent(phaseId: String, rowId: String, value: String) {
        updatePhaseDrafts(phaseId) { heavy, long ->
            heavy to long.map { if (it.id == rowId) it.copy(percentSurcharge = value) else it }
        }
    }

    fun deletePhaseLongRow(phaseId: String, rowId: String) {
        updatePhaseDrafts(phaseId) { heavy, long -> heavy to long.filterNot { it.id == rowId } }
    }

    private fun updatePhaseDrafts(
        phaseId: String,
        transform: (List<SurchargeHeavyTrains>, List<SurchargeLongTrains>) -> Pair<List<SurchargeHeavyTrains>, List<SurchargeLongTrains>>,
    ) {
        val (heavy, long) = phaseDrafts[phaseId] ?: (emptyList<SurchargeHeavyTrains>() to emptyList<SurchargeLongTrains>())
        phaseDrafts = phaseDrafts + (phaseId to transform(heavy, long))
        phaseSavePending = true
        rebuildPhaseState()
        phaseSaveJob?.cancel()
        phaseSaveJob = viewModelScope.launch {
            delay(500)
            persistPhaseSurcharges()
        }
    }

    /** Доплаты плеча уходят в `UserSettings.servicePhases` (§12.1, ~0,5 с debounce). */
    private suspend fun persistPhaseSurcharges() {
        if (!phaseSavePending) return
        phaseSavePending = false
        val drafts = phaseDrafts
        val settings = settingsUseCase.getUserSetting()
        var changed = false
        val phases = settings.servicePhases.map { phase ->
            val draft = drafts[phase.id] ?: return@map phase
            val heavy = validatedHeavyTrainSettings(draft.first) ?: return@map phase
            val long = validatedLongTrainSettings(draft.second) ?: return@map phase
            if (heavy == phase.surchargeHeavyTrainsList && long == phase.surchargeLongTrainsList) return@map phase
            changed = true
            phase.copy(surchargeHeavyTrainsList = heavy, surchargeLongTrainsList = long)
        }
        if (!changed) return
        settingsUseCase.saveSetting(
            settings.copy(
                servicePhases = phases,
                updateAt = settingsUseCase.nextUpdateAt(settings.updateAt),
            )
        ).collect {}
        autoPushSettings()
    }

    // ── Сохранение ───────────────────────────────────────────────────────────

    private fun scheduleAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(500)
            persistSetting(updateMonthOfYear = false)
        }
    }

    private fun applyValidEditableLists(target: SalarySetting) {
        validatedExtendedServiceSettings(extendedDraft)?.let { target.surchargeExtendedServicePhaseList = it }
        validatedHeavyTrainSettings(heavyDraft)?.let { target.surchargeHeavyTrainsList = it }
        validatedLongTrainSettings(longDraft)?.let { target.surchargeLongTrainsList = it }
    }

    /**
     * Порядок записи обязателен (§12): сначала локальная БД, затем выгрузка
     * на сервер (`settingsSyncPending` + `autoPushSettings`).
     */
    private suspend fun persistSetting(updateMonthOfYear: Boolean = false) {
        withContext(Dispatchers.Default) {
            var isChangeSaved = false
            val salarySetting = currentSalarySetting
            applyValidEditableLists(salarySetting)
            salarySettingUseCase.saveLocalEdit(salarySetting).collect { result ->
                if (result is com.z_company.core.ResultState.Success) isChangeSaved = true
            }
            if (updateMonthOfYear) {
                currentMonthOfYear?.let { month ->
                    salarySettingUseCase.updateMonthOfYear(month).collect {}
                    val userSettings = settingsUseCase.getUserSetting()
                    settingsUseCase.saveSetting(userSettings.copy(selectMonthOfYear = month)).collect {}
                    isChangeSaved = true
                }
            }
            if (isChangeSaved) autoPushSettings()
        }
    }

    /** Дублирование ожидающего автосохранения при выходе (§12, NonCancellable). */
    private fun flushPendingSaves() {
        autoSaveJob?.cancel()
        if (phaseSavePending) {
            CoroutineScope(NonCancellable + Dispatchers.Default).launch { persistPhaseSurcharges() }
        }
        val month = currentMonthOfYear
        val isMonthChanged = isTariffRateChanged() ||
            month?.dateSetTariffRate != initialDateSetTariffRate
        CoroutineScope(NonCancellable + Dispatchers.Default).launch {
            persistSetting(updateMonthOfYear = false)
            if (month != null) {
                calendarUseCase.updateMonthOfYear(month).collect {}
                settingsUseCase.updateMonthOfYearInUserSetting(month).collect {}
                if (isMonthChanged) autoPushSettings()
            }
        }
    }

    /** Вызывается из Swift при закрытии экрана (вместо onCleared). */
    fun onScreenExit() {
        flushPendingSaves()
    }

    private fun autoPushSettings() {
        sharedPrefs.setSettingsSyncPending(true)
        CoroutineScope(NonCancellable + Dispatchers.Default).launch {
            val token = secureTokenStorage.getAuthBearerTokenFlow().first() ?: return@launch
            syncManager.autoPushSettings("Bearer $token").collect {}
        }
    }

    // ── Watch callbacks (Swift) ──────────────────────────────────────────────

    fun watchState(callback: (SettingSalaryState) -> Unit) {
        viewModelScope.launch { state.collect { callback(it) } }
    }
}
