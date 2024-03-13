package com.example.route.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.ResultState
import com.example.data_local.setting.DataStoreRepository
import com.example.domain.entities.route.*
import com.example.domain.use_cases.*
import com.example.route.Const.NULLABLE_ID
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.properties.Delegates

class FormViewModel(private val routeId: String?) : ViewModel(), KoinComponent {
    private val routeUseCase: RouteUseCase by inject()

    private val dataStoreRepository: DataStoreRepository by inject()

    private val _uiState = MutableStateFlow(RouteFormUiState())
    val uiState = _uiState.asStateFlow()

    private var loadRouteJob: Job? = null
    private var saveRouteJob: Job? = null
    private var loadSettingsJob: Job? = null
    private var deleteLocoJob: Job? = null
    private var deleteTrainJob: Job? = null
    private var deletePassengerJob: Job? = null

    private var isNewRoute by Delegates.notNull<Boolean>()
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
                )
            }
        }

    private var minTimeRest by mutableStateOf<Long?>(null)

    init {
        if (routeId == NULLABLE_ID) {
            currentRoute = Route()
            isNewRoute = true
        } else {
            loadRoute(routeId!!)
            isNewRoute = false
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
        loadSettingsJob = dataStoreRepository.getMinTimeRest().onEach {
            minTimeRest = it
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

    fun preSaveRoute() {
        val state = _uiState.value.routeDetailState
        if (state is ResultState.Success) {
            state.data?.let { route ->
                saveRouteJob?.cancel()
                saveRouteJob = routeUseCase.saveRoute(route).onEach { saveRouteState ->
                    if (saveRouteState is ResultState.Success) {
                        subscribeToChanges(route.basicData.id)
                        changesHave()
                    }
                }.launchIn(viewModelScope)
            }
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
        if (isNewRoute) {
            val state = _uiState.value.routeDetailState
            if (state is ResultState.Success) {
                state.data?.let { route ->
                    routeUseCase.removeRoute(route).onEach { result ->
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

    fun showConfirmDialog(isShow: Boolean) {
        _uiState.update {
            it.copy(confirmExitDialogShow = isShow)
        }
    }

    fun clearRoute() {
        currentRoute = if (routeId == null) {
            Route()
        } else {
            Route(BasicData(id = routeId))
        }
        changesHave()
    }

    private fun subscribeToChanges(routeId: String) {
        loadRouteJob?.cancel()
        loadRouteJob =
            routeUseCase.routeDetails(routeId).onEach { routeState ->
                _uiState.update {
                    if (routeState is ResultState.Success) {
                        currentRoute = routeState.data
                    }
                    it.copy(routeDetailState = routeState)
                }
            }.launchIn(viewModelScope)
    }

    fun setNumber(value: String) {
        currentRoute = currentRoute?.copy(
            basicData = currentRoute!!.basicData.copy(
                number = value
            )
        )
        changesHave()
    }


    fun setNotes(text: String) {
        currentRoute = currentRoute?.copy(
            basicData = currentRoute!!.basicData.copy(
                notes = text
            )
        )
        changesHave()
    }

    fun setTimeStartWork(timeInLong: Long?) {
        currentRoute = currentRoute?.copy(
            basicData = currentRoute!!.basicData.copy(
                timeStartWork = timeInLong
            )
        )
        isValidTime()
        changesHave()
    }

    fun setTimeEndWork(timeInLong: Long?) {
        currentRoute = currentRoute?.copy(
            basicData = currentRoute!!.basicData.copy(
                timeEndWork = timeInLong
            )
        )
        isValidTime()
        changesHave()
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
        changesHave()
    }

    private fun getMinTimeRest(route: Route) {
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
        deleteLocoJob?.cancel()
        deleteLocoJob = routeUseCase.removeLoco(locomotive).launchIn(viewModelScope)
    }

    fun onDeleteTrain(train: Train) {
        deleteTrainJob?.cancel()
        deleteTrainJob = routeUseCase.removeTrain(train).launchIn(viewModelScope)
    }

    fun onDeletePassenger(passenger: Passenger) {
        deletePassengerJob?.cancel()
        deletePassengerJob = routeUseCase.removePassenger(passenger).launchIn(viewModelScope)
    }
}