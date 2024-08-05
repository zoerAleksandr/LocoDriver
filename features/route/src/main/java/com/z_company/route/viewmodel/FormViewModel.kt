package com.z_company.route.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.core.ResultState
import com.z_company.core.util.CalculateNightTime
import com.z_company.data_local.SharedPreferenceStorage
import com.z_company.domain.entities.NightTime
import com.z_company.domain.entities.route.*
import com.z_company.domain.use_cases.*
import com.z_company.domain.util.minus
import com.z_company.route.Const.NULLABLE_ID
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.properties.Delegates
import com.z_company.domain.util.plus

class FormViewModel(private val routeId: String?) : ViewModel(), KoinComponent {
    private val routeUseCase: RouteUseCase by inject()
    private val locoUseCase: LocomotiveUseCase by inject()
    private val trainUseCase: TrainUseCase by inject()
    private val passengerUseCase: PassengerUseCase by inject()
    private val photoUseCase: PhotoUseCase by inject()
    private val settingsUseCase: SettingsUseCase by inject()
    private val sharedPreferenceStorage: SharedPreferenceStorage by inject()

    private val _uiState = MutableStateFlow(RouteFormUiState())
    val uiState = _uiState.asStateFlow()

    private var loadRouteJob: Job? = null
    private var saveRouteJob: Job? = null
    private var loadSettingsJob: Job? = null
    private var deleteLocoJob: Job? = null
    private var deleteTrainJob: Job? = null
    private var deletePassengerJob: Job? = null
    private var deletePhotoJob: Job? = null

    private val deletedLocoList = mutableListOf<Locomotive>()
    private val deletedTrainList = mutableListOf<Train>()
    private val deletedPassengerList = mutableListOf<Passenger>()
    private val deletedPhotoList = mutableListOf<Photo>()

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

    var minTimeRest by mutableStateOf<Long?>(null)
    var nightTime: NightTime? = null
    var defaultWorkTime: Long? = null


    init {
        val changeHave = sharedPreferenceStorage.tokenIsChangesHave()
        _uiState.update {
            it.copy(
                changesHaveState = changeHave
            )
        }

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
                    currentRoute?.let { route ->
                        calculateRestTime(route)
                        getNightTimeInRoute(route)
                        calculationPassengerTime(route)
                    }
                }
                it.copy(routeDetailState = routeState)
            }
        }.launchIn(viewModelScope)
    }

    private fun loadSettings() {
        loadSettingsJob?.cancel()
        loadSettingsJob = settingsUseCase.getCurrentSettings().onEach { result ->
            if (result is ResultState.Success) {
                minTimeRest = result.data?.minTimeRest
                nightTime = result.data?.nightTime
                defaultWorkTime = result.data?.defaultWorkTime
                currentRoute?.let { route ->
                    calculateRestTime(route)
                    getNightTimeInRoute(route)
                }
            }
        }.launchIn(viewModelScope)
    }

    fun saveRoute() {
        if (uiState.value.errorMessage == null) {
            val state = _uiState.value.routeDetailState
            if (state is ResultState.Success) {
                state.data?.let { route ->
                    saveRouteJob?.cancel()
                    saveRouteJob = routeUseCase.saveRoute(route).onEach { saveRouteState ->
                        if (saveRouteState is ResultState.Success) {
                            deletedLocoList.forEach { locomotive ->
                                deleteLocoJob?.cancel()
                                deleteLocoJob =
                                    locoUseCase.removeLoco(locomotive).launchIn(viewModelScope)
                            }
                            deletedTrainList.forEach { train ->
                                deleteTrainJob?.cancel()
                                deleteTrainJob =
                                    trainUseCase.removeTrain(train).launchIn(viewModelScope)
                            }
                            deletedPassengerList.forEach { passenger ->
                                deletePassengerJob?.cancel()
                                deletePassengerJob = passengerUseCase.removePassenger(passenger)
                                    .launchIn(viewModelScope)
                            }
                            deletedPhotoList.forEach { photo ->
                                viewModelScope.launch {
                                    deletePhotoJob?.cancel()
                                    deletePhotoJob = photoUseCase.removePhoto(photo).launchIn(viewModelScope)
                                }.join()
                            }
                        }
                        _uiState.update {
                            it.copy(saveRouteState = saveRouteState)
                        }
                    }.launchIn(viewModelScope)
                }
                sharedPreferenceStorage.setTokenIsChangeHave(false)
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
        sharedPreferenceStorage.setTokenIsChangeHave(true)
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
        } else {
            _uiState.update {
                it.copy(
                    confirmExitDialogShow = false,
                    exitFromScreen = true
                )
            }
        }
        sharedPreferenceStorage.setTokenIsChangeHave(false)
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
                number = value.ifBlank { null }
            )
        )
        changesHave()
    }

    fun setNotes(text: String) {
        currentRoute = currentRoute?.copy(
            basicData = currentRoute!!.basicData.copy(
                notes = text.ifBlank { null }
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
        if (currentRoute?.basicData?.timeEndWork == null && defaultWorkTime != null){
            val endNightTime = timeInLong + defaultWorkTime
            currentRoute = currentRoute?.copy(
                basicData = currentRoute!!.basicData.copy(
                    timeEndWork = endNightTime
                )
            )
        }
        calculateRestTime(currentRoute!!)
        getNightTimeInRoute(currentRoute!!)
        isValidTime()
        changesHave()
    }

    fun setTimeEndWork(timeInLong: Long?) {
        currentRoute = currentRoute?.copy(
            basicData = currentRoute!!.basicData.copy(
                timeEndWork = timeInLong
            )
        )
        calculateRestTime(currentRoute!!)
        getNightTimeInRoute(currentRoute!!)
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
            calculateRestTime(currentRoute!!)
        }
        changesHave()
    }

    private fun calculateRestTime(route: Route) {
        getMinTimeRest(route)
        getFullRest(route)
    }

    private fun calculationPassengerTime(route: Route) {
        var passengerTime by mutableLongStateOf(0L)
        route.passengers.forEach { passenger ->
            passengerTime =
                passengerTime.plus(
                    (passenger.timeArrival - passenger.timeDeparture) ?: 0L
                )
        }
        _uiState.update {
            it.copy(
                passengerTime = passengerTime
            )
        }
    }

    private fun getNightTimeInRoute(route: Route) {
        nightTime?.let { time ->
            _uiState.update {
                it.copy(
                    nightTime = CalculateNightTime.getNightTime(
                        startMillis = route.basicData.timeStartWork,
                        endMillis = route.basicData.timeEndWork,
                        hourStart = time.startNightHour,
                        minuteStart = time.startNightMinute,
                        hourEnd = time.endNightHour,
                        minuteEnd = time.endNightMinute
                    )
                )
            }
        }
    }

    private fun getMinTimeRest(route: Route) {
        minTimeRest?.let { minTimeRest ->
            val timeRest = routeUseCase.getMinRest(route, minTimeRest)
            _uiState.update {
                it.copy(minTimeRest = timeRest)
            }
        }
    }

    private fun getFullRest(route: Route) {
        minTimeRest?.let { minTimeRest ->
            val fullTimeRest = routeUseCase.fullRest(route, minTimeRest)
            _uiState.update {
                it.copy(fullTimeRest = fullTimeRest)
            }
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
        deletedLocoList.add(locomotive)
        val locomotiveList = currentRoute?.locomotives.apply {
            this?.remove(locomotive)
        }
        locomotiveList?.let {
            currentRoute = currentRoute?.copy(
                locomotives = locomotiveList
            )
        }
        changesHave()
    }

    fun onDeleteTrain(train: Train) {
        deletedTrainList.add(train)
        val trainsList = currentRoute?.trains.apply {
            this?.remove(train)
        }
        trainsList?.let {
            currentRoute = currentRoute?.copy(
                trains = trainsList
            )
        }
        changesHave()
    }

    fun onDeletePassenger(passenger: Passenger) {
        deletedPassengerList.add(passenger)
        val passengerList = currentRoute?.passengers.apply {
            this?.remove(passenger)
        }
        passengerList?.let {
            currentRoute = currentRoute?.copy(
                passengers = passengerList
            )
        }
        changesHave()
    }

    fun onDeletePhoto(photo: Photo) {
        deletedPhotoList.add(photo)
        val photoList = currentRoute?.photos.apply {
            this?.remove(photo)
        }
        photoList?.let {
            currentRoute = currentRoute?.copy(
                photos = photoList
            )
        }
        changesHave()
    }
}