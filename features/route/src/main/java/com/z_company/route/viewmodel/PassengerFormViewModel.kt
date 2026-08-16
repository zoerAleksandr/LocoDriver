package com.z_company.route.viewmodel

import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.core.ResultState
import com.z_company.core.util.ConverterLongToTime
import com.z_company.core.util.DateAndTimeConverter
import com.z_company.domain.entities.route.Passenger
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.setting.UserSettings
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
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

    private val routeBasicId: String = basicId

    private val _uiState = MutableStateFlow(PassengerFormUiState())
    val uiState = _uiState.asStateFlow()

    private val _userSetting = MutableStateFlow(UserSettings())
    val userSetting = _userSetting.asStateFlow()

    private var route: Route = Route()

    var timeZoneText: String = "GMT+3"

    private var loadPassengerJob: Job? = null
    private var savePassengerJob: Job? = null
    private var autoSaveJob: Job? = null

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
        viewModelScope.launch {
            if (passengerId == NULLABLE_ID) {
                isNewPassenger = true
                currentPassenger = Passenger(basicId = basicId)
            } else {
                isNewPassenger = false
                loadPassenger(passengerId!!)
            }
            val initJob = this.launch {
                _userSetting.value = settingsUseCase.getUserSettingFlow().first()
                timeZoneText = settingsUseCase.getTimeZone(userSetting.value.timeZone)
                _uiState.update {
                    it.copy(
                        dateAndTimeConverter = DateAndTimeConverter(_userSetting.value)
                    )
                }
            }
            initJob.join()

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
                        _uiState.update { state ->
                            state.copy(
                                routeWorkStart = it.basicData.timeStartWork,
                                routeWorkEnd = it.basicData.timeEndWork
                            )
                        }
                    }
                }

            }.collect {}
        }
    }

    fun convertTimeToStringFormat(timeToLong: Long?): String {
        userSetting.value.let { settings ->
            return if (settings.isDecimalTime) {
                ConverterLongToTime.getTimeInStringDecimalFormat(timeToLong)
            } else {
                ConverterLongToTime.getTimeInStringFormat(timeToLong)
            }
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
        triggerAutoSave()
    }

    private fun triggerAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(500)
            savePassenger()
        }
    }

    /**
     * Пустой ли пассажир: ни одно значимое поле не заполнено. Используется, чтобы
     * не сохранять абсолютно пустой объект, если пользователь открыл форму нового
     * пассажира и вышел, ничего не введя.
     */
    private fun isPassengerEmpty(passenger: Passenger): Boolean {
        return passenger.trainNumber.isNullOrBlank() &&
                passenger.stationDeparture.isNullOrBlank() &&
                passenger.stationArrival.isNullOrBlank() &&
                passenger.timeArrival == null &&
                passenger.timeDeparture == null &&
                passenger.notes.isNullOrBlank() &&
                !passenger.isWorkStartByArrival
    }

    override fun onCleared() {
        autoSaveJob?.cancel()
        savePassengerJob?.cancel()
        CoroutineScope(NonCancellable + Dispatchers.IO).launch {
            val state = _uiState.value.passengerDetailState
            if (state is ResultState.Success) {
                state.data?.let { passenger ->
                    // Не сохраняем абсолютно пустой новый объект (форма открыта и закрыта
                    // без ввода). Существующего пассажира сохраняем всегда.
                    if (isNewPassenger && isPassengerEmpty(passenger)) return@launch
                    syncWorkStartToRoute(passenger)
                    passengerUseCase.savePassenger(passenger).collect {}
                }
            }
        }
        super.onCleared()
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
                    savePassengerJob = viewModelScope.launch {
                        // Флаг пассажира и явка родительского маршрута должны
                        // попасть в БД как одна логическая операция.
                        syncWorkStartToRoute(passenger)
                        passengerUseCase.savePassenger(passenger).collect { resultState ->
                            saveStationsName(passenger.stationDeparture, passenger.stationArrival)
                            _uiState.update {
                                it.copy(
                                    savePassengerState = resultState
                                )
                            }
                        }
                    }
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
        // Если этот пассажир — источник «явки по прибытию», держим timeStartWork
        // маршрута синхронным с временем прибытия.
        currentPassenger?.let { p ->
            if (p.isWorkStartByArrival) {
                viewModelScope.launch { syncWorkStartToRoute(p) }
            }
        }
    }

    /**
     * «Явка по прибытию пассажиром». Включение делает прибытие этого пассажира
     * началом рабочего времени маршрута (timeStartWork = timeArrival) и снимает
     * флаг с остальных пассажиров (единственная точка отсчёта). Выключение просто
     * снимает флаг — timeStartWork остаётся, пользователь может изменить явку вручную.
     */
    fun setWorkStartByArrival(enabled: Boolean) {
        val updated = currentPassenger?.copy(isWorkStartByArrival = enabled) ?: return
        currentPassenger = updated
        changesHave()
        viewModelScope.launch { syncWorkStartToRoute(updated) }
    }

    /**
     * Переносит состояние «явки по прибытию» текущего пассажира в маршрут:
     * обновляет [com.z_company.domain.entities.route.BasicData.timeStartWork],
     * снимает флаг с других пассажиров и сохраняет маршрут целиком (что также
     * помечает его несинхронизированным для отправки на сервер).
     * Использует свежий маршрут из БД, т.к. поле [route] мутируется в formValidTime().
     */
    private suspend fun syncWorkStartToRoute(current: Passenger) {
        // routeDetails эмитит Loading, затем Success — ждём именно Success.
        val routeId = current.basicId.ifBlank { routeBasicId }
        val routeState = routeUseCase.routeDetails(routeId).first { it is ResultState.Success }
        val freshRoute = (routeState as? ResultState.Success)?.data ?: return

        val mergedPassengers = freshRoute.passengers
            .map { p ->
                when {
                    p.passengerId == current.passengerId -> current
                    // Взаимоисключающе: включение на одном снимает флаг с остальных.
                    current.isWorkStartByArrival -> p.copy(isWorkStartByArrival = false)
                    else -> p
                }
            }
            .toMutableList()
            .also { list ->
                if (list.none { it.passengerId == current.passengerId }) list.add(current)
            }

        val currentBasic = freshRoute.basicData
        val newBasic = if (current.isWorkStartByArrival && current.timeArrival != null) {
            // Включение: запоминаем прежнюю явку (если ещё не сохранена) и ставим прибытие.
            currentBasic.copy(
                timeStartWorkBeforeArrival = currentBasic.timeStartWorkBeforeArrival
                    ?: currentBasic.timeStartWork,
                timeStartWork = current.timeArrival
            )
        } else if (!current.isWorkStartByArrival) {
            // Выключение: возвращаем прежнюю явку и очищаем резерв.
            currentBasic.copy(
                timeStartWork = currentBasic.timeStartWorkBeforeArrival ?: currentBasic.timeStartWork,
                timeStartWorkBeforeArrival = null
            )
        } else {
            currentBasic
        }

        val newRoute = freshRoute.copy(
            basicData = newBasic,
            passengers = mergedPassengers
        )
        // ResultState.flowRequest сначала эмитит Loading. Ожидаем конечный
        // результат, иначе first() отменит flow до выполнения записи.
        routeUseCase.saveRoute(newRoute).first { it !is ResultState.Loading }
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
                        timeDeparture = currentPassenger?.timeDeparture,
                        // Флаг нужен валидатору: для «явки по прибытию» отправление
                        // до начала работы допустимо (RouteUseCase.isValidPassenger).
                        isWorkStartByArrival = currentPassenger?.isWorkStartByArrival ?: false
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
        // Обновляем и отображаемый список (currentStationList), иначе открытая шторка
        // не убирает станцию визуально; плюс удаляем из общих настроек.
        initStationList.remove(value)
        currentStationList.remove(value)
        viewModelScope.launch {
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
