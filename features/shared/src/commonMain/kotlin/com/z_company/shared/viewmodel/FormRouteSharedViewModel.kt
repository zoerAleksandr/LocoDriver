package com.z_company.shared.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.core.ResultState
import com.z_company.domain.entities.route.Locomotive
import com.z_company.domain.entities.route.Passenger
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.Train
import com.z_company.domain.entities.route.UtilsForEntities.getPassengerTime
import com.z_company.domain.entities.route.UtilsForEntities.getWorkTime
import com.z_company.domain.entities.route.UtilsForEntities.getWorkingTimeOnAHoliday
import com.z_company.domain.entities.setting.UserSettings
import com.z_company.domain.use_cases.LocomotiveUseCase
import com.z_company.domain.use_cases.PassengerUseCase
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.domain.use_cases.TrainUseCase
import com.z_company.domain.util.CalculateNightTime
import com.z_company.shared.util.ConverterLongToTime
import com.z_company.shared.util.DateAndTimeConverter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Shared KMP ViewModel for the route create/edit form.
 *
 * Based on Android FormViewModel with constructor injection.
 * Supports load/save route, update fields, manage child entities,
 * settings-based time formatting, and route calculations.
 */
class FormRouteSharedViewModel(
    private val routeUseCase: RouteUseCase,
    private val locoUseCase: LocomotiveUseCase,
    private val trainUseCase: TrainUseCase,
    private val passengerUseCase: PassengerUseCase,
    private val settingsUseCase: SettingsUseCase,
) : ViewModel() {

    private val _route = MutableStateFlow<Route?>(null)
    val route: StateFlow<Route?> = _route.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSaved = MutableStateFlow(false)
    val isSaved: StateFlow<Boolean> = _isSaved.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Settings-based state
    private val _dateAndTimeConverter = MutableStateFlow<DateAndTimeConverter?>(null)
    val dateAndTimeConverter: StateFlow<DateAndTimeConverter?> = _dateAndTimeConverter.asStateFlow()

    private val _userSettings = MutableStateFlow<UserSettings?>(null)
    val userSettings: StateFlow<UserSettings?> = _userSettings.asStateFlow()

    // Route calculations
    private val _nightTimeMs = MutableStateFlow<Long?>(null)
    val nightTimeMs: StateFlow<Long?> = _nightTimeMs.asStateFlow()

    private val _holidayTimeMs = MutableStateFlow<Long?>(null)
    val holidayTimeMs: StateFlow<Long?> = _holidayTimeMs.asStateFlow()

    private val _passengerTimeMs = MutableStateFlow<Long?>(null)
    val passengerTimeMs: StateFlow<Long?> = _passengerTimeMs.asStateFlow()

    private val _workTimeMs = MutableStateFlow<Long?>(null)
    val workTimeMs: StateFlow<Long?> = _workTimeMs.asStateFlow()

    // Home rest calculation
    private val _homeRestDuration = MutableStateFlow<Long?>(null)
    val homeRestDuration: StateFlow<Long?> = _homeRestDuration.asStateFlow()

    private var loadJob: Job? = null
    private val deletedLocos = mutableListOf<Locomotive>()
    private val deletedTrains = mutableListOf<Train>()
    private val deletedPassengers = mutableListOf<Passenger>()

    init {
        // Load settings
        viewModelScope.launch {
            settingsUseCase.getUserSettingFlow().collectLatest { settings ->
                _userSettings.value = settings
                _dateAndTimeConverter.value = DateAndTimeConverter(settings)
            }
        }

        // Reactive calculations when route or settings change
        viewModelScope.launch(Dispatchers.Default) {
            combine(_route, _userSettings) { route, settings ->
                Pair(route, settings)
            }.collectLatest { (route, settings) ->
                if (route != null && settings != null) {
                    calculateRouteStats(route, settings)
                }
            }
        }
    }

    private suspend fun calculateRouteStats(route: Route, settings: UserSettings) {
        // Work time
        _workTimeMs.value = route.getWorkTime()

        // Passenger time
        _passengerTimeMs.value = route.getPassengerTime()

        // Night time
        val nightTime = settings.nightTime
        val nightMillis = CalculateNightTime.getNightTime(
            startMillis = route.basicData.timeStartWork,
            endMillis = route.basicData.timeEndWork,
            hourStart = nightTime.startNightHour,
            minuteStart = nightTime.startNightMinute,
            hourEnd = nightTime.endNightHour,
            minuteEnd = nightTime.endNightMinute,
            offsetInMoscow = settings.timeZone,
            breakStartMillis = route.basicData.timeStartBreak,
            breakEndMillis = route.basicData.timeEndBreak,
        ).first()
        _nightTimeMs.value = nightMillis

        // Holiday time
        val holidayTime = listOf(route).getWorkingTimeOnAHoliday(
            settings.selectMonthOfYear, settings.timeZone,
        ).first()
        _holidayTimeMs.value = holidayTime

        // Home rest (workTime * 2.6, minimum minTimeHomeRest)
        calculateHomeRest(route, settings)
    }

    private fun calculateHomeRest(route: Route, settings: UserSettings) {
        val endWork = route.basicData.timeEndWork ?: return
        val startWork = route.basicData.timeStartWork ?: return
        val workTime = endWork - startWork
        if (workTime <= 0) return
        val rawDuration = (workTime.toDouble() * 2.6).toLong()
        _homeRestDuration.value = maxOf(rawDuration, settings.minTimeHomeRest)
    }

    /**
     * Loads route by [routeId]. If null, creates a new empty route.
     */
    fun loadRoute(routeId: String?) {
        loadJob?.cancel()
        _isSaved.value = false
        _errorMessage.value = null
        deletedLocos.clear()
        deletedTrains.clear()
        deletedPassengers.clear()

        if (routeId == null) {
            _route.value = Route()
            _isLoading.value = false
            return
        }

        loadJob = viewModelScope.launch {
            _isLoading.value = true
            routeUseCase.routeDetails(routeId).collect { result ->
                when (result) {
                    is ResultState.Success -> {
                        _route.value = result.data ?: Route()
                        _isLoading.value = false
                    }
                    is ResultState.Error -> {
                        _errorMessage.value = result.entity.message ?: "Ошибка загрузки маршрута"
                        _isLoading.value = false
                    }
                    is ResultState.Loading -> _isLoading.value = true
                }
            }
        }
    }

    // --- Time formatting ---

    fun convertTimeToStringFormat(timeToLong: Long?): String {
        _userSettings.value?.let { settings ->
            return if (settings.isDecimalTime) {
                ConverterLongToTime.getTimeInStringDecimalFormat(timeToLong)
            } else {
                ConverterLongToTime.getTimeInStringFormat(timeToLong)
            }
        }
        return ConverterLongToTime.getTimeInStringFormat(timeToLong)
    }

    // --- Basic fields ---

    fun updateNumber(value: String) = updateRoute {
        copy(basicData = basicData.copy(number = value.ifBlank { null }))
    }

    fun updateNotes(value: String) = updateRoute {
        copy(basicData = basicData.copy(notes = value.ifBlank { null }))
    }

    fun toggleFavorite(value: Boolean) = updateRoute {
        copy(basicData = basicData.copy(isFavorite = value))
    }

    fun setOnePersonOperation(checked: Boolean) = updateRoute {
        copy(basicData = basicData.copy(isOnePersonOperation = checked))
    }

    // --- Time fields ---

    fun setTimeStartWork(time: Long?) = updateRoute {
        copy(basicData = basicData.copy(timeStartWork = time))
    }

    fun setTimeEndWork(time: Long?) = updateRoute {
        copy(basicData = basicData.copy(timeEndWork = time))
    }

    fun setTimeStartBreak(time: Long?) = updateRoute {
        copy(basicData = basicData.copy(timeStartBreak = time))
    }

    fun setTimeEndBreak(time: Long?) = updateRoute {
        copy(basicData = basicData.copy(timeEndBreak = time))
    }

    fun setRestPointOfTurnover(isRest: Boolean) = updateRoute {
        copy(basicData = basicData.copy(restPointOfTurnover = isRest))
    }

    // --- Delete child entities ---

    fun deleteLoco(loco: Locomotive) {
        _route.update { r ->
            r?.copy(locomotives = r.locomotives.filter { it != loco }.toMutableList())
        }
        deletedLocos.add(loco)
    }

    fun deleteTrain(train: Train) {
        _route.update { r ->
            r?.copy(trains = r.trains.filter { it != train }.toMutableList())
        }
        deletedTrains.add(train)
    }

    fun deletePassenger(passenger: Passenger) {
        _route.update { r ->
            r?.copy(passengers = r.passengers.filter { it != passenger }.toMutableList())
        }
        deletedPassengers.add(passenger)
    }

    // --- Save ---

    fun saveRoute() {
        val currentRoute = _route.value ?: return
        viewModelScope.launch {
            routeUseCase.saveRoute(currentRoute).collectLatest { result ->
                when (result) {
                    is ResultState.Success -> {
                        // Remove deleted child entities
                        deletedLocos.forEach { locoUseCase.removeLoco(it).collect {} }
                        deletedTrains.forEach { trainUseCase.removeTrain(it).collect {} }
                        deletedPassengers.forEach { passengerUseCase.removePassenger(it).collect {} }
                        _isSaved.value = true
                    }
                    is ResultState.Error -> _errorMessage.value =
                        result.entity.message ?: "Ошибка сохранения"
                    is ResultState.Loading -> { /* waiting */ }
                }
            }
        }
    }

    /**
     * Pre-saves the current route (used before navigating to child forms).
     * After save, subscribes to route updates so changes from child forms are reflected.
     */
    fun preSaveRoute(onSaved: (String) -> Unit = {}) {
        val currentRoute = _route.value ?: return
        viewModelScope.launch {
            routeUseCase.saveRoute(currentRoute).collect { result ->
                if (result is ResultState.Success) {
                    val basicId = currentRoute.basicData.id
                    // Subscribe to route changes
                    subscribeToChanges(basicId)
                    onSaved(basicId)
                }
            }
        }
    }

    private fun subscribeToChanges(routeId: String) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            routeUseCase.routeDetails(routeId).collect { result ->
                if (result is ResultState.Success) {
                    _route.value = result.data
                }
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    private inline fun updateRoute(block: Route.() -> Route) {
        _route.value = _route.value?.block()
    }
}
