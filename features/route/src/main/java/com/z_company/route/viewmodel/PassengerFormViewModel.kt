package com.z_company.route.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.core.ResultState
import com.z_company.domain.entities.route.Passenger
import com.z_company.domain.use_cases.PassengerUseCase
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
import kotlin.properties.Delegates

class PassengerFormViewModel(
    passengerId: String?,
    basicId: String
) : ViewModel(), KoinComponent {
    private val passengerUseCase: PassengerUseCase by inject()
    private val _uiState = MutableStateFlow(PassengerFormUiState())
    val uiState = _uiState.asStateFlow()

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
        val state = _uiState.value.passengerDetailState
        if (state is ResultState.Success) {
            state.data?.let { passenger ->
                savePassengerJob?.cancel()
                savePassengerJob =
                    passengerUseCase.savePassenger(passenger).onEach { resultState ->
                        _uiState.update {
                            it.copy(
                                savePassengerState = resultState
                            )
                        }
                    }.launchIn(viewModelScope)
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
            it.copy(errorTimeState = null)
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
        val timeValid = isTimeValid()
        if (!timeValid) {
            _uiState.update {
                it.copy(
                    resultTime = null,
                    errorTimeState = ResultState.Success(Unit),
                    formValid = false
                )
            }
        } else {
            val arrivalTime = currentPassenger?.timeArrival
            val departureTime = currentPassenger?.timeDeparture
            val resultTime = arrivalTime - departureTime
            _uiState.update {
                it.copy(
                    resultTime = resultTime,
                    formValid = true
                )
            }
        }
    }

    private fun isTimeValid(): Boolean {
        val arrivalTime = currentPassenger?.timeArrival
        val departureTime = currentPassenger?.timeDeparture
        return departureTime.compareWithNullable(arrivalTime)
    }
}