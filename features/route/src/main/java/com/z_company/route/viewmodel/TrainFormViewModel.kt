package com.z_company.route.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.core.ResultState
import com.z_company.domain.entities.ServicePhase
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.Station
import com.z_company.domain.entities.route.Train
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.domain.use_cases.TrainUseCase
import com.z_company.domain.util.addAllOrSkip
import com.z_company.domain.util.addOrReplace
import com.z_company.route.Const.NULLABLE_ID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.collections.toMutableList
import kotlin.properties.Delegates

class TrainFormViewModel(
    trainId: String?,
    basicId: String
) : ViewModel(), KoinComponent {
    private val trainUseCase: TrainUseCase by inject()
    private val settingsUseCase: SettingsUseCase by inject()
    private val routeUseCase: RouteUseCase by inject()
    private var route: Route = Route()

    private val _uiState = MutableStateFlow(TrainFormUiState())
    val uiState = _uiState.asStateFlow()

    private var loadTrainJob: Job? = null
    private var saveTrainJob: Job? = null

    var timeZoneText: String = "GMT+3"

    private var isNewTrain by Delegates.notNull<Boolean>()

    private val stationNameList = mutableStateListOf<String>()
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
                addingStation(stationName = servicePhase.departureStation)
            }
        } else {
            setDistance("")
        }
        _uiState.update {
            it.copy(
                selectedServicePhase = servicePhase
            )
        }
        hideDialogSelectServicePhase()
    }

    fun showDialogSelectServicePhase() {
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(
                        isShowDialogSelectServicePhase = true
                    )
                }
            }
        }
    }

    fun hideDialogSelectServicePhase() {
        _uiState.update {
            it.copy(
                isShowDialogSelectServicePhase = false
            )
        }
    }

    init {
        viewModelScope.launch {
            if (trainId == NULLABLE_ID) {
                isNewTrain = true
                currentTrain = Train(basicId = basicId)
            } else {
                isNewTrain = false
                loadTrain(trainId!!)
            }
            val initJob = this.launch {
                val setting = settingsUseCase.getUserSettingFlow().first()
                timeZoneText = settingsUseCase.getTimeZone(setting.timeZone)
            }
            initJob.join()

            combine(
                settingsUseCase.getFlowCurrentSettingsState(),
                routeUseCase.routeDetails(basicId)
            ) { settingState, routeState ->
                if (settingState is ResultState.Success) {
                    settingState.data?.let { settings ->
                        stationNameList.addAllOrSkip(settings.stationList.toMutableStateList())
                        servicePhaseList.clear()
                        servicePhaseList.addAllOrSkip(settings.servicePhases.toMutableStateList())
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

    private fun loadSetting(): Job {
        return viewModelScope.launch {
            settingsUseCase.getFlowCurrentSettingsState().collect {
                if (it is ResultState.Success) {
                    it.data?.let { settings ->
                        stationNameList.addAllOrSkip(settings.stationList.toMutableStateList())
                        servicePhaseList.clear()
                        servicePhaseList.addAllOrSkip(settings.servicePhases.toMutableStateList())
                    }
                    this.cancel()
                }
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
        if (isNewTrain) {
            val state = _uiState.value.trainDetailState
            if (state is ResultState.Success) {
                state.data?.let { train ->
                    trainUseCase.removeTrain(train).onEach { result ->
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

    private fun loadTrain(id: String) {
        loadTrainJob?.cancel()
        loadTrainJob = trainUseCase.getTrainById(id).onEach { resultState ->
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
        }.launchIn(viewModelScope)
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
                            stationName = state.station.data,
                            timeArrival = state.arrival.data,
                            timeDeparture = state.departure.data
                        )
                    }.toMutableList()
                    saveStationsName(train)
                    saveTrainJob?.cancel()
                    saveTrainJob = viewModelScope.launch {
                        trainUseCase.saveTrain(train).collect { resultState ->
                            _uiState.update {
                                it.copy(saveTrainState = resultState)
                            }
                        }
                    }
                }
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
                    )
                )
            )
        }
    }

    fun addingStation(stationName: String? = null) {
        stationsListState.add(
            StationFormState(
                id = Station().stationId,
                station = StationField(data = stationName, type = StationDataType.NAME)
            )
        )
        changesHave()
    }

    fun deleteStation(stationFormState: StationFormState) {
        checkFormValidStation()
        stationsListState.remove(stationFormState)
        changesHave()
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

    fun resetErrorMessage() {
        _uiState.update {
            it.copy(
                errorMessage = null
            )
        }
    }

    private fun checkFormValidStation() {
        viewModelScope.launch {
            route = route.copy(
                trains = mutableListOf(
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

//    private fun formValidStation(
//        station: Station
//    ): Boolean {
//
//        routeUseCase.isValidTrain(route)
//        val departure = station.timeDeparture
//        val arrival = station.timeArrival
//        return arrival.compareWithNullable(departure)
//    }

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
        .also {
            it.addAll(stationNameList)
        }

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