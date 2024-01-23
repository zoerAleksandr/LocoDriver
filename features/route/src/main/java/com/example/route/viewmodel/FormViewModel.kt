package com.example.route.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.ResultState
import com.example.domain.entities.UserSettings
import com.example.domain.entities.route.Locomotive
import com.example.domain.entities.route.Notes
import com.example.domain.entities.route.Passenger
import com.example.domain.entities.route.Route
import com.example.domain.entities.route.Train
import com.example.domain.use_cases.RouteUseCase
import com.example.domain.use_cases.SettingsUseCase
import com.example.route.Const.NULLABLE_ID
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class FormViewModel constructor(private val routeId: String?) : ViewModel(), KoinComponent {
    private val routeUseCase: RouteUseCase by inject()
    private val settingsUseCase: SettingsUseCase by inject()

    private val _uiState = MutableStateFlow(RouteFormUiState())
    val uiState = _uiState.asStateFlow()

    private val userSettingState = MutableStateFlow<ResultState<UserSettings?>>(ResultState.Loading)
    private var loadRouteJob: Job? = null
    private var saveRouteJob: Job? = null
    private var loadSettingsJob: Job? = null

    var currentRoute: Route?
        get() {
            return _uiState.value.routeDetailState.let {
                if (it is ResultState.Success) it.data else null
            }
        }
        private set(value) {
            _uiState.update {
                it.copy(
                    routeDetailState = ResultState.Success(value),
                    locoListState = ResultState.Success(value?.locomotives),
                    trainListState = ResultState.Success(value?.trains),
                    passengerListState = ResultState.Success(value?.passengers),
                    notesState = ResultState.Success(value?.notes)
                )
            }
        }

    private var setting: UserSettings?
        get() {
            return userSettingState.value.let {
                if (it is ResultState.Success) it.data else null
            }
        }
        private set(value) {
            userSettingState.update {
                ResultState.Success(value)
            }
        }

    init {
        if (routeId == NULLABLE_ID) {
            currentRoute = Route()
        } else {
            loadRoute(routeId!!)
        }
        loadSettings()
    }

    fun resetSaveState() {
        _uiState.update {
            it.copy(saveRouteState = null)
        }
    }

    private fun loadRoute(id: String) {
        if (routeId == currentRoute?.basicData?.id) return
        loadRouteJob?.cancel()
        loadRouteJob = routeUseCase.routeDetails(id).onEach { routeState ->
            _uiState.update {
                if (routeState is ResultState.Success) {
                    currentRoute = routeState.data
                }
                it.copy(routeDetailState = routeState)
            }
        }.launchIn(viewModelScope)
    }

    private fun loadSettings() {
        loadSettingsJob?.cancel()
        loadSettingsJob = settingsUseCase.loadSettings().onEach { settingState ->
            if (settingState is ResultState.Success) {
                setting = settingState.data
            }
        }.launchIn(viewModelScope)
    }

    fun saveRoute() {
        val state = _uiState.value.routeDetailState
        if (state is ResultState.Success) {
            state.data?.let { route ->
                saveRouteJob?.cancel()
                saveRouteJob = routeUseCase.saveRoute(route).onEach { saveRouteState ->
                    _uiState.update {
                        it.copy(saveRouteState = saveRouteState)
                    }
                }.launchIn(viewModelScope)
            }
        }
    }

    fun setNumber(value: String) {
        currentRoute = currentRoute?.copy(
            basicData = currentRoute!!.basicData.copy(
                number = value
            )
        )
    }

    fun setTimeStartWork(timeInLong: Long?) {
        currentRoute = currentRoute?.copy(
            basicData = currentRoute!!.basicData.copy(
                timeStartWork = timeInLong
            )
        )
        isValidTime()
    }

    fun setTimeEndWork(timeInLong: Long?) {
        currentRoute = currentRoute?.copy(
            basicData = currentRoute!!.basicData.copy(
                timeEndWork = timeInLong
            )
        )
        isValidTime()
    }

    fun setRestValue(value: Boolean) {
        currentRoute = currentRoute?.copy(
            basicData = currentRoute!!.basicData.copy(
                restPointOfTurnover = value
            )
        )
        if (value) {
            getMinTimeRest(currentRoute!!)
            getFullRest(currentRoute!!)
        }
    }

    private fun getMinTimeRest(route: Route) {
        val minTimeRest = setting?.minTimeRest
        val timeRest = routeUseCase.getMinRest(route, minTimeRest)
        _uiState.update {
            it.copy(minTimeRest = timeRest)
        }
    }

    private fun getFullRest(route: Route) {
        val fullTimeRest = routeUseCase.fullRest(route)
        _uiState.update {
            it.copy(fullTimeRest = fullTimeRest)
        }
    }

    private fun isValidTime() {
        val routeDetailState = _uiState.value.routeDetailState

        if (routeDetailState is ResultState.Success) {
            routeDetailState.data?.let {
                val errorMessage = if (!routeUseCase.isTimeWorkValid(it)) {
                    "Проверьте начало и окончание работы"
                } else {
                    null
                }
                _uiState.update { formState ->
                    formState.copy(errorMessage = errorMessage)
                }
            }
        }
    }

    fun onDeleteLoco(locomotive: Locomotive) {
        routeUseCase.removeLoco(locomotive)
    }

    fun onDeleteTrain(train: Train) {
        routeUseCase.removeTrain(train)
    }

    fun onDeletePassenger(passenger: Passenger) {
        routeUseCase.removePassenger(passenger)
    }

    fun onDeleteNotes(notes: Notes) {
        routeUseCase.removeNotes(notes)
    }

    fun createChildEntityLocomotive(): Locomotive {
        return Locomotive(basicId = currentRoute?.basicData?.id!!)
    }
}