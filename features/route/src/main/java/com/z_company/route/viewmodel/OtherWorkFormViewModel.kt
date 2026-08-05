package com.z_company.route.viewmodel

import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.core.ResultState
import com.z_company.core.util.DateAndTimeConverter
import com.z_company.domain.entities.route.Locomotive
import com.z_company.domain.entities.route.OtherWork
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.setting.UserSettings
import com.z_company.domain.repositories.SharedPreferencesRepositories
import com.z_company.domain.use_cases.OtherWorkUseCase
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.domain.util.addAllOrSkip
import com.z_company.domain.util.minus
import com.z_company.route.Const.NULLABLE_ID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.properties.Delegates

class OtherWorkFormViewModel(
    otherWorkId: String?,
    basicId: String
) : ViewModel(), KoinComponent {
    private val otherWorkUseCase: OtherWorkUseCase by inject()
    private val settingsUseCase: SettingsUseCase by inject()
    private val routeUseCase: RouteUseCase by inject()
    private val sharedPreferences: SharedPreferencesRepositories by inject()

    private val _uiState = MutableStateFlow(OtherWorkFormUiState())
    val uiState = _uiState.asStateFlow()

    private val _userSetting = MutableStateFlow(UserSettings())

    /** Одноразовые сообщения-тосты (например, для авто-подстановки времени). */
    private val _message = MutableSharedFlow<String>()
    val message = _message.asSharedFlow()

    /** Родительский маршрут — для авто-подстановки времени (явка/сдача, приёмка/сдача локо). */
    private var route: Route? = null

    var timeZoneText: String = "GMT+3"

    private var loadJob: Job? = null
    private var saveJob: Job? = null
    private var autoSaveJob: Job? = null

    private var isNewOtherWork by Delegates.notNull<Boolean>()

    private var initStationList = mutableListOf<String>()

    var currentOtherWork: OtherWork?
        get() = _uiState.value.otherWorkDetailState.let {
            if (it is ResultState.Success) it.data else null
        }
        private set(value) {
            _uiState.update {
                it.copy(otherWorkDetailState = ResultState.Success(value))
            }
        }

    init {
        viewModelScope.launch {
            if (otherWorkId == NULLABLE_ID) {
                isNewOtherWork = true
                // Новую запись предзаполняем последним выбранным типом.
                currentOtherWork = OtherWork(
                    basicId = basicId,
                    workType = sharedPreferences.getLastOtherWorkType()
                )
            } else {
                isNewOtherWork = false
                loadOtherWork(otherWorkId!!)
            }

            // Держим родительский маршрут актуальным — для кнопок авто-подстановки времени.
            routeUseCase.routeDetails(basicId).onEach { state ->
                if (state is ResultState.Success) route = state.data
            }.launchIn(viewModelScope)

            val settings = settingsUseCase.getUserSettingFlow().first()
            _userSetting.value = settings
            timeZoneText = settingsUseCase.getTimeZone(settings.timeZone)
            initStationList = settings.stationList.toMutableStateList()
            _uiState.update {
                it.copy(
                    dateAndTimeConverter = DateAndTimeConverter(settings),
                    workTypeList = buildTypeList(settings)
                )
            }

            settingsUseCase.getFlowCurrentSettingsState().onEach { state ->
                if (state is ResultState.Success) {
                    state.data?.let { s ->
                        initStationList = s.stationList.toMutableStateList()
                        _uiState.update { it.copy(workTypeList = buildTypeList(s)) }
                    }
                }
            }.launchIn(viewModelScope)
        }
    }

    private fun buildTypeList(settings: UserSettings): List<String> {
        return (OtherWork.PREDEFINED_TYPES + settings.otherWorkTypeList)
            .filter { it.isNotBlank() }
            .distinct()
    }

    private fun loadOtherWork(id: String) {
        loadJob?.cancel()
        loadJob = otherWorkUseCase.getOtherWorkById(id).onEach { result ->
            _uiState.update { it.copy(otherWorkDetailState = result) }
            if (result is ResultState.Success) {
                currentOtherWork = result.data
                recalcResultTime()
            }
        }.launchIn(viewModelScope)
    }

    private fun changesHave() {
        if (!_uiState.value.changesHaveState) {
            _uiState.update { it.copy(changesHaveState = true) }
        }
        triggerAutoSave()
    }

    private fun triggerAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(500)
            saveOtherWork()
        }
    }

    private fun recalcResultTime() {
        val start = currentOtherWork?.timeStart
        val end = currentOtherWork?.timeEnd
        val result = if (start != null && end != null) end - start else null
        val error = if (start != null && end != null && end <= start) {
            "Окончание должно быть позже начала."
        } else null
        _uiState.update {
            it.copy(
                resultTime = if (error == null) result else null,
                errorMessage = error
            )
        }
    }

    private fun isOtherWorkEmpty(ow: OtherWork): Boolean {
        return ow.workType.isNullOrBlank() &&
                ow.station.isNullOrBlank() &&
                ow.timeStart == null &&
                ow.timeEnd == null &&
                ow.notes.isNullOrBlank()
    }

    fun setWorkType(type: String) {
        val value = type.ifBlank { null }
        currentOtherWork = currentOtherWork?.copy(workType = value)
        // Запоминаем последний выбранный тип — подставится в следующую новую запись.
        if (value != null) sharedPreferences.setLastOtherWorkType(value)
        changesHave()
    }

    /**
     * Автоподстановка времени «От явки до окончания работы»:
     * начало = явка ([BasicData.timeStartWork]), окончание = сдача ([BasicData.timeEndWork]).
     */
    fun applyTimeFromWork() {
        val start = route?.basicData?.timeStartWork
        val end = route?.basicData?.timeEndWork
        if (start == null || end == null) {
            emitMessage("Укажите время явки и окончания работы в маршруте")
            return
        }
        currentOtherWork = currentOtherWork?.copy(timeStart = start, timeEnd = end)
        recalcResultTime()
        changesHave()
    }

    /**
     * Автоподстановка времени «От приёмки до сдачи локомотива».
     * Нет локо → тост. Один локо → подставляем сразу. Несколько → открываем шторку
     * выбора локомотива (как на главном), выбор задаёт время его приёмки/сдачи.
     */
    fun applyTimeFromLoco() {
        val locomotives = route?.locomotives.orEmpty()
        when {
            locomotives.isEmpty() -> emitMessage("В маршруте нет локомотива")
            locomotives.size == 1 -> applyLocoTime(locomotives.first())
            else -> _uiState.update { it.copy(locoPicker = locomotives) }
        }
    }

    /** Выбор локомотива в шторке — задаёт время по его приёмке/сдаче. */
    fun applyTimeFromLoco(locomotive: Locomotive) {
        _uiState.update { it.copy(locoPicker = null) }
        applyLocoTime(locomotive)
    }

    fun dismissLocoPicker() {
        _uiState.update { it.copy(locoPicker = null) }
    }

    private fun applyLocoTime(locomotive: Locomotive) {
        // Приоритет — время КП (контрольного поста): выезд → начало, въезд → окончание.
        // Если КП не указан — берём окончание приёмки / начало сдачи локомотива.
        val startBase = locomotive.timeBarrierOut ?: locomotive.timeEndOfAcceptance
        val endBase = locomotive.timeBarrierIn ?: locomotive.timeStartOfDelivery
        if (startBase == null || endBase == null) {
            emitMessage("Укажите время КП или окончание приёмки и начало сдачи локомотива")
            return
        }
        // Прочая работа не пересекается с границами: начало на минуту позже, окончание на минуту раньше.
        currentOtherWork = currentOtherWork?.copy(
            timeStart = startBase + 60_000L,
            timeEnd = endBase - 60_000L
        )
        recalcResultTime()
        changesHave()
    }

    private fun emitMessage(text: String) {
        viewModelScope.launch { _message.emit(text) }
    }

    /** Добавляет пользовательский тип работы (явно, по кнопке) и выбирает его. */
    fun addCustomType(name: String) {
        val value = name.trim()
        if (value.isBlank()) return
        setWorkType(value)
        if (!OtherWork.PREDEFINED_TYPES.contains(value)) {
            viewModelScope.launch(Dispatchers.IO) { settingsUseCase.setOtherWorkType(value) }
        }
    }

    /** Удаляет пользовательский тип из настроек; если он выбран — снимает выбор. */
    fun deleteCustomType(name: String) {
        viewModelScope.launch(Dispatchers.IO) { settingsUseCase.removeOtherWorkType(name) }
        if (currentOtherWork?.workType == name) {
            currentOtherWork = currentOtherWork?.copy(workType = null)
            changesHave()
        }
    }

    fun setStation(station: String) {
        currentOtherWork = currentOtherWork?.copy(station = station.ifBlank { null })
        changesHave()
    }

    fun setTimeStart(time: Long?) {
        currentOtherWork = currentOtherWork?.copy(timeStart = time)
        recalcResultTime()
        changesHave()
    }

    fun setTimeEnd(time: Long?) {
        currentOtherWork = currentOtherWork?.copy(timeEnd = time)
        recalcResultTime()
        changesHave()
    }

    fun setNotes(notes: String) {
        currentOtherWork = currentOtherWork?.copy(notes = notes.ifBlank { null })
        changesHave()
    }

    fun saveOtherWork() {
        if (_uiState.value.errorMessage != null) return
        val state = _uiState.value.otherWorkDetailState
        if (state is ResultState.Success) {
            state.data?.let { ow ->
                saveJob?.cancel()
                saveJob = otherWorkUseCase.saveOtherWork(ow).onEach { resultState ->
                    _uiState.update { it.copy(saveOtherWorkState = resultState) }
                }.launchIn(viewModelScope)
            }
        }
    }

    fun resetSaveState() {
        _uiState.update { it.copy(saveOtherWorkState = null) }
    }

    fun exitWithoutSaving() {
        if (isNewOtherWork) {
            val state = _uiState.value.otherWorkDetailState
            if (state is ResultState.Success) {
                state.data?.let { ow ->
                    otherWorkUseCase.removeOtherWork(ow).onEach { result ->
                        if (result is ResultState.Success) {
                            _uiState.update {
                                it.copy(confirmExitDialogShow = false, exitFromScreen = true)
                            }
                        }
                    }.launchIn(viewModelScope)
                }
            }
        } else {
            _uiState.update { it.copy(confirmExitDialogShow = false, exitFromScreen = true) }
        }
    }

    override fun onCleared() {
        autoSaveJob?.cancel()
        saveJob?.cancel()
        CoroutineScope(NonCancellable + Dispatchers.IO).launch {
            val state = _uiState.value.otherWorkDetailState
            if (state is ResultState.Success) {
                state.data?.let { ow ->
                    // Новую запись сохраняем, только если пользователь реально вносил данные.
                    // Тип работы может быть предзаполнен последним выбранным — сам по себе он
                    // не считается вводом, поэтому дополнительно проверяем changesHaveState.
                    if (isNewOtherWork && (!_uiState.value.changesHaveState || isOtherWorkEmpty(ow))) return@launch
                    if (_uiState.value.errorMessage != null) return@launch
                    otherWorkUseCase.saveOtherWork(ow).collect {}
                    // Станцию в общий список сохраняем только ЗДЕСЬ — финальным (закоммиченным)
                    // значением при выходе, а не на каждый keystroke, чтобы промежуточный ввод
                    // («Жа», «Жаро») не оседал мусором в списке станций.
                    ow.station?.takeIf { it.isNotBlank() }?.let { station ->
                        settingsUseCase.setStations(listOf(station))
                    }
                }
            }
        }
        super.onCleared()
    }

    // ── Автодополнение станций ──
    private var currentStationList: SnapshotStateList<String>
        get() = uiState.value.stationList
        private set(value) {
            _uiState.update { it.copy(stationList = value) }
        }

    fun onChangedDropDownContentStation(value: String) {
        if (value.isEmpty()) {
            currentStationList.addAllOrSkip(initStationList)
        } else {
            currentStationList.clear()
            initStationList
                .filter { it.startsWith(prefix = value, ignoreCase = true) }
                .filterNot { it == value }
                .forEach { currentStationList.add(it) }
        }
    }

    fun removeStationName(value: String) {
        // Убираем и из источника (initStationList), и из отображаемого списка
        // (currentStationList) — иначе открытая шторка не обновляется. Плюс удаляем
        // из общих настроек (единый список станций для всех экранов).
        initStationList.remove(value)
        currentStationList.remove(value)
        viewModelScope.launch {
            settingsUseCase.removeStation(value)
        }
    }
}
