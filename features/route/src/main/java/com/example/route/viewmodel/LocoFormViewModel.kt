package com.example.route.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.ResultState
import com.example.domain.entities.route.LocoType
import com.example.domain.entities.route.Locomotive
import com.example.domain.entities.route.SectionDiesel
import com.example.domain.use_cases.LocomotiveUseCase
import com.example.domain.util.addOrReplace
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

    private val _uiState = MutableStateFlow(LocoFormUiState())
    val uiState = _uiState.asStateFlow()

    private var loadLocoJob: Job? = null
    private var saveLocoJob: Job? = null

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

    init {
        if (locoId == NULLABLE_ID) {
            currentLoco = Locomotive(basicId = basicId)
        } else {
            loadLoco(locoId!!)
        }
    }

    private fun loadLoco(id: String) {
        if (locoId == currentLoco?.locoId) return
        loadLocoJob?.cancel()
        loadLocoJob = locomotiveUseCase.getLocoById(id).onEach { routeState ->
            _uiState.update {
                if (routeState is ResultState.Success) {
                    currentLoco = routeState.data
                }
                it.copy(locoDetailState = routeState)
            }
        }.launchIn(viewModelScope)
    }

    fun saveLoco() {
        val state = _uiState.value.locoDetailState
        if (state is ResultState.Success) {
            state.data?.let { loco ->
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

    fun setFuelAccepted(sectionDiesel: SectionDiesel, value: String) {
        val locoDetailState = _uiState.value.locoDetailState

        if (locoDetailState is ResultState.Success) {
            locoDetailState.data?.let {
                val sectionList = it.dieselSectionList
                val section = sectionList.find { section ->
                    section.sectionId == sectionDiesel.sectionId
                }
                section?.let { sec ->
                    sec.acceptedFuel = value.toDoubleOrNull()
                    sectionList.addOrReplace(sec)
                }
            }
        }
    }

    fun setFuelDelivery(sectionDiesel: SectionDiesel, value: String) {

    }

    fun deleteSection(sectionDiesel: SectionDiesel) {

    }

}