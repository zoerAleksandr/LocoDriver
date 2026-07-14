@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.z_company.route.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.core.ResultState
import com.z_company.core.util.DateAndTimeConverter
import com.z_company.core.util.isEmailValid
import com.z_company.domain.entities.setting.CrossMonthTimezone
import com.z_company.domain.entities.setting.ServicePhase
import com.z_company.domain.entities.User
import com.z_company.domain.entities.setting.UserSettings
import com.z_company.domain.entities.route.LocoType
import com.z_company.domain.repositories.SharedPreferencesRepositories
import com.z_company.domain.use_cases.CalendarUseCase
import com.z_company.domain.use_cases.NormaUseCase
import com.z_company.domain.use_cases.ProductionCalendarUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.repository.remote_rest.SettingManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import com.z_company.core.sendToSentry
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SettingsViewModel : ViewModel(), KoinComponent {
    private val settingsUseCase: SettingsUseCase by inject()
    private val calendarUseCase: CalendarUseCase by inject()
    private val normaUseCase: NormaUseCase by inject()
    private val sharedPreferenceStorage: SharedPreferencesRepositories by inject()
    private val productionCalendarUseCase: ProductionCalendarUseCase by inject()
    private val settingManager: SettingManager by inject()
    private val regionalHolidaysRepository: com.z_company.domain.repositories.RegionalHolidaysRepository by inject()

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    private var loadSettingsJob: Job? = null
    private var saveSettingsJob: Job? = null
    private var autoSaveJob: Job? = null
    private var loadCalendarJob: Job? = null

    var currentSettings: UserSettings?
        get() {
            return _uiState.value.settingDetails.let {
                if (it is ResultState.Success) it.data else null
            }
        }
        private set(value) {
            _uiState.update {
                it.copy(settingDetails = ResultState.Success(value))
            }
            scheduleAutoSave()
        }

    private var servicePhases: SnapshotStateList<ServicePhase>
        get() {
            return _uiState.value.servicePhases ?: mutableStateListOf()
        }
        private set(value) {
            _uiState.update {
                it.copy(
                    servicePhases = value
                )
            }
        }

    private val oneHourInMillis = 3_600_000L
    val timeZoneList = listOf(
        TimeZoneRussia("Калининград (MSK–1, UTC+2)", oneHourInMillis * -1),
        TimeZoneRussia("Москва (UTC+3)", oneHourInMillis * 0),
        TimeZoneRussia("Самара (MSK+1, UTC+4)", oneHourInMillis * 1),
        TimeZoneRussia("Екатеринбург (MSK+2, UTC+5)", oneHourInMillis * 2),
        TimeZoneRussia("Омск (MSK+3, UTC+6)", oneHourInMillis * 3),
        TimeZoneRussia("Красноярск (MSK+4, UTC+7)", oneHourInMillis * 4),
        TimeZoneRussia("Иркутск (MSK+5, UTC+8)", oneHourInMillis * 5),
        TimeZoneRussia("Якутск (MSK+6, UTC+9)", oneHourInMillis * 6),
        TimeZoneRussia("Владивосток (MSK+7, UTC+10)", oneHourInMillis * 7),
        TimeZoneRussia("Магадан (MSK+8, UTC+11)", oneHourInMillis * 8),
        TimeZoneRussia("Камчатка (MSK+9, UTC+12)", oneHourInMillis * 9),
        TimeZoneRussia("Анадырь (MSK+10, UTC+13)", oneHourInMillis * 10),
    )

    fun changeTimeFormat(){
        currentSettings?.let {
            currentSettings = it.copy(
                isDecimalTime = !it.isDecimalTime
            )
        }
    }

    fun changeTimePickerStyle() {
        currentSettings?.let {
            currentSettings = it.copy(
                useStandardTimePicker = !it.useStandardTimePicker
            )
        }
    }

    fun showDialogAddServicePhase(servicePhase: ServicePhase) {
        _uiState.update {
            it.copy(
                showDialogAddServicePhase = true
            )
        }
    }

    fun hideDialogAddServicePhase() {
        _uiState.update {
            it.copy(
                showDialogAddServicePhase = false
            )
        }
    }

    fun addServicePhase(servicePhase: ServicePhase, index: Int = -1) {
        if (index == -1) {
            servicePhases.add(servicePhase)
        } else {
            servicePhases[index] = servicePhase
        }
        _uiState.update {
            it.copy(
                selectedServicePhase = null
            )
        }
        hideDialogAddServicePhase()
        scheduleAutoSave()
    }

    fun removeStationName(name: String) {
        viewModelScope.launch {
            settingsUseCase.removeStation(name)
        }
    }

    fun deleteServicePhase(phase: ServicePhase) {
        val removed = servicePhases.removeAll { it.id == phase.id }
        if (removed) {
            scheduleAutoSave()
        }
    }

    fun selectToUpdateServicePhase(phase: ServicePhase, index: Int) {
        _uiState.update {
            it.copy(
                selectedServicePhase = Pair(phase, index)
            )
        }
        showDialogAddServicePhase(phase)
    }

    private fun loadPurchasesInfo() {
        viewModelScope.launch {
            val maxEndTime = sharedPreferenceStorage.getSubscriptionExpiration()
            val textEndTime = if (maxEndTime == 0L) {
                ""
            } else {
                uiState.value.dateAndTimeConverter?.getDateMiniAndTime(maxEndTime) ?: ""
            }
            _uiState.update {
                it.copy(
                    purchasesEndTime = ResultState.Success(textEndTime)
                )
            }
        }
    }

    init {
        loadSettings()
        loadMonthList()
        loadPurchasesInfo()
        observeNorma()
    }

    fun resetSaveState() {
        _uiState.update {
            it.copy(saveSettingsState = null)
        }
    }

    /**
     * Наблюдает за нормой часов через NormaUseCase.
     * Реагирует на смену selectMonthOfYear в настройках — переключает поток
     * нормы на новый месяц/год через flatMapLatest.
     * Обновляет [SettingsUiState.normaHours] при любом изменении данных.
     */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private fun observeNorma() {
        viewModelScope.launch {
            try {
                // Ждём загрузки настроек (нужен selectMonthOfYear)
                _uiState
                    .map { it.settingDetails }
                    .distinctUntilChanged()
                    .flatMapLatest { settingsState ->
                        val settings = (settingsState as? com.z_company.core.ResultState.Success)?.data
                        val month = settings?.selectMonthOfYear
                        if (month != null) {
                            normaUseCase.normaHoursFlow(month.year, month.month)
                        } else {
                            kotlinx.coroutines.flow.flowOf(null)
                        }
                    }
                    .collect { hours ->
                        _uiState.update { it.copy(normaHours = hours) }
                    }
            } catch (e: Exception) {
                e.sendToSentry("SettingsViewModel", "observeNorma")
            }
        }
    }

    private fun loadMonthList() {
        loadCalendarJob?.cancel()
        loadCalendarJob = calendarUseCase.loadFlowMonthOfYearListState().onEach { result ->
            _uiState.update { state ->
                state.copy(
                    monthList = result.map { it.month }.distinct().sorted(),
                    yearList = result.map { it.year }.distinct().sorted()
                )

            }
        }.launchIn(viewModelScope)
    }

    private fun loadSettings() {
        loadSettingsJob?.cancel()
        loadSettingsJob = viewModelScope.launch {
            try {
                // Подписываемся на изменения UserSettings в БД continuously,
                // чтобы после syncFromRemote (загрузка с сервера) UI Settings
                // отразил новые значения country/region/etc. Раньше был .first {}
                // — настройки кэшировались в памяти и UI не обновлялся.
                // Если пользователь сейчас правит настройку (autoSaveJob активен),
                // пропускаем эмит чтобы не затереть его in-progress изменения —
                // когда autoSave завершится, эмит из DB совпадёт с локальным.
                settingsUseCase.getFlowCurrentSettingsState()
                    .collect { result ->
                        if (result !is ResultState.Success) return@collect
                        if (autoSaveJob?.isActive == true) return@collect
                        result.data?.let { userSettings ->
                            val merged = userSettings.copy(
                                isShowLocoHeating = sharedPreferenceStorage.isShowLocoHeating(),
                                isShowLocoAuxiliary = sharedPreferenceStorage.isShowLocoAuxiliary(),
                                isShowLocoStatistics = sharedPreferenceStorage.isShowLocoStatistics(),
                                isShowLocoNorma = sharedPreferenceStorage.isShowLocoNorma(),
                                isShowOtherCurrent = sharedPreferenceStorage.isShowOtherCurrent()
                            )
                            _uiState.update {
                                it.copy(
                                    settingDetails = ResultState.Success(merged),
                                    dateAndTimeConverter = DateAndTimeConverter(merged),
                                    updateAt = merged.updateAt,
                                    servicePhases = merged.servicePhases.toMutableStateList()
                                )
                            }
                        }
                    }
            } catch (e: Exception) {
                e.sendToSentry("SettingsViewModel", "loadSettings")
            }
        }
        viewModelScope.launch {
            try {
                calendarUseCase.loadFlowMonthOfYearListState().collect { months ->
                    currentSettings?.let { setting ->
                        val current = setting.selectMonthOfYear
                        // Ищем актуальный месяц в обновлённых данных. ВАЖНО: в локальной
                        // БД могут быть дубли MonthOfYear для одного year+month (наследие
                        // старых импортов). find {} брал ПЕРВЫЙ — и это мог быть
                        // устаревший экземпляр без региональных HOLIDAY-тегов, из-за
                        // чего норма "сбрасывалась" обратно на стандартную через
                        // долю секунды после applyRegionalHolidays. Берём дедуплицированно
                        // самый "полный" (с наибольшим количеством days) — то же самое,
                        // что делает refreshSelectMonthOfYear().
                        val updatedMonth = months
                            .filter { it.year == current.year && it.month == current.month }
                            .maxByOrNull { it.days.size }
                            ?: current

                        // Если теги изменились (смена RU→BY/KZ или наоборот, либо
                        // applyRegionalHolidays пометил день как HOLIDAY) — обновляем
                        // selectMonthOfYear в settings, чтобы норма пересчиталась везде
                        if (updatedMonth.days.map { it.tag } != current.days.map { it.tag } ||
                            updatedMonth.days.map { it.isReleaseDay } != current.days.map { it.isReleaseDay }
                        ) {
                            currentSettings = setting.copy(selectMonthOfYear = updatedMonth)
                        }

                        _uiState.update {
                            it.copy(calendarState = ResultState.Success(updatedMonth))
                        }
                    }
                }
            } catch (e: Exception) {
                e.sendToSentry("SettingsViewModel", "loadCalendar")
            }
        }
    }

    private fun scheduleAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(300)
            saveSettingsQuietly()
        }
    }

    private fun saveSettingsQuietly() {
        val state = uiState.value.settingDetails
        if (state is ResultState.Success) {
            state.data?.let { settings ->
                settings.servicePhases = servicePhases.toMutableList()
                saveSettingsJob?.cancel()
                saveSettingsJob = viewModelScope.launch {
                    settingsUseCase.saveSetting(settings).collect { }
                }
            }
        }
    }

    override fun onCleared() {
        autoSaveJob?.cancel()
        saveSettingsJob?.cancel()
        // Гарантируем сохранение даже если пользователь покинул экран раньше 300мс
        CoroutineScope(NonCancellable + Dispatchers.IO).launch {
            val state = uiState.value.settingDetails
            if (state is ResultState.Success) {
                state.data?.let { settings ->
                    settings.servicePhases = servicePhases.toMutableList()
                    settingsUseCase.saveSetting(settings).collect {}
                }
            }
        }
        super.onCleared()
    }

    fun saveServicePhases() {
        scheduleAutoSave()
    }

    fun changeMinTimeRest(time: Long) {
        currentSettings = currentSettings?.copy(
            minTimeRestPointOfTurnover = time
        )
    }

    fun resetUploadState() {
        _uiState.update {
            it.copy(
                uploadState = null
            )
        }
    }

    fun resetDownloadState() {
        _uiState.update {
            it.copy(
                downloadState = null
            )
        }
    }

    fun changeDefaultWorkTime(timeInMillis: Long) {
        currentSettings = currentSettings?.copy(
            defaultWorkTime = timeInMillis
        )
    }

    fun changeDefaultLocoType() {
        currentSettings?.let { settings ->
            currentSettings = when (settings.defaultLocoType){
                LocoType.ELECTRIC -> {
                    settings.copy(
                        defaultLocoType = LocoType.DIESEL
                    )
                }

                LocoType.DIESEL -> {
                    settings.copy(
                        defaultLocoType = LocoType.ELECTRIC
                    )
                }
            }
        }
    }

    fun setDefaultLocoType(type: LocoType) {
        currentSettings = currentSettings?.copy(defaultLocoType = type)
    }

    fun changeMinTimeHomeRest(time: Long) {
        currentSettings = currentSettings?.copy(
            minTimeHomeRest = time
        )
    }

    fun changeStartNightTime(hour: Int, minute: Int) {
        currentSettings = currentSettings?.copy(
            nightTime = currentSettings!!.nightTime.copy(
                startNightHour = hour,
                startNightMinute = minute
            )
        )
    }

    fun changeEndNightTime(hour: Int, minute: Int) {
        currentSettings = currentSettings?.copy(
            nightTime = currentSettings!!.nightTime.copy(
                endNightHour = hour,
                endNightMinute = minute
            )
        )
    }

    fun changeUsingDefaultWorkTime(isUsing: Boolean) {
        currentSettings = currentSettings?.copy(
            usingDefaultWorkTime = isUsing
        )
    }

    fun changeConsiderFutureRoute(isConsider: Boolean) {
        currentSettings = currentSettings?.copy(
            isConsiderFutureRoute = isConsider
        )
    }

    fun changeShowBreak(isShow: Boolean) {
        currentSettings = currentSettings?.copy(
            isShowBreak = isShow
        )
    }

    fun changeShowOnePersonSwitch(isShow: Boolean) {
        currentSettings = currentSettings?.copy(
            isShowOnePersonSwitch = isShow
        )
    }

    fun changeShowLocoHeating(value: Boolean) {
        sharedPreferenceStorage.setShowLocoHeating(value)
        currentSettings = currentSettings?.copy(isShowLocoHeating = value)
    }

    fun changeShowLocoAuxiliary(value: Boolean) {
        sharedPreferenceStorage.setShowLocoAuxiliary(value)
        currentSettings = currentSettings?.copy(isShowLocoAuxiliary = value)
    }

    fun changeShowOtherCurrent(value: Boolean) {
        sharedPreferenceStorage.setShowOtherCurrent(value)
        currentSettings = currentSettings?.copy(isShowOtherCurrent = value)
    }

    fun setTimeZone(timeZone: Long) {
        currentSettings = currentSettings?.copy(
            timeZone = timeZone
        )
    }

    fun setCrossMonthTimezone(value: CrossMonthTimezone) {
        currentSettings = currentSettings?.copy(crossMonthTimezone = value)
    }

    /**
     * Обновляет selectMonthOfYear в настройках из актуальной таблицы MonthOfYear.
     * Вызывается после смены страны производственного календаря.
     * Синхронно читает текущие данные после applyProductionCalendar.
     */
    private fun refreshSelectMonthOfYear() {
        currentSettings?.let { setting ->
            val current = setting.selectMonthOfYear
            val allMonths = calendarUseCase.loadMonthOfYearList()
            // Берём месяц с наибольшим количеством дней (дедупликация по year+month)
            val refreshed = allMonths
                .filter { it.year == current.year && it.month == current.month }
                .maxByOrNull { it.days.size }
            if (refreshed != null && refreshed.days.isNotEmpty()) {
                currentSettings = setting.copy(selectMonthOfYear = refreshed)
            }
        }
    }

    /** Изменить страну календаря — сохраняет настройку и загружает производственный календарь */
    fun changeCountry(country: String) {
        val countryName = when (country) {
            "RU" -> "Россия"
            "KZ" -> "Казахстан"
            "BY" -> "Беларусь"
            else -> country
        }
        _uiState.update { it.copy(countryLoadingState = CountryLoadingState.Loading(countryName)) }
        val autoTimeZone = when (country) {
            "KZ" -> 7_200_000L  // Yekaterinburg UTC+5 = Moscow+2h
            "BY" -> 0L           // Minsk UTC+3 = Moscow
            else -> currentSettings?.timeZone ?: 0L  // RU: keep user's existing timezone
        }
        // Для КЗ и BY меню переходных маршрутов скрыто — сбрасываем в LOCAL
        val autoCrossMonthTimezone = if (country == "RU") {
            currentSettings?.crossMonthTimezone ?: CrossMonthTimezone.LOCAL
        } else {
            CrossMonthTimezone.LOCAL
        }
        // Нужен coroutine context для suspend-вызовов — выносим всё в viewModelScope.launch
        viewModelScope.launch {
            try {
                // Если регион выбран, но он не относится к новой стране — сбрасываем.
                // Например, был "RU-TA", сменили страну на Беларусь — региона быть не может.
                val currentRegion = currentSettings?.region
                val regionStillValid = currentRegion != null &&
                    regionalHolidaysRepository.getRegionByCode(currentRegion)?.country == country
                val newRegion = if (regionStillValid) currentRegion else null

                currentSettings = currentSettings?.copy(
                    country = country,
                    timeZone = autoTimeZone,
                    crossMonthTimezone = autoCrossMonthTimezone,
                    region = newRegion,
                )

                val now = Clock.System.now()
                    .toLocalDateTime(TimeZone.currentSystemDefault())
                val currentYear = now.year
                val currentMonth = now.monthNumber

                // Всегда загружаем текущий год при явной смене страны
                fetchAndApplyCalendar(country, currentYear)
                // С декабря — следующий год
                productionCalendarUseCase.nextYearToFetchIfDecember(currentMonth, currentYear)?.let { nextYear ->
                    fetchAndApplyCalendar(country, nextYear)
                }

                // Если после смены страны регион остался валидным — применяем
                // его праздники поверх свежего стандартного календаря.
                newRegion?.let { region ->
                    calendarUseCase.applyRegionalHolidays(region, currentYear).collect {}
                    productionCalendarUseCase.nextYearToFetchIfDecember(currentMonth, currentYear)?.let { nextYear ->
                        calendarUseCase.applyRegionalHolidays(region, nextYear).collect {}
                    }
                }

                // Очищаем данные других стран
                val allCountries = listOf("RU", "KZ", "BY")
                allCountries.filter { it != country }.forEach { otherCountry ->
                    productionCalendarUseCase.clearByCountry(otherCountry).collect {}
                }

                // После применения нового календаря сразу обновляем selectMonthOfYear
                // в настройках, чтобы норма пересчиталась без ожидания реактивного потока.
                // Реактивный поток в loadSettings() дополнительно обновит isReleaseDay.
                refreshSelectMonthOfYear()

                // Перезагружаем список регионов для новой страны
                reloadRegions()

                _uiState.update { it.copy(countryLoadingState = CountryLoadingState.Success) }
            } catch (e: Exception) {
                val msg = e.message ?: ""
                if (isNetworkErrorMessage(msg) || e is java.net.UnknownHostException || e is java.net.ConnectException || e is java.io.IOException) {
                    _uiState.update { it.copy(countryLoadingState = CountryLoadingState.NoInternet) }
                } else {
                    _uiState.update { it.copy(countryLoadingState = CountryLoadingState.Error) }
                }
                e.sendToSentry("SettingsViewModel", "changeCountry")
            }
        }
    }

    fun clearCountryLoadingState() {
        _uiState.update { it.copy(countryLoadingState = null) }
    }

    /**
     * Список регионов для текущей страны — StateFlow для UI.
     * Загружается при init и при каждом успешном changeCountry.
     */
    private val _regionsForCountry = MutableStateFlow<List<com.z_company.domain.entities.calendar.Region>>(emptyList())
    val regionsForCountry = _regionsForCountry.asStateFlow()

    /**
     * Флаг загрузки списка регионов (запрос к серверу /v1/regions).
     * UI использует чтобы показать индикатор вместо dropdown пока данные грузятся —
     * иначе при первом открытии настроек секция регионов "не появлялась" из-за
     * пустого списка, и пользователь не понимал что данные ещё догружаются.
     */
    private val _isRegionsLoading = MutableStateFlow(false)
    val isRegionsLoading = _isRegionsLoading.asStateFlow()

    init {
        // Реактивно загружаем регионы при изменении страны в настройках.
        // Раньше использовался хардкод delay(500) — если loadSettings успевал
        // дольше (медленный диск / первый запуск), reloadRegions вызывался
        // до того как country был известен → список регионов оставался пустым.
        viewModelScope.launch {
            try {
                _uiState
                    .map { (it.settingDetails as? ResultState.Success)?.data?.country }
                    .distinctUntilChanged()
                    .collect { country ->
                        if (country != null) {
                            reloadRegionsForCountry(country)
                        }
                    }
            } catch (e: Exception) {
                e.sendToSentry("SettingsViewModel", "init_regions")
            }
        }
    }

    private suspend fun reloadRegions() {
        val country = currentSettings?.country ?: "RU"
        reloadRegionsForCountry(country)
    }

    private suspend fun reloadRegionsForCountry(country: String) {
        _isRegionsLoading.value = true
        try {
            _regionsForCountry.value =
                regionalHolidaysRepository.getRegionsForCountry(country)
        } catch (e: Exception) {
            // Не валим экран настроек — оставляем старый список
            // (пользователь сможет повторить через смену страны).
            e.sendToSentry("SettingsViewModel", "reloadRegionsForCountry")
        } finally {
            _isRegionsLoading.value = false
        }
    }

    /**
     * Смена региона. АТОМАРНО: сначала проверяет сеть pre-flight запросом,
     * только при успехе коммитит регион в настройки и применяет к календарю.
     *
     * При ошибке сети/сервера регион НЕ меняется — пользователь увидит диалог
     * с конкретной причиной и старый регион останется выбранным.
     *
     * Управляет [RegionLoadingState] для показа пользователю прогресса.
     *
     * Запускается на Dispatchers.IO чтобы Main не блокировался — иначе
     * CircularProgressIndicator в диалоге "замирает" из-за пропущенных кадров.
     */
    fun changeRegion(region: String?) {
        val country = currentSettings?.country ?: return
        val previousRegion = currentSettings?.region

        // Если кликнули на тот же регион — ничего не делаем
        if (previousRegion == region) return

        // Имя для loading-диалога (resolve из списка регионов или fallback на код)
        val regionName: String = if (region == null) {
            "Без региона"
        } else {
            _regionsForCountry.value.firstOrNull { it.code == region }?.displayName ?: region
        }
        _uiState.update { it.copy(regionLoadingState = RegionLoadingState.Loading(regionName)) }

        // ВАЖНО: Dispatchers.IO — иначе DB writes на Main блокируют Choreographer
        // и анимация лоадера в диалоге "замирает".
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val now = Clock.System.now()
                    .toLocalDateTime(TimeZone.currentSystemDefault())
                val currentYear = now.year
                val currentMonth = now.monthNumber
                val nextYear = productionCalendarUseCase
                    .nextYearToFetchIfDecember(currentMonth, currentYear)

                // 1. Pre-flight: строгая загрузка с сервера — БЕЗ fallback.
                //    Если сеть не отвечает, бросит исключение, мы это поймаем
                //    и НЕ будем менять регион в настройках.
                if (region != null) {
                    regionalHolidaysRepository.loadHolidaysForRegionYearStrict(region, currentYear)
                    nextYear?.let {
                        regionalHolidaysRepository.loadHolidaysForRegionYearStrict(region, it)
                    }
                }

                // 2. Сеть работает — теперь коммитим регион в настройки
                currentSettings = currentSettings?.copy(region = region)

                // 3. Перезагружаем стандартный — сбрасывает старые региональные пометки
                fetchAndApplyCalendar(country, currentYear)
                nextYear?.let { fetchAndApplyCalendar(country, it) }

                // 4. Накладываем новые региональные сверху (теперь fallback допустим —
                //    данные уже подтверждены на шаге 1, applyRegionalHolidays использует
                //    тот же кэш регионов в репозитории)
                region?.let {
                    calendarUseCase.applyRegionalHolidays(it, currentYear).collect {}
                    nextYear?.let { ny ->
                        calendarUseCase.applyRegionalHolidays(it, ny).collect {}
                    }
                }

                // 5. Сохраняем настройки + обновляем выбранный месяц для пересчёта нормы
                currentSettings?.let { settingsUseCase.saveSetting(it).collect {} }
                refreshSelectMonthOfYear()

                _uiState.update { it.copy(regionLoadingState = RegionLoadingState.Success) }
            } catch (e: Exception) {
                // Откат: регион не менялся (мы коммитим только после pre-flight),
                // но на всякий случай гарантируем согласованность через явный copy.
                currentSettings = currentSettings?.copy(region = previousRegion)

                val msg = e.message ?: ""
                if (isNetworkErrorMessage(msg)
                    || e is java.net.UnknownHostException
                    || e is java.net.ConnectException
                    || e is java.io.IOException
                ) {
                    _uiState.update { it.copy(regionLoadingState = RegionLoadingState.NoInternet) }
                } else {
                    _uiState.update { it.copy(regionLoadingState = RegionLoadingState.Error) }
                }
                e.sendToSentry("SettingsViewModel", "changeRegion")
            }
        }
    }

    fun clearRegionLoadingState() {
        _uiState.update { it.copy(regionLoadingState = null) }
    }

    private fun isNetworkErrorMessage(msg: String): Boolean =
        msg.contains("Unable to resolve", ignoreCase = true) ||
        msg.contains("Connection refused", ignoreCase = true) ||
        msg.contains("timeout", ignoreCase = true) ||
        msg.contains("Failed to connect", ignoreCase = true) ||
        msg.contains("Network is unreachable", ignoreCase = true) ||
        msg.contains("ECONNREFUSED", ignoreCase = true) ||
        msg.contains("UnknownHostException", ignoreCase = true) ||
        msg.contains("ConnectException", ignoreCase = true)

    private suspend fun fetchAndApplyCalendar(country: String, year: Int) {
        settingManager.getProductionCalendarFromRemote(country, year).collect { state ->
            if (state is com.z_company.core.ResultState.Success) {
                val days = state.data
                productionCalendarUseCase.saveCalendar(days).collect {}
                calendarUseCase.applyProductionCalendar(days).collect {}
            } else if (state is ResultState.Error) {
                throw RuntimeException(state.entity.message ?: state.entity.throwable?.message ?: "Ошибка загрузки")
            }
        }
    }

    private val _passenger12hOption = MutableStateFlow(resolvePassenger12hOption())
    val passenger12hOption = _passenger12hOption.asStateFlow()

    private fun resolvePassenger12hOption(): Passenger12hOption {
        val dontAsk = sharedPreferenceStorage.isPassenger12hDontAskAgain()
        if (!dontAsk) return Passenger12hOption.ALWAYS_ASK
        return if (sharedPreferenceStorage.isPassenger12hAutoAccepted()) {
            Passenger12hOption.AUTO
        } else {
            Passenger12hOption.NEVER_ASK
        }
    }

    fun setPassenger12hOption(option: Passenger12hOption) {
        when (option) {
            Passenger12hOption.ALWAYS_ASK -> {
                sharedPreferenceStorage.setPassenger12hDontAskAgain(false)
                sharedPreferenceStorage.setPassenger12hAutoAccepted(false)
            }
            Passenger12hOption.AUTO -> {
                sharedPreferenceStorage.setPassenger12hDontAskAgain(true)
                sharedPreferenceStorage.setPassenger12hAutoAccepted(true)
            }
            Passenger12hOption.NEVER_ASK -> {
                sharedPreferenceStorage.setPassenger12hDontAskAgain(true)
                sharedPreferenceStorage.setPassenger12hAutoAccepted(false)
            }
        }
        _passenger12hOption.value = option
    }
}

enum class Passenger12hOption(val label: String, val hint: String) {
    ALWAYS_ASK(
        "Всегда спрашивать",
        "При работе свыше 12-ти часов будет предложено создать следование пассажиром."
    ),
    AUTO(
        "Автоматически",
        "При включении время свыше 12 часов будет автоматически записано как следование пассажиром."
    ),
    NEVER_ASK(
        "Никогда не спрашивать",
        "Шторка не будет показана."
    )
}

data class TimeZoneRussia(
    val description: String,
    val offsetOfMoscow: Long
)