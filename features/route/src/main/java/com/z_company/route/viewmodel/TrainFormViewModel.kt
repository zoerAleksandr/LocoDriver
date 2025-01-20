package com.z_company.route.viewmodel

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.core.ResultState
import com.z_company.domain.entities.route.Station
import com.z_company.domain.entities.route.Train
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.domain.use_cases.TrainUseCase
import com.z_company.domain.util.addAllOrSkip
import com.z_company.domain.util.addOrReplace
import com.z_company.domain.util.compareWithNullable
import com.z_company.route.Const.NULLABLE_ID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.properties.Delegates

class TrainFormViewModel(
    trainId: String?,
    basicId: String
) : ViewModel(), KoinComponent {
    private val trainUseCase: TrainUseCase by inject()
    private val settingsUseCase: SettingsUseCase by inject()

    private val _uiState = MutableStateFlow(TrainFormUiState())
    val uiState = _uiState.asStateFlow()

    private var loadTrainJob: Job? = null
    private var saveTrainJob: Job? = null

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

    init {
        if (trainId == NULLABLE_ID) {
            isNewTrain = true
            currentTrain = Train(basicId = basicId)
        } else {
            isNewTrain = false
            loadTrain(trainId!!)
        }
        viewModelScope.launch {
            loadSetting().join()
        }
    }

    private suspend fun loadSetting(): Job {
        return viewModelScope.launch {
            settingsUseCase.getCurrentSettings().collect {
                if (it is ResultState.Success) {
                    it.data?.let { settings ->
                        stationNameList.addAllOrSkip(settings.stationList.toMutableStateList())
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
                it.copy(trainDetailState = resultState)
            }
        }.launchIn(viewModelScope)
    }

    fun saveTrain() {
        val state = _uiState.value.trainDetailState
        if (state is ResultState.Success) {
            state.data?.let { train ->
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
                saveTrainJob = trainUseCase.saveTrain(train).onEach { resultState ->
                    _uiState.update {
                        it.copy(saveTrainState = resultState)
                    }
                }.launchIn(viewModelScope)
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
            val formValid = formValidStation(station)
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
                    formValid = StationIsValidField(
                        data = formValid
                    )
                )
            )
        }
    }

    fun addingStation() {
        stationsListState.add(
            StationFormState(
                id = Station().stationId
            )
        )
        changesHave()
    }

    fun deleteStation(stationFormState: StationFormState) {
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
                val formValid = formValidStation(stationsListState[event.index])
                stationsListState[event.index] = stationsListState[event.index].copy(
                    formValid = stationsListState[event.index].formValid.copy(
                        data = formValid
                    )
                )
                if (!formValid) {
                    _uiState.update {
                        it.copy(
                            errorMessage = "Некорректное время"
                        )
                    }
                }
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

    private fun formValidStation(
        station: StationFormState
    ): Boolean {
        val departure = station.departure.data
        val arrival = station.arrival.data
        return arrival.compareWithNullable(departure)
    }

    private fun formValidStation(
        station: Station
    ): Boolean {
        val departure = station.timeDeparture
        val arrival = station.timeArrival
        return arrival.compareWithNullable(departure)
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