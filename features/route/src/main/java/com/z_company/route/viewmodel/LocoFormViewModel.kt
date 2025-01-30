package com.z_company.route.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.core.ResultState
import com.z_company.domain.entities.UserSettings
import com.z_company.domain.entities.route.LocoType
import com.z_company.domain.entities.route.Locomotive
import com.z_company.domain.entities.route.SectionDiesel
import com.z_company.domain.entities.route.SectionElectric
import com.z_company.domain.use_cases.LocomotiveUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.domain.util.CalculationEnergy
import com.z_company.domain.util.addAllOrSkip
import com.z_company.domain.util.addOrReplace
import com.z_company.domain.util.str
import com.z_company.route.Const.NULLABLE_ID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.math.BigDecimal
import kotlin.properties.Delegates

class LocoFormViewModel(
    private val locoId: String?,
    basicId: String
) : ViewModel(), KoinComponent {
    private val locomotiveUseCase: LocomotiveUseCase by inject()
    private val settingsUseCase: SettingsUseCase by inject()
    private val _uiState = MutableStateFlow(LocoFormUiState())
    val uiState = _uiState.asStateFlow()

    private var loadLocoJob: Job? = null
    private var saveLocoJob: Job? = null
    private var saveCoefficientJob: Job? = null

    private val locomotiveSeriesList = mutableStateListOf<String>()
    private var isNewLoco by Delegates.notNull<Boolean>()

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
    private var currentSetting: UserSettings?
        get() {
            return _uiState.value.settingsState.let {
                if (it is ResultState.Success) it.data else null
            }
        }
        set(value) {
            _uiState.update {
                it.copy(
                    settingsState = ResultState.Success(value)
                )
            }
        }

    private fun changesHave() {
        if (!_uiState.value.changesHaveState) {
            _uiState.update {
                it.copy(
                    changesHaveState = true
                )
            }
        }
    }

    fun exitWithoutSaving() {
        if (isNewLoco) {
            val state = _uiState.value.locoDetailState
            if (state is ResultState.Success) {
                state.data?.let { loco ->
                    viewModelScope.launch {
                        locomotiveUseCase.removeLoco(loco).collect { result ->
                            if (result is ResultState.Success) {
                                _uiState.update {
                                    it.copy(
                                        confirmExitDialogShow = false,
                                        exitFromScreen = true
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            _uiState.update {
                it.copy(
                    confirmExitDialogShow = false,
                    exitFromScreen = true
                )
            }
        }
    }

    fun checkBeforeExitTheScreen() {
        if (_uiState.value.changesHaveState) {
            _uiState.update {
                it.copy(confirmExitDialogShow = true)
            }
        } else {
            _uiState.update {
                it.copy(exitFromScreen = true)
            }
        }
    }

    fun changeShowConfirmDialog(isShow: Boolean) {
        _uiState.update {
            it.copy(confirmExitDialogShow = isShow)
        }
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
                        data = section.acceptedEnergy?.toPlainString() ?: "",
                        type = ElectricSectionType.ACCEPTED
                    ),
                    delivery = ElectricSectionFieldState(
                        data = section.deliveryEnergy?.toPlainString() ?: "",
                        type = ElectricSectionType.DELIVERY
                    ),
                    recoveryAccepted = ElectricSectionFieldState(
                        data = section.acceptedRecovery?.toPlainString() ?: "",
                        type = ElectricSectionType.RECOVERY_ACCEPTED
                    ),
                    recoveryDelivery = ElectricSectionFieldState(
                        data = section.deliveryRecovery?.toPlainString() ?: "",
                        type = ElectricSectionType.RECOVERY_DELIVERY
                    ),
                    resultVisibility = isVisibilityResultElectricSection(
                        section.acceptedEnergy?.toPlainString() ?: "",
                        section.deliveryEnergy?.toPlainString() ?: "",
                        section.acceptedRecovery?.toPlainString() ?: "",
                        section.deliveryRecovery?.toPlainString() ?: ""
                    ),
                    expandItemState = isExpandElectricItem(
                        section.acceptedRecovery,
                        section.deliveryRecovery
                    )
                )
            )
        }
    }

    init {
        viewModelScope.launch {
            loadSetting().join()

            if (locoId == NULLABLE_ID) {
                isNewLoco = true
                currentLoco = Locomotive(
                    basicId = basicId,
                    type = currentSetting?.defaultLocoType ?: LocoType.ELECTRIC
                )
            } else {
                isNewLoco = false
                loadLoco(locoId!!)
            }
        }
    }

    private suspend fun loadSetting(): Job {
        return viewModelScope.launch {
            settingsUseCase.getFlowCurrentSettingsState().collect {
                if (it is ResultState.Success) {
                    currentSetting = it.data
                    currentSetting?.let { setting ->
                        currentSetting = setting
                        locomotiveSeriesList.addAllOrSkip(setting.locomotiveSeriesList.toMutableStateList())
                    }
                    this.cancel()
                }
            }
        }
    }

    private fun saveCoefficient(data: String?) {
        viewModelScope.launch {
            saveCoefficientJob?.cancel()
            val double = data?.toDoubleOrNull()
            double?.let {
                settingsUseCase.setDieselCoefficient(double).collect {}
            }
        }
    }

    private fun saveLocomotiveSeries(value: String) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsUseCase.setLocomotiveSeries(value)
        }
    }

    private var mutableLocomotiveSeriesList = mutableStateListOf<String>()
        .also {
            it.addAll(locomotiveSeriesList)
        }

    var seriesList: SnapshotStateList<String>
        get() {
            return mutableLocomotiveSeriesList
        }
        set(value) {
            mutableLocomotiveSeriesList = value
        }

    fun changeExpandedMenu(value: Boolean) {
        _uiState.update {
            it.copy(
                isExpandedDropDownMenuSeries = value
            )
        }
    }

    fun onChangedDropDownContent(value: String) {
        if (value.isEmpty()) {
            changeExpandedMenu(false)
            mutableLocomotiveSeriesList.addAllOrSkip(locomotiveSeriesList)
        } else {
            mutableLocomotiveSeriesList.clear()
            val newSeriesList =
                locomotiveSeriesList
                    .filter { it.startsWith(prefix = value, ignoreCase = true) }
                    .filterNot { it == value }
                    .toMutableStateList()
            newSeriesList.forEach { series ->
                mutableLocomotiveSeriesList.add(series)
                changeExpandedMenu(true)
            }
        }
    }

    fun removeSeries(value: String) {
        viewModelScope.launch {
            seriesList.remove(value)
            mutableLocomotiveSeriesList.remove(value)
            settingsUseCase.removeLocomotiveSeries(value)
        }
    }

    private fun loadLoco(id: String) {
        if (locoId == currentLoco?.locoId) return
        loadLocoJob?.cancel()
        loadLocoJob = viewModelScope.launch {
            locomotiveUseCase.getLocoById(id).collect { routeState ->
                _uiState.update {
                    if (routeState is ResultState.Success) {
                        currentLoco = routeState.data
                        routeState.data?.let { loco ->
                            setSectionData(loco)
                        }
                    }
                    it.copy(locoDetailState = routeState)
                }
            }
        }
    }

    fun saveLoco() {
        val state = _uiState.value.locoDetailState
        if (state is ResultState.Success) {
            state.data?.let { loco ->
                loco.series?.let { series ->
                    saveLocomotiveSeries(series)
                }
                when (loco.type) {
                    LocoType.ELECTRIC -> {
                        loco.electricSectionList = electricSectionListState.map { state ->
                            SectionElectric(
                                sectionId = state.sectionId,
                                acceptedEnergy = state.accepted.data?.toBigDecimalOrNull(),
                                deliveryEnergy = state.delivery.data?.toBigDecimalOrNull(),
                                acceptedRecovery = state.recoveryAccepted.data?.toBigDecimalOrNull(),
                                deliveryRecovery = state.recoveryDelivery.data?.toBigDecimalOrNull()
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
                saveLocoJob = viewModelScope.launch {
                    locomotiveUseCase.saveLocomotive(loco).collect { saveLocoState ->
                        _uiState.update {
                            it.copy(saveLocoState = saveLocoState)
                        }
                    }
                }
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
            number = number.ifBlank { null }
        )
        changesHave()
    }

    fun setSeries(series: String) {
        currentLoco = currentLoco?.copy(
            series = series.ifBlank { null }
        )
        changesHave()
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
        changesHave()
    }

    fun setStartAcceptedTime(timeInLong: Long?) {
        currentLoco = currentLoco?.copy(
            timeStartOfAcceptance = timeInLong
        )
        isValidAcceptedTime()
        changesHave()
    }

    fun setEndAcceptedTime(timeInLong: Long?) {
        currentLoco = currentLoco?.copy(
            timeEndOfAcceptance = timeInLong
        )
        isValidAcceptedTime()
        changesHave()
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
        changesHave()
    }

    fun setStartDeliveryTime(timeInLong: Long?) {
        currentLoco = currentLoco?.copy(
            timeStartOfDelivery = timeInLong
        )
        isValidDeliveryTime()
        changesHave()
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

    fun setCoefficient(index: Int, coefficient: String?) {
        onDieselSectionEvent(
            DieselSectionEvent.EnteredCoefficient(
                index, coefficient?.toDoubleOrNull()
            )
        )
        changesHave()
    }

    fun deleteSectionDiesel(dieselSectionFormState: DieselSectionFormState) {
        dieselSectionListState.remove(dieselSectionFormState)
        changesHave()
    }

    fun setEnergyAccepted(index: Int, data: String?) {
        onElectricSectionEvent(
            ElectricSectionEvent.EnteredAccepted(
                index = index, data = data
            )
        )
    }

    fun setEnergyDelivery(index: Int, data: String?) {
        onElectricSectionEvent(
            ElectricSectionEvent.EnteredDelivery(
                index = index, data = data
            )
        )
    }

    fun setRecoveryAccepted(index: Int, data: String?) {
        onElectricSectionEvent(
            ElectricSectionEvent.EnteredRecoveryAccepted(
                index = index, data = data
            )
        )
    }

    fun setRecoveryDelivery(index: Int, data: String?) {
        onElectricSectionEvent(
            ElectricSectionEvent.EnteredRecoveryDelivery(
                index = index, data = data
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
        changesHave()
    }

    fun addingSectionDiesel() {
        val coefficient = currentSetting?.lastEnteredDieselCoefficient
        dieselSectionListState.add(
            DieselSectionFormState(
                sectionId = SectionDiesel().sectionId,
                coefficient = DieselSectionFieldState(
                    coefficient.str(),
                    DieselSectionType.COEFFICIENT
                )
            )
        )
        electricSectionListState.clear()
        changesHave()
    }

    fun addingSectionElectric() {
        electricSectionListState.add(
            ElectricSectionFormState(
                sectionId = SectionElectric().sectionId
            )
        )
        dieselSectionListState.clear()
        changesHave()
    }

    private fun isExpandElectricItem(
        acceptedRecovery: BigDecimal?,
        deliveryRecovery: BigDecimal?,
    ): Boolean {
        return (acceptedRecovery != null || deliveryRecovery != null)
    }

    fun isExpandElectricItem(index: Int, isExpand: Boolean) {
        electricSectionListState[index] = electricSectionListState[index].copy(
            expandItemState = isExpand
        )
    }

    private fun onElectricSectionEvent(event: ElectricSectionEvent) {
        changesHave()
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
                val isExpand = isExpandElectricItem(
                    acceptedRecovery?.toBigDecimalOrNull(),
                    deliveryRecovery?.toBigDecimalOrNull()
                )
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
        return ((accepted != null && delivery != null)
                || (acceptedRecovery != null && deliveryRecovery != null))
    }

    private fun validateElectricSection(
        index: Int,
        inputValue: String?,
        type: ElectricSectionType
    ): Boolean {
        return when (type) {

            ElectricSectionType.ACCEPTED -> {
                val delivery = electricSectionListState[index].delivery.data

                delivery?.let { del ->
                    inputValue?.let { acc ->
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
                val accepted = electricSectionListState[index].accepted.data

                accepted?.let { acc ->
                    inputValue?.let { del ->
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
                val delivery =
                    electricSectionListState[index].recoveryDelivery.data

                delivery?.let { del ->
                    inputValue?.let { acc ->
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
                    electricSectionListState[index].recoveryAccepted.data

                accepted?.let { acc ->
                    inputValue?.let { del ->
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
        changesHave()
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