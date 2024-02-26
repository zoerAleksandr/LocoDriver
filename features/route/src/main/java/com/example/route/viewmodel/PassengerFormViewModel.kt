package com.example.route.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.ResultState
import com.example.domain.entities.route.Passenger
import com.example.domain.use_cases.PassengerUseCase
import com.example.domain.util.compareWithNullable
import com.example.route.Const.NULLABLE_ID
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import com.example.domain.util.minus

class PassengerFormViewModel constructor(
    private val passengerId: String?,
    private val basicId: String
) : ViewModel(), KoinComponent {
    private val passengerUseCase: PassengerUseCase by inject()
    private val _uiState = MutableStateFlow(PassengerFormUiState())
    val uiState = _uiState.asStateFlow()

    private var loadPassengerJob: Job? = null
    private var savePassengerJob: Job? = null
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
            currentPassenger = Passenger(basicId = basicId)
        } else {
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
    }

    fun setNumberTrain(number: String) {
        currentPassenger = currentPassenger?.copy(
            trainNumber = number
        )
    }

    fun setStationDeparture(station: String) {
        currentPassenger = currentPassenger?.copy(
            stationDeparture = station
        )
    }

    fun setStationArrival(station: String) {
        currentPassenger = currentPassenger?.copy(
            stationArrival = station
        )
    }

    fun setTimeDeparture(time: Long?) {
        currentPassenger = currentPassenger?.copy(
            timeDeparture = time
        )
        formValidTime()
    }

    fun setTimeArrival(time: Long?) {
        currentPassenger = currentPassenger?.copy(
            timeArrival = time
        )
        formValidTime()
    }

    fun setNotes(notes: String) {
        currentPassenger = currentPassenger?.copy(
            notes = notes
        )
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