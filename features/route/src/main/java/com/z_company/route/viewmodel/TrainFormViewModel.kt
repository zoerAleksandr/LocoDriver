package com.z_company.route.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.core.ResultState
import com.z_company.core.util.DateAndTimeConverter
import com.z_company.core.util.TimeManager
import com.z_company.domain.entities.setting.ServicePhase
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.Station
import com.z_company.domain.entities.route.Train
import com.z_company.domain.entities.route.TrainAssist
import com.z_company.domain.repositories.SharedPreferencesRepositories
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.domain.use_cases.TrainUseCase
import com.z_company.domain.util.addAllOrSkip
import com.z_company.domain.util.addOrReplace
import com.z_company.route.Const.NULLABLE_ID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.z_company.core.sendToSentry
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.collections.toMutableList
import kotlin.properties.Delegates

class TrainFormViewModel(
    trainId: String?,
    basicId: String
) : ViewModel(), KoinComponent {
    private val timeManager = TimeManager()
    private val sharedPreferenceStorage: SharedPreferencesRepositories by inject()
    private val trainUseCase: TrainUseCase by inject()
    private val settingsUseCase: SettingsUseCase by inject()
    private val routeUseCase: RouteUseCase by inject()
    private var route: Route = Route()
    private val _uiState = MutableStateFlow(TrainFormUiState())
    val uiState = _uiState.asStateFlow()

    private var loadTrainJob: Job? = null
    private var saveTrainJob: Job? = null
    private var autoSaveJob: Job? = null

    var timeZoneText: String = "GMT+3"

    private var isNewTrain by Delegates.notNull<Boolean>()

    private val stationNameList = mutableStateListOf<String>()

    // Locomotive series for dropdown (same pattern as LocoFormViewModel)
    private val locomotiveSeriesList = mutableStateListOf<String>()
    private var mutableFilteredSeriesList = mutableStateListOf<String>()

    var dropDownSeriesList: SnapshotStateList<String>
        get() = mutableFilteredSeriesList
        set(value) { mutableFilteredSeriesList = value }
    var currentTrain: Train?
        get() {
            return _uiState.value.trainDetailState.let {
                if (it is ResultState.Success) it.data else null
            }
        }
        private set(value) {
            _uiState.update {
                it.copy(
                    trainDetailState = ResultState.Success(value),
                )
            }
        }

    private var stationsListState: SnapshotStateList<StationFormState>
        get() {
            return _uiState.value.stationsListState ?: mutableStateListOf()
        }
        set(value) {
            _uiState.update {
                it.copy(
                    stationsListState = value
                )
            }
        }

    private var servicePhaseList: SnapshotStateList<ServicePhase>
        get() {
            return _uiState.value.servicePhaseList
        }
        set(value) {
            _uiState.update {
                it.copy(
                    servicePhaseList = value
                )
            }
        }

    fun setSelectedServicePhase(servicePhase: ServicePhase?) {
        if (servicePhase != null) {
            setDistance(servicePhase.distance.toString())
            if (servicePhase != uiState.value.selectedServicePhase || uiState.value.selectedServicePhase == null) {
                if (stationsListState.isEmpty()) {
                    addingStation(stationName = servicePhase.departureStation)
                    addingStation(stationName = servicePhase.arrivalStation)
                } else {
                    // Обновить первую станцию
                    if (stationsListState.first().station.data != servicePhase.departureStation) {
                        stationsListState[0] = stationsListState[0].copy(
                            station = StationField(
                                data = servicePhase.departureStation,
                                type = StationDataType.NAME
                            )
                        )
                    }
                    // Обновить/добавить последнюю станцию
                    if (stationsListState.size == 1) {
                        addingStation(stationName = servicePhase.arrivalStation)
                    } else {
                        val lastIdx = stationsListState.lastIndex
                        if (stationsListState[lastIdx].station.data != servicePhase.arrivalStation) {
                            stationsListState[lastIdx] = stationsListState[lastIdx].copy(
                                station = StationField(
                                    data = servicePhase.arrivalStation,
                                    type = StationDataType.NAME
                                )
                            )
                        }
                    }
                    changesHave()
                }
            }
        } else {
            setDistance("")
        }
        _uiState.update {
            it.copy(
                selectedServicePhase = servicePhase
            )
        }
    }

    fun loadStationsSortOrder() {
        viewModelScope.launch {
            val isReversed = sharedPreferenceStorage.isReversedSortStationList() // Используйте ваш репозиторий
            _uiState.update { it.copy(isStationsReversed = isReversed) }
        }
    }

    fun toggleStationsSortOrder() {
        val newReversed = !_uiState.value.isStationsReversed
        _uiState.update { it.copy(isStationsReversed = newReversed) }
        sharedPreferenceStorage.toggleStationsSortOrder(newReversed)
    }

    init {
        viewModelScope.launch {
            try {
                loadStationsSortOrder()
                if (trainId == NULLABLE_ID) {
                    isNewTrain = true
                    currentTrain = Train(basicId = basicId)
                } else {
                    isNewTrain = false
                    loadTrain(trainId!!)
                }
                val initJob = this.launch {
                    val setting = settingsUseCase.getUserSettingFlow().first()
                    timeZoneText = "GMT+3"
                }
                initJob.join()

                // Load route once
                val routeState = routeUseCase.routeDetails(basicId).first()
                if (routeState is ResultState.Success) {
                    routeState.data?.let { route = it }
                }

                // Subscribe to settings changes (service phases can be updated)
                settingsUseCase.getFlowCurrentSettingsState().collect { settingState ->
                    if (settingState is ResultState.Success) {
                        settingState.data.let { settings ->
                            _uiState.update {
                                it.copy(
                                    dateAndTimeConverter = DateAndTimeConverter(settings)
                                )
                            }
                            stationNameList.addAllOrSkip(settings.stationList.toMutableStateList())
                            mutableStationList.addAllOrSkip(stationNameList)
                            locomotiveSeriesList.addAllOrSkip(settings.locomotiveSeriesList.toMutableStateList())
                            mutableFilteredSeriesList.addAllOrSkip(locomotiveSeriesList)
                            servicePhaseList.clear()
                            servicePhaseList.addAllOrSkip(settings.servicePhases.toMutableStateList())
                        }
                    }
                }
            } catch (e: Exception) {
                e.sendToSentry("TrainFormViewModel", "init")
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
        triggerAutoSave()
    }

    private fun triggerAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(500)
            saveTrainSilently()
        }
    }

    /** Автосохранение без диалога service-phase — вызывается только из triggerAutoSave/onCleared */
    private fun saveTrainSilently() {
        val state = _uiState.value.trainDetailState
        if (state is ResultState.Success) {
            state.data?.let { train ->
                train.servicePhase = uiState.value.selectedServicePhase
                train.stations = stationsListState.map { s ->
                    Station(
                        stationId = s.id,
                        trainId = train.trainId,
                        stationName = s.station.data,
                        timeArrival = s.arrival.data,
                        timeDeparture = s.departure.data,
                        trackNumber = s.trackNumber
                    )
                }.toMutableList()
                performSave(train)
            }
        }
    }

    /**
     * Пустой ли поезд: ни номеров, ни характеристик, ни станций, ни толкача/
     * двойной тяги/сдвоенного, ни выбранного плеча. Нужно, чтобы не сохранять
     * абсолютно пустой объект, если форму нового поезда открыли и закрыли без ввода.
     * Станции берутся из [stationsListState], т.к. в [Train] они переносятся только при save.
     */
    private fun isTrainEmpty(train: Train): Boolean {
        val stationsEmpty = stationsListState.all { s ->
            s.station.data.isNullOrBlank() && s.arrival.data == null &&
                    s.departure.data == null && s.trackNumber.isNullOrBlank()
        }
        return train.number.isNullOrBlank() &&
                train.additionalNumbers.all { it.isBlank() } &&
                train.distance.isNullOrBlank() &&
                train.weight.isNullOrBlank() &&
                train.axle.isNullOrBlank() &&
                train.conditionalLength.isNullOrBlank() &&
                train.pusher == null &&
                train.doubleTraction == null &&
                train.doubledTrain == null &&
                uiState.value.selectedServicePhase == null &&
                stationsEmpty
    }

    override fun onCleared() {
        autoSaveJob?.cancel()
        saveTrainJob?.cancel()
        CoroutineScope(NonCancellable + Dispatchers.IO).launch {
            val state = _uiState.value.trainDetailState
            if (state is ResultState.Success) {
                state.data?.let { train ->
                    // Не сохраняем абсолютно пустой новый объект (форма открыта и закрыта
                    // без ввода). Существующий поезд сохраняем всегда.
                    if (isNewTrain && isTrainEmpty(train)) return@launch
                    train.servicePhase = uiState.value.selectedServicePhase
                    train.stations = stationsListState.map { s ->
                        Station(
                            stationId = s.id,
                            trainId = train.trainId,
                            stationName = s.station.data,
                            timeArrival = s.arrival.data,
                            timeDeparture = s.departure.data,
                            trackNumber = s.trackNumber
                        )
                    }.toMutableList()
                    trainUseCase.saveTrain(train).collect {}
                }
            }
        }
        super.onCleared()
    }

    private fun loadTrain(id: String) {
        loadTrainJob?.cancel()
        _uiState.update { it.copy(trainDetailState = ResultState.Loading()) }
        loadTrainJob = viewModelScope.launch {
            val resultState = trainUseCase.getTrainById(id)
                .first { it !is ResultState.Loading }
            _uiState.update {
                if (resultState is ResultState.Success) {
                    currentTrain = resultState.data
                    resultState.data?.let { train ->
                        setStations(train.stations)
                    }
                }
                it.copy(
                    selectedServicePhase = currentTrain?.servicePhase,
                    trainDetailState = resultState
                )
            }
        }
    }

    fun saveTrain() {
        if (uiState.value.errorMessage == null) {
            val state = _uiState.value.trainDetailState
            if (state is ResultState.Success) {
                state.data?.let { train ->
                    train.servicePhase = uiState.value.selectedServicePhase
                    train.stations = stationsListState.map { state ->
                        Station(
                            stationId = state.id,
                            trainId = train.trainId,
                            stationName = state.station.data,
                            timeArrival = state.arrival.data,
                            timeDeparture = state.departure.data,
                            trackNumber = state.trackNumber
                        )
                    }.toMutableList()

                    // Check if first+last station matches an existing service phase
                    val firstStation = stationsListState.firstOrNull()?.station?.data
                    val lastStation = stationsListState.lastOrNull()?.station?.data
                    if (!firstStation.isNullOrBlank() && !lastStation.isNullOrBlank() && stationsListState.size >= 2) {
                        val matchingPhase = servicePhaseList.any { phase ->
                            phase.departureStation == firstStation && phase.arrivalStation == lastStation
                        }
                        if (!matchingPhase) {
                            _uiState.update {
                                it.copy(
                                    showCreateServicePhaseSheet = true,
                                    suggestedDepartureStation = firstStation,
                                    suggestedArrivalStation = lastStation
                                )
                            }
                            return
                        }
                    }

                    performSave(train)
                }
            }
        }
    }

    private fun performSave(train: Train) {
        saveStationsName(train)
        saveAssistSeries()
        saveTrainJob?.cancel()
        saveTrainJob = viewModelScope.launch {
            trainUseCase.saveTrain(train).collect { resultState ->
                _uiState.update {
                    it.copy(saveTrainState = resultState)
                }
            }
        }
    }

    fun cancelCreateServicePhaseSheet() {
        _uiState.update {
            it.copy(showCreateServicePhaseSheet = false)
        }
        // User dismissed the sheet — cancel save, stay on screen
    }

    fun skipServicePhaseAndSave() {
        _uiState.update {
            it.copy(showCreateServicePhaseSheet = false)
        }
        // Save the train without creating a service phase
        val state = _uiState.value.trainDetailState
        if (state is ResultState.Success) {
            state.data?.let { performSave(it) }
        }
    }

    fun createServicePhaseAndSave(distance: String, createReverse: Boolean = false) {
        _uiState.update {
            it.copy(showCreateServicePhaseSheet = false)
        }
        viewModelScope.launch {
            val uiValue = _uiState.value
            val dist = distance.toIntOrNull() ?: 0
            val newPhase = ServicePhase(
                departureStation = uiValue.suggestedDepartureStation,
                arrivalStation = uiValue.suggestedArrivalStation,
                distance = dist
            )
            // Get current settings and add new service phase
            val currentSettings = settingsUseCase.getUserSettingFlow().first()
            val newPhases = mutableListOf(newPhase)
            if (createReverse) {
                newPhases.add(
                    ServicePhase(
                        departureStation = uiValue.suggestedArrivalStation,
                        arrivalStation = uiValue.suggestedDepartureStation,
                        distance = dist
                    )
                )
            }
            val updatedSettings = currentSettings.copy(
                servicePhases = currentSettings.servicePhases + newPhases
            )
            settingsUseCase.saveSetting(updatedSettings).first()

            // Now save the train
            val state = _uiState.value.trainDetailState
            if (state is ResultState.Success) {
                state.data?.let { train ->
                    train.servicePhase = newPhase
                    performSave(train)
                }
            }
        }
    }

    /** Добавить новое плечо в настройки (сохраняется в UserSettings.servicePhases). */
    fun addServicePhase(phase: ServicePhase) {
        viewModelScope.launch {
            val settings = settingsUseCase.getUserSettingFlow().first()
            val updated = settings.copy(servicePhases = settings.servicePhases + phase)
            settingsUseCase.saveSetting(updated).first()
        }
    }

    /** Обновить существующее плечо (по id). Если оно сейчас выбрано — обновляем ссылку. */
    fun updateServicePhase(newPhase: ServicePhase) {
        viewModelScope.launch {
            val settings = settingsUseCase.getUserSettingFlow().first()
            val updatedList = settings.servicePhases.map {
                if (it.id == newPhase.id) newPhase else it
            }
            settingsUseCase.saveSetting(settings.copy(servicePhases = updatedList)).first()
            if (_uiState.value.selectedServicePhase?.id == newPhase.id) {
                _uiState.update { it.copy(selectedServicePhase = newPhase) }
                setDistance(newPhase.distance.toString())
            }
        }
    }

    /** Удалить плечо. Если оно было выбрано — снимаем выбор. */
    fun deleteServicePhase(phase: ServicePhase) {
        viewModelScope.launch {
            val settings = settingsUseCase.getUserSettingFlow().first()
            val updatedList = settings.servicePhases.filterNot { it.id == phase.id }
            settingsUseCase.saveSetting(settings.copy(servicePhases = updatedList)).first()
            if (_uiState.value.selectedServicePhase?.id == phase.id) {
                _uiState.update { it.copy(selectedServicePhase = null) }
            }
        }
    }

    private fun saveStationsName(train: Train) {
        viewModelScope.launch(Dispatchers.IO) {
            val list = train.stations
                .map { it.stationName ?: "" }
            stationNameList.addAll(list)
            settingsUseCase.setStations(list)
        }
    }

    fun removeStationName(value: String) {
        viewModelScope.launch {
            stationNameList.remove(value)
            mutableStationList.remove(value)
            settingsUseCase.removeStation(value)
        }
    }

    fun resetSaveState() {
        _uiState.update {
            it.copy(saveTrainState = null)
        }
    }

    fun setNumber(number: String) {
        currentTrain = currentTrain?.copy(
            number = number.ifBlank { null }
        )
        changesHave()
    }

    fun addAdditionalNumber(number: String) {
        currentTrain?.let { train ->
            val updated = train.additionalNumbers.toMutableList()
            updated.add(number)
            currentTrain = train.copy(additionalNumbers = updated)
            changesHave()
        }
    }

    fun removeAdditionalNumber(index: Int) {
        currentTrain?.let { train ->
            val updated = train.additionalNumbers.toMutableList()
            if (index in updated.indices) {
                updated.removeAt(index)
                currentTrain = train.copy(additionalNumbers = updated)
                changesHave()
            }
        }
    }

    fun setDistance(distance: String) {
        currentTrain = currentTrain?.copy(
            distance = distance.ifBlank { null }
        )
        changesHave()
    }

    fun setWeight(weight: String) {
        currentTrain = currentTrain?.copy(
            weight = weight.ifBlank { null }
        )
        changesHave()
    }

    fun setAxle(axle: String) {
        currentTrain = currentTrain?.copy(
            axle = axle.ifBlank { null }
        )
        changesHave()
    }

    fun setConditionalLength(length: String) {
        currentTrain = currentTrain?.copy(
            conditionalLength = length.ifBlank { null }
        )
        changesHave()
    }

    // --- Pusher (Толкач) ---
    fun addPusher() {
        currentTrain = currentTrain?.copy(pusher = TrainAssist())
        changesHave()
    }

    fun removePusher() {
        currentTrain = currentTrain?.copy(pusher = null)
        changesHave()
    }

    fun setPusherSeries(value: String) {
        currentTrain = currentTrain?.copy(
            pusher = (currentTrain?.pusher ?: TrainAssist()).copy(locomotiveSeries = value.ifBlank { null })
        )
        changesHave()
    }

    fun setPusherNumber(value: String) {
        currentTrain = currentTrain?.copy(
            pusher = (currentTrain?.pusher ?: TrainAssist()).copy(locomotiveNumber = value.ifBlank { null })
        )
        changesHave()
    }

    fun setPusherDriverName(value: String) {
        currentTrain = currentTrain?.copy(
            pusher = (currentTrain?.pusher ?: TrainAssist()).copy(driverName = value.ifBlank { null })
        )
        changesHave()
    }

    fun setPusherNotes(value: String) {
        currentTrain = currentTrain?.copy(
            pusher = (currentTrain?.pusher ?: TrainAssist()).copy(notes = value.ifBlank { null })
        )
        changesHave()
    }

    /** Направление толкача: true = «Я толкаю», false = «Меня толкают» (через isFirst). */
    fun setPusherIsFirst(isFirst: Boolean) {
        currentTrain = currentTrain?.copy(
            pusher = (currentTrain?.pusher ?: TrainAssist()).copy(isFirst = isFirst)
        )
        changesHave()
    }

    // --- DoubleTraction (Двойная тяга) ---
    fun addDoubleTraction() {
        currentTrain = currentTrain?.copy(doubleTraction = TrainAssist())
        changesHave()
    }

    fun removeDoubleTraction() {
        currentTrain = currentTrain?.copy(doubleTraction = null)
        changesHave()
    }

    fun setDoubleTractionSeries(value: String) {
        currentTrain = currentTrain?.copy(
            doubleTraction = (currentTrain?.doubleTraction ?: TrainAssist()).copy(locomotiveSeries = value.ifBlank { null })
        )
        changesHave()
    }

    fun setDoubleTractionNumber(value: String) {
        currentTrain = currentTrain?.copy(
            doubleTraction = (currentTrain?.doubleTraction ?: TrainAssist()).copy(locomotiveNumber = value.ifBlank { null })
        )
        changesHave()
    }

    fun setDoubleTractionDriverName(value: String) {
        currentTrain = currentTrain?.copy(
            doubleTraction = (currentTrain?.doubleTraction ?: TrainAssist()).copy(driverName = value.ifBlank { null })
        )
        changesHave()
    }

    fun setDoubleTractionNotes(value: String) {
        currentTrain = currentTrain?.copy(
            doubleTraction = (currentTrain?.doubleTraction ?: TrainAssist()).copy(notes = value.ifBlank { null })
        )
        changesHave()
    }

    // --- DoubledTrain (Сдвоенный поезд) ---
    fun addDoubledTrain() {
        currentTrain = currentTrain?.copy(doubledTrain = TrainAssist())
        changesHave()
    }

    fun removeDoubledTrain() {
        currentTrain = currentTrain?.copy(doubledTrain = null)
        changesHave()
    }

    fun setDoubledTrainSeries(value: String) {
        currentTrain = currentTrain?.copy(
            doubledTrain = (currentTrain?.doubledTrain ?: TrainAssist()).copy(locomotiveSeries = value.ifBlank { null })
        )
        changesHave()
    }

    fun setDoubledTrainNumber(value: String) {
        currentTrain = currentTrain?.copy(
            doubledTrain = (currentTrain?.doubledTrain ?: TrainAssist()).copy(locomotiveNumber = value.ifBlank { null })
        )
        changesHave()
    }

    fun setDoubledTrainDriverName(value: String) {
        currentTrain = currentTrain?.copy(
            doubledTrain = (currentTrain?.doubledTrain ?: TrainAssist()).copy(driverName = value.ifBlank { null })
        )
        changesHave()
    }

    fun setDoubledTrainNotes(value: String) {
        currentTrain = currentTrain?.copy(
            doubledTrain = (currentTrain?.doubledTrain ?: TrainAssist()).copy(notes = value.ifBlank { null })
        )
        changesHave()
    }

    fun setDoubledTrainIsFirst(isFirst: Boolean) {
        currentTrain = currentTrain?.copy(
            doubledTrain = (currentTrain?.doubledTrain ?: TrainAssist()).copy(isFirst = isFirst)
        )
        changesHave()
    }

    // --- Series dropdown ---
    fun changeSeriesMenuExpanded(sectionId: String, expanded: Boolean) {
        _uiState.update {
            it.copy(expandedSeriesSectionId = if (expanded) sectionId else null)
        }
    }

    fun onChangedSeriesDropDown(sectionId: String, content: String) {
        if (content.isEmpty()) {
            mutableFilteredSeriesList.addAllOrSkip(locomotiveSeriesList)
        } else {
            mutableFilteredSeriesList.clear()
            val filtered = locomotiveSeriesList
                .filter { it.startsWith(prefix = content, ignoreCase = true) }
                .filterNot { it == content }
                .toMutableStateList()
            filtered.forEach { ser ->
                mutableFilteredSeriesList.add(ser)
                changeSeriesMenuExpanded(sectionId, true)
            }
        }
    }

    fun removeSeries(series: String) {
        viewModelScope.launch {
            dropDownSeriesList.remove(series)
            settingsUseCase.removeLocomotiveSeries(series)
            changesHave()
        }
    }

    fun saveAssistSeries() {
        viewModelScope.launch(Dispatchers.IO) {
            val seriesToSave = listOfNotNull(
                currentTrain?.pusher?.locomotiveSeries,
                currentTrain?.doubleTraction?.locomotiveSeries,
                currentTrain?.doubledTrain?.locomotiveSeries
            ).filter { it.isNotBlank() }

            seriesToSave.forEach { series ->
                settingsUseCase.setLocomotiveSeries(series)
                if (series !in locomotiveSeriesList) {
                    locomotiveSeriesList.add(0, series)
                    mutableFilteredSeriesList.add(0, series)
                }
            }
        }
    }

    private fun setStations(stations: MutableList<Station>) {
        stations.forEach { station ->
            stationsListState.addOrReplace(
                StationFormState(
                    id = station.stationId,
                    station = StationField(
                        data = station.stationName,
                        type = StationDataType.NAME
                    ),
                    arrival = StationFieldDate(
                        data = station.timeArrival,
                        type = StationDataType.ARRIVAL
                    ),
                    departure = StationFieldDate(
                        data = station.timeDeparture,
                        type = StationDataType.DEPARTURE
                    ),
                    trackNumber = station.trackNumber
                )
            )
        }
    }

    fun addingStation(stationName: String? = null) {
        val newStation = StationFormState(
            id = Station().stationId,
            station = StationField(data = stationName, type = StationDataType.NAME)
        )
        val hasServicePhase = _uiState.value.selectedServicePhase != null
        if (hasServicePhase && stationsListState.size >= 2 && stationName == null) {
            stationsListState.add(stationsListState.lastIndex, newStation)
        } else {
            stationsListState.add(newStation)
        }
        changesHave()
    }

    fun deleteStation(stationFormState: StationFormState) {
        stationsListState.removeAll { it.id == stationFormState.id }
        _uiState.update { it.copy(errorMessage = null) }
        changesHave()
        checkFormValidStation()
    }

    /**
     * Сканирует станции от конца к началу, находит последнее заполненное время.
     * При наличии плеча обслуживания — пропускает последнюю станцию (прибытие плеча).
     * Для каждой станции проверяет сначала departure, потом arrival.
     * @return Pair(индекс станции, тип поля) или null если ничего не заполнено.
     */
    private fun findLastFilledTime(): Pair<Int, StationDataType>? {
        val hasServicePhase = _uiState.value.selectedServicePhase != null
        val endIdx = if (hasServicePhase && stationsListState.size >= 2)
            stationsListState.lastIndex - 1
        else
            stationsListState.lastIndex

        for (i in endIdx downTo 0) {
            val station = stationsListState[i]
            if (station.departure.data != null) return Pair(i, StationDataType.DEPARTURE)
            if (station.arrival.data != null) return Pair(i, StationDataType.ARRIVAL)
        }
        return null
    }

    fun isNextDeparture(): Boolean {
        if (stationsListState.isEmpty()) return true

        val lastFilled = findLastFilledTime()
        if (lastFilled == null) return true

        return lastFilled.second == StationDataType.ARRIVAL
    }

    fun onGoClicked() {
        // Секунды сохраняем (не обрезаем до минуты), чтобы секундомер стартовал с нуля.
        val now = timeManager.nowExact()

        val hasServicePhase = _uiState.value.selectedServicePhase != null

        if (stationsListState.isEmpty()) {
            addingStation()
            setDepartureTime(0, now)
            return
        }

        val lastFilled = findLastFilledTime()

        if (lastFilled == null) {
            // Нет заполненных времён → departure первой станции
            setDepartureTime(0, now)
            return
        }

        val (idx, type) = lastFilled

        if (type == StationDataType.ARRIVAL) {
            // Последнее заполненное — arrival → установить departure на той же станции
            setDepartureTime(idx, now)
        } else {
            // Последнее заполненное — departure → arrival на следующей станции
            val nextIdx = idx + 1
            val isServicePhaseArrival = hasServicePhase && nextIdx == stationsListState.lastIndex
            if (nextIdx <= stationsListState.lastIndex && !isServicePhaseArrival) {
                // Следующая станция уже существует и это не конечная станция плеча
                setArrivalTime(nextIdx, now)
            } else {
                // Нужно добавить новую станцию (или nextIdx указывает на конечную станцию плеча)
                addingStation() // с плечом вставит перед последней, без — в конец
                if (hasServicePhase) {
                    setArrivalTime(stationsListState.lastIndex - 1, now)
                } else {
                    setArrivalTime(stationsListState.lastIndex, now)
                }
            }
        }
    }

    // Per-station reorder
    fun startReorderStation(stationId: String) {
        _uiState.update { it.copy(reorderingStationId = stationId) }
    }

    fun stopReorderStation() {
        _uiState.update { it.copy(reorderingStationId = null) }
    }

    // BottomSheet editing
    fun startEditingStation(index: Int) {
        _uiState.update { it.copy(editingStationIndex = index) }
    }

    fun startAddingNewStation() {
        _uiState.update { it.copy(editingStationIndex = -1) }
    }

    fun stopEditingStation() {
        _uiState.update { it.copy(editingStationIndex = null) }
    }

    fun saveStationFromSheet(index: Int, name: String?, arrival: Long?, departure: Long?, trackNumber: String?) {
        if (index == -1) {
            val newStation = StationFormState(
                id = Station().stationId,
                station = StationField(data = name, type = StationDataType.NAME),
                arrival = StationFieldDate(data = arrival, type = StationDataType.ARRIVAL),
                departure = StationFieldDate(data = departure, type = StationDataType.DEPARTURE),
                trackNumber = trackNumber
            )
            val hasServicePhase = _uiState.value.selectedServicePhase != null
            if (hasServicePhase && stationsListState.size >= 2) {
                stationsListState.add(stationsListState.lastIndex, newStation)
            } else {
                stationsListState.add(newStation)
            }
        } else if (index in stationsListState.indices) {
            stationsListState[index] = stationsListState[index].copy(
                station = StationField(data = name, type = StationDataType.NAME),
                arrival = StationFieldDate(data = arrival, type = StationDataType.ARRIVAL),
                departure = StationFieldDate(data = departure, type = StationDataType.DEPARTURE),
                trackNumber = trackNumber
            )
        }
        changesHave()
        checkFormValidStation()
        stopEditingStation()
    }

    fun requestDeleteStation(index: Int) {
        _uiState.update { it.copy(confirmDeleteStationIndex = index) }
    }

    fun confirmDeleteStation() {
        val index = _uiState.value.confirmDeleteStationIndex ?: return
        _uiState.update { it.copy(confirmDeleteStationIndex = null) }
        deleteStationFromSheet(index)
    }

    fun cancelDeleteStation() {
        _uiState.update { it.copy(confirmDeleteStationIndex = null) }
    }

    fun deleteStationFromSheet(index: Int) {
        if (index in stationsListState.indices) {
            stationsListState.removeAt(index)
            _uiState.update { it.copy(errorMessage = null) }
            changesHave()
            checkFormValidStation()
        }
        stopEditingStation()
    }

    fun moveStation(fromIndex: Int, toIndex: Int) {
        if (toIndex < 0 || toIndex >= stationsListState.size) return
        val item = stationsListState.removeAt(fromIndex)
        stationsListState.add(toIndex, item)
        changesHave()
        // reorderingStationId stays the same — follows by station ID, not index
    }

    private fun onStationEvent(event: StationEvent) {
        when (event) {
            is StationEvent.EnteredStationName -> {
                stationsListState[event.index] = stationsListState[event.index].copy(
                    station = stationsListState[event.index].station.copy(
                        data = event.data
                    )
                )
            }

            is StationEvent.EnteredDepartureTime -> {
                stationsListState[event.index] = stationsListState[event.index].copy(
                    departure = stationsListState[event.index].departure.copy(
                        data = event.data
                    )
                )
            }

            is StationEvent.EnteredArrivalTime -> {
                stationsListState[event.index] = stationsListState[event.index].copy(
                    arrival = stationsListState[event.index].arrival.copy(
                        data = event.data
                    )
                )
            }

            is StationEvent.FocusChange -> {
                checkFormValidStation()
            }
        }
    }

    private fun checkFormValidStation() {
        viewModelScope.launch {
            route.trains = mutableListOf(
                currentTrain!!.copy(
                    stations = stationsListState.map { state ->
                        Station(
                            stationId = state.id,
                            stationName = state.station.data,
                            timeArrival = state.arrival.data,
                            timeDeparture = state.departure.data
                        )
                    }.toMutableList()
                )
            )

            val validState = routeUseCase.isValidTrain(route).first()
            if (validState is ResultState.Error) {
                _uiState.update {
                    it.copy(
                        errorMessage = validState.entity.message
                    )
                }
            }
            if (validState is ResultState.Success) {
                _uiState.update {
                    it.copy(
                        errorMessage = null
                    )
                }
            }
        }
    }

    fun setStationName(index: Int, s: String?) {
        onStationEvent(
            StationEvent.EnteredStationName(
                index, s
            )
        )
        s?.let {
            onChangedDropDownContent(index, s)
        }
        changesHave()
    }

    fun setDepartureTime(index: Int, time: Long?) {
        onStationEvent(
            StationEvent.EnteredDepartureTime(
                index, time
            )
        )
        focusChangedStation(index, StationDataType.DEPARTURE)
        changesHave()
    }

    fun setArrivalTime(index: Int, time: Long?) {
        onStationEvent(
            StationEvent.EnteredArrivalTime(
                index, time
            )
        )
        focusChangedStation(index, StationDataType.ARRIVAL)
        changesHave()
    }

    private fun focusChangedStation(index: Int, field: StationDataType) {
        onStationEvent(
            StationEvent.FocusChange(
                index, field
            )
        )
    }

    private var mutableStationList = mutableStateListOf<String>()

    var stationList: SnapshotStateList<String>
        get() {
            return mutableStationList
        }
        set(value) {
            mutableStationList = value
        }

    fun changeExpandedMenu(index: Int, value: Boolean) {
        _uiState.update {
            it.copy(
                isExpandedDropDownMenuStation = Pair(index, value)
            )
        }
    }

    fun onChangedDropDownContent(index: Int, value: String) {
        if (value.isEmpty()) {
            changeExpandedMenu(index, false)
            mutableStationList.addAllOrSkip(stationNameList)
        } else {
            mutableStationList.clear()
            val newStationList =
                stationNameList
                    .filter { it.startsWith(prefix = value, ignoreCase = true) }
                    .filterNot { it == value }
                    .toMutableStateList()
            newStationList.forEach { st ->
                mutableStationList.add(st)
                changeExpandedMenu(index, true)
            }
        }
    }
}