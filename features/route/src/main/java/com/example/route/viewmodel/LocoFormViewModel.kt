package com.example.route.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.ResultState
import com.example.data_local.setting.DataStoreRepository
import com.example.domain.entities.route.LocoType
import com.example.domain.entities.route.Locomotive
import com.example.domain.entities.route.SectionDiesel
import com.example.domain.entities.route.SectionElectric
import com.example.domain.use_cases.LocomotiveUseCase
import com.example.domain.util.CalculationEnergy
import com.example.domain.util.addOrReplace
import com.example.domain.util.str
import com.example.route.Const.NULLABLE_ID
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class LocoFormViewModel constructor(
    private val locoId: String?,
    private val basicId: String
) : ViewModel(), KoinComponent {
    private val locomotiveUseCase: LocomotiveUseCase by inject()
    private val dataStoreRepository: DataStoreRepository by inject()

    private val _uiState = MutableStateFlow(LocoFormUiState())
    val uiState = _uiState.asStateFlow()

    private var loadLocoJob: Job? = null
    private var saveLocoJob: Job? = null
    private var loadCoefficientJob: Job? = null
    private var saveCoefficientJob: Job? = null
    private var loadDefaultTypeLoco: Job? = null

    var currentLoco: Locomotive?
        get() {
            return _uiState.value.locoDetailState.let {
                if (it is ResultState.Success) it.data else null
            }
        }
        private set(value) {
            _uiState.update {
                it.copy(
                    locoDetailState = ResultState.Success(value),
                )
            }
        }

    private var electricSectionListState: SnapshotStateList<ElectricSectionFormState>
        get() {
            return _uiState.value.electricSectionList ?: mutableStateListOf()
        }
        private set(value) {
            _uiState.update {
                it.copy(
                    electricSectionList = value
                )
            }
        }

    private var dieselSectionListState: SnapshotStateList<DieselSectionFormState>
        get() {
            return _uiState.value.dieselSectionList ?: mutableStateListOf()
        }
        private set(value) {
            _uiState.update {
                it.copy(
                    dieselSectionList = value
                )
            }
        }

    private var lastEnteredCoefficient by mutableStateOf(0.0)
    private var defaultTypeLoco by mutableStateOf(LocoType.ELECTRIC)

    fun clearAllField() {
        currentLoco = currentLoco?.copy(
            series = null,
            number = null,
            type = defaultTypeLoco,
            timeStartOfAcceptance = null,
            timeEndOfAcceptance = null,
            timeStartOfDelivery = null,
            timeEndOfDelivery = null
        )
        electricSectionListState.clear()
        dieselSectionListState.clear()
    }

    private fun setSectionData(locomotive: Locomotive) {
        locomotive.dieselSectionList.forEach { section ->
            dieselSectionListState.addOrReplace(
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
                    )
                )
            )
        }
        locomotive.electricSectionList.forEach { section ->
            electricSectionListState.addOrReplace(
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
                    resultVisibility = isVisibilityResultElectricSection(
                        section.acceptedEnergy?.str(),
                        section.deliveryEnergy?.str(),
                        section.acceptedRecovery?.str(),
                        section.deliveryRecovery?.str()
                    ),
                    expandItemState = isExpandElectricItem(
                        section.acceptedRecovery?.str(),
                        section.deliveryRecovery?.str()
                    )
                )
            )
        }
    }

    init {
        if (locoId == NULLABLE_ID) {
            currentLoco = Locomotive(basicId = basicId)
        } else {
            loadLoco(locoId!!)
        }
        loadSetting()
    }

    private fun loadSetting() {
        loadCoefficientJob?.cancel()
        loadCoefficientJob = dataStoreRepository.getDieselCoefficient().onEach {
            lastEnteredCoefficient = it
        }.launchIn(viewModelScope)
        loadDefaultTypeLoco?.cancel()
        loadDefaultTypeLoco = dataStoreRepository.getTypeLoco().onEach {
            defaultTypeLoco = it
        }.launchIn(viewModelScope)
    }

    private fun saveCoefficient(data: String?) {
        saveCoefficientJob?.cancel()
        saveCoefficientJob = dataStoreRepository
            .setDieselCoefficient(data?.toDoubleOrNull()).launchIn(viewModelScope)
    }

    private fun loadLoco(id: String) {
        if (locoId == currentLoco?.locoId) return
        loadLocoJob?.cancel()
        loadLocoJob = locomotiveUseCase.getLocoById(id).onEach { routeState ->
            _uiState.update {
                if (routeState is ResultState.Success) {
                    currentLoco = routeState.data
                    routeState.data?.let { loco ->
                        setSectionData(loco)
                    }
                }
                it.copy(locoDetailState = routeState)
            }
        }.launchIn(viewModelScope)
    }

    fun saveLoco() {
        val state = _uiState.value.locoDetailState
        if (state is ResultState.Success) {
            state.data?.let { loco ->
                when (loco.type) {
                    LocoType.ELECTRIC -> {
                        loco.electricSectionList = electricSectionListState.map { state ->
                            SectionElectric(
                                sectionId = state.sectionId,
                                acceptedEnergy = state.accepted.data?.toDoubleOrNull(),
                                deliveryEnergy = state.delivery.data?.toDoubleOrNull(),
                                acceptedRecovery = state.recoveryAccepted.data?.toDoubleOrNull(),
                                deliveryRecovery = state.recoveryDelivery.data?.toDoubleOrNull()
                            )
                        }.toMutableList()
                    }

                    LocoType.DIESEL -> {
                        loco.dieselSectionList = dieselSectionListState.map { state ->
                            SectionDiesel(
                                sectionId = state.sectionId,
                                acceptedFuel = state.accepted.data?.toDoubleOrNull(),
                                deliveryFuel = state.delivery.data?.toDoubleOrNull(),
                                coefficient = state.coefficient.data?.toDoubleOrNull(),
                                fuelSupply = state.refuel.data?.toDoubleOrNull(),
                                coefficientSupply = 0.0
                            )
                        }.toMutableStateList()
                    }
                }

                saveLocoJob?.cancel()
                saveLocoJob =
                    locomotiveUseCase.saveLocomotive(loco).onEach { saveLocoState ->
                        _uiState.update {
                            it.copy(saveLocoState = saveLocoState)
                        }
                    }.launchIn(viewModelScope)
            }
        }
    }

    fun resetSaveState() {
        _uiState.update {
            it.copy(saveLocoState = null)
        }
    }

    fun setNumber(number: String) {
        currentLoco = currentLoco?.copy(
            number = number
        )
    }

    fun setSeries(series: String) {
        currentLoco = currentLoco?.copy(
            series = series
        )
    }

    fun changeLocoType(index: Int) {
        val type = when (index) {
            0 -> {
                LocoType.ELECTRIC
            }

            1 -> {
                LocoType.DIESEL
            }

            else -> {
                LocoType.ELECTRIC
            }
        }
        currentLoco = currentLoco?.copy(
            type = type
        )
    }

    fun setStartAcceptedTime(timeInLong: Long?) {
        currentLoco = currentLoco?.copy(
            timeStartOfAcceptance = timeInLong
        )
        isValidAcceptedTime()
    }

    fun setEndAcceptedTime(timeInLong: Long?) {
        currentLoco = currentLoco?.copy(
            timeEndOfAcceptance = timeInLong
        )
        isValidAcceptedTime()
    }

    private fun isValidAcceptedTime() {
        val locoDetailState = _uiState.value.locoDetailState

        if (locoDetailState is ResultState.Success) {
            locoDetailState.data?.let {
                val errorMessage = if (!locomotiveUseCase.isValidAcceptedTime(it)) {
                    "Ошибка"
                } else {
                    null
                }
                _uiState.update { formState ->
                    formState.copy(errorMessage = errorMessage)
                }
            }
        }
    }

    fun setEndDeliveryTime(timeInLong: Long?) {
        currentLoco = currentLoco?.copy(
            timeEndOfDelivery = timeInLong
        )
        isValidDeliveryTime()
    }

    fun setStartDeliveryTime(timeInLong: Long?) {
        currentLoco = currentLoco?.copy(
            timeStartOfDelivery = timeInLong
        )
        isValidDeliveryTime()
    }

    private fun isValidDeliveryTime() {
        val locoDetailState = _uiState.value.locoDetailState

        if (locoDetailState is ResultState.Success) {
            locoDetailState.data?.let {
                val errorMessage = if (!locomotiveUseCase.isValidDeliveryTime(it)) {
                    "Ошибка"
                } else {
                    null
                }
                _uiState.update { formState ->
                    formState.copy(errorMessage = errorMessage)
                }
            }
        }
    }

    fun setFuelAccepted(index: Int, s: String?) {
        onDieselSectionEvent(
            DieselSectionEvent.EnteredAccepted(
                index, s
            )
        )
    }

    fun setFuelDelivery(index: Int, s: String?) {
        onDieselSectionEvent(
            DieselSectionEvent.EnteredDelivery(
                index, s
            )
        )
    }

    fun setRefuel(index: Int, d: String?) {
        onDieselSectionEvent(
            DieselSectionEvent.EnteredRefuel(
                index, d?.toDoubleOrNull()
            )
        )
    }

    fun showRefuelDialog(value: Pair<Boolean, Int>){
        _uiState.update {
            it.copy(
                refuelDialogShow = value
            )
        }
    }
    fun showCoefficientDialog(value: Pair<Boolean, Int>){
        _uiState.update {
            it.copy(
                coefficientDialogShow = value
            )
        }
    }


    fun setCoefficient(index: Int, coefficient: String?) {
        onDieselSectionEvent(
            DieselSectionEvent.EnteredCoefficient(
                index, coefficient?.toDoubleOrNull()
            )
        )
    }

    fun deleteSectionDiesel(dieselSectionFormState: DieselSectionFormState) {
        dieselSectionListState.remove(dieselSectionFormState)
    }

    fun setEnergyAccepted(index: Int, s: String?) {
        onElectricSectionEvent(
            ElectricSectionEvent.EnteredAccepted(
                index = index, data = s
            )
        )
    }

    fun setEnergyDelivery(index: Int, s: String?) {
        onElectricSectionEvent(
            ElectricSectionEvent.EnteredDelivery(
                index = index, data = s
            )
        )
    }

    fun setRecoveryAccepted(index: Int, s: String?) {
        onElectricSectionEvent(
            ElectricSectionEvent.EnteredRecoveryAccepted(
                index = index, data = s
            )
        )
    }

    fun setRecoveryDelivery(index: Int, s: String?) {
        onElectricSectionEvent(
            ElectricSectionEvent.EnteredRecoveryDelivery(
                index = index, data = s
            )
        )
    }

    fun focusChangedElectricSection(index: Int, fieldName: ElectricSectionType) {
        onElectricSectionEvent(
            ElectricSectionEvent.FocusChange(
                index, fieldName
            )
        )
    }

    fun focusChangedDieselSection(index: Int, fieldName: DieselSectionType) {
        onDieselSectionEvent(
            DieselSectionEvent.FocusChange(
                index, fieldName
            )
        )
    }

    fun deleteSectionElectric(sectionElectricForm: ElectricSectionFormState) {
        electricSectionListState.remove(sectionElectricForm)
    }

    fun addingSectionDiesel() {
        dieselSectionListState.add(
            DieselSectionFormState(
                sectionId = SectionDiesel().sectionId,
                coefficient = DieselSectionFieldState(
                    lastEnteredCoefficient.str(),
                    DieselSectionType.COEFFICIENT
                )
            )
        )
        electricSectionListState.clear()
    }

    fun addingSectionElectric() {
        electricSectionListState.add(
            ElectricSectionFormState(
                sectionId = SectionElectric().sectionId
            )
        )
        dieselSectionListState.clear()
    }

    private fun isExpandElectricItem(
        acceptedRecovery: String?,
        deliveryRecovery: String?,
    ): Boolean {
        return (!acceptedRecovery.isNullOrBlank() || !deliveryRecovery.isNullOrBlank())
    }

    fun isExpandElectricItem(index: Int, isExpand: Boolean){
        electricSectionListState[index] = electricSectionListState[index].copy(
            expandItemState = isExpand
        )
    }

    private fun onElectricSectionEvent(event: ElectricSectionEvent) {
        when (event) {
            is ElectricSectionEvent.EnteredAccepted -> {
                electricSectionListState[event.index] = electricSectionListState[event.index].copy(
                    accepted = electricSectionListState[event.index].accepted.copy(
                        data = event.data
                    )
                )
            }

            is ElectricSectionEvent.EnteredDelivery -> {
                electricSectionListState[event.index] = electricSectionListState[event.index].copy(
                    delivery = electricSectionListState[event.index].delivery.copy(
                        data = event.data
                    )
                )
            }

            is ElectricSectionEvent.EnteredRecoveryAccepted -> {
                electricSectionListState[event.index] = electricSectionListState[event.index].copy(
                    recoveryAccepted = electricSectionListState[event.index].recoveryAccepted.copy(
                        data = event.data
                    )
                )
            }

            is ElectricSectionEvent.EnteredRecoveryDelivery -> {
                electricSectionListState[event.index] = electricSectionListState[event.index].copy(
                    recoveryDelivery = electricSectionListState[event.index].recoveryDelivery.copy(
                        data = event.data
                    )
                )
            }

            is ElectricSectionEvent.FocusChange -> {
                val accepted = electricSectionListState[event.index].accepted.data
                val delivery = electricSectionListState[event.index].delivery.data
                val acceptedRecovery =
                    electricSectionListState[event.index].recoveryAccepted.data
                val deliveryRecovery =
                    electricSectionListState[event.index].recoveryDelivery.data

                val isVisibilityResult = isVisibilityResultElectricSection(
                    accepted, delivery, acceptedRecovery, deliveryRecovery
                )
                val isExpand = isExpandElectricItem(acceptedRecovery, deliveryRecovery)
                electricSectionListState[event.index] = electricSectionListState[event.index].copy(
                    resultVisibility = isVisibilityResult,
                    expandItemState = isExpand
                )

                when (event.fieldName) {
                    ElectricSectionType.ACCEPTED -> {
                        val isValid = validateElectricSection(
                            index = event.index,
                            inputValue = electricSectionListState[event.index].accepted.data,
                            type = ElectricSectionType.ACCEPTED
                        )

                        electricSectionListState[event.index] =
                            electricSectionListState[event.index].copy(
                                formValid = isValid
                            )
                    }

                    ElectricSectionType.DELIVERY -> {
                        val isValid = validateElectricSection(
                            index = event.index,
                            inputValue = electricSectionListState[event.index].delivery.data,
                            type = ElectricSectionType.DELIVERY
                        )

                        electricSectionListState[event.index] =
                            electricSectionListState[event.index].copy(
                                formValid = isValid
                            )
                    }

                    ElectricSectionType.RECOVERY_ACCEPTED -> {
                        val isValid = validateElectricSection(
                            index = event.index,
                            inputValue = electricSectionListState[event.index].recoveryAccepted.data,
                            type = ElectricSectionType.RECOVERY_ACCEPTED
                        )

                        electricSectionListState[event.index] =
                            electricSectionListState[event.index].copy(
                                formValid = isValid
                            )
                    }

                    ElectricSectionType.RECOVERY_DELIVERY -> {
                        val isValid = validateElectricSection(
                            index = event.index,
                            inputValue = electricSectionListState[event.index].recoveryDelivery.data,
                            type = ElectricSectionType.RECOVERY_DELIVERY
                        )

                        electricSectionListState[event.index] =
                            electricSectionListState[event.index].copy(
                                formValid = isValid
                            )
                    }
                }
            }
        }
    }

    private fun isVisibilityResultElectricSection(
        accepted: String?,
        delivery: String?,
        acceptedRecovery: String?,
        deliveryRecovery: String?
    ): Boolean {
        return ((!accepted.isNullOrBlank() && !delivery.isNullOrBlank())
                || (!acceptedRecovery.isNullOrBlank() && !deliveryRecovery.isNullOrBlank()))
    }

    private fun validateElectricSection(
        index: Int,
        inputValue: String?,
        type: ElectricSectionType
    ): Boolean {
        return when (type) {

            ElectricSectionType.ACCEPTED -> {
                val accepted = inputValue?.toDoubleOrNull()
                val delivery = electricSectionListState[index].delivery.data?.toDoubleOrNull()

                delivery?.let { del ->
                    accepted?.let { acc ->
                        if (acc > del) {
                            electricSectionListState[index] = electricSectionListState[index].copy(
                                errorMessage = "Принято больше чем сдано"
                            )
                            return false
                        }
                    }
                }
                true
            }

            ElectricSectionType.DELIVERY -> {
                val accepted = electricSectionListState[index].accepted.data?.toDoubleOrNull()
                val delivery = inputValue?.toDoubleOrNull()

                accepted?.let { acc ->
                    delivery?.let { del ->
                        if (del < acc) {
                            electricSectionListState[index] = electricSectionListState[index].copy(
                                errorMessage = "Сдано меньше чем принято"
                            )
                            return false
                        }
                    }
                }
                true
            }

            ElectricSectionType.RECOVERY_ACCEPTED -> {
                val accepted = inputValue?.toDoubleOrNull()
                val delivery =
                    electricSectionListState[index].recoveryDelivery.data?.toDoubleOrNull()

                delivery?.let { del ->
                    accepted?.let { acc ->
                        if (acc > del) {
                            electricSectionListState[index] = electricSectionListState[index].copy(
                                errorMessage = "Принято больше чем сдано"
                            )
                            return false
                        }
                    }
                }
                true
            }

            ElectricSectionType.RECOVERY_DELIVERY -> {
                val accepted =
                    electricSectionListState[index].recoveryAccepted.data?.toDoubleOrNull()
                val delivery = inputValue?.toDoubleOrNull()

                accepted?.let { acc ->
                    delivery?.let { del ->
                        if (del < acc) {
                            electricSectionListState[index] = electricSectionListState[index].copy(
                                errorMessage = "Сдано меньше чем принято"
                            )
                            return false
                        }
                    }
                }
                true
            }
        }
    }

    private fun onDieselSectionEvent(event: DieselSectionEvent) {
        when (event) {
            is DieselSectionEvent.EnteredAccepted -> {
                dieselSectionListState[event.index] = dieselSectionListState[event.index].copy(
                    accepted = dieselSectionListState[event.index].accepted.copy(
                        data = event.data
                    )
                )
            }

            is DieselSectionEvent.EnteredDelivery -> {
                dieselSectionListState[event.index] = dieselSectionListState[event.index].copy(
                    delivery = dieselSectionListState[event.index].delivery.copy(
                        data = event.data
                    )
                )
            }

            is DieselSectionEvent.EnteredCoefficient -> {
                dieselSectionListState[event.index] = dieselSectionListState[event.index].copy(
                    coefficient = dieselSectionListState[event.index].coefficient.copy(
                        data = event.data?.str()
                    )
                )
            }

            is DieselSectionEvent.EnteredRefuel -> {
                dieselSectionListState[event.index] = dieselSectionListState[event.index].copy(
                    refuel = dieselSectionListState[event.index].coefficient.copy(
                        data = event.data?.str()
                    )
                )
            }

            is DieselSectionEvent.FocusChange -> {
                when (event.fieldName) {
                    DieselSectionType.ACCEPTED -> {
                        val isValid = validateDieselSection(
                            index = event.index,
                            inputValue = dieselSectionListState[event.index].accepted.data,
                            type = DieselSectionType.ACCEPTED
                        )
                        dieselSectionListState[event.index] =
                            dieselSectionListState[event.index].copy(
                                formValid = isValid
                            )
                    }

                    DieselSectionType.DELIVERY -> {
                        val isValid = validateDieselSection(
                            index = event.index,
                            inputValue = dieselSectionListState[event.index].delivery.data,
                            type = DieselSectionType.DELIVERY
                        )
                        dieselSectionListState[event.index] =
                            dieselSectionListState[event.index].copy(
                                formValid = isValid
                            )
                    }

                    DieselSectionType.COEFFICIENT -> {
                        val isValid = validateDieselSection(
                            index = event.index,
                            inputValue = dieselSectionListState[event.index].coefficient.data,
                            type = DieselSectionType.COEFFICIENT
                        )
                        dieselSectionListState[event.index] =
                            dieselSectionListState[event.index].copy(
                                formValid = isValid
                            )
                        saveCoefficient(dieselSectionListState[event.index].coefficient.data)
                    }

                    DieselSectionType.REFUEL -> {
                        val isValid = validateDieselSection(
                            index = event.index,
                            inputValue = dieselSectionListState[event.index].refuel.data,
                            type = DieselSectionType.REFUEL
                        )
                        dieselSectionListState[event.index] =
                            dieselSectionListState[event.index].copy(
                                formValid = isValid
                            )
                    }
                }
            }
        }
    }

    private fun validateDieselSection(
        index: Int,
        inputValue: String?,
        type: DieselSectionType
    ): Boolean {
        return when (type) {
            DieselSectionType.ACCEPTED -> {
                val accepted = inputValue?.toDoubleOrNull()
                val delivery = dieselSectionListState[index].delivery.data?.toDoubleOrNull()
                val refuel = dieselSectionListState[index].refuel.data?.toDoubleOrNull()

                val result = CalculationEnergy.getTotalFuelConsumption(accepted, delivery, refuel)
                result?.let {
                    if (it < 0) {
                        dieselSectionListState[index] = dieselSectionListState[index].copy(
                            errorMessage = "Принял меньше чем сдал"
                        )
                        return false
                    }
                }
                true
            }

            DieselSectionType.DELIVERY -> {
                val accepted = dieselSectionListState[index].accepted.data?.toDoubleOrNull()
                val delivery = inputValue?.toDoubleOrNull()
                val refuel = dieselSectionListState[index].refuel.data?.toDoubleOrNull()

                val result = CalculationEnergy.getTotalFuelConsumption(accepted, delivery, refuel)
                result?.let {
                    if (it < 0) {
                        dieselSectionListState[index] = dieselSectionListState[index].copy(
                            errorMessage = "Сдал больше чем принял"
                        )
                        return false
                    }
                }
                true
            }

            DieselSectionType.COEFFICIENT -> {
                val coefficient = inputValue?.toDoubleOrNull()
                coefficient?.let {
                    if (it > 1) {
                        dieselSectionListState[index] = dieselSectionListState[index].copy(
                            errorMessage = "Коэффициент больше 1.0"
                        )
                        return false
                    }
                }
                true
            }

            DieselSectionType.REFUEL -> {
                val accepted = dieselSectionListState[index].accepted.data?.toDoubleOrNull()
                val delivery = dieselSectionListState[index].delivery.data?.toDoubleOrNull()
                val refuel = inputValue?.toDoubleOrNull()

                val result = CalculationEnergy.getTotalFuelConsumption(accepted, delivery, refuel)
                result?.let {
                    if (it < 0) {
                        dieselSectionListState[index] = dieselSectionListState[index].copy(
                            errorMessage = "Не хватает экипировки"
                        )
                        return false
                    }
                }
                true
            }
        }
    }
}