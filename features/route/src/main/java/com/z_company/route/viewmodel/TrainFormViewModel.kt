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
import com.z_company.domain.entities.route.TrainDataVersion
import com.z_company.domain.repositories.SharedPreferencesRepositories
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.domain.use_cases.TrainUseCase
import com.z_company.domain.util.addAllOrSkip
import com.z_company.domain.util.addOrReplace
import com.z_company.domain.util.sanitizeNumericInput
import com.z_company.route.Const.NULLABLE_ID
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.z_company.domain.util.GeoFix
import com.z_company.domain.util.ServicePhaseOrder
import com.z_company.domain.util.ServicePhaseStationPicker
import com.z_company.domain.util.StationDirectory
import com.z_company.domain.util.ServicePhaseStationsMerge
import com.z_company.domain.util.StationSuggestionOrder
import com.z_company.route.util.DeviceLocation
import kotlinx.coroutines.CoroutineScope
import com.z_company.domain.util.displayTimeZone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.z_company.core.sendToSentry
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.collections.toMutableList
import kotlin.properties.Delegates

class TrainFormViewModel(
    trainId: String?,
    basicId: String
) : ViewModel(), KoinComponent {
    private val brakeRouteId: String = basicId
    private val timeManager = TimeManager()
    private val sharedPreferenceStorage: SharedPreferencesRepositories by inject()
    private val trainUseCase: TrainUseCase by inject()
    private val settingsUseCase: SettingsUseCase by inject()
    private val stationDirectory: com.z_company.domain.use_cases.StationDirectoryUseCase by inject()
    private val routeUseCase: RouteUseCase by inject()
    private val autoRecord: com.z_company.route.autorecord.AutoRecordManager by inject()
    private val trackRepository: com.z_company.domain.track.TrackRepository by inject()
    private var autoEditorTrainId: String? = null
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

    fun setSelectedServicePhase(selected: ServicePhase?) {
        // В рейс кладём копию без станций плеча — они берутся из справочника по id.
        val servicePhase = selected?.forTrain()
        if (servicePhase != null) {
            setDistance(servicePhase.distance.takeIf { it > 0 }?.toString() ?: "")
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
            val showSegments = sharedPreferenceStorage.isShowSegments()
            _uiState.update { it.copy(isStationsReversed = isReversed, showSegments = showSegments) }
        }
    }

    fun toggleStationsSortOrder() {
        val newReversed = !_uiState.value.isStationsReversed
        _uiState.update { it.copy(isStationsReversed = newReversed) }
        sharedPreferenceStorage.toggleStationsSortOrder(newReversed)
    }

    // ─── Станции плеча по местоположению (SCREEN_SPECS §6.6) ───────────────
    // Объявлены ДО init: init их уже использует (порядок инициализации свойств).
    private val appContext: android.content.Context by inject()
    private val stopNamer: com.z_company.domain.util.StopStationNamer by inject()
    /** Координата прошлого добавления станции — чтобы не подставлять одно и то же стоя на месте. */
    /** Последняя известная координата — для сортировки подсказок. */
    // lastFix/phaseUsage/phaseLastUsed - Compose-состояние: порядок плеч читается при композиции и должен
    // пересортироваться, когда статистика/координата приходят позже открытия экрана.
    private var lastFix: GeoFix? by mutableStateOf(null)
    private var stationUsage: Map<String, Int> = emptyMap()
    private var phaseUsage: Map<String, Int> by mutableStateOf(emptyMap())
    private var phaseLastUsed: Map<String, Long> by mutableStateOf(emptyMap())

    init {
        loadUsageStats()
        refreshFix()
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
                // «АВТО» пишет события в эту форму, пока она открыта (иначе автосохранение затрёт запись).
                (trainId.takeIf { it != NULLABLE_ID } ?: currentTrain?.trainId)?.let { id ->
                    autoEditorTrainId = id
                    autoRecord.registerEditor(id, ::applyAutoEvent)
                }
                val initJob = this.launch {
                    val setting = settingsUseCase.getUserSettingFlow().first()
                    timeZoneText = "GMT+3"
                    userCountry = setting.country
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
                                    dateAndTimeConverter = DateAndTimeConverter(settings),
                                    passengerWagonLengthMeters = settings.passengerWagonLengthMeters,
                                )
                            }
                            com.z_company.core.ui.perf.ScreenPerf.section("train.settingsLists") {
                                // Подсказки станций: настройки + справочник станций.
                                stationNameList.addAllOrSkip(
                                    com.z_company.route.util.mergeStationSuggestions(
                                        settings.stationList,
                                        stationDirectory.stations().map { it.name },
                                    )
                                )
                                mutableStationList.addAllOrSkip(stationNameList)
                                locomotiveSeriesList.addAllOrSkip(settings.locomotiveSeriesList)
                                mutableFilteredSeriesList.addAllOrSkip(locomotiveSeriesList)
                                servicePhaseList.clear()
                                // Координаты станций плеч — из справочника станций (единый источник).
                                stationDirectory.backfillOnce(settings.servicePhases)
                                servicePhaseList.addAllOrSkip(stationDirectory.resolve(settings.servicePhases))
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.sendToSentry("TrainFormViewModel", "init")
            }
        }

    }

    /** Данные поезда формы и последнего локомотива маршрута для предзаполнения справки ВУ-45. */
    fun brakePrefill(): com.z_company.domain.brake.BrakePrefill {
        val train = currentTrain
        val loco = route.locomotives.lastOrNull()
        return com.z_company.domain.brake.BrakePrefill(
            weight = train?.weight,
            axles = train?.axle,
            trainNumber = train?.number,
            locoSeries = loco?.series,
            locoNumber = loco?.number,
            routeId = brakeRouteId.takeIf { it.isNotBlank() && it != com.z_company.route.Const.NULLABLE_ID },
            trainId = train?.trainId,
        )
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
                        trackNumber = s.trackNumber,
                        isFinalStation = s.isFinalStation,
                        isPassingStation = s.isPassingStation,
                        segmentTrackNumber = s.segmentTrackNumber,
                        segmentNotes = s.segmentNotes
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
        autoEditorTrainId?.let { autoRecord.unregisterEditor(it) }
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
                            trackNumber = s.trackNumber,
                            isFinalStation = s.isFinalStation,
                            isPassingStation = s.isPassingStation,
                            segmentTrackNumber = s.segmentTrackNumber,
                            segmentNotes = s.segmentNotes
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
            // Нули из БД/синхронизации («0», «0.0») — это «не задано»: поля показываем пустыми.
            val loaded = if (resultState is ResultState.Success) {
                ResultState.Success(resultState.data?.withoutZeroParams())
            } else resultState
            _uiState.update {
                if (loaded is ResultState.Success) {
                    currentTrain = loaded.data
                    loaded.data?.let { train ->
                        setStations(train.stations)
                    }
                }
                it.copy(
                    selectedServicePhase = currentTrain?.servicePhase,
                    trainDetailState = loaded
                )
            }
        }
    }

    private fun String?.zeroToNull(): String? = this?.takeUnless { ZERO.matches(it.trim()) }

    private fun Train.withoutZeroParams(): Train = copy(
        distance = distance.zeroToNull(),
        weight = weight.zeroToNull(),
        axle = axle.zeroToNull(),
        conditionalLength = conditionalLength.zeroToNull(),
        dataVersions = dataVersions.map {
            it.copy(
                weight = it.weight.zeroToNull(),
                axle = it.axle.zeroToNull(),
                conditionalLength = it.conditionalLength.zeroToNull(),
            )
        },
    )

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
                            trackNumber = state.trackNumber,
                            isFinalStation = state.isFinalStation,
                            isPassingStation = state.isPassingStation,
                            segmentTrackNumber = state.segmentTrackNumber,
                            segmentNotes = state.segmentNotes
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
            sharedPreferenceStorage.setSettingsSyncPending(true)

            // Now save the train
            val state = _uiState.value.trainDetailState
            if (state is ResultState.Success) {
                state.data?.let { train ->
                    train.servicePhase = newPhase.forTrain()
                    performSave(train)
                }
            }
        }
    }


    private fun loadUsageStats() {
        viewModelScope.launch(Dispatchers.Default) {
            runCatching {
                val routes = routeUseCase.getListRoutes()
                val trains = routes.flatMap { r -> r.trains.map { r to it } }
                stationUsage = StationSuggestionOrder.usage(trains.asSequence().flatMap { (_, t) -> t.stations.asSequence().map { it.stationName } })
                phaseUsage = trains.mapNotNull { (_, t) -> t.servicePhase?.id }.groupingBy { it }.eachCount()
                phaseLastUsed = trains.mapNotNull { (r, t) -> t.servicePhase?.id?.let { id -> id to (r.basicData.timeStartWork ?: 0L) } }
                    .groupBy({ it.first }, { it.second }).mapValues { (_, v) -> v.max() }
            }.onFailure { it.sendToSentry("TrainFormViewModel", "loadUsageStats") }
        }
    }

    private fun refreshFix(onFix: (GeoFix) -> Unit = {}) {
        viewModelScope.launch {
            val fix = DeviceLocation.currentFix(appContext) ?: return@launch
            lastFix = fix
            onFix(fix)
        }
    }

    /** Карта поезда (§6.9, состояние 3): станция без координат, которую можно определить по GPS, либо null. */
    fun trainMapMissingStation(): String? {
        val phase = fullSelectedPhase()?.let { stationDirectory.resolve(it) }
        val phaseNames = phase?.let { p ->
            buildList {
                if (p.departurePoint == null) add(p.departureStation)
                if (p.arrivalPoint == null) add(p.arrivalStation)
            }
        }.orEmpty()
        return StationDirectory.stationWithoutCoordinates(
            stationsListState.map { it.station.data },
            phaseNames,
            stationDirectory.stations(),
        )
    }

    /** Станции выбранного плеча с координатами по порядку для будущего маршрута на карте поезда (§6.9, состояние 2). */
    /**
     * Поезда маршрута для линии поезда на профиле карты: id, номер и длина в метрах по условной длине (у пассажирского номера -
     * число вагонов × длина вагона из настроек, у остальных × 14 м). Редактируемый сейчас поезд берётся со своими несохранёнными
     * значениями; поездов в маршруте нет - только он.
     */
    internal fun trainMapTrains(): List<com.z_company.route.component.servicephase.ProfileTrain> {
        val wagon = uiState.value.passengerWagonLengthMeters
        val current = currentTrain
        val all = route.trains.map { if (current != null && it.trainId == current.trainId) current else it }
            .let { list -> if (current != null && list.none { it.trainId == current.trainId }) list + current else list }
        return all.map { t ->
            com.z_company.route.component.servicephase.ProfileTrain(
                id = t.trainId,
                number = t.number,
                lengthM = com.z_company.route.ui.conditionalLengthToMeters(t.conditionalLength, t.number, wagon),
            )
        }
    }

    fun trainMapStops(): List<com.z_company.domain.util.TrainMapStop> =
        fullSelectedPhase()?.let { stationDirectory.resolve(it) }?.let(com.z_company.domain.util.TrainMapRoute::stops).orEmpty()

    /** Записанный трек поезда для карты (SCREEN_SPECS §6.9, этап 4а). */
    class TrainMapTrack(
        /** Все точки (по времени) — для тапа по пройденному пути. */
        val points: List<com.z_company.domain.track.TrackPoint>,
        /** Прореженные точки для отрисовки линии. */
        val drawPoints: List<com.z_company.domain.track.TrackPoint>,
        val stops: List<com.z_company.domain.track.TrainStopMark>,
        /** Пояс отображения времени (displayTimeZone). */
        val zone: String,
        /** Поезд пассажирский/пригородный - шкала цвета скорости с пассажирскими порогами (§6.9). */
        val passenger: Boolean = false,
    )

    /** Трек поезда из локального хранилища, асинхронно (не на главном потоке); null — трека нет. */
    suspend fun trainMapTrack(): TrainMapTrack? {
        val id = currentTrain?.trainId ?: return null
        val stationTimes = stationsListState.map {
            com.z_company.domain.track.StationTimes(it.station.data, it.arrival.data, it.departure.data, it.isPassingStation)
        }
        val zone = runCatching { settingsUseCase.getUserSettingFlow().first().displayTimeZone() }.getOrDefault("GMT+3")
        return kotlinx.coroutines.withContext(Dispatchers.Default) {
            val points = trackRepository.tracksForTrain(id)
                .sortedBy { it.startedAt }
                .flatMap { trackRepository.points(it.trackId) }
                .sortedBy { it.timeMs }
                // Подменные точки GNSS (старые записи) не должны давать ложные старт/финиш и линию через полстраны (§6.9).
                .let { com.z_company.domain.util.GnssPlausibility.sanitize(it) }
            if (points.isEmpty()) return@withContext null
            val dwells = com.z_company.domain.track.TrackAnalysis.detectDwells(points)
            TrainMapTrack(
                points = points,
                drawPoints = com.z_company.domain.track.TrackAnalysis.decimate(points),
                stops = nameUnnamedStops(
                    com.z_company.domain.track.TrackAnalysis.stopMarks(points, dwells, stationTimes),
                    currentTrain?.servicePhase, stationTimes.map { it.name },
                ),
                zone = zone,
                passenger = com.z_company.domain.track.SpeedScale.isPassengerTrain(currentTrain),
            )
        }
    }

    /**
     * Остановки по треку без станции поезда получают название той же функцией, что «АВТО» (плечо и порядок станций,
     * радиус 1 км, иначе справочник); станции поезда остаются как есть (единый источник правды, SCREEN_SPECS §6.9).
     */
    private suspend fun nameUnnamedStops(
        marks: List<com.z_company.domain.track.TrainStopMark>,
        phase: com.z_company.domain.entities.setting.ServicePhase?,
        trainStationNames: List<String?>,
    ): List<com.z_company.domain.track.TrainStopMark> {
        if (marks.none { it.name == null }) return marks
        val dir = runCatching { stationDirectory.stations() }.getOrNull() ?: return marks
        val points = StationDirectory.points(dir)
        val names = StationDirectory.names(dir)
        val written = ServicePhaseStationPicker.writtenNames(trainStationNames, phase != null)
        val full = phase?.let { sp -> servicePhaseList.firstOrNull { it.id == sp.id } ?: sp }?.let { stationDirectory.resolve(it) }
        return marks.map { mk ->
            if (mk.name != null) mk
            else mk.copy(
                name = stopNamer.name(
                    full, written, mk.latitude, mk.longitude, points, names,
                    onDiscovered = { stationDirectory.fillCoordinates(it.name, it.latitude, it.longitude) },
                ),
            )
        }
    }

    /** Пояс отображения времени (displayTimeZone) для ввода планового времени прибытия на карте (§6.9). */
    suspend fun trainMapZone(): String =
        runCatching { settingsUseCase.getUserSettingFlow().first().displayTimeZone() }.getOrDefault("GMT+3")

    /** Ключ хранения плана прибытия на карте - id поезда; null - поезд ещё не создан. */
    fun trainMapPlanKey(): String? = currentTrain?.trainId?.toString()

    /** Состояние записи трека для пустого графика скорости (§6.9): согласие, «АВТО», цель; null - поезд ещё не определён. */
    suspend fun trainMapRecordingState(): com.z_company.domain.track.TrackRecordingState? {
        val thisId = currentTrain?.trainId ?: return null
        val target = autoRecord.targetTrainId()
        val otherNumber = if (target != null && target != thisId) {
            runCatching {
                kotlinx.coroutines.withContext(Dispatchers.Default) {
                    trainUseCase.getTrainById(target).first { it !is ResultState.Loading }.let { (it as? ResultState.Success)?.data?.number }
                }
            }.getOrNull()
        } else null
        return autoRecord.trackRecordingState(thisId, otherNumber)
    }

    /** Предупреждение «Запись уже идёт», если «АВТО» пишет другой поезд (иначе null) - для «Сделать целью» (§6.9). */
    suspend fun trainMapConflictText(): String? {
        val thisId = currentTrain?.trainId?.toString() ?: return null
        val enabled = autoRecord.state.value.enabled
        return if (com.z_company.domain.autorecord.AutoTargetRules.conflictsWith(enabled, autoRecord.targetTrainId(), thisId)) {
            autoRecord.conflictText()
        } else null
    }

    /** Сделать этот поезд целью записи «АВТО» (кнопка пустого графика, §6.9). */
    fun trainMapMakeTarget(): Boolean {
        val id = currentTrain?.trainId ?: return false
        return autoRecord.setTarget(id)
    }

    /** Текущее местоположение для карты поезда; null — нет разрешения/сигнала. */
    suspend fun trainMapCurrentFix(): GeoFix? = DeviceLocation.currentFix(appContext)

    /** Определить координаты [stationName] по текущему месту и записать в справочник (только если их там нет). */
    suspend fun saveStationCoordinatesFromGps(stationName: String): Boolean {
        val fix = DeviceLocation.currentFix(appContext, timeoutMs = 10_000) ?: return false
        lastFix = fix
        stationDirectory.fillCoordinates(stationName, fix.latitude, fix.longitude)
        return true
    }

    /** Полное плечо (со станциями) для выбранного в поезде — в рейсе хранится копия без станций. */
    private fun fullSelectedPhase(): ServicePhase? {
        val id = _uiState.value.selectedServicePhase?.id ?: return null
        return servicePhaseList.firstOrNull { it.id == id }
    }

    /** У выбранного плеча есть координаты станций — подстановка по местоположению имеет смысл. */
    fun selectedPhaseHasCoordinates(): Boolean = fullSelectedPhase()?.let(ServicePhaseStationPicker::hasCoordinates) == true

    /**
     * Название для новой станции по местоположению - ТА ЖЕ функция, что у «АВТО», карты и просмотра трека
     * ([StopStationNamer], SCREEN_SPECS §6.2 п.6/6а): радиус 1 км; с плечом - по порядку плеча (не ранее последней
     * записанной станции, повтор допускается), иначе справочник; без плеча (или без координат у плеча) - станция по
     * рельсам с сервера, при сбое - справочник по прямой.
     */
    private suspend fun suggestStationName(names: List<String?>, fix: GeoFix): String? =
        kotlinx.coroutines.withContext(Dispatchers.Default) {
            val phase = fullSelectedPhase()?.let { stationDirectory.resolve(it) }
            val dir = stationDirectory.stations()
            stopNamer.name(
                phase,
                ServicePhaseStationPicker.writtenNames(names, _uiState.value.selectedServicePhase != null),
                fix.latitude, fix.longitude,
                StationDirectory.points(dir), StationDirectory.names(dir),
                onDiscovered = { stationDirectory.fillCoordinates(it.name, it.latitude, it.longitude) },
            )
        }

    /**
     * Подставить название новой станции [stationId] по местоположению — асинхронно, когда
     * придёт координата; только если пользователь сам ничего не ввёл.
     */
    private fun autoNameNewStation(stationId: String) {
        refreshFix { fix ->
            viewModelScope.launch {
                val names = stationsListState.filter { it.id != stationId }.map { it.station.data }
                val name = suggestStationName(names, fix)
                val idx = stationsListState.indexOfFirst { it.id == stationId }
                if (name != null && idx >= 0 && stationsListState[idx].station.data.isNullOrBlank()) {
                    stationsListState[idx] = stationsListState[idx].copy(station = StationField(data = name, type = StationDataType.NAME))
                    changesHave()
                }
            }
        }
    }

    /** Порядок плеч в шторке выбора: последнее с текущей станции → другие с неё → по частоте. */
    fun sortedServicePhases(phases: List<ServicePhase>): List<ServicePhase> {
        val here = ServicePhaseOrder.currentStation(phases, stationsListState.firstOrNull()?.station?.data, lastFix)
        return ServicePhaseOrder.sort(phases, here, phaseUsage, phaseLastUsed)
    }

    private fun sortSuggestions(names: List<String>): List<String> =
        StationSuggestionOrder.sort(
            names,
            stationUsage,
            StationDirectory.points(stationDirectory.stations()) + ServicePhaseStationPicker.knownPoints(servicePhaseList),
            lastFix,
        )

    // Правки плеч и справочника станций — read-modify-write одних настроек: по очереди.
    private val settingsMutex = Mutex()

    /** Страна пользователя — для автоподбора станций плеча. */
    var userCountry by mutableStateOf("RU")
        private set

    /** Новые названия из автоподбора станций плеча — в справочник станций. */
    fun addStationNames(names: List<String>) {
        viewModelScope.launch {
            settingsMutex.withLock {
                val settings = settingsUseCase.getUserSettingFlow().first()
                val existing = settings.stationList.map { it.trim().lowercase() }.toSet()
                val fresh = names.map(String::trim).filter { it.isNotBlank() && it.lowercase() !in existing }.distinct()
                if (fresh.isEmpty()) return@withLock
                settingsUseCase.saveSetting(settings.copy(stationList = fresh + settings.stationList)).first()
                sharedPreferenceStorage.setSettingsSyncPending(true)
                fresh.forEach { if (it !in mutableStationList) mutableStationList.add(it) }
            }
        }
    }

    /**
     * Добавить новое плечо в настройки (сохраняется в UserSettings.servicePhases). Редактор плеча
     * сохраняет автоматически и повторно — плечо с тем же id заменяется, а не дублируется.
     */
    fun addServicePhase(phase: ServicePhase) {
        viewModelScope.launch {
            settingsMutex.withLock {
            val settings = settingsUseCase.getUserSettingFlow().first()
            val phases = if (settings.servicePhases.any { it.id == phase.id }) {
                settings.servicePhases.map { if (it.id == phase.id) phase else it }
            } else {
                settings.servicePhases + phase
            }
            val updated = settings.copy(servicePhases = phases)
            settingsUseCase.saveSetting(updated).first()
            sharedPreferenceStorage.setSettingsSyncPending(true)
            }
        }
    }

    /** Обновить существующее плечо (по id). Если оно сейчас выбрано — обновляем ссылку. */
    fun updateServicePhase(newPhase: ServicePhase) {
        viewModelScope.launch {
            settingsMutex.withLock {
                val settings = settingsUseCase.getUserSettingFlow().first()
                val updatedList = settings.servicePhases.map {
                    if (it.id == newPhase.id) newPhase else it
                }
                settingsUseCase.saveSetting(settings.copy(servicePhases = updatedList)).first()
            }
            sharedPreferenceStorage.setSettingsSyncPending(true)
            if (_uiState.value.selectedServicePhase?.id == newPhase.id) {
                _uiState.update { it.copy(selectedServicePhase = newPhase.forTrain()) }
                setDistance(newPhase.distance.takeIf { it > 0 }?.toString() ?: "")
            }
        }
    }

    /** Удалить плечо. Если оно было выбрано — снимаем выбор. */
    fun deleteServicePhase(phase: ServicePhase) {
        viewModelScope.launch {
            val settings = settingsUseCase.getUserSettingFlow().first()
            val updatedList = settings.servicePhases.filterNot { it.id == phase.id }
            settingsUseCase.saveSetting(settings.copy(servicePhases = updatedList)).first()
            sharedPreferenceStorage.setSettingsSyncPending(true)
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
        currentTrain?.let { train ->
            val value = weight.sanitizeNumericInput().ifBlank { null }
            currentTrain = train.copy(
                weight = value,
                dataVersions = train.dataVersions.updateLast { it.copy(weight = value) }
            )
        }
        changesHave()
    }

    fun setAxle(axle: String) {
        currentTrain?.let { train ->
            val value = axle.sanitizeNumericInput().ifBlank { null }
            currentTrain = train.copy(
                axle = value,
                dataVersions = train.dataVersions.updateLast { it.copy(axle = value) }
            )
        }
        changesHave()
    }

    fun setConditionalLength(length: String) {
        currentTrain?.let { train ->
            val value = length.sanitizeNumericInput(allowDecimal = true).ifBlank { null }
            currentTrain = train.copy(
                conditionalLength = value,
                dataVersions = train.dataVersions.updateLast { it.copy(conditionalLength = value) }
            )
        }
        changesHave()
    }

    fun addTrainDataVersion(
        stationId: String,
        stationName: String?,
        weight: String,
        axle: String,
        conditionalLength: String
    ) {
        currentTrain?.let { train ->
            val versions = train.dataVersions.toMutableList()
            if (versions.isEmpty()) {
                versions += TrainDataVersion(
                    weight = train.weight,
                    axle = train.axle,
                    conditionalLength = train.conditionalLength
                )
            }
            val newVersion = TrainDataVersion(
                stationId = stationId,
                stationName = stationName?.ifBlank { null },
                weight = weight.sanitizeNumericInput().ifBlank { null },
                axle = axle.sanitizeNumericInput().ifBlank { null },
                conditionalLength = conditionalLength.sanitizeNumericInput(allowDecimal = true)
                    .ifBlank { null },
                changedAt = System.currentTimeMillis()
            )
            val existingIndex = versions.indexOfFirst { it.stationId == stationId }
            if (existingIndex >= 0) {
                versions[existingIndex] = newVersion.copy(
                    changedAt = versions[existingIndex].changedAt.takeIf { it != 0L }
                        ?: newVersion.changedAt
                )
            } else {
                versions += newVersion
            }
            val latest = versions.last()
            currentTrain = train.copy(
                weight = latest.weight,
                axle = latest.axle,
                conditionalLength = latest.conditionalLength,
                dataVersions = versions
            )
            changesHave()
        }
    }

    fun updateTrainDataVersion(
        index: Int,
        weight: String,
        axle: String,
        conditionalLength: String
    ) {
        currentTrain?.let { train ->
            if (index !in train.dataVersions.indices) return
            val versions = train.dataVersions.toMutableList()
            versions[index] = versions[index].copy(
                weight = weight.ifBlank { null },
                axle = axle.ifBlank { null },
                conditionalLength = conditionalLength.ifBlank { null }
            )
            val latest = versions.last()
            currentTrain = train.copy(
                weight = latest.weight,
                axle = latest.axle,
                conditionalLength = latest.conditionalLength,
                dataVersions = versions
            )
            changesHave()
        }
    }

    fun deleteTrainDataVersion(index: Int) {
        currentTrain?.let { train ->
            if (index !in train.dataVersions.indices) return
            val versions = train.dataVersions.toMutableList().apply { removeAt(index) }
            val latest = versions.lastOrNull()
            currentTrain = train.copy(
                weight = latest?.weight ?: train.weight,
                axle = latest?.axle ?: train.axle,
                conditionalLength = latest?.conditionalLength ?: train.conditionalLength,
                dataVersions = versions
            )
            changesHave()
        }
    }

    private fun List<TrainDataVersion>.updateLast(
        transform: (TrainDataVersion) -> TrainDataVersion
    ): List<TrainDataVersion> = if (isEmpty()) this else toMutableList().also {
        it[lastIndex] = transform(it[lastIndex])
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

    // --- Вагонник (осмотр/закрепление состава перед прицепкой) ---
    fun addCarInspector() {
        currentTrain = currentTrain?.copy(carInspector = com.z_company.domain.entities.route.CarInspector())
        changesHave()
    }

    fun removeCarInspector() {
        currentTrain = currentTrain?.copy(carInspector = null)
        changesHave()
    }

    fun setCarInspectorFullName(value: String) {
        currentTrain = currentTrain?.copy(
            carInspector = (currentTrain?.carInspector ?: com.z_company.domain.entities.route.CarInspector())
                .copy(fullName = value.ifBlank { null })
        )
        changesHave()
    }

    fun setCarInspectorTabNumber(value: String) {
        currentTrain = currentTrain?.copy(
            carInspector = (currentTrain?.carInspector ?: com.z_company.domain.entities.route.CarInspector())
                .copy(tabNumber = value.ifBlank { null })
        )
        changesHave()
    }

    fun setCarInspectorCouplingTime(value: Long?) {
        currentTrain = currentTrain?.copy(
            carInspector = (currentTrain?.carInspector ?: com.z_company.domain.entities.route.CarInspector())
                .copy(couplingTime = value)
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

    /**
     * Событие кнопки «АВТО» (SCREEN_SPECS §6.2 п.6а), пока форма открыта: те же правила, что в БД,
     * но применённые к состоянию формы. false — записывать нечего (форма ещё грузится / событие пропущено).
     */
    private suspend fun applyAutoEvent(event: com.z_company.domain.autorecord.StopEvent, name: String?): Boolean {
        val train = currentTrain ?: return false
        if (_uiState.value.stationsListState == null) return false
        val snapshot = train.copy(
            servicePhase = _uiState.value.selectedServicePhase,
            stations = stationsListState.map { s ->
                Station(
                    stationId = s.id,
                    trainId = train.trainId,
                    stationName = s.station.data,
                    timeArrival = s.arrival.data,
                    timeDeparture = s.departure.data,
                    trackNumber = s.trackNumber,
                    isFinalStation = s.isFinalStation,
                    isPassingStation = s.isPassingStation,
                    segmentTrackNumber = s.segmentTrackNumber,
                    segmentNotes = s.segmentNotes
                )
            }.toMutableList()
        )
        val updated = com.z_company.domain.autorecord.AutoTrainWriter.apply(snapshot, event, name) ?: return false
        stationsListState.clear()
        setStations(updated.stations)
        changesHave()
        return true
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
                    trackNumber = station.trackNumber,
                    isFinalStation = station.isFinalStation,
                    isPassingStation = station.isPassingStation,
                    segmentTrackNumber = station.segmentTrackNumber,
                    segmentNotes = station.segmentNotes
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
            // Проходная станция (время проследования) в логике кнопки ▶ не участвует.
            if (station.isPassingStation) continue
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
            autoNameNewStation(stationsListState[0].id)
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
            var nextIdx = idx + 1
            // Проходные станции пропускаем: остановку записываем на следующую непроходную.
            while (nextIdx <= stationsListState.lastIndex && stationsListState[nextIdx].isPassingStation) nextIdx++
            val isServicePhaseArrival = hasServicePhase && nextIdx == stationsListState.lastIndex
            if (nextIdx <= stationsListState.lastIndex && !isServicePhaseArrival) {
                // Следующая станция уже существует и это не конечная станция плеча
                setArrivalTime(nextIdx, now)
            } else {
                // Нужно добавить новую станцию (или nextIdx указывает на конечную станцию плеча)
                addingStation() // с плечом вставит перед последней, без — в конец
                val newIdx = if (hasServicePhase) stationsListState.lastIndex - 1 else stationsListState.lastIndex
                setArrivalTime(newIdx, now)
                autoNameNewStation(stationsListState[newIdx].id)
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
        _uiState.update { it.copy(editingStationIndex = -1, suggestedNewStationName = null) }
        refreshFix { fix ->
            viewModelScope.launch {
                val name = suggestStationName(stationsListState.map { it.station.data }, fix)
                if (_uiState.value.editingStationIndex == -1) _uiState.update { it.copy(suggestedNewStationName = name) }
            }
        }
    }

    fun stopEditingStation() {
        _uiState.update { it.copy(editingStationIndex = null) }
    }

    fun saveStationFromSheet(
        index: Int,
        name: String?,
        arrival: Long?,
        departure: Long?,
        trackNumber: String?,
        isFinalStation: Boolean = false,
        isPassingStation: Boolean = false,
        newStationId: String? = null
    ) {
        if (index == -1) {
            val newStation = StationFormState(
                id = newStationId ?: Station().stationId,
                station = StationField(data = name, type = StationDataType.NAME),
                arrival = StationFieldDate(data = arrival, type = StationDataType.ARRIVAL),
                departure = StationFieldDate(data = departure, type = StationDataType.DEPARTURE),
                trackNumber = trackNumber,
                isFinalStation = isFinalStation,
                isPassingStation = isPassingStation
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
                trackNumber = trackNumber,
                isFinalStation = isFinalStation,
                isPassingStation = isPassingStation
            )
        }
        changesHave()
        checkFormValidStation()
        stopEditingStation()
    }

    /** Показать/скрыть карточки перегонов. Выбор запоминается между входами на экран. */
    fun toggleSegmentsVisibility() {
        val newValue = !_uiState.value.showSegments
        _uiState.update { it.copy(showSegments = newValue) }
        sharedPreferenceStorage.setShowSegments(newValue)
    }

    // ── Перегон (между station[afterIndex] и station[afterIndex + 1]) ──
    fun startEditingSegment(afterIndex: Int) {
        _uiState.update { it.copy(editingSegmentAfterIndex = afterIndex) }
    }

    fun stopEditingSegment() {
        _uiState.update { it.copy(editingSegmentAfterIndex = null) }
    }

    fun saveSegmentFromSheet(
        afterIndex: Int,
        fromName: String?,
        toName: String?,
        trackNumber: String?,
        notes: String?
    ) {
        val toIndex = afterIndex + 1
        if (afterIndex in stationsListState.indices) {
            stationsListState[afterIndex] = stationsListState[afterIndex].copy(
                station = stationsListState[afterIndex].station.copy(data = fromName)
            )
        }
        if (toIndex in stationsListState.indices) {
            stationsListState[toIndex] = stationsListState[toIndex].copy(
                station = stationsListState[toIndex].station.copy(data = toName),
                segmentTrackNumber = trackNumber,
                segmentNotes = notes
            )
        } else if (toIndex == stationsListState.size && !toName.isNullOrBlank()) {
            // «Будущий перегон»: второй станции ещё нет. Название, введённое
            // в поле «До», и создаёт её — иначе перегон некуда было бы записать.
            stationsListState.add(
                StationFormState(
                    id = Station().stationId,
                    station = StationField(data = toName, type = StationDataType.NAME),
                    segmentTrackNumber = trackNumber,
                    segmentNotes = notes
                )
            )
        }
        changesHave()
        checkFormValidStation()
        stopEditingSegment()
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
            mutableStationList.clear()
            mutableStationList.addAll(sortSuggestions(stationNameList.distinct()))
        } else {
            mutableStationList.clear()
            val newStationList =
                sortSuggestions(
                    stationNameList
                        .filter { com.z_company.route.util.stationNameMatches(it, value) }
                        .filterNot { it == value }
                        .distinct()
                ).toMutableStateList()
            newStationList.forEach { st ->
                mutableStationList.add(st)
                changeExpandedMenu(index, true)
            }
        }
    }
}

private val ZERO = Regex("^0+([.,]0+)?$")
