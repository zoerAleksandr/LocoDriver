package com.z_company.route.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.core.ResultState
import com.z_company.domain.util.CalculateNightTime
import com.z_company.data_local.SharedPreferenceStorage
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.NightTime
import com.z_company.domain.entities.route.*
import com.z_company.domain.entities.route.UtilsForEntities.getHomeRest
import com.z_company.domain.use_cases.*
import com.z_company.domain.util.minus
import com.z_company.route.Const.NULLABLE_ID
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import com.z_company.domain.util.plus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import ru.rustore.sdk.review.RuStoreReviewManagerFactory
import ru.rustore.sdk.review.model.ReviewInfo
import java.util.UUID

class FormViewModel(
    private val routeId: String?,
    private val isCopy: Boolean = false,
    private val application: Application,
) : ViewModel(),
    KoinComponent {
    private val routeUseCase: RouteUseCase by inject()
    private val locoUseCase: LocomotiveUseCase by inject()
    private val trainUseCase: TrainUseCase by inject()
    private val passengerUseCase: PassengerUseCase by inject()
    private val photoUseCase: PhotoUseCase by inject()
    private val settingsUseCase: SettingsUseCase by inject()
    private val sharedPreferenceStorage: SharedPreferenceStorage by inject()

    val reviewManager = RuStoreReviewManagerFactory.create(application.applicationContext)

    private val _uiState = MutableStateFlow(RouteFormUiState())
    val uiState = _uiState.asStateFlow()

    private val _dialogRestUiState = MutableStateFlow(DialogRestUiState())
    val dialogRestUiState = _dialogRestUiState.asStateFlow()

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

    private var isNewRoute = if (routeId == NULLABLE_ID) {
        true
    } else {
        false
    }
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

    private var currentMonthOfYear: MonthOfYear? = null

    private var nightTime: NightTime? = null
    private var defaultWorkTime: Long? = null
    private var usingDefaultWorkTime: Boolean = false

    init {
        if (routeId == NULLABLE_ID) {
            currentRoute = Route()
        } else {
            loadRoute(routeId!!, isCopy)
        }
        viewModelScope.launch(Dispatchers.IO) {
            prepareReviewDialog()
        }

        val changeHave = sharedPreferenceStorage.tokenIsChangesHave()
        _uiState.update {
            it.copy(
                changesHaveState = changeHave
            )
        }

        loadSettings()
    }

    fun resetSaveState() {
        _uiState.update {
            it.copy(saveRouteState = null)
        }
    }

    private fun loadRoute(id: String, isCopy: Boolean) {
        if (routeId == currentRoute?.basicData?.id) return
        loadRouteJob?.cancel()
        loadRouteJob = routeUseCase.routeDetails(id).onEach { routeState ->
            if (routeState is ResultState.Success) {
                currentRoute = routeState.data
                currentRoute?.let { route ->
                    calculateRestTime(route)
                    getNightTimeInRoute(route)
                    calculationPassengerTime(route)
                }
            }
            _uiState.update {
                it.copy(
                    routeDetailState = routeState,
                    isCopy = isCopy
                )
            }
            loadRouteJob?.cancel()
        }.launchIn(viewModelScope)
    }

    private var reviewInfo: ReviewInfo? = null
    private suspend fun getRoutesCount(): Int {
        delay(10L)
        return routeUseCase.listRouteWithDeleting().size
    }

    private fun isShowReviewDialog(count: Int): Boolean {
        return (count > 10 && count % 5 == 0)
    }
    private suspend fun prepareReviewDialog() = coroutineScope {
        val count = async { getRoutesCount() }.await()
        val isShow = isShowReviewDialog(count)
        if (isShow) {
            reviewManager.requestReviewFlow()
                .addOnSuccessListener { info ->
                    reviewInfo = info
                }
                .addOnFailureListener { throwable ->
                    Log.w("ZZZ", "prepareReviewDialog throwable = $throwable")
                }
        }
    }

    private fun showReviewDialog(reviewInfo: ReviewInfo) {
        reviewManager.launchReviewFlow(reviewInfo)
            .addOnSuccessListener {
                Log.i("ZZZ", "showReviewDialog Success")
            }
            .addOnFailureListener { throwable ->
                Log.w("ZZZ", "showReviewDialog Failure = $throwable")
            }
    }

    private fun loadSettings() {
        loadSettingsJob?.cancel()
        loadSettingsJob = settingsUseCase.getCurrentSettings().onEach { result ->
            if (result is ResultState.Success) {
                _dialogRestUiState.update {
                    it.copy(
                        minTimeRestPointOfTurnover = result.data?.minTimeRestPointOfTurnover,
                        minTimeHomeRest = result.data?.minTimeHomeRest
                    )
                }
                currentMonthOfYear = result.data?.selectMonthOfYear
                nightTime = result.data?.nightTime
                defaultWorkTime = result.data?.defaultWorkTime
                usingDefaultWorkTime = result.data?.usingDefaultWorkTime ?: false
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

                    var routeToSave = route
                    if (isCopy) {
                        val newBasicId = UUID.randomUUID().toString()
                        routeToSave = routeToSave.copy(
                            basicData = routeToSave.basicData.copy(id = newBasicId)
                        )
                        routeToSave.trains.forEach { train ->
                            train.trainId = UUID.randomUUID().toString()
                            train.basicId = newBasicId
                        }
                        routeToSave.locomotives.forEach { locomotive ->
                            locomotive.locoId = UUID.randomUUID().toString()
                            locomotive.basicId = newBasicId
                        }
                        routeToSave.passengers.forEach { passenger ->
                            passenger.passengerId = UUID.randomUUID().toString()
                            passenger.basicId = newBasicId
                        }

                    }

                    saveRouteJob?.cancel()
                    saveRouteJob = routeUseCase.saveRoute(routeToSave).onEach { saveRouteState ->
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
                                    deletePhotoJob =
                                        photoUseCase.removePhoto(photo).launchIn(viewModelScope)
                                }.join()
                            }
                            reviewInfo?.let { info ->
                                showReviewDialog(info)
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
        if (currentRoute?.basicData?.timeEndWork == null && usingDefaultWorkTime) {

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
        viewModelScope.launch {
            currentRoute = currentRoute?.copy(
                basicData = currentRoute!!.basicData.copy(
                    restPointOfTurnover = value
                )
            )
            currentRoute?.let { route ->
                calculateRestTime(route)
            }
            changesHave()
        }
    }

    private fun calculateRestTime(route: Route) {
        if (route.basicData.restPointOfTurnover) {
            getMinTimeRest(route)
            getFullRest(route)
        } else {
            calculationHomeRest(route)
        }
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

    private fun calculationHomeRest(route: Route) {
        val routesList = mutableListOf<Route>()
        viewModelScope.launch(Dispatchers.IO) {
            currentMonthOfYear?.let { monthOfYear ->
                this.launch {
                    routeUseCase.listRoutesByMonth(monthOfYear).collect { resultCurrentMonth ->
                        if (resultCurrentMonth is ResultState.Success) {
                            routesList.addAll(resultCurrentMonth.data)
                            this.cancel()
                        }
                    }
                }.join()
                this.launch {
                    val previousMonthOfYear = if (monthOfYear.month != 0) {
                        monthOfYear.copy(month = monthOfYear.month - 1)
                    } else {
                        monthOfYear.copy(
                            year = monthOfYear.year - 1,
                            month = 11
                        )
                    }
                    routeUseCase.listRoutesByMonth(previousMonthOfYear)
                        .collect { resultCurrentMonth ->
                            if (resultCurrentMonth is ResultState.Success) {
                                routesList.addAll(resultCurrentMonth.data)
                                this.cancel()
                            }
                        }
                }.join()
            }
            if (isNewRoute) {
                routesList.add(route)
            }

            val sortedRouteList = routesList.sortedBy {
                it.basicData.timeStartWork
            }.distinct()

            if (sortedRouteList.any { it.basicData.id == route.basicData.id }) {
                val homeRest = route.getHomeRest(
                    parentList = sortedRouteList,
                    minTimeHomeRest = dialogRestUiState.value.minTimeHomeRest
                )
                _dialogRestUiState.update {
                    it.copy(
                        untilTimeHomeRest = ResultState.Success(homeRest)
                    )
                }

            } else {
                _dialogRestUiState.update {
                    it.copy(
                        untilTimeHomeRest = ResultState.Success(null)
                    )
                }
            }
        }
    }

    private fun getMinTimeRest(route: Route) {
        val timeRest = routeUseCase.getMinRest(
            route = route,
            minTimeRest = dialogRestUiState.value.minTimeRestPointOfTurnover
        )
        _dialogRestUiState.update {
            it.copy(minUntilTimeRestPointOfTurnover = ResultState.Success(timeRest))
        }
    }

    private fun getFullRest(route: Route) {
        val fullTimeRest = routeUseCase.fullRest(
            route = route,
            minTimeRest = dialogRestUiState.value.minTimeRestPointOfTurnover
        )
        _dialogRestUiState.update {
            it.copy(fullUntilTimeRestPointOfTurnover = ResultState.Success(fullTimeRest))
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
        val locomotiveList = mutableListOf<Locomotive>()
        currentRoute?.locomotives?.let { collection ->
            locomotiveList.addAll(collection)
            locomotiveList.remove(locomotive)
        }

        currentRoute = currentRoute?.copy(
            locomotives = locomotiveList
        )
        changesHave()
    }

    fun onDeleteTrain(train: Train) {
        deletedTrainList.add(train)
        val trainsList = mutableListOf<Train>()
        currentRoute?.trains?.let { collection ->
            trainsList.addAll(collection)
            trainsList.remove(train)
        }
        currentRoute = currentRoute?.copy(
            trains = trainsList
        )

        changesHave()
    }

    fun onDeletePassenger(passenger: Passenger) {
        deletedPassengerList.add(passenger)
        val passengerList = mutableListOf<Passenger>()

        currentRoute?.passengers?.let { collection ->
            passengerList.addAll(collection)
            passengerList.remove(passenger)
        }
        currentRoute = currentRoute?.copy(
            passengers = passengerList
        )

        changesHave()
    }
}