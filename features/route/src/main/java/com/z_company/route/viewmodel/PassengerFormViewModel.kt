package com.z_company.route.viewmodel

import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.core.ResultState
import com.z_company.domain.entities.route.Passenger
import com.z_company.domain.entities.route.Route
import com.z_company.domain.use_cases.PassengerUseCase
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.domain.util.addAllOrSkip
import com.z_company.domain.util.compareWithNullable
import com.z_company.route.Const.NULLABLE_ID
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import com.z_company.domain.util.minus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.properties.Delegates

class PassengerFormViewModel(
    passengerId: String?,
    basicId: String
) : ViewModel(), KoinComponent {
    private val passengerUseCase: PassengerUseCase by inject()
    private val settingsUseCase: SettingsUseCase by inject()
    private val routeUseCase: RouteUseCase by inject()

    private val _uiState = MutableStateFlow(PassengerFormUiState())
    val uiState = _uiState.asStateFlow()

    private var route: Route = Route()

    private var loadPassengerJob: Job? = null
    private var savePassengerJob: Job? = null

    private var isNewPassenger by Delegates.notNull<Boolean>()

    var currentPassenger: Passenger?
        get() {
            return _uiState.value.passengerDetailState.let {
                if (it is ResultState.Success) it.data else null
            }
        }
        private set(value) {
            _uiState.update {
                it.copy(
                    passengerDetailState = ResultState.Success(value),
                )
            }
        }

    init {
        if (passengerId == NULLABLE_ID) {
            isNewPassenger = true
            currentPassenger = Passenger(basicId = basicId)
        } else {
            isNewPassenger = false
            loadPassenger(passengerId!!)
        }
        viewModelScope.launch {
            combine(
                settingsUseCase.getFlowCurrentSettingsState(),
                routeUseCase.routeDetails(basicId)
            ) { settingState, routeState ->
                if (settingState is ResultState.Success) {
                    settingState.data?.let { settings ->
                        initStationList = settings.stationList.toMutableStateList()
                    }
                }

                if (routeState is ResultState.Success) {
                    routeState.data?.let {
                        route = it
                    }
                }

            }.collect {}
        }
    }

    private fun loadPassenger(passengerId: String) {
        loadPassengerJob?.cancel()
        loadPassengerJob = passengerUseCase.getPassengerById(passengerId).onEach { result ->
            _uiState.update {
                it.copy(
                    passengerDetailState = result
                )
            }
            if (result is ResultState.Success) {
                currentPassenger = result.data
                formValidTime()
            }
        }.launchIn(viewModelScope)
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
        if (isNewPassenger) {
            val state = _uiState.value.passengerDetailState
            if (state is ResultState.Success) {
                state.data?.let { passenger ->
                    passengerUseCase.removePassenger(passenger).onEach { result ->
                        if (result is ResultState.Success) {
                            _uiState.update {
                                it.copy(
                                    confirmExitDialogShow = false,
                                    exitFromScreen = true
                                )
                            }
                        }
                    }.launchIn(viewModelScope)
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

    fun savePassenger() {
        if (uiState.value.errorMessage == null) {
            val state = _uiState.value.passengerDetailState
            if (state is ResultState.Success) {
                state.data?.let { passenger ->
                    savePassengerJob?.cancel()
                    savePassengerJob =
                        passengerUseCase.savePassenger(passenger).onEach { resultState ->
                            saveStationsName(passenger.stationDeparture, passenger.stationArrival)
                            _uiState.update {
                                it.copy(
                                    savePassengerState = resultState
                                )
                            }
                        }.launchIn(viewModelScope)
                }
            }
        }
    }

    fun resetSaveState() {
        _uiState.update {
            it.copy(savePassengerState = null)
        }
    }

    fun resetErrorState() {
        _uiState.update {
            it.copy(errorMessage = null)
        }
    }

    fun clearAllField() {
        currentPassenger = currentPassenger?.copy(
            trainNumber = null,
            stationDeparture = null,
            stationArrival = null,
            timeDeparture = null,
            timeArrival = null,
            notes = null
        )
        formValidTime()
        changesHave()
    }

    fun setNumberTrain(number: String) {
        currentPassenger = currentPassenger?.copy(
            trainNumber = number.ifBlank { null }
        )
        changesHave()
    }

    fun setStationDeparture(station: String) {
        currentPassenger = currentPassenger?.copy(
            stationDeparture = station.ifBlank { null }
        )
        changesHave()
    }

    fun setStationArrival(station: String) {
        currentPassenger = currentPassenger?.copy(
            stationArrival = station.ifBlank { null }
        )
        changesHave()
    }

    fun setTimeDeparture(time: Long?) {
        currentPassenger = currentPassenger?.copy(
            timeDeparture = time
        )
        formValidTime()
        changesHave()
    }

    fun setTimeArrival(time: Long?) {
        currentPassenger = currentPassenger?.copy(
            timeArrival = time
        )
        formValidTime()
        changesHave()
    }

    fun setNotes(notes: String) {
        currentPassenger = currentPassenger?.copy(
            notes = notes.ifBlank { null }
        )
        changesHave()
    }

    private fun formValidTime() {
        viewModelScope.launch {
            route = route.copy(
                passengers = mutableListOf(
                    Passenger(
                        timeArrival = currentPassenger?.timeArrival,
                        timeDeparture = currentPassenger?.timeDeparture
                    )
                )
            )
            val validState = routeUseCase.isValidPassenger(route).first()

            if (validState is ResultState.Error) {
                withContext(Dispatchers.Main) {
                    _uiState.update { formState ->
                        formState.copy(
                            resultTime = null,
                            formValid = false,
                            errorMessage = validState.entity.message
                        )
                    }
                }

            }
            if (validState is ResultState.Success) {
                val arrivalTime = currentPassenger?.timeArrival
                val departureTime = currentPassenger?.timeDeparture
                val resultTime = arrivalTime - departureTime

                withContext(Dispatchers.Main) {
                    _uiState.update { formState ->
                        formState.copy(
                            resultTime = resultTime,
                            formValid = true,
                            errorMessage = null
                        )
                    }
                }
            }
        }
    }

    private var initStationList = mutableListOf<String>()

    private var currentStationList: SnapshotStateList<String>
        get() {
            return uiState.value.stationList
        }
        private set(value) {
            _uiState.update {
                it.copy(
                    stationList = value
                )
            }
        }

    private fun saveStationsName(departureStation: String?, arrivalStation: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            val stations = mutableListOf<String>()
            departureStation?.let {
                stations.add(it)
            }
            arrivalStation?.let {
                stations.add(it)
            }
            if (stations.isNotEmpty()) {
                settingsUseCase.setStations(stations)
            }
        }
    }

    fun changeExpandMenuDepartureStation(isExpand: Boolean) {
        _uiState.update {
            it.copy(
                isExpandMenuDepartureStation = isExpand
            )
        }
    }

    fun changeExpandMenuArrivalStation(isExpand: Boolean) {
        _uiState.update {
            it.copy(
                isExpandMenuArrivalStation = isExpand
            )
        }
    }

    fun removeStationName(value: String) {
        viewModelScope.launch {
            initStationList.remove(value)
            settingsUseCase.removeStation(value)
        }
    }

    fun onChangedDropDownContentDepartureStation(value: String) {
        if (value.isEmpty()) {
            changeExpandMenuDepartureStation(false)
            currentStationList.addAllOrSkip(initStationList)
        } else {
            currentStationList.clear()
            val newStationList =
                initStationList
                    .filter { it.startsWith(prefix = value, ignoreCase = true) }
                    .filterNot { it == value }
                    .toMutableStateList()
            newStationList.forEach { st ->
                currentStationList.add(st)
                changeExpandMenuDepartureStation(true)
            }
        }
    }

    fun onChangedDropDownContentArrivalStation(value: String) {
        if (value.isEmpty()) {
            changeExpandMenuArrivalStation(false)
            currentStationList.addAllOrSkip(initStationList)
        } else {
            currentStationList.clear()
            val newStationList =
                initStationList
                    .filter { it.startsWith(prefix = value, ignoreCase = true) }
                    .filterNot { it == value }
                    .toMutableStateList()
            newStationList.forEach { st ->
                currentStationList.add(st)
                changeExpandMenuArrivalStation(true)
            }
        }
    }
}