@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.z_company.iosapp.viewmodel

import com.z_company.core.ResultState
import com.z_company.domain.entities.route.BasicData
import com.z_company.domain.entities.route.OverRestRoutes.overRestPayment
import com.z_company.domain.entities.route.Passenger
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.UtilsForEntities.getPassengerTime
import com.z_company.domain.entities.route.UtilsForEntities.getPassengerTimeOutsideWork
import com.z_company.domain.entities.route.UtilsForEntities.getPassengerTimeWithinWork
import com.z_company.domain.entities.route.UtilsForEntities.getPureWorkTime
import com.z_company.domain.entities.route.UtilsForEntities.getWorkStartWaitingInterval
import com.z_company.domain.entities.route.UtilsForEntities.getWorkStartWaitingNightTime
import com.z_company.domain.entities.route.UtilsForEntities.getWorkTime
import com.z_company.domain.entities.route.UtilsForEntities.getWorkingTimeOnAHoliday
import com.z_company.domain.entities.route.UtilsForEntities.isTimeWorkValid
import com.z_company.domain.autorecord.TrainOrdering
import com.z_company.domain.entities.route.reidentifyForImport
import com.z_company.domain.entities.setting.SalarySetting
import com.z_company.domain.entities.setting.UserSettings
import com.z_company.domain.repositories.RouteRepository
import com.z_company.domain.repositories.SharedPreferencesRepositories
import com.z_company.domain.salary.SalaryCalculationHelper
import com.z_company.domain.use_cases.PartnerUseCase
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.domain.use_cases.SalarySettingUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.domain.use_cases.StationDirectoryUseCase
import com.z_company.domain.util.CalculateNightTime
import com.z_company.domain.util.SharedRouteHolder
import com.z_company.domain.util.TimeCalculationContext
import com.z_company.domain.util.currencySymbol
import com.z_company.domain.util.displayTimeZone
import com.z_company.domain.util.toMoneyString
import com.z_company.domain.util.usesOnePersonPassengerRate
import com.z_company.repository.SecureTokenStorage
import com.z_company.repository.remote_rest.ShareRouteManager
import com.z_company.repository.remote_rest.SyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/**
 * iOS-аналог Android `FormViewModel` (SCREEN_SPECS §5): форма маршрута.
 *
 * Регистрируется как Koin `factory` — по экземпляру на открытую форму (копия маршрута
 * открывается поверх оригинала, общий singleton затёр бы состояние). Swift вызывает
 * [dispose] в `deinit` обёртки.
 *
 * BasicData держится в памяти, из БД подтягиваются только дочерние разделы
 * (`locomotives/trains/passengers/otherWorks/partners/photos`) и времена, которые меняют
 * дочерние экраны (`timeEndWork`, `timeStartWork`, `timeStartWorkBeforeArrival`) — §5.2.
 */
class RouteFormIosViewModel(
    private val routeUseCase: RouteUseCase,
    private val settingsUseCase: SettingsUseCase,
    private val salarySettingUseCase: SalarySettingUseCase,
    private val partnerUseCase: PartnerUseCase,
    private val routeRepository: RouteRepository,
    private val stationDirectory: StationDirectoryUseCase,
    private val sharedPrefs: SharedPreferencesRepositories,
    private val secureTokenStorage: SecureTokenStorage,
    private val shareRouteManager: ShareRouteManager,
    private val syncManager: SyncManager,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // ── Источники ─────────────────────────────────────────────────────────────
    private val currentRoute = MutableStateFlow<Route?>(null)
    private val userSettings = MutableStateFlow<UserSettings?>(null)
    private val salarySetting = MutableStateFlow<SalarySetting?>(null)

    // ── Выходные состояния ────────────────────────────────────────────────────
    private val _state = MutableStateFlow(RouteFormState())
    private val _salary = MutableStateFlow(SalaryUi())
    private val _rest = MutableStateFlow(RestUi())

    /** Одноразовые события доставляются колбэком [watchEvents]. */
    private var eventSink: ((RouteFormEvent) -> Unit)? = null
    private val pendingEvents = mutableListOf<RouteFormEvent>()

    // ── Внутреннее состояние ──────────────────────────────────────────────────
    private var routeId: String? = null
    private var isNewOrCopy = false
    private var isPersistedToDb = false
    private var changesHaveState = false
    private var isSharedPreview = false
    private var countLoadRoute = 0

    private var autoSaveJob: Job? = null
    private var loadRouteJob: Job? = null
    private var saveRouteJob: Job? = null
    private var disableArrivalJob: Job? = null

    private var duplicate: DuplicateInternal? = null
    private var showPassenger12h = false

    private data class DuplicateInternal(val existing: Route, val exitAfterSave: Boolean)

    init {
        scope.launch {
            settingsUseCase.getUserSettingFlow().collect { userSettings.value = it }
        }
        scope.launch {
            // Живая подписка: смена «среднего часа» на другом экране сразу пересчитывает форму (§5.1).
            salarySettingUseCase.salarySettingFlow().collect { salarySetting.value = it }
        }
        scope.launch {
            combine(currentRoute, userSettings, salarySetting) { r, u, s -> Triple(r, u, s) }
                .collectLatest { (route, settings, salSetting) ->
                    if (route != null && settings != null && salSetting != null) {
                        recompute(route, settings, salSetting)
                    }
                }
        }
    }

    // ═══ Подписки для Swift ═══════════════════════════════════════════════════

    fun watchState(callback: (RouteFormState) -> Unit) {
        scope.launch { _state.collect { callback(it) } }
    }

    fun watchSalary(callback: (SalaryUi) -> Unit) {
        scope.launch { _salary.collect { callback(it) } }
    }

    fun watchRest(callback: (RestUi) -> Unit) {
        scope.launch { _rest.collect { callback(it) } }
    }

    fun watchEvents(callback: (RouteFormEvent) -> Unit) {
        eventSink = callback
        pendingEvents.toList().also { pendingEvents.clear() }.forEach(callback)
    }

    fun dispose() {
        autoSaveJob?.cancel()
        // §5.2: onCleared дописывает маршрут, только если он уже в БД и есть несохранённые изменения.
        if (isPersistedToDb && changesHaveState) {
            val route = currentRoute.value
            if (route != null) {
                CoroutineScope(NonCancellable + Dispatchers.Default).launch {
                    routeUseCase.saveRoute(route.withPassengerArrivalFlagsFromDb()).collect {}
                }
            }
        }
        scope.cancel()
    }

    private fun emit(event: RouteFormEvent) {
        val sink = eventSink
        if (sink != null) sink(event) else pendingEvents.add(event)
    }

    // ═══ Загрузка ═════════════════════════════════════════════════════════════

    /**
     * [routeId] == null → новый маршрут (в БД не пишется до первого изменения).
     * [isCopy] → существующий маршрут через `reidentifyForImport` (§5.1).
     * [sharedPreview] → маршрут по публичной ссылке (лежит в БД с isDeleted=true).
     */
    fun start(routeId: String?, isCopy: Boolean, sharedPreview: Boolean) {
        this.routeId = routeId
        isNewOrCopy = routeId == null || isCopy
        isPersistedToDb = !isNewOrCopy
        isSharedPreview = sharedPreview || (routeId != null && SharedRouteHolder.consume(routeId))
        countLoadRoute = 0
        loadRouteJob?.cancel()
        loadRouteJob = scope.launch {
            if (routeId == null) {
                // Постоянный напарник (если задан) сразу в новом маршруте (§8.5.4).
                currentRoute.value = partnerUseCase.withPermanentPartner(Route(basicData = BasicData()))
                // НЕ сохраняем: пустой маршрут не должен попадать в БД.
            } else {
                routeUseCase.routeDetails(routeId).collectLatest { result ->
                    if (result !is ResultState.Success) return@collectLatest
                    val loaded = if (isCopy) {
                        result.data?.reidentifyForImport()?.let(partnerUseCase::withPermanentPartner)
                    } else result.data
                    countLoadRoute += 1
                    if (countLoadRoute == 1) {
                        currentRoute.value = loaded?.withWorkStartFromPassenger()
                    } else {
                        mergeSubsectionsFromDb(loaded)
                    }
                }
            }
        }
        publishState()
    }

    /** Подтягивает только подразделы и времена, которые правят дочерние экраны. */
    private fun mergeSubsectionsFromDb(dbRoute: Route?) {
        dbRoute ?: return
        currentRoute.update { inMemory ->
            (inMemory?.copy(
                locomotives = dbRoute.locomotives,
                trains = dbRoute.trains,
                passengers = dbRoute.passengers,
                otherWorks = dbRoute.otherWorks,
                partners = dbRoute.partners,
                photos = dbRoute.photos,
                basicData = inMemory.basicData.copy(
                    timeEndWork = dbRoute.basicData.timeEndWork,
                    timeStartWork = dbRoute.basicData.timeStartWork,
                    timeStartWorkBeforeArrival = dbRoute.basicData.timeStartWorkBeforeArrival,
                )
            ) ?: dbRoute).withWorkStartFromPassenger()
        }
    }

    private fun Route.withWorkStartFromPassenger(): Route {
        val arrival = passengers.firstOrNull {
            it.isWorkStartByArrival && it.timeArrival != null
        }?.workStartTime ?: return this
        return if (basicData.timeStartWork == arrival) this
        else copy(basicData = basicData.copy(timeStartWork = arrival))
    }

    /** Перед КАЖДОЙ записью флаг «явки по прибытию» перечитывается из БД (§5.2). */
    private suspend fun Route.withPassengerArrivalFlagsFromDb(): Route {
        disableArrivalJob?.join()
        val flagInDb = routeRepository.loadPassengerListByBasicId(basicData.id)
            .associate { it.passengerId to it.isWorkStartByArrival }
        if (flagInDb.isEmpty()) return this
        return copy(
            passengers = passengers.map { p ->
                val inDb = flagInDb[p.passengerId]
                if (inDb == null || inDb == p.isWorkStartByArrival) p
                else p.copy(isWorkStartByArrival = inDb)
            }.toMutableList()
        )
    }

    private fun subscribeToChanges(id: String) {
        loadRouteJob?.cancel()
        loadRouteJob = scope.launch {
            routeUseCase.routeDetails(id).collect { result ->
                if (result is ResultState.Success) mergeSubsectionsFromDb(result.data)
            }
        }
    }

    // ═══ Автосейв ═════════════════════════════════════════════════════════════

    private fun changesHave() {
        sharedPrefs.setTokenIsChangeHave(true)
        changesHaveState = true
        triggerAutoSave()
    }

    /** Автосейв с дебаунсом 500 мс (§5.2). */
    private fun triggerAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = scope.launch {
            delay(500)
            val route = currentRoute.value ?: return@launch
            val wasInDb = isPersistedToDb
            routeUseCase.saveRoute(route.withPassengerArrivalFlagsFromDb()).collect { result ->
                if (!wasInDb && result is ResultState.Success) {
                    isPersistedToDb = true
                    subscribeToChanges(route.basicData.id)
                }
            }
        }
    }

    /**
     * Перед переходом в дочерний раздел (§5.2): маршрут в БД и нет изменений — записи нет;
     * иначе сохраняем, включаем подписку и только потом [onDone] (навигация).
     */
    fun preSave(onDone: () -> Unit) {
        if (isPersistedToDb && !changesHaveState) {
            onDone()
            return
        }
        val route = currentRoute.value
        if (route == null) {
            onDone()
            return
        }
        autoSaveJob?.cancel()
        saveRouteJob?.cancel()
        saveRouteJob = scope.launch {
            val toSave = route.withPassengerArrivalFlagsFromDb()
            var done = false
            routeUseCase.saveRoute(toSave).collect { r ->
                if (r is ResultState.Success && !done) {
                    done = true
                    isPersistedToDb = true
                    subscribeToChanges(route.basicData.id)
                    changesHave()
                }
            }
            onDone()
        }
    }

    // ═══ Сеттеры полей ════════════════════════════════════════════════════════

    private fun updateBasic(transform: (BasicData) -> BasicData) {
        currentRoute.update { it?.copy(basicData = transform(it.basicData)) }
        changesHave()
        publishState()
    }

    fun setNumber(value: String) = updateBasic { it.copy(number = value.ifBlank { null }) }

    fun setOnePerson(value: Boolean) = updateBasic { it.copy(isOnePersonOperation = value) }

    fun setNotes(value: String) = updateBasic { it.copy(notes = value.ifBlank { null }) }

    fun setFavorite() = updateBasic { it.copy(isFavorite = !it.isFavorite) }

    fun setRestPointOfTurnover(value: Boolean) = updateBasic { it.copy(restPointOfTurnover = value) }

    private fun Long?.truncateToMinute(): Long? = this?.let { it - it % 60_000L }

    /**
     * Явка. При включённом «Стандартном времени работы» сдача = явка + defaultWorkTime
     * (заменяет ранее заданную) — §5.3 п.8.
     */
    fun setTimeStartWork(ms: Long?) {
        val start = ms.truncateToMinute()
        val settings = userSettings.value
        val useDefault = settings?.usingDefaultWorkTime == true
        currentRoute.update { route ->
            route?.copy(
                basicData = route.basicData.copy(
                    timeStartWork = start,
                    timeEndWork = if (start != null && useDefault) {
                        start + (settings?.defaultWorkTime ?: 0L)
                    } else route.basicData.timeEndWork
                )
            )
        }
        changesHave()
        publishState()
        if (start != null && useDefault) checkWorkTimeExceeds12h()
    }

    fun setTimeEndWork(ms: Long?) {
        updateBasic { it.copy(timeEndWork = ms.truncateToMinute()) }
        if (ms != null) checkWorkTimeExceeds12h()
    }

    fun setTimeStartBreak(ms: Long?) = updateBasic { it.copy(timeStartBreak = ms.truncateToMinute()) }

    fun setTimeEndBreak(ms: Long?) = updateBasic { it.copy(timeEndBreak = ms.truncateToMinute()) }

    // ═══ «Явка по прибытию» (§5.2, §5.8) ═════════════════════════════════════

    /** Подтверждение «Изменить время явки?» → снять режим у всех пассажиров (в UI и сразу в БД). */
    fun disableWorkStartByArrival() {
        currentRoute.update { route ->
            route?.copy(
                passengers = route.passengers.map { it.copy(isWorkStartByArrival = false) }.toMutableList(),
                basicData = route.basicData.copy(timeStartWorkBeforeArrival = null)
            )
        }
        changesHave()
        publishState()
        val basicId = currentRoute.value?.basicData?.id ?: return
        clearWorkStartByArrivalInDb(basicId, restorePreviousStart = false)
    }

    private fun clearWorkStartByArrivalInDb(
        basicId: String,
        restorePreviousStart: Boolean,
        before: (suspend () -> Unit)? = null,
    ) {
        // Вне scope формы: уход с экрана не должен отменить запись.
        disableArrivalJob = CoroutineScope(NonCancellable + Dispatchers.Default).launch {
            before?.invoke()
            val state = routeUseCase.routeDetails(basicId).first { it is ResultState.Success }
            val fresh = (state as? ResultState.Success)?.data ?: return@launch
            val reserve = fresh.basicData.timeStartWorkBeforeArrival
            if (fresh.passengers.none { it.isWorkStartByArrival } && reserve == null) return@launch
            routeUseCase.saveRoute(
                fresh.copy(
                    passengers = fresh.passengers.map { it.copy(isWorkStartByArrival = false) }.toMutableList(),
                    basicData = fresh.basicData.copy(
                        timeStartWork = if (restorePreviousStart) {
                            reserve ?: fresh.basicData.timeStartWork
                        } else fresh.basicData.timeStartWork,
                        timeStartWorkBeforeArrival = null
                    )
                )
            ).first { it !is ResultState.Loading }
        }
    }

    // ═══ Удаление дочерних сущностей (сразу из БД, §5.2) ═════════════════════

    fun deleteLoco(id: String) {
        val loco = currentRoute.value?.locomotives?.firstOrNull { it.locoId == id } ?: return
        currentRoute.update { r -> r?.copy(locomotives = r.locomotives.filter { it.locoId != id }.toMutableList()) }
        scope.launch { routeRepository.removeLoco(loco).collect {} }
        changesHave()
        publishState()
    }

    fun deleteTrain(id: String) {
        val train = currentRoute.value?.trains?.firstOrNull { it.trainId == id } ?: return
        currentRoute.update { r -> r?.copy(trains = r.trains.filter { it.trainId != id }.toMutableList()) }
        scope.launch { routeRepository.removeTrain(train).collect {} }
        changesHave()
        publishState()
    }

    fun deleteOtherWork(id: String) {
        val work = currentRoute.value?.otherWorks?.firstOrNull { it.otherWorkId == id } ?: return
        currentRoute.update { r -> r?.copy(otherWorks = r.otherWorks.filter { it.otherWorkId != id }.toMutableList()) }
        scope.launch { routeRepository.removeOtherWork(work).collect {} }
        changesHave()
        publishState()
    }

    fun deletePartner(id: String) {
        val partner = currentRoute.value?.partners?.firstOrNull { it.routePartnerId == id } ?: return
        currentRoute.update { r -> r?.copy(partners = r.partners.filter { it.routePartnerId != id }.toMutableList()) }
        scope.launch { routeRepository.removePartner(partner).collect {} }
        changesHave()
        publishState()
    }

    /** Удаление поездки-источника «явки по прибытию» возвращает прежнюю явку (§5.3 п.9, §7.2). */
    fun deletePassenger(id: String) {
        val passenger = currentRoute.value?.passengers?.firstOrNull { it.passengerId == id } ?: return
        val wasSource = passenger.isWorkStartByArrival
        currentRoute.update { route ->
            route?.copy(
                passengers = route.passengers.filter { it.passengerId != id }.toMutableList(),
                basicData = if (wasSource) {
                    route.basicData.copy(
                        timeStartWork = route.basicData.timeStartWorkBeforeArrival ?: route.basicData.timeStartWork,
                        timeStartWorkBeforeArrival = null
                    )
                } else route.basicData
            )
        }
        if (wasSource) {
            val basicId = passenger.basicId.ifBlank { currentRoute.value?.basicData?.id.orEmpty() }
            clearWorkStartByArrivalInDb(basicId, restorePreviousStart = true) {
                routeRepository.removePassenger(passenger).collect {}
            }
        } else {
            scope.launch { routeRepository.removePassenger(passenger).collect {} }
        }
        changesHave()
        publishState()
    }

    // ═══ Сохранение, дубли, shared preview (§5.2, §5.6) ═══════════════════════

    /** «Готово»: нет изменений → просто закрыть (кроме shared preview / isDeleted). */
    fun onSaveClick() {
        val needsPersistAnyway = isSharedPreview || currentRoute.value?.basicData?.isDeleted == true
        if (!changesHaveState && !needsPersistAnyway) {
            sharedPrefs.setTokenIsChangeHave(false)
            emit(RouteFormEvent(kind = "exit"))
            return
        }
        checkDuplicateAndSave(exitAfterSave = false)
    }

    fun saveSharedRouteAndExit() = checkDuplicateAndSave(exitAfterSave = true)

    private fun normalizeToMinute(ms: Long) = (ms / 60_000L) * 60_000L

    private fun findDuplicateByStartWork(current: Route): Route? {
        val minute = normalizeToMinute(current.basicData.timeStartWork ?: return null)
        return routeUseCase.getListRoutes().firstOrNull { other ->
            other.basicData.id != current.basicData.id &&
                other.basicData.timeStartWork?.let { normalizeToMinute(it) == minute } == true
        }
    }

    private fun checkDuplicateAndSave(exitAfterSave: Boolean) {
        val route = currentRoute.value ?: return
        scope.launch {
            val dup = findDuplicateByStartWork(route)
            if (dup != null) {
                duplicate = DuplicateInternal(dup, exitAfterSave)
                publishState()
            } else {
                performSave(exitAfterSave)
            }
        }
    }

    /** Закрытие шторки дубля = отмена, ничего не сохраняется (§5.6). */
    fun dismissDuplicate() {
        duplicate = null
        publishState()
    }

    fun confirmReplaceDuplicate() {
        val dup = duplicate ?: return
        duplicate = null
        publishState()
        scope.launch {
            routeUseCase.removeRoute(dup.existing).collect {}
            performSave(dup.exitAfterSave)
        }
    }

    fun confirmKeepBothDuplicates() {
        val dup = duplicate ?: return
        duplicate = null
        publishState()
        scope.launch { performSave(dup.exitAfterSave) }
    }

    private fun performSave(exitAfterSave: Boolean) {
        autoSaveJob?.cancel()
        saveRouteJob?.cancel()
        saveRouteJob = scope.launch {
            val inMemory = currentRoute.value ?: return@launch
            val route = inMemory.withPassengerArrivalFlagsFromDb()
            val toSave = if (isSharedPreview || route.basicData.isDeleted) {
                route.copy(basicData = route.basicData.copy(isDeleted = false))
            } else route
            if (exitAfterSave) {
                isSharedPreview = false
                emit(RouteFormEvent(kind = "exit"))
                routeUseCase.saveRoute(toSave).collect {}
                return@launch
            }
            routeUseCase.saveRoute(toSave).collectLatest { result ->
                when (result) {
                    is ResultState.Success -> {
                        isPersistedToDb = true
                        isSharedPreview = false
                        changesHaveState = false
                        sharedPrefs.setTokenIsChangeHave(false)
                        emit(RouteFormEvent(kind = "saved"))
                        val savedId = toSave.basicData.id
                        // Тосты — через 400 мс, после анимации навигации (§5.2).
                        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
                            delay(400)
                            emit(RouteFormEvent(kind = "snackbar", text = "Маршрут сохранен"))
                            val raw = secureTokenStorage.getAuthBearerTokenFlow().first()
                            if (raw.isNullOrBlank()) return@launch
                            syncManager.syncRoute(savedId, "Bearer $raw").collect { r ->
                                if (r is ResultState.Success) {
                                    emit(RouteFormEvent(kind = "snackbar", text = "Маршрут сохранен в облаке"))
                                }
                            }
                        }
                    }
                    is ResultState.Error -> emit(
                        RouteFormEvent(kind = "snackbar", text = "Ошибка: ${result.entity.message ?: "не удалось сохранить"}")
                    )
                    is ResultState.Loading -> Unit
                }
            }
        }
    }

    /** «Удалить маршрут» → в корзину (§9.4) и закрыть экран. */
    fun deleteRoute() {
        val route = currentRoute.value ?: return
        autoSaveJob?.cancel()
        scope.launch {
            routeUseCase.markAsRemoved(route).collect { r ->
                if (r is ResultState.Success) {
                    changesHaveState = false
                    isPersistedToDb = false
                    emit(RouteFormEvent(kind = "exit"))
                }
            }
        }
    }

    /** «Отмена» в шторке «Получен новый маршрут» — выйти, запись остаётся с isDeleted=true (BUG-20). */
    fun cancelSharedPreview() {
        changesHaveState = false
        emit(RouteFormEvent(kind = "exit"))
    }

    /** «Просмотр»: шторка скрыта, форма остаётся в режиме shared (Готово снимет isDeleted). */
    private var sharedSheetDismissed = false

    // ═══ Шаринг (§5.6) ════════════════════════════════════════════════════════

    fun share() {
        val route = currentRoute.value ?: return
        scope.launch {
            try {
                val raw = secureTokenStorage.getAuthBearerTokenFlow().first()
                if (raw.isNullOrBlank()) {
                    emit(RouteFormEvent(kind = "snackbar", text = "Войдите в аккаунт, чтобы делиться маршрутами"))
                    return@launch
                }
                shareRouteManager.createShareLink(route, "Bearer $raw").collect { result ->
                    when (result) {
                        is ResultState.Success -> {
                            val (text, subject) = shareText(route, result.data)
                            emit(RouteFormEvent(kind = "share", text = text, subject = subject))
                        }
                        is ResultState.Error -> emit(
                            RouteFormEvent(kind = "snackbar", text = result.entity.message ?: "Не удалось создать ссылку")
                        )
                        is ResultState.Loading -> Unit
                    }
                }
            } catch (e: Exception) {
                emit(RouteFormEvent(kind = "snackbar", text = "Ошибка создания ссылки"))
            }
        }
    }

    /** Текст как в Android `ShareLinkData.fromRoute` (дата — в поясе отображения). */
    private fun shareText(route: Route, url: String): Pair<String, String> {
        val offset = displayOffsetMs()
        val start = route.basicData.timeStartWork
        val text = buildString {
            append("Маршрут из приложения «Машинист» 🚂")
            append("\n")
            start?.let { append(" от ${formatDate4(it, offset)} ${formatTime(it, offset)}") }
            append("\n")
            val stations = route.trains.flatMap { it.stations }.sortedBy { it.orderIndex }
            val first = stations.firstOrNull()?.stationName?.takeIf { it.isNotBlank() }
            val last = stations.lastOrNull()?.stationName?.takeIf { it.isNotBlank() }
            if (first != null && last != null && first != last) append(", $first — $last")
            append("\n\n")
            append("Чтобы открыть маршрут, нажмите на ссылку внизу")
            append("\n\n")
            append(url)
        }
        val subject = start?.let { "Маршрут от ${formatDate4(it, offset)} ${formatTime(it, offset)}" } ?: "Маршрут"
        return text to subject
    }

    // ═══ Смена > 12 ч (§5.5) ═════════════════════════════════════════════════

    private fun checkWorkTimeExceeds12h() {
        val route = currentRoute.value ?: return
        val start = route.basicData.timeStartWork ?: return
        val end = route.basicData.timeEndWork ?: return
        if (end - start <= 12 * 3_600_000L) return
        if (sharedPrefs.isPassenger12hDontAskAgain()) {
            if (sharedPrefs.isPassenger12hAutoAccepted()) {
                val (dep, arr) = prefilledPassengerTimes(start, end)
                savePassengerFromSheet(prefilledDepartureStation(route), null, dep, arr)
            }
            return
        }
        showPassenger12h = true
        publishState()
    }

    private fun prefilledPassengerTimes(start: Long, end: Long): Pair<Long, Long> {
        val dep = (start + 12 * 3_600_000L + 60_000L).let { it - it % 60_000L }
        val arr = (end - 60_000L).let { it - it % 60_000L }
        return dep to arr
    }

    private fun prefilledDepartureStation(route: Route): String? =
        route.trains.lastOrNull()?.stations?.lastOrNull()?.stationName

    fun dismissPassenger12h() {
        showPassenger12h = false
        publishState()
    }

    /** Сначала пишем маршрут (`preSaveRoute`), затем `Passenger` c `basicId` маршрута. */
    fun savePassengerFromSheet(
        stationDeparture: String?,
        stationArrival: String?,
        timeDeparture: Long,
        timeArrival: Long,
    ) {
        showPassenger12h = false
        publishState()
        val route = currentRoute.value ?: return
        val basicId = route.basicData.id
        preSave {
            scope.launch {
                val passenger = Passenger(
                    basicId = basicId,
                    stationDeparture = stationDeparture?.ifBlank { null },
                    stationArrival = stationArrival?.ifBlank { null },
                    timeDeparture = timeDeparture - timeDeparture % 60_000L,
                    timeArrival = timeArrival - timeArrival % 60_000L,
                )
                routeRepository.savePassenger(passenger).collect { r ->
                    if (r is ResultState.Success) subscribeToChanges(basicId)
                }
            }
        }
    }

    /** Подсказки станций для автодополнения (справочник станций). */
    fun suggestStations(query: String): List<String> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return emptyList()
        return stationDirectory.stations().map { it.name }
            .filter { it.lowercase().contains(q) }
            .distinct()
            .take(8)
    }

    // ═══ Пересчёт производных (§5.1) ═════════════════════════════════════════

    private var nightTimeValue: Long? = null
    private var holidayTimeValue: Long = 0L
    private var errorMessageValue: String? = null
    private var nightWarnValue: List<NightWarnRowUi> = emptyList()
    private var salaryTile: Pair<String, Boolean> = "0 ₽" to false

    private suspend fun recompute(route: Route, settings: UserSettings, salSetting: SalarySetting) {
        val helper = SalaryCalculationHelper(
            userSettings = settings,
            salarySetting = salSetting,
            allRoutes = listOf(route),
            workScheduleProfile = sharedPrefs.getWorkScheduleProfile(),
        )
        coroutineScope {
            launch { calculateSalary(helper, route, settings, salSetting) }
            launch { computeNight(route, settings) }
            launch { computeHoliday(route, settings) }
            launch { computeRest(route, settings) }
            launch { computeError(route) }
            launch { computeNightWarn(route, settings) }
        }
        publishState()
    }

    private suspend fun computeError(route: Route) {
        val r = routeUseCase.isValidBasicData(route).first()
        errorMessageValue = (r as? ResultState.Error)?.entity?.message
    }

    private suspend fun computeNight(route: Route, settings: UserSettings) {
        val night = settings.nightTime
        val millis = CalculateNightTime.getNightTime(
            startMillis = route.basicData.timeStartWork,
            endMillis = route.basicData.timeEndWork,
            hourStart = night.startNightHour,
            minuteStart = night.startNightMinute,
            hourEnd = night.endNightHour,
            minuteEnd = night.endNightMinute,
            offsetInMoscow = settings.timeZone,
            breakStartMillis = route.basicData.timeStartBreak,
            breakEndMillis = route.basicData.timeEndBreak,
        ).first()
        // Ночная часть зазора «прибытие → явка» (018M) — как в расчёте денег.
        val waitingNight = route.getWorkStartWaitingNightTime(
            night.startNightHour, night.startNightMinute, night.endNightHour, night.endNightMinute,
            settings.timeZone,
        )
        nightTimeValue = millis?.plus(waitingNight)
    }

    private suspend fun computeHoliday(route: Route, settings: UserSettings) {
        holidayTimeValue = listOf(route).getWorkingTimeOnAHoliday(settings.selectMonthOfYear, settings.timeZone).first()
    }

    /**
     * «Вторая ночь подряд» (§5.5): предыдущий маршрут — ближайший по явке среди маршрутов
     * ВЫБРАННОГО в настройках месяца. BUG-19 (паритет с Android).
     */
    private suspend fun computeNightWarn(route: Route, settings: UserSettings) {
        val start = route.basicData.timeStartWork
        val end = route.basicData.timeEndWork
        if (start == null || end == null) {
            nightWarnValue = emptyList()
            return
        }
        val monthState = routeUseCase.listRoutesByMonth(settings.selectMonthOfYear, settings.timeZone)
            .first { it is ResultState.Success }
        val prev = (monthState as? ResultState.Success)?.data
            ?.filter { it.basicData.id != route.basicData.id }
            ?.filter { it.basicData.timeStartWork != null && it.basicData.timeEndWork != null }
            ?.filter { (it.basicData.timeStartWork ?: 0L) < start }
            ?.maxByOrNull { it.basicData.timeStartWork ?: 0L }
        val periods = CalculateNightTime.getConsecutiveNightPeriods(
            previousStartMillis = prev?.basicData?.timeStartWork,
            previousEndMillis = prev?.basicData?.timeEndWork,
            previousRestPointOfTurnover = prev?.basicData?.restPointOfTurnover == true,
            currentStartMillis = start,
            currentEndMillis = end,
            offsetInMoscow = settings.timeZone,
        )
        val offset = displayOffsetMs(settings)
        nightWarnValue = periods.mapIndexed { index, period ->
            val label = when {
                index == periods.lastIndex -> "Этот маршрут"
                period.source == CalculateNightTime.ConsecutiveNightSource.TURNOVER_REST -> "Отдых в пункте оборота"
                else -> "Предыдущий маршрут"
            }
            NightWarnRowUi(
                label = label,
                rangeText = "${formatDateTimeShort(period.startMillis, offset)} – ${formatDateTimeShort(period.endMillis, offset)}",
                nightText = formatHhMm(period.intervals.sumOf { it.second - it.first }),
                start = period.startMillis,
                end = period.endMillis,
                nightIntervals = period.intervals.map { NightIntervalUi(it.first, it.second) },
            )
        }
    }

    // ── Зарплата (§5.4) ───────────────────────────────────────────────────────

    private suspend fun calculateSalary(
        helper: SalaryCalculationHelper,
        route: Route,
        setting: UserSettings,
        salSetting: SalarySetting,
    ) {
        val currency = currencySymbol(setting.country)
        val workTime = route.getPureWorkTime()
        val subtitle = route.basicData.number?.takeIf { it.isNotBlank() }?.let { "Маршрут №$it" }
        if (workTime == null) {
            _salary.value = SalaryUi(subtitle = subtitle, isCalculated = false)
            salaryTile = "0 $currency" to false
            return
        }
        val tariffRate = setting.selectMonthOfYear.tariffRate
        val isSetTariffRate = tariffRate != 0.0
        coroutineScope {
            val dTariff = async { helper.getMoneyAtWorkTimeAtTariffSingleRoute().first().let { if (it < 0.0) 0.0 else it } }
            val dWorkForPay = async { helper.getWorkTimeAtTariffSingleRouteFlow().first().coerceAtLeast(0L) }
            val dNight = async { helper.getMoneyAtNightTimeFlow().first() }
            val dZonal = async { helper.getMoneyZonalSurchargeFlow().first() }
            val dZonalPercent = async { helper.getPercentZonalSurchargeFlow().first() }
            val dZonalTime = async { helper.getTimeZonalSurchargeFlow().first() }
            val dPassenger = async { helper.getMoneyAtPassengerFlow().first() }
            val dWaiting = async { helper.getMoneyAtPassengerWaitingFlow().first() }
            val dWaitingTime = async { helper.getPassengerWaitingTimeFlow().first() }
            val dOutside = async { helper.getMoneyAtPassengerOutsideWorkFlow().first() }
            val dHoliday = async { helper.getMoneyAtHolidayFlow().first() }
            val dMileageMoney = async { helper.getMoneyLinearMileageFlow().first() }
            val dMileageAccruals = async { helper.getLinearMileageAccrualsFlow().first() }
            val dExtended = async { helper.getMoneyListSurchargeExtendedServicePhaseFlow().first().sum() }
            val dHeavy = async { helper.getMoneyListSurchargeExtendedHeavyTrainsFlow().first().sum() }
            val dLong = async { helper.getMoneyListSurchargeLongTrainsFlow().first().sum() }
            val dDoubled1 = async { helper.getMoneyDoubledTrainFirstSurchargeFlow(listOf(route)).first() }
            val dDoubled2 = async { helper.getMoneyDoubledTrainSecondSurchargeFlow(listOf(route)).first() }
            val dQualification = async { helper.getMoneyAtQualificationClassFlow().first() }
            val dNordic = async { helper.getMoneyNordicSurcharge().first() }
            val dDistrict = async { helper.getMoneyDistrictSurcharge().first() }
            val dHarmful = async { helper.getMoneyHarmfulnessFlow().first() }
            val dOther = async { helper.getMoneyOtherSurchargeFlow().first() }
            val dOverRest = async {
                val previous = routeUseCase.previousRouteForOverRestFlow(route, TimeCalculationContext.from(setting)).first()
                val payment = route.overRestPayment(
                    previous = previous,
                    minTimeRest = setting.minTimeRestPointOfTurnover,
                    tariffRate = tariffRate,
                )
                payment.money to payment.timeMillis
            }
            val isPassengerTrain = route.usesOnePersonPassengerRate()
            val dOnePerson = async {
                if (isPassengerTrain) helper.getMoneyOnePersonOperationPassengerTrainFlow().first()
                else helper.getMoneyOnePersonOperationFlow().first()
            }
            val dOnePersonTime = async {
                if (isPassengerTrain) helper.getTimeOnePersonOperationPassengerTrainFlow().first()
                else helper.getTimeOnePersonOperationFlow().first()
            }

            val moneyTariff = dTariff.await()
            val workForPay = dWorkForPay.await()
            val night = dNight.await()
            val zonal = dZonal.await()
            val passenger = dPassenger.await()
            val waiting = dWaiting.await()
            val waitingTime = dWaitingTime.await()
            val outside = dOutside.await()
            val holiday = dHoliday.await()
            val mileageMoney = dMileageMoney.await()
            val accruals = dMileageAccruals.await()
            val extended = dExtended.await()
            val heavy = dHeavy.await()
            val long = dLong.await()
            val doubled1 = dDoubled1.await()
            val doubled2 = dDoubled2.await()
            val otherSurcharge = dQualification.await() + dNordic.await() + dDistrict.await() +
                dHarmful.await() + dOther.await()
            val (overRestMoney, overRestTime) = dOverRest.await()
            val onePerson = dOnePerson.await()
            val onePersonTime = dOnePersonTime.await()
            val zonalPercent = dZonalPercent.await()
            val zonalTime = dZonalTime.await()
            val onePersonPercent = if (isPassengerTrain) salSetting.onePersonOperationPassengerTrainPercent
            else salSetting.onePersonOperationPercent

            val businessTripMoney = helper.getMoneyBusinessTripFlow().first()
            val hasBusinessTripPart = helper.hasBusinessTripRoutes()

            // Жёсткий инвариант командировки: маршрут целиком в командировке — только её оплата.
            if (helper.isEntirelyBusinessTrip()) {
                _salary.value = SalaryUi(
                    subtitle = subtitle,
                    isCalculated = true,
                    total = businessTripMoney.toMoneyString(currency),
                    rows = listOfNotNull(
                        businessTripMoney.takeIf { it != 0.0 }?.let {
                            SalaryRowUi("Командировка (по среднему)", "оплата только по среднему часу", it.toMoneyString(currency))
                        }
                    ),
                    averageHourMissing = businessTripMoney == 0.0,
                    tariffMissing = !isSetTariffRate,
                )
                salaryTile = businessTripMoney.toMoneyString(currency) to true
                businessTripFlag = true
                return@coroutineScope
            }
            businessTripFlag = hasBusinessTripPart

            val surchargeAtTrains = extended + heavy + long + doubled1 + doubled2
            val trainTypes = buildList {
                if (heavy != 0.0) add("тяжеловесный")
                if (long != 0.0) add("длинносоставный")
                if (extended != 0.0) add("удлинённое плечо")
                if (doubled1 + doubled2 != 0.0) add("сдвоенный")
            }
            val total = moneyTariff + night + zonal + passenger + waiting + outside + holiday +
                mileageMoney + surchargeAtTrains + onePerson + otherSurcharge + overRestMoney + businessTripMoney

            // Подсказки-формулы (§5.4, §11.9)
            val rateStr = tariffRate.takeIf { it != 0.0 }?.toMoneyString(currency)
            val nightTime = nightTimeValue
            val passengerTime = route.getPassengerTimeWithinWork().takeIf { it > 0L }
            val outsideTime = route.getPassengerTimeOutsideWork().takeIf { it > 0L }
            fun timeRatePercentHint(time: Long?, percent: Double?, fallback: String, percentText: String? = null): String =
                if (rateStr != null && time != null && time > 0L && (percent != null || percentText != null)) {
                    val pct = percentText ?: "${formatPercent(percent!!)} %"
                    "${formatHhMm(time)} × $rateStr/ч × $pct"
                } else fallback
            fun timeRateHint(time: Long?, fallback: String): String =
                if (rateStr != null && time != null && time > 0L) "${formatHhMm(time)} × $rateStr/ч" else fallback

            val hourlyHint = if (rateStr != null) "${formatHhMm(workForPay)} × $rateStr/ч" else "оплата рабочего времени по тарифу"
            val holidayHint = holidayTimeValue.takeIf { it > 0L }
                ?.let { "${formatHhMm(it)} в праздничные дни" } ?: "доплата за работу в праздничные дни"

            val rows = mutableListOf<SalaryRowUi>()
            fun add(label: String, hint: String, value: Double?) {
                if (value != null && value != 0.0) rows.add(SalaryRowUi(label, hint, value.toMoneyString(currency)))
            }
            add("Почасовая оплата", hourlyHint, moneyTariff)
            add("Праздничные", holidayHint, holiday)
            add("Зональная надбавка", timeRatePercentHint(zonalTime, zonalPercent, "надбавка за разъездной характер работы"), zonal)
            add("Ночные", timeRatePercentHint(nightTime, salSetting.nightTimePercent, "доплата за работу в ночное время"), night)
            add("Пассажиром", timeRateHint(passengerTime, "оплата времени следования пассажиром"), passenger)
            add("Пассажиром до явки", timeRateHint(outsideTime, "следование пассажиром до явки"), outside)
            // BUG-40 (паритет с Android): ожидание 018M входит в итог, но строка есть только в Android-шторке при ненулевой сумме
            add("Ожидание пассажиром", timeRateHint(waitingTime, "ожидание следования пассажиром"), waiting)
            accruals.forEach { accrual ->
                val distance = if (accrual.distance % 1.0 == 0.0) accrual.distance.toLong().toString()
                else formatOneDecimal(accrual.distance)
                add("Доплата за пробег", "${accrual.phaseName}: $distance км × ${accrual.rate.toMoneyString(currency)}/км", accrual.money)
            }
            add("Одно лицо", timeRatePercentHint(onePersonTime, onePersonPercent, "надбавка за работу в одно лицо"), onePerson)
            add("Доплаты за поезд", trainTypes.takeIf { it.isNotEmpty() }?.joinToString(", ") ?: "доплаты за категорию поезда", surchargeAtTrains)
            add("Прочие доплаты", "дополнительные начисления", otherSurcharge)
            add("Переотдых", timeRatePercentHint(overRestTime, null, "компенсация за переотдых", percentText = "2/3"), overRestMoney)
            add("Командировка (по среднему)", "оплата только по среднему часу", businessTripMoney)

            _salary.value = SalaryUi(
                subtitle = subtitle,
                isCalculated = true,
                total = total.toMoneyString(currency),
                rows = rows,
                averageHourMissing = hasBusinessTripPart && businessTripMoney == 0.0,
                tariffMissing = !isSetTariffRate,
            )
            salaryTile = total.toMoneyString(currency) to true
        }
    }

    private var businessTripFlag = false

    // ── Отдых (§5.7) ──────────────────────────────────────────────────────────

    private suspend fun computeRest(route: Route, settings: UserSettings) {
        val offset = displayOffsetMs(settings)
        val start = route.basicData.timeStartWork
        val end = route.basicData.timeEndWork
        val isPoint = route.basicData.restPointOfTurnover
        val allRoutes = routeUseCase.getListRoutes()
        val second = isSecondTurnaroundRest(route, allRoutes)
        val actual = calculateActualRest(route, settings)

        fun item(title: String, duration: Long, until: Long) =
            RestItemUi(title, formatDuration(duration), formatDateTimeShort(until, offset))

        val actualItem = actual?.takeIf { it.first > 0 }?.let { item("Фактический отдых", it.first, it.second) }

        if (isPoint) {
            if (start == null || end == null || !route.isTimeWorkValid()) {
                _rest.value = RestUi(
                    isPointOfTurnover = true,
                    unavailableMessage = "Невозможно рассчитать время отдыха.\nПроверьте начало и окончание работы.",
                )
                return
            }
            val min = if (second) settings.minTimeRestPointOfTurnoverSecond else settings.minTimeRestPointOfTurnover
            // Отдых от полного отработанного времени (с перерывом и проездом до явки)
            val work = route.getWorkTime() ?: (end - start)
            var half = work / 2
            if (half % 60_000L != 0L) half += 60_000L // BUG-13 (паритет с Android)
            val shortRest = if (half > min) half else min
            val fullRest = if (work > min) work else min
            _rest.value = RestUi(
                isPointOfTurnover = true,
                isSecondTurnaround = second,
                items = listOfNotNull(
                    item("Короткий отдых", shortRest, end + shortRest),
                    item("Полный отдых", fullRest, end + fullRest),
                    actualItem,
                ),
            )
        } else {
            val home = calculateHomeRest(route, settings)
            if (home == null) {
                _rest.value = RestUi(
                    isPointOfTurnover = false,
                    unavailableMessage = "Невозможно рассчитать время отдыха.\nПроверьте начало и окончание работы во всей цепочке маршрутов.",
                )
                return
            }
            val (duration, endTime, minEnd) = home
            _rest.value = RestUi(
                isPointOfTurnover = false,
                items = listOfNotNull(
                    item("Минимальный отдых", minEnd - (endTime - duration), minEnd),
                    item("Полный отдых", duration, endTime),
                    actualItem,
                ),
                footnote = "Полный = время работы × 2,6 минус отдых в пункте оборота.",
            )
        }
    }

    /** Второй отдых в ПО подряд: у маршрута отдых в ПО и ближайший предыдущий по явке — тоже (§5.7). */
    private fun isSecondTurnaroundRest(route: Route, routes: List<Route>): Boolean {
        if (!route.basicData.restPointOfTurnover) return false
        val start = route.basicData.timeStartWork ?: return false
        val previous = routes.asSequence()
            .filter { it.basicData.id != route.basicData.id }
            .filter { !it.basicData.isDeleted }
            .filter { (it.basicData.timeStartWork ?: Long.MAX_VALUE) < start }
            .maxByOrNull { it.basicData.timeStartWork ?: Long.MIN_VALUE }
        return previous?.basicData?.restPointOfTurnover == true
    }

    private fun previousMonthOf(m: com.z_company.domain.entities.MonthOfYear) =
        if (m.month > 0) m.copy(month = m.month - 1, days = emptyList())
        else m.copy(year = m.year - 1, month = 11, days = emptyList())

    private fun nextMonthOf(m: com.z_company.domain.entities.MonthOfYear) =
        if (m.month < 11) m.copy(month = m.month + 1, days = emptyList())
        else m.copy(year = m.year + 1, month = 0, days = emptyList())

    private suspend fun routesOfMonth(m: com.z_company.domain.entities.MonthOfYear, tz: Long): List<Route>? {
        val r = routeUseCase.listRoutesByMonth(m, tz).first { it is ResultState.Success || it is ResultState.Error }
        return (r as? ResultState.Success)?.data
    }

    /** Домашний отдых (§5.7 п.3): Полный = max(Σработы × 2,6 − Σотдыхов в ПО, minTimeHomeRest). BUG-14. */
    private suspend fun calculateHomeRest(route: Route, settings: UserSettings): Triple<Long, Long, Long>? {
        val start = route.basicData.timeStartWork
        val end = route.basicData.timeEndWork
        if (start == null || end == null) return null
        val month = settings.selectMonthOfYear
        val current = routesOfMonth(month, settings.timeZone) ?: return null
        val prev = routesOfMonth(previousMonthOf(month), settings.timeZone) ?: return null
        val sorted = (current + prev).sortedByDescending { it.basicData.timeStartWork ?: 0L }.toMutableList()
        val existing = sorted.indexOfFirst { it.basicData.id == route.basicData.id }
        if (existing != -1) {
            sorted[existing] = route
        } else {
            val insertIndex = sorted.indexOfFirst { (it.basicData.timeStartWork ?: 0L) < start }
            if (insertIndex == -1) sorted.add(route) else sorted.add(insertIndex, route)
        }
        val index = sorted.indexOfFirst { it.basicData.id == route.basicData.id }
        if (index == -1) return null
        val chain = mutableListOf(sorted[index])
        var next = index + 1
        while (next < sorted.size && sorted[next].basicData.restPointOfTurnover) {
            chain.add(sorted[next])
            next++
        }
        if (chain.any { it.basicData.timeStartWork == null || it.basicData.timeEndWork == null }) return null
        val sumWork = chain.sumOf { it.basicData.timeEndWork!! - it.basicData.timeStartWork!! }
        var sumRest = 0L
        for (i in 1 until chain.size) {
            sumRest += chain[i - 1].basicData.timeStartWork!! - chain[i].basicData.timeEndWork!!
        }
        val raw = (sumWork.toDouble() * 2.6).toLong()
        val duration = maxOf(raw - sumRest, settings.minTimeHomeRest)
        return Triple(duration, end + duration, end + settings.minTimeHomeRest)
    }

    /** Фактический отдых до следующей явки среди маршрутов выбранного и следующего месяца (§5.7 п.4). BUG-19. */
    private suspend fun calculateActualRest(route: Route, settings: UserSettings): Pair<Long, Long>? {
        val end = route.basicData.timeEndWork ?: return null
        val start = route.basicData.timeStartWork ?: return null
        val month = settings.selectMonthOfYear
        val current = routesOfMonth(month, settings.timeZone) ?: emptyList()
        val next = routesOfMonth(nextMonthOf(month), settings.timeZone) ?: emptyList()
        val nextStart = (current + next).asSequence()
            .filter { it.basicData.id != route.basicData.id }
            .mapNotNull { it.basicData.timeStartWork }
            .filter { it > start }
            .minOrNull()
        return if (nextStart != null && nextStart > end) (nextStart - end) to nextStart else null
    }

    // ═══ Публикация состояния ═════════════════════════════════════════════════

    private fun publishState() {
        val route = currentRoute.value
        val settings = userSettings.value
        if (route == null || settings == null) {
            _state.value = _state.value.copy(isLoading = true)
            return
        }
        val offset = displayOffsetMs(settings)
        val basic = route.basicData
        val decimal = settings.isDecimalTime
        val title = when {
            routeId == null -> "Новый маршрут"
            basic.number.isNullOrBlank() -> "Маршрут б/н"
            else -> "Маршрут · №${basic.number}"
        }

        val summary = if (basic.timeStartWork != null && basic.timeEndWork != null) {
            val pure = route.getPureWorkTime()
            if (pure != null) {
                val outside = route.getPassengerTimeOutsideWork()
                val waiting = route.getWorkStartWaitingInterval()?.durationMillis ?: 0L
                val within = route.getPassengerTimeWithinWork()
                WorkSummaryUi(
                    passengerBeforeStart = outside.takeIf { it > 0L }?.let { formatHhMm(it) },
                    waitingBeforeStart = waiting.takeIf { it > 0L }?.let { formatHhMm(it) },
                    worked = if (decimal) formatDecimal(pure) else formatHhMm(pure),
                    night = nightTimeValue?.takeIf { it > 0L }?.let { formatHhMm(it) },
                    passengerInside = within.takeIf { it > 0L }?.let { formatHhMm(it) },
                )
            } else null
        } else null

        val d = duplicate
        val p12 = if (showPassenger12h && basic.timeStartWork != null && basic.timeEndWork != null) {
            prefilledPassengerTimes(basic.timeStartWork!!, basic.timeEndWork!!)
        } else 0L to 0L

        _state.value = RouteFormState(
            isLoading = false,
            basicId = basic.id,
            title = title,
            number = basic.number.orEmpty(),
            notes = basic.notes.orEmpty(),
            isOnePerson = basic.isOnePersonOperation,
            isFavorite = basic.isFavorite,
            restPointOfTurnover = basic.restPointOfTurnover,
            timeStartWork = basic.timeStartWork,
            timeEndWork = basic.timeEndWork,
            timeStartBreak = basic.timeStartBreak,
            timeEndBreak = basic.timeEndBreak,
            startWorkByArrival = route.passengers.any { it.isWorkStartByArrival },
            errorMessage = errorMessageValue,
            isBusinessTrip = businessTripFlag,
            currencySymbol = currencySymbol(settings.country),
            displayOffsetSeconds = (offset / 1000L).toInt(),
            showOnePersonSwitch = settings.isShowOnePersonSwitch,
            showPartner = settings.isShowPartner,
            showBreak = settings.isShowBreak,
            showLocomotive = settings.isShowLocomotive,
            showTrain = settings.isShowTrain,
            showOtherWork = settings.isShowOtherWork,
            showPassenger = settings.isShowPassenger,
            usingDefaultWorkTime = settings.usingDefaultWorkTime,
            summary = summary,
            salaryTileText = salaryTile.first,
            salaryTileIsCalculated = salaryTile.second,
            partners = route.partners.map {
                RoutePartnerRow(
                    id = it.routePartnerId,
                    sourcePartnerId = it.sourcePartnerId,
                    shortName = partnerShortName(it.fullName).ifBlank { "Напарник" },
                    initials = partnerInitials(it.fullName),
                    tabNumber = it.tabNumber?.takeIf { n -> n.isNotBlank() },
                )
            },
            locomotives = locomotiveRows(route),
            trains = trainRows(route),
            otherWorks = otherWorkRows(route),
            passengers = passengerRows(route),
            nightWarn = nightWarnValue,
            isSharedPreview = isSharedPreview && !sharedSheetDismissed,
            duplicate = d?.let {
                DuplicateUi(
                    existingStartText = it.existing.basicData.timeStartWork?.let { ms ->
                        "${formatDate2(ms, offset)} ${formatTime(ms, offset)}"
                    }.orEmpty(),
                    exitAfterSave = it.exitAfterSave,
                )
            },
            showPassenger12h = showPassenger12h,
            p12Departure = p12.first,
            p12Arrival = p12.second,
            p12StationDeparture = prefilledDepartureStation(route).orEmpty(),
        )
    }

    /** Для шторки «Получен новый маршрут»: «Просмотр» прячет шторку, форма остаётся в режиме shared. */
    fun hideSharedSheet() {
        sharedSheetDismissed = true
        publishState()
    }

    // ── Строки дочерних разделов (§5.3 п.9) ──────────────────────────────────

    private fun locomotiveRows(route: Route): List<RouteChildRow> {
        val sorted = route.locomotives.filter { it.timeStartOfAcceptance != null }
            .sortedBy { it.timeStartOfAcceptance } +
            route.locomotives.filter { it.timeStartOfAcceptance == null }
        return sorted.mapIndexed { index, loco ->
            if (loco.series.isNullOrBlank() && loco.number.isNullOrBlank()) {
                RouteChildRow(loco.locoId, "${loco.type.text} № ${index + 1}", isMono = false, tail = "")
            } else {
                val series = loco.series ?: loco.type.text
                val numberText = if (loco.number != null) "№${loco.number}" else ""
                RouteChildRow(loco.locoId, "$series $numberText", isMono = true, tail = "")
            }
        }
    }

    private fun trainRows(route: Route): List<RouteChildRow> =
        TrainOrdering.sortForDisplay(route.trains).mapIndexed { index, train ->
            val names = train.stations.mapNotNull { it.stationName?.takeIf { n -> n.isNotBlank() } }
            val start = names.firstOrNull() ?: ""
            val end = if (names.size > 1) " - ${names.last()}" else ""
            if (train.number.isNullOrBlank()) {
                RouteChildRow(
                    train.trainId,
                    if (names.isEmpty()) "Поезд № ${index + 1}" else "$start$end",
                    isMono = false, tail = "",
                )
            } else {
                RouteChildRow(train.trainId, "№ ${train.number}", isMono = true, tail = " $start$end")
            }
        }

    private fun passengerRows(route: Route): List<RouteChildRow> =
        route.passengers.mapIndexed { index, p ->
            if (p.trainNumber.isNullOrBlank() && p.stationDeparture.isNullOrBlank() && p.stationArrival.isNullOrBlank()) {
                RouteChildRow(p.passengerId, "Пассажиром № ${index + 1}", isMono = false, tail = "")
            } else {
                val number = p.trainNumber?.takeIf { it.isNotBlank() }?.let { "№ $it" } ?: ""
                val dep = p.stationDeparture ?: ""
                val arr = p.stationArrival?.let { " - $it" } ?: ""
                if (number.isNotEmpty()) RouteChildRow(p.passengerId, number, isMono = true, tail = " $dep $arr")
                else RouteChildRow(p.passengerId, "", isMono = false, tail = "$dep $arr")
            }
        }

    private fun otherWorkRows(route: Route): List<RouteChildRow> =
        route.otherWorks.mapIndexed { index, w ->
            val type = w.workType?.takeIf { it.isNotBlank() }
            val station = w.station?.takeIf { it.isNotBlank() }
            if (type == null && station == null) {
                RouteChildRow(w.otherWorkId, "Прочая работа № ${index + 1}", isMono = false, tail = "")
            } else {
                RouteChildRow(w.otherWorkId, "${type ?: "Прочая работа"}${station?.let { " · $it" } ?: ""}", isMono = false, tail = "")
            }
        }

    // ═══ Форматирование (порт ConverterLongToTime / DateAndTimeConverter) ═════

    /** Смещение пояса отображения в мс: `displayTimeZone()` = "GMT+N" (§0.1). */
    private fun displayOffsetMs(settings: UserSettings? = userSettings.value): Long {
        val tz = settings?.displayTimeZone() ?: "GMT+3"
        val hours = tz.removePrefix("GMT").removePrefix("UTC").toIntOrNull() ?: 3
        return hours * 3_600_000L
    }

    private fun local(ms: Long, offset: Long) =
        Instant.fromEpochMilliseconds(ms + offset).toLocalDateTime(TimeZone.UTC)

    private fun two(v: Int) = v.toString().padStart(2, '0')

    private fun formatTime(ms: Long, offset: Long): String = local(ms, offset).let { "${two(it.hour)}:${two(it.minute)}" }

    private fun formatDate2(ms: Long, offset: Long): String =
        local(ms, offset).let { "${two(it.dayOfMonth)}.${two(it.monthNumber)}.${two(it.year % 100)}" }

    private fun formatDate4(ms: Long, offset: Long): String =
        local(ms, offset).let { "${two(it.dayOfMonth)}.${two(it.monthNumber)}.${it.year}" }

    /** `dd.MM HH:mm` (`getDateMiniAndTime`). */
    private fun formatDateTimeShort(ms: Long, offset: Long): String =
        local(ms, offset).let { "${two(it.dayOfMonth)}.${two(it.monthNumber)} ${two(it.hour)}:${two(it.minute)}" }

    /** `HH:MM` (`getTimeInStringFormat`). */
    private fun formatHhMm(ms: Long): String {
        if (ms < 0) return "00:00"
        val totalMinutes = ms / 60_000
        return "${two((totalMinutes / 60).toInt())}:${two((totalMinutes % 60).toInt())}"
    }

    /** `H,dd` (`getTimeInStringDecimalFormat`). */
    private fun formatDecimal(ms: Long): String {
        val totalMinutes = ms / 60_000
        val hours = totalMinutes / 60
        val fraction = ((totalMinutes % 60) / 60.0 * 100).let { kotlin.math.round(it).toInt() }
        return "$hours,${two(fraction)}"
    }

    /** «1д 2ч 5м» (`formatDurationFromMillis`): нулевые дни/часы опускаются, минуты всегда. */
    private fun formatDuration(ms: Long): String {
        val totalMinutes = ms / 60_000
        val days = totalMinutes / 1440
        val hours = (totalMinutes % 1440) / 60
        val minutes = totalMinutes % 60
        return buildString {
            if (days > 0) append("${days}д ")
            if (hours > 0) append("${hours}ч ")
            append("${minutes}м")
        }.trim()
    }

    private fun formatPercent(v: Double): String =
        if (v % 1.0 == 0.0) v.toLong().toString() else formatOneDecimal(v)

    private fun formatOneDecimal(v: Double): String {
        val tenths = kotlin.math.round(v * 10).toLong()
        return "${tenths / 10},${kotlin.math.abs(tenths % 10)}"
    }

    // Имена напарников (порт PartnerUi.kt)
    private fun partnerInitials(fullName: String?): String {
        val words = fullName.orEmpty().trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        return when {
            words.isEmpty() -> "?"
            words.size == 1 -> words[0].take(1).uppercase()
            else -> (words[0].take(1) + words[1].take(1)).uppercase()
        }
    }

    private fun partnerShortName(fullName: String?): String {
        val words = fullName.orEmpty().trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.isEmpty()) return ""
        val initials = words.drop(1).joinToString(" ") { "${it.take(1).uppercase()}." }
        return if (initials.isBlank()) words[0] else "${words[0]} $initials"
    }
}
