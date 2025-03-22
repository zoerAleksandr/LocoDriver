package com.z_company.route.viewmodel

import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.core.ResultState
import com.z_company.core.util.DateAndTimeConverter.getMonthFullText
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.SalarySetting
import com.z_company.domain.entities.SurchargeExtendedServicePhase
import com.z_company.domain.entities.SurchargeHeavyTrains
import com.z_company.domain.use_cases.SalarySettingUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.domain.util.addOrReplace
import com.z_company.domain.util.str
import com.z_company.domain.util.toDoubleOrZero
import com.z_company.domain.util.toIntOrZero
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SettingSalaryViewModel : ViewModel(), KoinComponent {
    private val salarySettingUseCase: SalarySettingUseCase by inject()
    private val userSettingUseCase: SettingsUseCase by inject()

    private var initialValueTariffRate: Double? = null
    private var currentMonthOfYear: MonthOfYear? = null

    private val _uiState = MutableStateFlow(SettingSalaryUIState())
    val uiState = _uiState.asStateFlow()

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
                    harmfulnessPercent = ResultState.Success(value?.harmfulnessPercent.str()),
                    longDistanceTrainPercent = ResultState.Success(value?.surchargeLongDistanceTrain.str()),
                    lengthLongDistanceTrain = ResultState.Success(value?.lengthLongDistanceTrain.str()),
                    zonalSurcharge = ResultState.Success(value?.zonalSurcharge.str()),
                    surchargeQualificationClass = ResultState.Success(value?.surchargeQualificationClass.str()),
                    otherSurchargeState = ResultState.Success(value?.otherSurcharge.str()),
                    ndfl = ResultState.Success(value?.ndfl.str()),
                    unionistsRetentionState = ResultState.Success(value?.unionistsRetention.str()),
                    otherRetention = ResultState.Success(value?.otherRetention.str())
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

    init {
        loadSalarySetting()
        loadUserSetting()
    }

    private fun loadUserSetting() {
        viewModelScope.launch(Dispatchers.IO) {
            val userSettings = this.async { userSettingUseCase.getUserSetting() }.await()
            val tariffRate =
                this.async { salarySettingUseCase.getTariffRateFromCurrentMonthOfYear(userSettings.selectMonthOfYear) }
                    .await()
            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(
                        tariffRate = ResultState.Success(tariffRate.str()),
                        currentMonth = ResultState.Success(userSettings.selectMonthOfYear.month.getMonthFullText()),
                        currentYear = ResultState.Success(userSettings.selectMonthOfYear.year.toString())
                    )
                }
            }
            initialValueTariffRate = tariffRate
            currentMonthOfYear = userSettings.selectMonthOfYear
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
                if (initValue == month.tariffRate) {
                    saveSetting()
                } else {
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
        _uiState.update {
            it.copy(
                saveSettingState = ResultState.Loading
            )
        }
        viewModelScope.launch {
            changeTariffRateInOnlyInOneMonthOfYear()
            saveSetting()
        }
    }

    fun saveSettingAndTariffRateCurrentAndNextMonth() {
        _uiState.update {
            it.copy(
                saveSettingState = ResultState.Loading
            )
        }
        viewModelScope.launch {
            changeTariffRateCurrentAndNextMonths()
            saveSetting()
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

    private fun saveSetting() {
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

                    salarySettingUseCase.saveSalarySetting(salarySetting).collect { saveResult ->
                        withContext(Dispatchers.Main) {
                            _uiState.update {
                                it.copy(
                                    saveSettingState = saveResult
                                )
                            }
                        }

                        if (saveResult is ResultState.Success) {
                            currentMonthOfYear?.let { month ->
                                var userSettings =
                                    this.async { userSettingUseCase.getUserSetting() }.await()
                                userSettings = userSettings.copy(
                                    selectMonthOfYear = month
                                )
                                userSettingUseCase.saveSetting(userSettings).collect{}
                            }
                        }
                    }
                }
            }
        }
    }

    private fun loadSalarySetting() {
        viewModelScope.launch {
            salarySettingUseCase.getFlowSalarySetting().collect { result ->
                if (result is ResultState.Success) {
                    currentSalarySetting = if (result.data != null) {
                        result.data
                    } else {
                        SalarySetting()
                    }
                    currentSalarySetting?.let {
                        setSurchargeExtendedServicePhaseListState(it.surchargeExtendedServicePhaseList)
                        setSurchargeHeavyTrainState(it.surchargeHeavyTrainsList)
                    }
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
    }

    fun setSurchargeLongTrain(value: String) {
        currentSalarySetting = currentSalarySetting?.copy(
            surchargeLongDistanceTrain = value.toDoubleOrZero()
        )
        _uiState.update {
            it.copy(
                longDistanceTrainPercent = ResultState.Success(value),
                isErrorInputLongDistanceTrainPercent = isErrorInputDouble(value)
            )
        }
    }

    fun setLengthLongDistanceTrain(value: String) {
        currentSalarySetting = currentSalarySetting?.copy(
            lengthLongDistanceTrain = value.toIntOrZero()
        )
        _uiState.update {
            it.copy(
                lengthLongDistanceTrain = ResultState.Success(value),
                isErrorInputLengthLongDistanceTrain = isErrorInputDouble(value)
            )
        }
    }


    fun addSurchargeHeavyTrain() {
        surchargeHeavyTrainsState.add(SurchargeHeavyTrains())
    }

    fun setSurchargeHeavyTrainPercent(index: Int, percent: String) {
        surchargeHeavyTrainsState[index] = surchargeHeavyTrainsState[index].copy(
            percentSurcharge = percent
        )
    }

    fun setSurchargeHeavyTrainWeight(index: Int, weight: String) {
        surchargeHeavyTrainsState[index] = surchargeHeavyTrainsState[index].copy(
            weight = weight
        )
    }

    fun deleteSurchargeHeavyTrain(index: Int) {
        surchargeHeavyTrainsState.removeAt(index)
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

    fun addSurchargeExtendedServicePhase() {
        surchargeExtendedServicePhaseListState
            .add(SurchargeExtendedServicePhase())
    }

    fun setSurchargeExtendedServicePhasePercent(index: Int, percent: String) {
        surchargeExtendedServicePhaseListState[index] =
            surchargeExtendedServicePhaseListState[index].copy(
                percentSurcharge = percent
            )
    }

    fun setSurchargeExtendedServicePhaseDistance(index: Int, distance: String) {
        surchargeExtendedServicePhaseListState[index] =
            surchargeExtendedServicePhaseListState[index].copy(
                distance = distance
            )
    }

    fun deleteSurchargeExtendedServicePhase(index: Int) {
        surchargeExtendedServicePhaseListState.removeAt(index)
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
    }
}