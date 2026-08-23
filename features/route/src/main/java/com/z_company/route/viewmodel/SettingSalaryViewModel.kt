package com.z_company.route.viewmodel

import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.core.ResultState
import com.z_company.domain.entities.DateSetTariffRate
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.setting.SalarySetting
import com.z_company.domain.entities.setting.SurchargeExtendedServicePhase
import com.z_company.domain.entities.setting.SurchargeHeavyTrains
import com.z_company.domain.entities.setting.SurchargeLongTrains
import com.z_company.domain.use_cases.CalendarUseCase
import com.z_company.domain.use_cases.SalarySettingUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.domain.repositories.SharedPreferencesRepositories
import com.z_company.repository.SecureTokenStorage
import com.z_company.repository.remote_rest.SyncManager
import com.z_company.domain.util.addOrReplace
import com.z_company.domain.util.str
import com.z_company.domain.util.toDoubleOrZero
import com.z_company.domain.util.toIntOrZero
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.z_company.core.sendToSentry
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.Calendar

class SettingSalaryViewModel : ViewModel(), KoinComponent {
    private val salarySettingUseCase: SalarySettingUseCase by inject()
    private val calendarUseCase: CalendarUseCase by inject()
    private val userSettingUseCase: SettingsUseCase by inject()
    private val sharedPrefs: SharedPreferencesRepositories by inject()
    private val syncManager: SyncManager by inject()
    private val secureTokenStorage: SecureTokenStorage by inject()

    private var initialValueTariffRate: Double? = null
    private var currentMonthOfYear: MonthOfYear? = null
    private var autoSaveJob: Job? = null

    private val _uiState = MutableStateFlow(SettingSalaryUIState())
    val uiState = _uiState.asStateFlow()

    private var isInitialLoad = true

    private var currentSalarySetting: SalarySetting?
        get() {
            return _uiState.value.settingSalaryState.let {
                if (it is ResultState.Success) it.data else null
            }
        }
        private set(value) {
            _uiState.update {
                it.copy(
                    settingSalaryState = ResultState.Success(value),
                    averagePaymentHour = ResultState.Success(value?.averagePaymentHour.str()),
                    districtCoefficient = ResultState.Success(value?.districtCoefficient.str()),
                    nordicCoefficient = ResultState.Success(value?.nordicPercent.str()),
                    onePersonOperationPercent = ResultState.Success(value?.onePersonOperationPercent.str()),
                    onePersonOperationPassengerTrainPercent = ResultState.Success(value?.onePersonOperationPassengerTrainPercent.str()),
                    harmfulnessPercent = ResultState.Success(value?.harmfulnessPercent.str()),
                    zonalSurcharge = ResultState.Success(value?.zonalSurcharge.str()),
                    surchargeQualificationClass = ResultState.Success(value?.surchargeQualificationClass.str()),
                    surchargeHeavyLongDistanceTrains = ResultState.Success(value?.surchargeHeavyLongDistanceTrains.str()),
                    otherSurchargeState = ResultState.Success(value?.otherSurcharge.str()),
                    ndfl = ResultState.Success(value?.ndfl.str()),
                    unionistsRetentionState = ResultState.Success(value?.unionistsRetention.str()),
                    otherRetention = ResultState.Success(value?.otherRetention.str()),
                    welfarePercentState = ResultState.Success(value?.welfarePercent.str()),
                    alimonyPercentState = ResultState.Success(value?.alimonyPercent.str()),
                    showUnderworkPayments = value?.showUnderworkPayments ?: true
                )
            }
        }

    private var surchargeExtendedServicePhaseListState: SnapshotStateList<SurchargeExtendedServicePhase>
        get() {
            return _uiState.value.surchargeExtendedServicePhaseList
        }
        private set(value) {
            _uiState.update {
                it.copy(
                    surchargeExtendedServicePhaseList = value
                )
            }
        }

    private var surchargeHeavyTrainsState: SnapshotStateList<SurchargeHeavyTrains>
        get() {
            return _uiState.value.surchargeHeavyTrain
        }
        private set(value) {
            _uiState.update {
                it.copy(
                    surchargeHeavyTrain = value
                )
            }
        }

    private var surchargeLongTrainsState: SnapshotStateList<SurchargeLongTrains>
        get() {
            return _uiState.value.surchargeLongTrain
        }
        private set(value) {
            _uiState.update {
                it.copy(
                    surchargeLongTrain = value
                )
            }
        }

    init {
        loadSalarySetting()
        loadUserSetting()
    }

    private fun loadUserSetting() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val userSettings = this.async { userSettingUseCase.getUserSetting() }.await()
                val selectedMonth = userSettings.selectMonthOfYear

                // Ищем MonthOfYear в таблице по году+месяцу — это надёжнее чем поиск по ID,
                // т.к. ID в UserSettings.selectMonthOfYear может не совпадать с ID в таблице
                // (например, после syncFromRemote на новом устройстве).
                val allMonths = calendarUseCase.loadFlowMonthOfYearListState().first()
                val monthFromTable = allMonths.find {
                    it.year == selectedMonth.year && it.month == selectedMonth.month
                }
                val actualMonth = monthFromTable ?: selectedMonth
                val tariffRate = actualMonth.tariffRate

                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            oldTariffRate = ResultState.Success(actualMonth.dateSetTariffRate?.oldRate.str()),
                            tariffRate = ResultState.Success(tariffRate.str()),
                            currentMonthOfYear = actualMonth
                        )
                    }
                }
                initialValueTariffRate = tariffRate
                currentMonthOfYear = actualMonth
            } catch (e: Exception) {
                e.sendToSentry("SettingSalaryViewModel", "loadUserSetting")
            }
        }
    }

    fun resetSaveState() {
        _uiState.update {
            it.copy(saveSettingState = null)
        }
    }

    fun hideDialogTariffRate() {
        _uiState.update {
            it.copy(
                isShowDialogChangeTariffRate = false
            )
        }
    }

    fun checkForChangesTariffRate() {
        initialValueTariffRate?.let { initValue ->
            currentMonthOfYear?.let { month ->
                if (initValue != month.tariffRate) {
                    _uiState.update {
                        it.copy(
                            isShowDialogChangeTariffRate = true
                        )
                    }
                }
            }
        }
    }

    fun saveSettingAndOnlyMonthTariffRate() {
        viewModelScope.launch {
            changeTariffRateInOnlyInOneMonthOfYear()
            saveSetting(updateMonthOfYear = true)
        }
    }

    fun saveSettingAndTariffRateCurrentAndNextMonth() {
        viewModelScope.launch {
            changeTariffRateCurrentAndNextMonths()
            saveSetting(updateMonthOfYear = true)
        }
    }

    private suspend fun changeTariffRateInOnlyInOneMonthOfYear() {
        viewModelScope.launch {
            currentMonthOfYear?.let { monthOfYear ->
                salarySettingUseCase.updateTariffRateOnlyInOneMonthOfYear(
                    newTariffRate = monthOfYear.tariffRate,
                    monthId = monthOfYear.id
                ).collect { result ->
                    if (result is ResultState.Success) {
                        this.cancel()
                    }
                }
            }
        }.join()
    }

    private suspend fun changeTariffRateCurrentAndNextMonths() {
        viewModelScope.launch {
            currentMonthOfYear?.let { monthOfYear ->
                salarySettingUseCase.updateTariffRateCurrentAndNextMonths(
                    newTariffRate = monthOfYear.tariffRate,
                    currentMonthId = monthOfYear.id
                ).collect { result ->
                    if (result is ResultState.Success) {
                        this.cancel()
                    }
                }
            }
        }.join()
    }

    private fun saveSetting(updateMonthOfYear: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            val state = uiState.value.settingSalaryState
            if (state is ResultState.Success) {
                state.data?.let { salarySetting ->
                    salarySetting.surchargeExtendedServicePhaseList =
                        surchargeExtendedServicePhaseListState.map { servicePhase ->
                            SurchargeExtendedServicePhase(
                                id = servicePhase.id,
                                distance = servicePhase.distance,
                                percentSurcharge = servicePhase.percentSurcharge
                            )
                        }.toMutableList()
                    salarySetting.surchargeHeavyTrainsList =
                        surchargeHeavyTrainsState.map { surcharge ->
                            SurchargeHeavyTrains(
                                id = surcharge.id,
                                weight = surcharge.weight,
                                percentSurcharge = surcharge.percentSurcharge
                            )
                        }.toMutableList()
                    salarySetting.surchargeLongTrainsList =
                        surchargeLongTrainsState.map { surcharge ->
                            SurchargeLongTrains(
                                id = surcharge.id,
                                conditionalLength = surcharge.conditionalLength,
                                percentSurcharge = surcharge.percentSurcharge
                            )
                        }.toMutableList()

                    salarySettingUseCase.saveSalarySetting(salarySetting).collect { saveResult ->
                        if (saveResult is ResultState.Success) autoPushSettings()
                        if (updateMonthOfYear && saveResult is ResultState.Success) {
                            currentMonthOfYear?.let { month ->
                                salarySettingUseCase.updateMonthOfYear(month).collect {}
                                var userSettings =
                                    this.async { userSettingUseCase.getUserSetting() }.await()
                                userSettings = userSettings.copy(
                                    selectMonthOfYear = month
                                )
                                userSettingUseCase.saveSetting(userSettings).collect {}
                            }
                        }
                    }
                }
            }
        }
    }

    private fun autoPushSettings() {
        sharedPrefs.setSettingsSyncPending(true)
        viewModelScope.launch(Dispatchers.IO) {
            val token = secureTokenStorage.getAuthBearerTokenFlow().first() ?: return@launch
            syncManager.autoPushSettings("Bearer $token").collect {}
        }
    }

    private fun scheduleAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(500)
            saveSetting()
        }
    }

    override fun onCleared() {
        autoSaveJob?.cancel()
        // Сохраняем данные в NonCancellable-контексте, чтобы save завершился
        // даже если ViewModel уже очищается (пользователь покинул экран раньше 500мс)
        CoroutineScope(NonCancellable + Dispatchers.IO).launch {
            // 1. Сохраняем SalarySetting (настройки зарплаты)
            val state = uiState.value.settingSalaryState
            if (state is ResultState.Success) {
                state.data?.let { salarySetting ->
                    salarySetting.surchargeExtendedServicePhaseList =
                        surchargeExtendedServicePhaseListState.map { servicePhase ->
                            SurchargeExtendedServicePhase(
                                id = servicePhase.id,
                                distance = servicePhase.distance,
                                percentSurcharge = servicePhase.percentSurcharge
                            )
                        }.toMutableList()
                    salarySetting.surchargeHeavyTrainsList =
                        surchargeHeavyTrainsState.map { surcharge ->
                            SurchargeHeavyTrains(
                                id = surcharge.id,
                                weight = surcharge.weight,
                                percentSurcharge = surcharge.percentSurcharge
                            )
                        }.toMutableList()
                    salarySetting.surchargeLongTrainsList =
                        surchargeLongTrainsState.map { surcharge ->
                            SurchargeLongTrains(
                                id = surcharge.id,
                                conditionalLength = surcharge.conditionalLength,
                                percentSurcharge = surcharge.percentSurcharge
                            )
                        }.toMutableList()
                    salarySettingUseCase.saveSalarySetting(salarySetting).collect {}
                }
            }
            // 2. Сохраняем MonthOfYear целиком (tariffRate + dateSetTariffRate).
            // Важно: saveSetting(updateMonthOfYear = true) запускает inner-корутину на
            // viewModelScope, которая отменяется при onCleared. Поэтому здесь сохраняем
            // полный MonthOfYear через NonCancellable-scope, чтобы dateSetTariffRate
            // не терялась при быстром уходе с экрана.
            val month = currentMonthOfYear
            if (month != null) {
                calendarUseCase.updateMonthOfYear(month).collect {}
                // КРИТИЧНО: обновляем selectMonthOfYear в UserSettings, чтобы
                // SalaryCalculationViewModel получил актуальный tariffRate/dateSetTariffRate
                // через getUserSettingFlow(). Без этого flow не эмитит новое значение
                // и расчёт ЗП остаётся на старых данных.
                userSettingUseCase.updateMonthOfYearInUserSetting(month).collect {}
            }
        }
        super.onCleared()
    }

    private fun loadSalarySetting() {
        viewModelScope.launch {
            val result = salarySettingUseCase.getFlowSalarySetting().first { it is ResultState.Success }
            if (result is ResultState.Success) {
                currentSalarySetting = result.data ?: SalarySetting()
                currentSalarySetting?.let {
                    setSurchargeExtendedServicePhaseListState(it.surchargeExtendedServicePhaseList)
                    setSurchargeHeavyTrainState(it.surchargeHeavyTrainsList)
                    setSurchargeLongTrainState(it.surchargeLongTrainsList)
                }
            }
        }
    }

    private fun isErrorInputDouble(value: String): Boolean {
        if (value.isBlank()) return false
        return value.toDoubleOrNull() == null
    }

    fun setTariffRate(value: String) {
        currentMonthOfYear = currentMonthOfYear?.copy(
            tariffRate = value.toDoubleOrZero()
        )
        _uiState.update {
            it.copy(
                tariffRate = ResultState.Success(value),
                isErrorInputTariffRate = isErrorInputDouble(value)
            )
        }
    }

    fun setSurchargeHeavyLongDistanceTrains(value: String) {
        currentSalarySetting = currentSalarySetting?.copy(
            surchargeHeavyLongDistanceTrains = value.toDoubleOrZero()
        )
        _uiState.update {
            it.copy(
                surchargeHeavyLongDistanceTrains = ResultState.Success(value),
                isErrorInputSurchargeHeavyLongDistanceTrains = isErrorInputDouble(value)
            )
        }
        scheduleAutoSave()
    }

    fun setOldTariffRate(value: String) {
        currentMonthOfYear = currentMonthOfYear?.copy(
            dateSetTariffRate = currentMonthOfYear!!.dateSetTariffRate?.copy(
                oldRate = value.toDoubleOrZero()
            )
        )

        _uiState.update {
            it.copy(
                oldTariffRate = ResultState.Success(value),
                isErrorInputOldTariffRate = isErrorInputDouble(value)
            )
        }
    }

    fun setDateSetTariffRate(date: Calendar) {
        val dayOfMonth = date.get(Calendar.DAY_OF_MONTH)
        val month = date.get(Calendar.MONTH) // Месяцы в Calendar от 0 до 11
        val year = date.get(Calendar.YEAR)
        viewModelScope.launch(Dispatchers.IO) {
            // Проверяем, совпадает ли месяц/год с текущим
            if (currentMonthOfYear?.month != month || currentMonthOfYear?.year != year) {
                // Загружаем список MonthOfYear
                val monthList = calendarUseCase.loadFlowMonthOfYearListState().first()

                // Ищем подходящий MonthOfYear по месяцу и году
                val newMonthOfYear = monthList.find { it.month == month && it.year == year }

                // Если найден, обновляем currentMonthOfYear
                withContext(Dispatchers.Main) {
                    currentMonthOfYear = newMonthOfYear ?: MonthOfYear(
                        // Если не найден, создаем новый (дефолт)
                        month = month,
                        year = year,
                    )
                    _uiState.update {
                        it.copy(
                            currentMonthOfYear = currentMonthOfYear,
                            tariffRate = ResultState.Success(
                                currentMonthOfYear?.tariffRate.str() ?: "0.0"
                            )
                        )
                    }
                    initialValueTariffRate = currentMonthOfYear?.tariffRate
                }
            }

            withContext(Dispatchers.Main) {
                if (dayOfMonth == 1) {
                    currentMonthOfYear = currentMonthOfYear?.copy(
                        dateSetTariffRate = null
                    )
                    _uiState.update {
                        it.copy(
                            currentMonthOfYear = currentMonthOfYear,
                        )
                    }
                } else {
                    val currentDateSetTariffRate = currentMonthOfYear?.dateSetTariffRate
                    currentMonthOfYear = if (currentDateSetTariffRate == null) {
                        currentMonthOfYear?.copy(
                            dateSetTariffRate = DateSetTariffRate(
                                dateNewRate = dayOfMonth,
                                // oldRate = ставка за начало месяца (до даты смены тарифа)
                                oldRate = initialValueTariffRate ?: 0.0,
                            )
                        )
                    } else {
                        currentMonthOfYear?.copy(
                            dateSetTariffRate = currentMonthOfYear!!.dateSetTariffRate?.copy(
                                dateNewRate = dayOfMonth,
                            )
                        )
                    }
                    _uiState.update {
                        it.copy(
                            currentMonthOfYear = currentMonthOfYear,
                            oldTariffRate = ResultState.Success(currentMonthOfYear!!.dateSetTariffRate!!.oldRate.toString()),
                        )
                    }
                }
            }
        }
    }

    fun setAveragePaymentHour(value: String) {
        currentSalarySetting = currentSalarySetting?.copy(
            averagePaymentHour = value.toDoubleOrZero()
        )
        _uiState.update {
            it.copy(
                averagePaymentHour = ResultState.Success(value),
                isErrorInputAveragePayment = isErrorInputDouble(value)
            )
        }
        scheduleAutoSave()
    }

    fun setDistrictCoefficient(value: String) {
        currentSalarySetting = currentSalarySetting?.copy(
            districtCoefficient = value.toDoubleOrZero()
        )
        _uiState.update {
            it.copy(
                districtCoefficient = ResultState.Success(value),
                isErrorInputDistrictCoefficient = isErrorInputDouble(value)
            )
        }
        scheduleAutoSave()
    }

    fun setNordicCoefficient(value: String) {
        currentSalarySetting = currentSalarySetting?.copy(
            nordicPercent = value.toDoubleOrZero()
        )
        _uiState.update {
            it.copy(
                nordicCoefficient = ResultState.Success(value),
                isErrorInputNordicCoefficient = isErrorInputDouble(value)
            )
        }
        scheduleAutoSave()
    }

    fun setZonalSurcharge(value: String) {
        currentSalarySetting = currentSalarySetting?.copy(
            zonalSurcharge = value.toDoubleOrZero()
        )
        _uiState.update {
            it.copy(
                zonalSurcharge = ResultState.Success(value),
                isErrorInputZonalSurcharge = isErrorInputDouble(value)
            )
        }
        scheduleAutoSave()
    }

    fun setSurchargeQualificationClass(value: String) {
        currentSalarySetting = currentSalarySetting?.copy(
            surchargeQualificationClass = value.toDoubleOrZero()
        )
        _uiState.update {
            it.copy(
                surchargeQualificationClass = ResultState.Success(value),
                isErrorInputSurchargeQualificationClass = isErrorInputDouble(value)
            )
        }
        scheduleAutoSave()
    }

    fun setOnePersonOperationPercent(value: String) {
        currentSalarySetting = currentSalarySetting?.copy(
            onePersonOperationPercent = value.toDoubleOrZero()
        )
        _uiState.update {
            it.copy(
                onePersonOperationPercent = ResultState.Success(value),
                isErrorInputOnePersonOperation = isErrorInputDouble(value)
            )
        }
        scheduleAutoSave()
    }

    fun setOnePersonOperationPassengerTrainPercent(value: String) {
        currentSalarySetting = currentSalarySetting?.copy(
            onePersonOperationPassengerTrainPercent = value.toDoubleOrZero()
        )
        _uiState.update {
            it.copy(
                onePersonOperationPassengerTrainPercent = ResultState.Success(value),
                isErrorInputOnePersonOperationPassengerTrain = isErrorInputDouble(value)
            )
        }
        scheduleAutoSave()
    }

    fun setHarmfulnessPercent(value: String) {
        currentSalarySetting = currentSalarySetting?.copy(
            harmfulnessPercent = value.toDoubleOrZero()
        )
        _uiState.update {
            it.copy(
                harmfulnessPercent = ResultState.Success(value),
                isErrorInputHarmfulnessPercent = isErrorInputDouble(value)
            )
        }
        scheduleAutoSave()
    }

    fun addSurchargeHeavyTrain() {
        surchargeHeavyTrainsState.add(SurchargeHeavyTrains())
        scheduleAutoSave()
    }

    fun setSurchargeHeavyTrainPercent(index: Int, percent: String) {
        surchargeHeavyTrainsState[index] = surchargeHeavyTrainsState[index].copy(
            percentSurcharge = percent
        )
        scheduleAutoSave()
    }

    fun setSurchargeHeavyTrainWeight(index: Int, weight: String) {
        surchargeHeavyTrainsState[index] = surchargeHeavyTrainsState[index].copy(
            weight = weight
        )
        scheduleAutoSave()
    }

    fun deleteSurchargeHeavyTrain(index: Int) {
        surchargeHeavyTrainsState.removeAt(index)
        scheduleAutoSave()
    }

    private fun setSurchargeHeavyTrainState(surcharges: List<SurchargeHeavyTrains>) {
        surchargeHeavyTrainsState.clear()
        surcharges.forEach { surchargeHeavyTrains ->
            surchargeHeavyTrainsState.addOrReplace(
                SurchargeHeavyTrains(
                    id = surchargeHeavyTrains.id,
                    percentSurcharge = surchargeHeavyTrains.percentSurcharge,
                    weight = surchargeHeavyTrains.weight
                )
            )
        }
    }

    fun addSurchargeLongTrain() {
        surchargeLongTrainsState.add(SurchargeLongTrains())
        scheduleAutoSave()
    }

    fun setSurchargeLongTrainPercent(index: Int, percent: String) {
        surchargeLongTrainsState[index] = surchargeLongTrainsState[index].copy(
            percentSurcharge = percent
        )
        scheduleAutoSave()
    }

    fun setSurchargeLongTrainLength(index: Int, length: String) {
        surchargeLongTrainsState[index] = surchargeLongTrainsState[index].copy(
            conditionalLength = length
        )
        scheduleAutoSave()
    }

    fun deleteSurchargeLongTrain(index: Int) {
        surchargeLongTrainsState.removeAt(index)
        scheduleAutoSave()
    }

    private fun setSurchargeLongTrainState(surcharges: List<SurchargeLongTrains>) {
        surchargeLongTrainsState.clear()
        surcharges.forEach { surchargeLongTrains ->
            surchargeLongTrainsState.addOrReplace(
                SurchargeLongTrains(
                    id = surchargeLongTrains.id,
                    percentSurcharge = surchargeLongTrains.percentSurcharge,
                    conditionalLength = surchargeLongTrains.conditionalLength
                )
            )
        }
    }

    fun addSurchargeExtendedServicePhase() {
        surchargeExtendedServicePhaseListState
            .add(SurchargeExtendedServicePhase())
        scheduleAutoSave()
    }

    fun setSurchargeExtendedServicePhasePercent(index: Int, percent: String) {
        surchargeExtendedServicePhaseListState[index] =
            surchargeExtendedServicePhaseListState[index].copy(
                percentSurcharge = percent
            )
        scheduleAutoSave()
    }

    fun setSurchargeExtendedServicePhaseDistance(index: Int, distance: String) {
        surchargeExtendedServicePhaseListState[index] =
            surchargeExtendedServicePhaseListState[index].copy(
                distance = distance
            )
        scheduleAutoSave()
    }

    fun deleteSurchargeExtendedServicePhase(index: Int) {
        surchargeExtendedServicePhaseListState.removeAt(index)
        scheduleAutoSave()
    }

    private fun setSurchargeExtendedServicePhaseListState(servicePhases: List<SurchargeExtendedServicePhase>) {
        surchargeExtendedServicePhaseListState.clear()
        servicePhases.forEach { servicePhase ->
            surchargeExtendedServicePhaseListState.addOrReplace(
                SurchargeExtendedServicePhase(
                    id = servicePhase.id,
                    distance = servicePhase.distance,
                    percentSurcharge = servicePhase.percentSurcharge
                )
            )
        }
    }

    fun setOtherSurcharge(value: String) {
        currentSalarySetting = currentSalarySetting?.copy(
            otherSurcharge = value.toDoubleOrZero()
        )
        _uiState.update {
            it.copy(
                otherSurchargeState = ResultState.Success(value),
                isErrorInputOtherSurcharge = isErrorInputDouble(value)
            )
        }
        scheduleAutoSave()
    }

    fun setNDFL(value: String) {
        currentSalarySetting = currentSalarySetting?.copy(
            ndfl = value.toDoubleOrZero()
        )
        _uiState.update {
            it.copy(
                ndfl = ResultState.Success(value),
                isErrorInputNdfl = isErrorInputDouble(value)
            )
        }
        scheduleAutoSave()
    }

    fun setUnionistsRetention(value: String) {
        currentSalarySetting = currentSalarySetting?.copy(
            unionistsRetention = value.toDoubleOrZero()
        )
        _uiState.update {
            it.copy(
                unionistsRetentionState = ResultState.Success(value),
                isErrorInputUnionistsRetention = isErrorInputDouble(value)
            )
        }
        scheduleAutoSave()
    }

    fun setOtherRetention(value: String) {
        currentSalarySetting = currentSalarySetting?.copy(
            otherRetention = value.toDoubleOrZero()
        )
        _uiState.update {
            it.copy(
                otherRetention = ResultState.Success(value),
                isErrorInputOtherRetention = isErrorInputDouble(value)
            )
        }
        scheduleAutoSave()
    }

    fun setWelfarePercent(value: String) {
        currentSalarySetting = currentSalarySetting?.copy(
            welfarePercent = value.toDoubleOrZero()
        )
        _uiState.update {
            it.copy(
                welfarePercentState = ResultState.Success(value),
                isErrorInputWelfarePercent = isErrorInputDouble(value)
            )
        }
        scheduleAutoSave()
    }

    fun setAlimonyPercent(value: String) {
        currentSalarySetting = currentSalarySetting?.copy(
            alimonyPercent = value.toDoubleOrZero()
        )
        _uiState.update {
            it.copy(
                alimonyPercentState = ResultState.Success(value),
                isErrorInputAlimonyPercent = isErrorInputDouble(value)
            )
        }
        scheduleAutoSave()
    }

    fun setShowUnderworkPayments(value: Boolean) {
        currentSalarySetting = currentSalarySetting?.copy(
            showUnderworkPayments = value
        )
        _uiState.update {
            it.copy(showUnderworkPayments = value)
        }
        scheduleAutoSave()
    }
}
