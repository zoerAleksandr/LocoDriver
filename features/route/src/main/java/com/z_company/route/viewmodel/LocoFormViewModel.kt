package com.z_company.route.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.core.ResultState
import com.z_company.core.util.DateAndTimeConverter
import com.z_company.domain.entities.setting.UserSettings
import com.z_company.domain.entities.route.LocoType
import com.z_company.domain.entities.route.Locomotive
import com.z_company.domain.entities.route.SectionDiesel
import com.z_company.domain.entities.route.SectionElectric
import com.z_company.domain.repositories.SharedPreferencesRepositories
import com.z_company.domain.use_cases.LocomotiveUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.domain.util.CalculationEnergy
import com.z_company.domain.util.addAllOrSkip
import com.z_company.domain.util.addOrReplace
import com.z_company.domain.util.str
import com.z_company.route.Const.NULLABLE_ID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.String
import kotlin.properties.Delegates

class LocoFormViewModel(
    private val locoId: String?,
    private val basicId: String
) : ViewModel(), KoinComponent {
    private val locomotiveUseCase: LocomotiveUseCase by inject()
    private val settingsUseCase: SettingsUseCase by inject()
    private val sharedPreferenceStorage: SharedPreferencesRepositories by inject()

    private val _uiState = MutableStateFlow(LocoFormUiState())
    val uiState = _uiState.asStateFlow()

    private var loadLocoJob: Job? = null
    private var saveLocoJob: Job? = null
    private var saveCoefficientJob: Job? = null

    var timeZoneText: String = "GMT+3"

    private val _seriesList = MutableStateFlow(mutableStateListOf<String>())
    val seriesList = _seriesList.asStateFlow()

    private var mutableSeriesList = mutableStateListOf<String>()
        .also {
            it.addAll(seriesList.value)
        }

    var dropDownSeriesList: SnapshotStateList<String>
        get() {
            return mutableSeriesList
        }
        set(value) {
            mutableSeriesList = value
        }

    private var isNewLoco by Delegates.notNull<Boolean>()

    private val _currentLoco = MutableStateFlow<Locomotive?>(null)
    val currentLoco = _currentLoco.asStateFlow()

    private val _electricSectionListState =
        MutableStateFlow<SnapshotStateList<ElectricSectionFormState>>(mutableStateListOf())
    val electricSectionListState = _electricSectionListState.asStateFlow()

    private val _dieselSectionListState =
        MutableStateFlow<SnapshotStateList<DieselSectionFormState>>(mutableStateListOf())
    val dieselSectionListState = _dieselSectionListState.asStateFlow()

    private val _settings = MutableStateFlow<UserSettings?>(null)
    val settings = _settings.asStateFlow()

    init {
        viewModelScope.launch {
            val isKiloMode = sharedPreferenceStorage.isInputDieselInKilo()

            this.launch {
                val sett = settingsUseCase.getUserSettingFlow().first()
                _settings.value = sett
                _uiState.update {
                    it.copy(
                        dateAndTimeConverter = DateAndTimeConverter(sett),
                        isKiloMode = isKiloMode
                    )
                }
                _seriesList.update { list ->
                    list.addAllOrSkip(sett.locomotiveSeriesList.toMutableStateList())
                    list
                }
                mutableSeriesList.addAllOrSkip(sett.locomotiveSeriesList.toMutableStateList())
                timeZoneText = "GMT+${sett.timeZone}"
            }.join()
            if (locoId == NULLABLE_ID) {
                isNewLoco = true
                _currentLoco.value = Locomotive(
                    basicId = basicId,
                    type = _settings.value?.defaultLocoType ?: LocoType.ELECTRIC
                )
                _electricSectionListState.update { list ->
                    list.add(
                        ElectricSectionFormState(
                            sectionId = SectionElectric().sectionId
                        )
                    )
                    list
                }
                val coefficient = _settings.value?.lastEnteredDieselCoefficient

                _dieselSectionListState.update { list ->
                    list.add(
                        DieselSectionFormState(
                            sectionId = SectionDiesel().sectionId,
                            coefficient = DieselSectionFieldState(
                                coefficient?.str() ?: "",
                                DieselSectionType.COEFFICIENT
                            )
                        )
                    )
                    list
                }
            } else {
                isNewLoco = false
                loadLoco(locoId!!)
            }
        }
    }

    fun toggleIsKiloMode() {
        val isKiloMode = !_uiState.value.isKiloMode
        _uiState.update { it.copy(isKiloMode = isKiloMode) }
        sharedPreferenceStorage.toggleInputDieselInKilo(isKiloMode)
    }

    private fun loadLoco(locoId: String) {
        loadLocoJob?.cancel()
        loadLocoJob = viewModelScope.launch {
            val result = locomotiveUseCase.getLocoById(locoId)
                .first { it is ResultState.Success || it is ResultState.Error }
            if (result is ResultState.Success) {
                result.data?.let { loco ->
                    _currentLoco.value = loco
                    setSectionData(loco)
                }
            }
        }
    }

    private fun setSectionData(locomotive: Locomotive) {
        val heating =
            locomotive.heatingCounterAccepted != null || locomotive.heatingCounterDelivery != null
        var otherCurrent =
            locomotive.normaElectricCurrent2 != null && locomotive.normaElectricCurrent2 != 0

        // добавить очистку одного списка при инициализации
        if (locomotive.type == LocoType.DIESEL) {
//            _dieselSectionListState.value.clear()
            locomotive.dieselSectionList.forEach { section ->
                _dieselSectionListState.update { list ->
                    list.addOrReplace(
                        DieselSectionFormState(
                            sectionId = section.sectionId,
                            accepted = DieselSectionFieldState(
                                data = section.acceptedFuel?.str() ?: "",
                                type = DieselSectionType.ACCEPTED
                            ),
                            delivery = DieselSectionFieldState(
                                data = section.deliveryFuel?.str() ?: "",
                                type = DieselSectionType.DELIVERY
                            ),
                            coefficient = DieselSectionFieldState(
                                data = section.coefficient?.str() ?: "",
                                type = DieselSectionType.COEFFICIENT
                            ),
                            refuel = DieselSectionFieldState(
                                data = section.fuelSupply?.str() ?: "",
                                type = DieselSectionType.REFUEL
                            ),
                            refuelInKilo = DieselSectionFieldState(
                                data = section.fuelSupplyInKilo?.str() ?: "",
                                type = DieselSectionType.REFUEL
                            ),
                            refuelCoefficient = DieselSectionFieldState(
                                data = section.coefficientSupply?.str() ?: "",
                                type = DieselSectionType.REFUEL
                            ),
                        )
                    )
                    list
                }
            }
        }
        if (locomotive.type == LocoType.ELECTRIC) {
            locomotive.electricSectionList.forEach { section ->
                _electricSectionListState.update { list ->
                    list.addOrReplace(
                        ElectricSectionFormState(
                            sectionId = section.sectionId,
                            accepted = ElectricSectionFieldState(
                                data = section.acceptedEnergy?.str() ?: "",
                                type = ElectricSectionType.ACCEPTED
                            ),
                            delivery = ElectricSectionFieldState(
                                data = section.deliveryEnergy?.str() ?: "",
                                type = ElectricSectionType.DELIVERY
                            ),
                            recoveryAccepted = ElectricSectionFieldState(
                                data = section.acceptedRecovery?.str() ?: "",
                                type = ElectricSectionType.RECOVERY_ACCEPTED
                            ),
                            recoveryDelivery = ElectricSectionFieldState(
                                data = section.deliveryRecovery?.str() ?: "",
                                type = ElectricSectionType.RECOVERY_DELIVERY
                            ),
                            accepted2 = ElectricSectionFieldState(
                                data = section.acceptedEnergyOtherCurrent?.str() ?: "",
                                type = ElectricSectionType.ACCEPTED2
                            ),
                            delivery2 = ElectricSectionFieldState(
                                data = section.deliveryEnergyOtherCurrent?.str() ?: "",
                                type = ElectricSectionType.DELIVERY2
                            ),
                            recoveryAccepted2 = ElectricSectionFieldState(
                                data = section.acceptedRecoveryOtherCurrent?.str() ?: "",
                                type = ElectricSectionType.RECOVERY_ACCEPTED2
                            ),
                            recoveryDelivery2 = ElectricSectionFieldState(
                                data = section.deliveryRecoveryOtherCurrent?.str() ?: "",
                                type = ElectricSectionType.RECOVERY_DELIVERY2
                            ),

                            resultVisibility = isVisibilityResultElectricSection(
                                section.acceptedEnergy?.str() ?: "",
                                section.deliveryEnergy?.str() ?: "",
                                section.acceptedRecovery?.str() ?: "",
                                section.deliveryRecovery?.str() ?: "",
                                section.acceptedEnergyOtherCurrent?.str() ?: "",
                                section.deliveryEnergyOtherCurrent?.str() ?: "",
                                section.acceptedRecoveryOtherCurrent?.str() ?: "",
                                section.deliveryRecoveryOtherCurrent?.str() ?: "",
                            ),

                            expandItemState = isExpandElectricItem(
                                section.acceptedRecovery,
                                section.deliveryRecovery,
                                section.acceptedRecoveryOtherCurrent,
                                section.deliveryRecoveryOtherCurrent
                            ),
                        )
                    )
                    list
                }

                if (section.acceptedEnergyOtherCurrent != null ||
                    section.deliveryEnergyOtherCurrent != null ||
                    section.acceptedRecoveryOtherCurrent != null ||
                    section.deliveryRecoveryOtherCurrent != null
                ) {
                    otherCurrent = true
                }
            }
        }

        _uiState.update {
            it.copy(
                isShowHeatingCounter = heating,
                isShowOtherCurrent = otherCurrent
            )
        }
    }

    fun setNumber(number: String) {
        _currentLoco.update { it?.copy(number = number) }
        changesHave()
    }

    fun setSeries(series: String) {
        _currentLoco.update { it?.copy(series = series) }
        changesHave()
    }

    fun changeLocoType(locoType: LocoType) {
        _currentLoco.update { it?.copy(type = locoType) }
        changesHave()
    }

    fun setNormaElectricCurrent1(value: String) {
        _currentLoco.update { it?.copy(normaElectricCurrent1 = value.toIntOrNull()) }
    }

    fun setNormaElectricCurrent2(value: String) {
        _currentLoco.update { it?.copy(normaElectricCurrent2 = value.toIntOrNull()) }
    }

    fun setNormaDiesel(value: String) {
        _currentLoco.update { it?.copy(normaDiesel = value) }
    }

    fun setHeatingCounterAccepted(value: String) {
        _currentLoco.update { it?.copy(heatingCounterAccepted = value.toDoubleOrNull()) }
    }

    fun setHeatingCounterDelivery(value: String) {
        _currentLoco.update { it?.copy(heatingCounterDelivery = value.toDoubleOrNull()) }
    }

    fun showHeatingCounter(value: Boolean) {
        _uiState.update { it.copy(isShowHeatingCounter = value) }
    }

    fun showOtherCurrent(value: Boolean) {
        _uiState.update {
            it.copy(
                isShowOtherCurrent = value
            )
        }
    }

    fun setStartAcceptanceTime(time: Long?) {
        _currentLoco.update { it?.copy(timeStartOfAcceptance = time) }
        changesHave()
    }

    fun setEndAcceptanceTime(time: Long?) {
        _currentLoco.update { it?.copy(timeEndOfAcceptance = time) }
        changesHave()
    }

    fun setStartDeliveryTime(time: Long?) {
        _currentLoco.update { it?.copy(timeStartOfDelivery = time) }
        changesHave()
    }

    fun setEndDeliveryTime(time: Long?) {
        _currentLoco.update { it?.copy(timeEndOfDelivery = time) }
        changesHave()
    }

    fun setFuelAccepted(index: Int, value: String?) {
        updateDieselSection(index) { copy(accepted = accepted.copy(data = value ?: "")) }
        changesHave()
    }

    fun setFuelDelivery(index: Int, value: String?) {
        updateDieselSection(index) { copy(delivery = delivery.copy(data = value ?: "")) }
        changesHave()
    }

    fun setCoefficient(index: Int, value: String?) {
        updateDieselSection(index) { copy(coefficient = coefficient.copy(data = value ?: "")) }
        changesHave()
    }

    fun setRefuelDiesel(index: Int, inputValue: String?) {
        updateDieselSection(index) {
            val coeff = refuelCoefficient.data?.toDoubleOrNull()
            val kiloValue = coeff?.let { c ->
                inputValue?.toDoubleOrNull()?.let { liters ->
                    CalculationEnergy.rounding(liters * c, 2)?.str() ?: ""
                }
            } ?: refuelInKilo.data

            copy(
                refuel = DieselSectionFieldState(inputValue, DieselSectionType.REFUEL),
                refuelInKilo = DieselSectionFieldState(kiloValue, DieselSectionType.REFUEL_IN_KILO)
            )
        }
        changesHave()
        val isValid = validateDieselSection(index, inputValue, DieselSectionType.REFUEL)
        updateDieselSection(index) {
            copy(
                formValid = isValid,
                errorMessage = if (!isValid) "Ошибка валидации" else ""
            )
        }
    }

    fun setRefuelInKiloDiesel(index: Int, inputValue: String?) {
        updateDieselSection(index) {
            val coeff = refuelCoefficient.data?.toDoubleOrNull()
            val literValue = if (coeff != null && coeff != 0.0) {
                inputValue?.toDoubleOrNull()?.let { kg ->
                    CalculationEnergy.rounding(kg / coeff, 2)?.str() ?: ""
                }
            } else refuel.data

            copy(
                refuelInKilo = DieselSectionFieldState(
                    inputValue,
                    DieselSectionType.REFUEL_IN_KILO
                ),
                refuel = DieselSectionFieldState(literValue, DieselSectionType.REFUEL)
            )
        }
        changesHave()
        val isValid = validateDieselSection(index, inputValue, DieselSectionType.REFUEL_IN_KILO)
        updateDieselSection(index) {
            copy(
                formValid = isValid,
                errorMessage = if (!isValid) "Ошибка валидации" else ""
            )
        }
    }

    fun setRefuelCoefficientDiesel(index: Int, inputValue: String?) {
        updateDieselSection(index) {
            val coeff = inputValue?.toDoubleOrNull()
            val liters = refuel.data?.toDoubleOrNull()
            val kiloValue = if (coeff != null && liters != null) {
                CalculationEnergy.rounding(liters * coeff, 2)?.str() ?: ""
            } else refuelInKilo.data

            copy(
                refuelCoefficient = DieselSectionFieldState(
                    inputValue,
                    DieselSectionType.REFUEL_COEFFICIENT
                ),
                refuelInKilo = DieselSectionFieldState(kiloValue, DieselSectionType.REFUEL_IN_KILO)
            )
        }
        changesHave()
        val isValid = validateDieselSection(index, inputValue, DieselSectionType.REFUEL_COEFFICIENT)
        updateDieselSection(index) {
            copy(
                formValid = isValid,
                errorMessage = if (!isValid) "Ошибка валидации" else ""
            )
        }
    }

    fun setEnergyAccepted(index: Int, value: String?) {
        updateElectricSection(index) { copy(accepted = accepted.copy(data = value ?: "")) }
        changesHave()
    }

    fun setEnergyDelivery(index: Int, value: String?) {
        updateElectricSection(index) { copy(delivery = delivery.copy(data = value ?: "")) }
        changesHave()
    }

    fun setRecoveryAccepted(index: Int, value: String?) {
        updateElectricSection(index) {
            copy(
                recoveryAccepted = recoveryAccepted.copy(
                    data = value ?: ""
                )
            )
        }
        changesHave()
    }

    fun setRecoveryDelivery(index: Int, value: String?) {
        updateElectricSection(index) {
            copy(
                recoveryDelivery = recoveryDelivery.copy(
                    data = value ?: ""
                )
            )
        }
        changesHave()
    }

    fun setEnergyAccepted2(index: Int, value: String?) {
        updateElectricSection(index) { copy(accepted2 = accepted2.copy(data = value ?: "")) }
        changesHave()
    }

    fun setEnergyDelivery2(index: Int, value: String?) {
        updateElectricSection(index) { copy(delivery2 = delivery2.copy(data = value ?: "")) }
        changesHave()
    }

    fun setRecoveryAccepted2(index: Int, value: String?) {
        updateElectricSection(index) {
            copy(
                recoveryAccepted2 = recoveryAccepted2.copy(
                    data = value ?: ""
                )
            )
        }
        changesHave()
    }

    fun setRecoveryDelivery2(index: Int, value: String?) {
        updateElectricSection(index) {
            copy(
                recoveryDelivery2 = recoveryDelivery2.copy(
                    data = value ?: ""
                )
            )
        }
        changesHave()
    }

    private fun updateDieselSection(
        index: Int,
        block: DieselSectionFormState.() -> DieselSectionFormState
    ) {
        _dieselSectionListState.update { list ->
            val updatedList = list.toMutableList()
            if (index in updatedList.indices) {
                updatedList[index] = updatedList[index].block()
            }
            updatedList.toMutableStateList()
        }
    }

    private fun updateElectricSection(
        index: Int,
        block: ElectricSectionFormState.() -> ElectricSectionFormState
    ) {
        _electricSectionListState.update { list ->
            val updatedList = list.toMutableList()
            if (index in updatedList.indices) {
                val current = updatedList[index]
                val updatedState = current.block()
                // Добавляем расчёт видимости результата
                val resultVisibility = isVisibilityResultElectricSection(
                    accepted = updatedState.accepted.data ?: "",
                    delivery = updatedState.delivery.data ?: "",
                    acceptedRecovery = updatedState.recoveryAccepted.data ?: "",
                    deliveryRecovery = updatedState.recoveryDelivery.data ?: "",
                    accepted2 = updatedState.accepted2.data ?: "",
                    delivery2 = updatedState.delivery2.data ?: "",
                    acceptedRecovery2 = updatedState.recoveryAccepted2.data ?: "",
                    deliveryRecovery2 = updatedState.recoveryDelivery2.data ?: ""
                )

                updatedList[index] = updatedState.copy(resultVisibility = resultVisibility)
            }
            updatedList.toMutableStateList()
        }
        changesHave()
    }

    fun deleteSectionDiesel(section: DieselSectionFormState) {
        _dieselSectionListState.update { list ->
            list.remove(section)
            list
        }
        changesHave()
    }

    fun addingSectionDiesel() {
        val coefficient = settings.value?.lastEnteredDieselCoefficient
        _dieselSectionListState.update { list ->
            list.add(
                DieselSectionFormState(
                    sectionId = SectionDiesel().sectionId,
                    coefficient = DieselSectionFieldState(
                        coefficient?.str() ?: "",
                        DieselSectionType.COEFFICIENT
                    )
                )
            )
            list
        }
        changesHave()
    }

    fun deleteSectionElectric(section: ElectricSectionFormState) {
        _electricSectionListState.update { list ->
            list.remove(section)
            list
        }
        changesHave()
    }

    fun addingSectionElectric() {
        _electricSectionListState.update { list ->
            list.add(ElectricSectionFormState(sectionId = SectionElectric().sectionId))
            list
        }
        changesHave()
    }

    fun focusChangedDieselSection(index: Int, type: DieselSectionType) {
        val inputValue = when (type) {
            DieselSectionType.ACCEPTED -> _dieselSectionListState.value.getOrNull(index)?.accepted?.data
            DieselSectionType.DELIVERY -> _dieselSectionListState.value.getOrNull(index)?.delivery?.data
            DieselSectionType.COEFFICIENT -> {
                saveCoefficient(_dieselSectionListState.value.getOrNull(index)?.coefficient?.data)
                _dieselSectionListState.value.getOrNull(index)?.coefficient?.data
            }

            DieselSectionType.REFUEL -> _dieselSectionListState.value.getOrNull(index)?.refuel?.data
            DieselSectionType.REFUEL_IN_KILO -> _dieselSectionListState.value.getOrNull(index)?.refuelInKilo?.data
            DieselSectionType.REFUEL_COEFFICIENT -> _dieselSectionListState.value.getOrNull(index)?.refuelCoefficient?.data
        }
        val isValid = validateDieselSection(index, inputValue, type)
        updateDieselSection(index) {
            copy(
                formValid = isValid,
                errorMessage = if (!isValid) "Ошибка валидации" else "" // Адаптировать errorMessage
            )
        }
    }

    fun focusChangedElectricSection(index: Int, type: ElectricSectionType) {
        val inputValue = when (type) {
            ElectricSectionType.ACCEPTED -> _electricSectionListState.value.getOrNull(index)?.accepted?.data
            ElectricSectionType.DELIVERY -> _electricSectionListState.value.getOrNull(index)?.delivery?.data
            ElectricSectionType.RECOVERY_ACCEPTED -> _electricSectionListState.value.getOrNull(index)?.recoveryAccepted?.data
            ElectricSectionType.RECOVERY_DELIVERY -> _electricSectionListState.value.getOrNull(index)?.recoveryDelivery?.data
            ElectricSectionType.ACCEPTED2 -> _electricSectionListState.value.getOrNull(index)?.accepted2?.data
            ElectricSectionType.DELIVERY2 -> _electricSectionListState.value.getOrNull(index)?.delivery2?.data
            ElectricSectionType.RECOVERY_ACCEPTED2 -> _electricSectionListState.value.getOrNull(
                index
            )?.recoveryAccepted2?.data

            ElectricSectionType.RECOVERY_DELIVERY2 -> _electricSectionListState.value.getOrNull(
                index
            )?.recoveryDelivery2?.data

        }
        val isValid = validateElectricSection(index, inputValue, type)
        updateElectricSection(index) {
            copy(
                formValid = isValid,
                errorMessage = if (!isValid) "Ошибка валидации" else ""
            )
        }
    }

    private fun isExpandElectricItem(
        acceptedRecovery: Double?,
        deliveryRecovery: Double?,
        acceptedRecovery2: Double?,
        deliveryRecovery2: Double?,
    ): Boolean {
        return (acceptedRecovery != null || deliveryRecovery != null || acceptedRecovery2 != null || deliveryRecovery2 != null)
    }

    fun isExpandElectricItem(isExpand: Boolean) {
        electricSectionListState.value.forEachIndexed { index, _ ->
            electricSectionListState.value[index] = electricSectionListState.value[index].copy(
                expandItemState = isExpand
            )
        }
    }

    private fun isVisibilityResultElectricSection(
        accepted: String?,
        delivery: String?,
        acceptedRecovery: String?,
        deliveryRecovery: String?,
        accepted2: String?,
        delivery2: String?,
        acceptedRecovery2: String?,
        deliveryRecovery2: String?
    ): Boolean {
        return ((accepted != null && delivery != null) || (acceptedRecovery != null && deliveryRecovery != null)) ||
                ((accepted2 != null && delivery2 != null) || (acceptedRecovery2 != null && deliveryRecovery2 != null))
    }

    private fun validateElectricSection(
        index: Int,
        inputValue: String?,
        type: ElectricSectionType
    ): Boolean {
        // Исходная логика валидации
        when (type) {
            ElectricSectionType.ACCEPTED -> {
                val delivery = _electricSectionListState.value[index].delivery.data
                delivery?.let { del ->
                    inputValue?.let { acc ->
                        if (acc > del) {
                            updateElectricSection(index) { copy(errorMessage = "Принято больше чем сдано") }
                            return false
                        }
                    }
                }
            }

            ElectricSectionType.DELIVERY -> {
                val accepted = electricSectionListState.value[index].accepted.data

                accepted?.let { acc ->
                    inputValue?.let { del ->
                        if (del < acc) {
                            updateElectricSection(index) { copy(errorMessage = "Сдано меньше чем принято") }
                            return false
                        }
                    }
                }
                true
            }

            ElectricSectionType.RECOVERY_ACCEPTED -> {
                val delivery = electricSectionListState.value[index].recoveryDelivery.data

                delivery?.let { del ->
                    inputValue?.let { acc ->
                        if (acc > del) {
                            updateElectricSection(index) { copy(errorMessage = "Принято больше чем сдано") }
                            return false
                        }
                    }
                }
                true
            }

            ElectricSectionType.RECOVERY_DELIVERY -> {
                val accepted = electricSectionListState.value[index].recoveryAccepted.data

                accepted?.let { acc ->
                    inputValue?.let { del ->
                        if (del < acc) {
                            updateElectricSection(index) { copy(errorMessage = "Сдано меньше чем принято") }
                            return false
                        }
                    }
                }
                true
            }

            ElectricSectionType.ACCEPTED2 -> {
                val delivery2 = _electricSectionListState.value[index].delivery2.data
                delivery2?.let { del ->
                    inputValue?.let { acc ->
                        if (acc > del) {
                            updateElectricSection(index) { copy(errorMessage = "Принято больше чем сдано") }
                            return false
                        }
                    }
                }
            }

            ElectricSectionType.DELIVERY2 -> {
                val accepted2 = electricSectionListState.value[index].accepted2.data

                accepted2?.let { acc ->
                    inputValue?.let { del ->
                        if (del < acc) {
                            updateElectricSection(index) { copy(errorMessage = "Сдано меньше чем принято") }
                            return false
                        }
                    }
                }
                true
            }

            ElectricSectionType.RECOVERY_ACCEPTED2 -> {
                val delivery2 = electricSectionListState.value[index].recoveryDelivery2.data

                delivery2?.let { del ->
                    inputValue?.let { acc ->
                        if (acc > del) {
                            updateElectricSection(index) { copy(errorMessage = "Принято больше чем сдано") }
                            return false
                        }
                    }
                }
                true
            }

            ElectricSectionType.RECOVERY_DELIVERY2 -> {
                val accepted2 = electricSectionListState.value[index].recoveryAccepted2.data

                accepted2?.let { acc ->
                    inputValue?.let { del ->
                        if (del < acc) {
                            updateElectricSection(index) { copy(errorMessage = "Сдано меньше чем принято") }
                            return false
                        }
                    }
                }
                true
            }
        }
        return true
    }

    private fun validateDieselSection(
        index: Int,
        inputValue: String?,
        type: DieselSectionType
    ): Boolean {
        // Исходная логика валидации
        when (type) {
            DieselSectionType.ACCEPTED -> {
                val accepted = inputValue?.toDoubleOrNull()
                val delivery = _dieselSectionListState.value[index].delivery.data?.toDoubleOrNull()
                val refuel = _dieselSectionListState.value[index].refuel.data?.toDoubleOrNull()

                val result = CalculationEnergy.getTotalFuelConsumption(accepted, delivery, refuel)
                result?.let {
                    if (it < 0) {
                        updateDieselSection(index) { copy(errorMessage = "Принял меньше чем сдал") }
                        return false
                    }
                }
            }

            DieselSectionType.DELIVERY -> {
                val accepted = dieselSectionListState.value[index].accepted.data?.toDoubleOrNull()
                val delivery = inputValue?.toDoubleOrNull()
                val refuel = dieselSectionListState.value[index].refuel.data?.toDoubleOrNull()

                val result = CalculationEnergy.getTotalFuelConsumption(accepted, delivery, refuel)
                result?.let {
                    if (it < 0) {
                        updateDieselSection(index) { copy(errorMessage = "Сдал больше чем принял") }
                        return false
                    }
                }
                true
            }

            DieselSectionType.COEFFICIENT -> {
                val coefficient = inputValue?.toDoubleOrNull()
                coefficient?.let {
                    if (it > 1) {
                        updateDieselSection(index) { copy(errorMessage = "Коэффициент больше 1.0") }
                        return false
                    }
                }
                true
            }

            DieselSectionType.REFUEL -> {
                val accepted = dieselSectionListState.value[index].accepted.data?.toDoubleOrNull()
                val delivery = dieselSectionListState.value[index].delivery.data?.toDoubleOrNull()
                val refuel = inputValue?.toDoubleOrNull()

                val result = CalculationEnergy.getTotalFuelConsumption(accepted, delivery, refuel)
                result?.let {
                    if (it < 0) {
                        updateDieselSection(index) { copy(errorMessage = "Не хватает экипировки") }
                        return false
                    }
                }
                true
            }

            DieselSectionType.REFUEL_IN_KILO -> {
                val accepted = dieselSectionListState.value[index].accepted.data?.toDoubleOrNull()
                val delivery = dieselSectionListState.value[index].delivery.data?.toDoubleOrNull()
                val refuel = inputValue?.toDoubleOrNull()

                val result = CalculationEnergy.getTotalFuelConsumption(accepted, delivery, refuel)
                result?.let {
                    if (it < 0) {
                        updateDieselSection(index) { copy(errorMessage = "Не хватает экипировки") }
                        return false
                    }
                }
                true
            }

            DieselSectionType.REFUEL_COEFFICIENT -> {
                val coefficient = inputValue?.toDoubleOrNull()
                coefficient?.let {
                    if (it > 1) {
                        updateDieselSection(index) { copy(errorMessage = "Коэффициент больше 1.0") }
                        return false
                    }
                }
                true
            }
        }
        return true
    }

    fun saveCoefficient(coefficient: String?) {
        viewModelScope.launch {
            saveCoefficientJob?.cancel()
            val double = coefficient?.toDoubleOrNull()
            double?.let {
                settingsUseCase.setDieselCoefficient(double).collect {}
            }
        }
    }

    fun changeExpandedMenu(expanded: Boolean) {
        _uiState.update { it.copy(isExpandedDropDownMenuSeries = expanded) }
    }


    fun onChangedDropDownContent(content: String) {
        if (content.isEmpty()) {
            mutableSeriesList.addAllOrSkip(seriesList.value)
        } else {
            mutableSeriesList.clear()
            val newSeriesList =
                seriesList.value
                    .filter { it.startsWith(prefix = content, ignoreCase = true) }
                    .filterNot { it == content }
                    .toMutableStateList()
            newSeriesList.forEach { ser ->
                mutableSeriesList.add(ser)
                changeExpandedMenu(true)
            }
        }
    }

    fun removeSeries(series: String) {
        viewModelScope.launch {
            dropDownSeriesList.remove(series)
            settingsUseCase.removeLocomotiveSeries(series)
            changesHave()
        }
    }

    fun saveLoco() {
        saveLocoJob?.cancel()
        saveLocoJob = viewModelScope.launch(Dispatchers.IO) {
            _currentLoco.value?.let { loco ->
                val updatedLoco = loco.copy(
                    dieselSectionList = _dieselSectionListState.value.map { state ->
                        SectionDiesel(
                            sectionId = state.sectionId,
                            acceptedFuel = state.accepted.data?.toDoubleOrNull(),
                            deliveryFuel = state.delivery.data?.toDoubleOrNull(),
                            coefficient = state.coefficient.data?.toDoubleOrNull(),
                            fuelSupply = state.refuel.data?.toDoubleOrNull(),
                            fuelSupplyInKilo = state.refuelInKilo.data?.toDoubleOrNull(),
                            coefficientSupply = state.refuelCoefficient.data?.toDoubleOrNull(),
                        )
                    }.toMutableStateList(),

                    electricSectionList = _electricSectionListState.value.map { state ->
                        SectionElectric(
                            sectionId = state.sectionId,
                            acceptedEnergy = state.accepted.data?.toBigDecimalOrNull(),
                            deliveryEnergy = state.delivery.data?.toBigDecimalOrNull(),
                            acceptedRecovery = state.recoveryAccepted.data?.toBigDecimalOrNull(),
                            deliveryRecovery = state.recoveryDelivery.data?.toBigDecimalOrNull(),
                            acceptedEnergyOtherCurrent = state.accepted2.data?.toBigDecimalOrNull(),
                            deliveryEnergyOtherCurrent = state.delivery2.data?.toBigDecimalOrNull(),
                            acceptedRecoveryOtherCurrent = state.recoveryAccepted2.data?.toBigDecimalOrNull(),
                            deliveryRecoveryOtherCurrent = state.recoveryDelivery2.data?.toBigDecimalOrNull(),
                        )
                    }.toMutableList()
                )
                updatedLoco.series?.let { series ->
                    settingsUseCase.setLocomotiveSeries(series)
                }
                locomotiveUseCase.saveLocomotive(updatedLoco).collect { result ->
                    _uiState.update { it.copy(saveLocoState = result) }
                }
            }
        }
    }

    fun resetSaveState() {
        _uiState.update { it.copy(saveLocoState = ResultState.Loading()) }
    }

    private fun changesHave() {
        if (!_uiState.value.changesHaveState) {
            _uiState.update { it.copy(changesHaveState = true) }
        }
    }
}