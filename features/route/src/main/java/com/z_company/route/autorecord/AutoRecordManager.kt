package com.z_company.route.autorecord

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.core.app.NotificationCompat
import com.z_company.core.ResultState
import com.z_company.core.gps.GpsPermissions
import com.z_company.core.gps.GpsPoint
import com.z_company.core.gps.GpsRecorder
import com.z_company.core.gps.GpsRecorderConfig
import com.z_company.core.sendToSentry
import com.z_company.domain.autorecord.AutoStationLocator
import com.z_company.domain.autorecord.AutoTrainWriter
import com.z_company.domain.autorecord.MotionSample
import com.z_company.domain.autorecord.StopDetector
import com.z_company.domain.autorecord.StopEvent
import com.z_company.domain.autorecord.TrainOrdering
import com.z_company.domain.gnss.GnssLevel
import com.z_company.domain.gnss.GnssQuality
import com.z_company.domain.gnss.GnssQualityConfig
import com.z_company.domain.gnss.GnssQualityRules
import com.z_company.domain.entities.route.Train
import com.z_company.domain.entities.route.UtilsForEntities.findCurrentRoute
import com.z_company.domain.entities.setting.UserSettings
import com.z_company.domain.track.TrackPoint
import com.z_company.domain.track.TrackRecordingState
import com.z_company.domain.track.TrackRepository
import com.z_company.domain.track.AltitudeContribStore
import com.z_company.domain.track.AltitudeSessionBuffer
import com.z_company.domain.track.PendingContribSession
import com.z_company.domain.track.TrackWriter
import com.z_company.domain.track.newContribSessionId
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.domain.util.displayTimeZone
import com.z_company.domain.use_cases.StationDirectoryUseCase
import com.z_company.domain.use_cases.TrainUseCase
import com.z_company.domain.util.StationDirectory
import com.z_company.route.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Состояние кнопки «АВТО»: включена ли и до какого момента (автовыключение). */
data class AutoRecordState(
    val enabled: Boolean = false,
    val untilMs: Long = 0L,
    /** Система остановила запись (убили службу/процесс, нельзя перезапустить); нужен ручной запуск. */
    val interrupted: Boolean = false,
)

/**
 * Кнопка «АВТО» (SCREEN_SPECS §6.2 п.6а): записывает стоянки и отправления текущего поезда по
 * скорости GPS. Первый потребитель переиспользуемого [GpsRecorder]. Состояние (вкл/время) живёт
 * в SharedPreferences устройства, на сервер не уходит; точки трека не хранятся, координаты не
 * логируются.
 */
class AutoRecordManager(
    private val context: Context,
    private val routeUseCase: RouteUseCase,
    private val trainUseCase: TrainUseCase,
    private val stationDirectory: StationDirectoryUseCase,
    private val prefs: SharedPreferences,
    private val scope: CoroutineScope,
    private val settingsUseCase: SettingsUseCase,
    /** Единое определение названия станции по месту (плечо / сервер по рельсам / справочник); null - только справочник и плечо. */
    private val stopNamer: com.z_company.domain.util.StopStationNamer? = null,
    /** Состояние изменилось (для обновления виджетов). */
    private val onChanged: suspend () -> Unit = {},
    /** Включить/отключить периодическую проверку живучести службы (WorkManager в app). */
    private val onWatchdog: (Boolean) -> Unit = {},
    private val nowMs: () -> Long = { System.currentTimeMillis() },
    /** Локальные треки (SCREEN_SPECS §6.9); null — запись трека недоступна. */
    private val trackRepository: TrackRepository? = null,
    /** Согласие №1 на сохранение треков; null — как «нет согласия». */
    private val trackConsent: TrackConsentStore? = null,
    /**
     * Запланировать отправку треков (WorkManager в app). Зовётся при старте приложения и по окончании
     * сеанса «АВТО»; воркер сам ждёт, пока трек «затихнет», и пропускает прогон без согласия.
     */
    private val onTrackUpload: () -> Unit = {},
    /**
     * Временное хранилище точек для вклада высот (согласие №2, SCREEN_SPECS §6.9); null - вклад недоступен.
     * Не зависит от согласия №1 и не пишет в Track.db.
     */
    private val altitudeStore: AltitudeContribStore? = null,
    /** Запланировать отправку вклада высот (WorkManager в app) после сеанса «АВТО». */
    private val onAltitudeContrib: () -> Unit = {},
) {
    private val _state = MutableStateFlow(AutoRecordState())
    val state: StateFlow<AutoRecordState> = _state.asStateFlow()

    /** Качество связи со спутниками (SCREEN_SPECS §6.9); UNKNOWN, пока запись не идёт. */
    val gnssQuality: StateFlow<GnssQuality> get() = GpsRecorder.quality

    private var detector = StopDetector()
    private var qualityJob: Job? = null
    private var lostSinceMs: Long? = null
    private var lostNotified = false
    private var pointsJob: Job? = null
    private var watchJob: Job? = null
    private var expiryJob: Job? = null
    private val writeLock = Mutex()
    @Volatile
    private var settings: UserSettings = UserSettings()
    private var lastBeatWriteMs = 0L
    private val trackWriter = trackRepository?.let { TrackWriter(it) }
    private val altitudeBuffer = AltitudeSessionBuffer()

    init {
        if (prefs.getLong(KEY_UNTIL, 0L) == 0L && prefs.getBoolean(KEY_INTERRUPTED, false)) {
            _state.value = AutoRecordState(interrupted = true)
        }
        // Настройки нужны для часового пояса «до чч:мм» (§0.1) и поиска текущего маршрута.
        scope.launch {
            settingsUseCase.getUserSettingFlow().catch { }.collect { s ->
                settings = s
                val zone = s.displayTimeZone()
                if (prefs.getString(KEY_TZ, null) != zone) {
                    prefs.edit().putString(KEY_TZ, zone).apply()
                    val st = _state.value
                    if (st.enabled) {
                        runCatching { GpsRecorder.start(context, recorderConfig(st.untilMs)) }
                        runCatching { onChanged() }
                    }
                }
            }
        }
    }

    /** Открытый экран «Поезд» регистрируется здесь, чтобы события шли в его форму, а не мимо неё. */
    private class Editor(val trainId: String, val apply: suspend (StopEvent, String?) -> Boolean)

    @Volatile
    private var editor: Editor? = null

    fun registerEditor(trainId: String, apply: suspend (StopEvent, String?) -> Boolean) {
        editor = Editor(trainId, apply)
    }

    fun unregisterEditor(trainId: String) {
        if (editor?.trainId == trainId) editor = null
    }

    /** Нужны ли разрешения перед [enable] (для запроса из UI). */
    fun missingPermissions(): Array<String> = GpsPermissions.missing(context)

    /**
     * Включить. Вызывать, когда приложение на экране и геолокация разрешена (иначе false).
     * Автовыключение — через [AUTO_OFF_MS].
     */
    fun enable(trainId: String? = null): Boolean {
        if (!GpsPermissions.hasLocation(context)) return false
        if (_state.value.enabled) return true
        val until = nowMs() + AUTO_OFF_MS
        // Целевой поезд: из кнопки на экране «Поезд»; без контекста — фиксируется асинхронно ниже.
        prefs.edit().putLong(KEY_UNTIL, until).putLong(KEY_BEAT, nowMs())
            .putBoolean(KEY_INTERRUPTED, false)
            .apply { if (trainId != null) putString(KEY_TARGET, trainId) else remove(KEY_TARGET) }
            .apply()
        if (trainId == null) {
            // Одноразовая фиксация по TrainOrdering в момент включения; нет маршрута/поездов — при первом событии.
            scope.launch {
                runCatching {
                    val route = withContext(Dispatchers.Default) { currentRoute() } ?: return@launch
                    TrainOrdering.pickAutoTarget(route.trains)?.let { saveTarget(it.trainId) }
                }
            }
        }
        cancelAlerts()
        prefs.edit().remove(KEY_STOPPED).remove(KEY_STOPPED_SINCE).remove(KEY_INTERRUPTED_UNTIL).apply()
        setState(AutoRecordState(true, until))
        warnNoConsentOnce()
        return begin(until)
    }

    fun disable() = stopInternal(notifyExpired = false)

    /**
     * Состояние записи трека для диагностики пустого графика (SCREEN_SPECS §6.9): согласие, АВТО, цель.
     * [thisTrainId] - поезд, на экране которого открыта карта; [otherTrainNumber] - номер целевого, если он другой.
     */
    fun trackRecordingState(thisTrainId: String?, otherTrainNumber: String? = null) = TrackRecordingState(
        consent = trackConsent?.saveTracks?.value == true,
        autoEnabled = _state.value.enabled,
        targetTrainId = prefs.getString(KEY_TARGET, null),
        thisTrainId = thisTrainId,
        otherTrainNumber = otherTrainNumber,
    )

    /**
     * Цель кнопки записи на карте (SCREEN_SPECS §6.9): id поезда, в который писать трек. [openedTrainId] - поезд
     * экрана, из которого открыта карта (null - с Главного: текущий по явке / последний поезд, иначе новый поезд
     * в текущем маршруте, иначе новый маршрут с явкой «сейчас» и пустым поездом). Возвращает null при ошибке.
     */
    suspend fun resolveRecordTarget(openedTrainId: String?): String? = withContext(Dispatchers.Default) {
        runCatching {
            val route = currentRoute()
            when (val t = TrainOrdering.pickRecordTarget(openedTrainId, route?.basicData?.id, route?.trains)) {
                is TrainOrdering.RecordTarget.Existing -> t.trainId
                is TrainOrdering.RecordTarget.NewTrainInRoute -> {
                    val train = Train(basicId = t.routeId)
                    trainUseCase.updateTrain(train).first { it !is ResultState.Loading }
                    train.trainId
                }
                TrainOrdering.RecordTarget.NewRoute -> {
                    val basic = com.z_company.domain.entities.route.BasicData(timeStartWork = nowMs())
                    val train = Train(basicId = basic.id)
                    val newRoute = com.z_company.domain.entities.route.Route(
                        basicData = basic, trains = mutableListOf(train),
                    )
                    routeUseCase.saveRoute(newRoute).first { it !is ResultState.Loading }
                    train.trainId
                }
            }
        }.onFailure { it.sendToSentry("AutoRecordManager", "resolveRecordTarget") }.getOrNull()
    }

    /** Поезд, который сейчас пишет «АВТО» (null - цель не определена). */
    fun targetTrainId(): String? = prefs.getString(KEY_TARGET, null)

    /**
     * Сделать [trainId] целью записи, не перезапуская сеанс (следующая точка откроет новый трек для этого поезда).
     * Только при включённом «АВТО».
     */
    fun setTarget(trainId: String): Boolean {
        if (!_state.value.enabled) return false
        saveTarget(trainId)
        return true
    }

    /** Из Application после старта графа: возобновить, если включено и не истекло. */
    fun restore() {
        // Фоновая очистка треков старше 180 дней (не избранных) при старте приложения.
        trackRepository?.let { repo ->
            scope.launch(Dispatchers.Default) {
                runCatching { repo.purgeExpired(nowMs()) }.onFailure { it.sendToSentry("AutoRecordManager", "purgeTracks") }
            }
            // Отправка накопленного и очистка осиротевших треков (воркер без сети/входа просто выходит).
            runCatching { onTrackUpload() }
        }
        // Вклад высот (согласие №2): неотправленные сеансы - в отправку; без согласия - удалить.
        altitudeStore?.let { st ->
            runCatching {
                if (trackConsent?.altitudeContrib?.value == true) {
                    if (st.pendingIds().isNotEmpty()) onAltitudeContrib()
                } else {
                    st.clear()
                }
            }
        }
        val until = prefs.getLong(KEY_UNTIL, 0L)
        if (until == 0L) return
        if (nowMs() >= until) {
            prefs.edit().remove(KEY_UNTIL).apply()
            GpsRecorder.stop(context)
            return
        }
        setState(AutoRecordState(true, until))
        // Не стартовали из фона — не выключаемся молча: если службы нет, expiryJob/watchdog
        // переведут АВТО в «прервано» с уведомлением.
        begin(until, resume = true)
    }

    /**
     * Приложение вышло на передний план (MainActivity.onStart). Если АВТО было «прервано» только потому, что процесс
     * поднялся в фоне (виджет/воркер/после force-stop) и службу нельзя стартовать из фона, а время работы ещё не вышло —
     * тихо возобновляем запись (теперь старт из переднего плана разрешён), без ручного нажатия «АВТО».
     */
    fun onAppForeground() {
        val st = _state.value
        if (st.enabled) {
            // Процесс поднялся в фоне (restore() не смог стартовать службу), а приложение уже на экране: старт разрешён —
            // повторяем до истечения таймаута, иначе через 15 с АВТО ушло бы в «прервано» при открытом приложении.
            if (!GpsRecorder.isRunning.value) {
                runCatching { GpsRecorder.start(context, recorderConfig(st.untilMs)) }
                    .onFailure { it.sendToSentry("AutoRecordManager", "foregroundStart") }
            }
            return
        }
        if (!st.interrupted) return
        val until = prefs.getLong(KEY_INTERRUPTED_UNTIL, 0L)
        if (until <= nowMs() || !GpsPermissions.hasLocation(context)) return
        prefs.edit().putLong(KEY_UNTIL, until).putLong(KEY_BEAT, nowMs()).putBoolean(KEY_INTERRUPTED, false)
            .remove(KEY_INTERRUPTED_UNTIL).apply()
        cancelAlerts()
        setState(AutoRecordState(true, until))
        begin(until, resume = true)
    }

    /**
     * Проверка живучести (WorkManager, ~15 мин): АВТО включено, а службы/точек нет.
     * Службы нет → пробуем перезапустить; не вышло — «прервано» + уведомление. Служба есть, но
     * точек нет > [STALE_MS] — мягкий перезапуск подписки (без «прервано»: GPS пропадает в тоннелях).
     * Заодно напоминает, если текущего маршрута нет.
     */
    suspend fun checkAlive() {
        val until = prefs.getLong(KEY_UNTIL, 0L)
        if (until == 0L) return
        val now = nowMs()
        if (now >= until) {
            stopInternal(notifyExpired = true)
            return
        }
        if (!_state.value.enabled) restore()
        if (!GpsRecorder.isRunning.value) {
            begin(until, resume = true)
            delay(START_TIMEOUT_MS + 5_000L)
            if (_state.value.enabled && !GpsRecorder.isRunning.value) {
                markInterrupted()
                return
            }
        } else if (now - prefs.getLong(KEY_BEAT, until - AUTO_OFF_MS) > STALE_MS) {
            runCatching { GpsRecorder.start(context, recorderConfig(until)) }
        }
        val hasRoute = withContext(Dispatchers.Default) { currentRoute() } != null
        if (!hasRoute) notifyNoRoute()
    }

    /** Служба остановлена системой и не поднимается: индикатор «прервано» + уведомление. */
    private fun markInterrupted() {
        pointsJob?.cancel()
        watchJob?.cancel()
        expiryJob?.cancel()
        // Время работы запоминаем: при выходе приложения на передний план запись возобновится (onAppForeground).
        val resumeUntil = _state.value.untilMs.takeIf { it > 0 } ?: prefs.getLong(KEY_UNTIL, 0L)
        prefs.edit().remove(KEY_UNTIL).putBoolean(KEY_INTERRUPTED, true).putLong(KEY_INTERRUPTED_UNTIL, resumeUntil).apply()
        resetTrack()
        onWatchdog(false)
        setState(AutoRecordState(interrupted = true))
        GpsRecorder.stop(context)
        val open = launchIntent(enable = true)
        notify(
            INTERRUPTED_ID, "АВТО остановлено системой. Включить снова", open,
            actions = emptyList(),
        )
    }

    private fun setState(new: AutoRecordState) {
        _state.value = new
        scope.launch { runCatching { onChanged() } }
    }

    private fun begin(until: Long, resume: Boolean = false): Boolean {
        onWatchdog(true)
        detector = StopDetector()
        // После перезапуска процесса продолжаем с сохранённого состояния «стоим/едем», если запись прервалась недавно.
        if (resume && prefs.contains(KEY_STOPPED) && nowMs() - prefs.getLong(KEY_BEAT, 0L) <= RESUME_MAX_GAP_MS) {
            detector.seed(prefs.getBoolean(KEY_STOPPED, false), prefs.getLong(KEY_STOPPED_SINCE, 0L))
        }
        pointsJob?.cancel()
        watchJob?.cancel()
        expiryJob?.cancel()
        qualityJob?.cancel()
        lostSinceMs = null
        lostNotified = false
        qualityJob = scope.launch { GpsRecorder.quality.collect { onQuality(it) } }

        pointsJob = scope.launch {
            GpsRecorder.points.collect { onPoint(it) }
        }
        // Служба остановилась снаружи (кнопка «Выключить» в уведомлении) — выключаемся и мы.
        watchJob = scope.launch {
            var sawRunning = false
            GpsRecorder.isRunning.collect { running ->
                if (running) sawRunning = true
                else if (sawRunning) {
                    stopInternal(notifyExpired = false, stopService = false)
                }
            }
        }
        expiryJob = scope.launch {
            // Служба не поднялась (например, нельзя стартовать из фона) — не оставляем «включено» без GPS.
            delay(START_TIMEOUT_MS)
            if (_state.value.enabled && !GpsRecorder.isRunning.value) {
                markInterrupted()
                return@launch
            }
            val left = until - nowMs()
            if (left > 0) delay(left)
            if (_state.value.enabled) stopInternal(notifyExpired = true)
        }
        return try {
            GpsRecorder.start(context, recorderConfig(until))
            true
        } catch (e: Exception) {
            // ForegroundServiceStartNotAllowedException и т. п. — без координат и ПДн.
            e.sendToSentry("AutoRecordManager", "start")
            false
        }
    }

    private fun stopInternal(notifyExpired: Boolean, stopService: Boolean = true) {
        qualityJob?.cancel()
        lostSinceMs = null
        lostNotified = false
        pointsJob?.cancel()
        watchJob?.cancel()
        expiryJob?.cancel()
        prefs.edit().remove(KEY_UNTIL).remove(KEY_TARGET).remove(KEY_STOPPED).remove(KEY_STOPPED_SINCE)
            .remove(KEY_INTERRUPTED_UNTIL).putBoolean(KEY_INTERRUPTED, false).apply()
        resetTrack()
        onWatchdog(false)
        cancelAlerts()
        val wasEnabled = _state.value.enabled
        setState(AutoRecordState())
        if (stopService) GpsRecorder.stop(context)
        if (wasEnabled && notifyExpired) showExpiredNotification()
    }

    private suspend fun onPoint(p: GpsPoint) {
        val s = _state.value
        if (!s.enabled) return
        val now = nowMs()
        if (now - lastBeatWriteMs > BEAT_WRITE_MS) {
            lastBeatWriteMs = now
            prefs.edit().putLong(KEY_BEAT, now).apply()
        }
        if (nowMs() >= s.untilMs) {
            stopInternal(notifyExpired = true)
            return
        }
        recordTrackPoint(p)
        val event = detector.onSample(
            MotionSample(p.timeMs, p.latitude, p.longitude, p.speedKmh, p.accuracyM),
        ) ?: return
        prefs.edit().putBoolean(KEY_STOPPED, detector.isStopped).putLong(KEY_STOPPED_SINCE, detector.stoppedSince).apply()
        try {
            writeLock.withLock { handleEvent(event) }
        } catch (e: Exception) {
            e.sendToSentry("AutoRecordManager", "handleEvent")
        }
    }

    /** Точка трека: только при согласии №1, в поезд, где нажата «АВТО»; прореживается TrackWriter. */
    private fun recordTrackPoint(p: GpsPoint) {
        recordAltitudeSample(p)
        val writer = trackWriter ?: return
        val consent = trackConsent?.saveTracks?.value == true
        val target = prefs.getString(KEY_TARGET, null)
        try {
            synchronized(writer) {
                writer.onPoint(
                    target,
                    TrackPoint(p.timeMs, p.latitude, p.longitude, p.speedKmh, p.accuracyM, p.altitudeM, p.baroAltitudeM),
                    consent,
                    // Потеря сигнала / плохая точность: «точку стоянки» не пишем (§6.9). Возраст fix по часам
                    // устройства не сравниваем: при неверном времени телефона отбрасывались бы все точки.
                    signalOk = GpsRecorder.quality.value.level != GnssLevel.LOST &&
                        GnssQualityRules.isUsableFix(0L, p.accuracyM),
                )
            }
        } catch (e: Exception) {
            e.sendToSentry("AutoRecordManager", "trackPoint") // без координат
        }
    }

    /** Точка для вклада высот: только при согласии №2 (независимо от №1), в память до конца сеанса. */
    private fun recordAltitudeSample(p: GpsPoint) {
        if (altitudeStore == null || trackConsent?.altitudeContrib?.value != true) return
        synchronized(altitudeBuffer) {
            altitudeBuffer.add(
                TrackPoint(p.timeMs, p.latitude, p.longitude, p.speedKmh, p.accuracyM, p.altitudeM, p.baroAltitudeM),
            )
        }
    }

    /** Сеанс закончен: накопленные точки - во временный файл для воркера (если согласие №2 ещё действует). */
    private fun flushAltitudeSession() {
        val samples = synchronized(altitudeBuffer) { altitudeBuffer.drain() }
        val store = altitudeStore ?: return
        if (samples.isEmpty() || trackConsent?.altitudeContrib?.value != true) return
        try {
            store.save(PendingContribSession(newContribSessionId(), nowMs(), samples))
            runCatching { onAltitudeContrib() }
        } catch (e: Exception) {
            e.sendToSentry("AutoRecordManager", "altitudeFlush") // без координат
        }
    }

    private fun resetTrack() {
        flushAltitudeSession()
        trackWriter?.let { synchronized(it) { it.reset() } }
        // Сеанс закончен — трек готов к отправке (воркер подождёт паузу без новых точек).
        if (trackConsent?.saveTracks?.value == true) runCatching { onTrackUpload() }
    }

    /** Однократное предупреждение, если АВТО включено без согласия на сохранение трека. */
    private fun warnNoConsentOnce() {
        val consent = trackConsent ?: return
        if (consent.takeNoConsentWarning()) {
            notify(NO_CONSENT_ID, TrackConsentStore.NO_CONSENT_WARNING, launchIntent(enable = false), emptyList())
        }
    }

    private fun saveTarget(id: String) {
        prefs.edit().putString(KEY_TARGET, id).apply()
    }

    private suspend fun handleEvent(event: StopEvent) {
        // Цель - поезд, где нажата «АВТО»: ищем среди поездов ВСЕХ неудалённых маршрутов, не только текущего.
        val saved = prefs.getString(KEY_TARGET, null)
        val targeted = withContext(Dispatchers.Default) {
            TrainOrdering.findTargetTrain(
                routeUseCase.getListRoutes().filter { !it.basicData.isDeleted }.map { it.trains }, saved,
            )
        }
        val train = targeted ?: run {
            // Цели нет / поезд удалён: fallback по правилу TrainOrdering на текущем маршруте.
            val route = withContext(Dispatchers.Default) { currentRoute() }
            if (route == null) {
                // Писать некуда (маршрута нет / время работы вышло) - событие отбрасываем, но не молчим.
                notifyNoRoute()
                return
            }
            TrainOrdering.pickTarget(route.trains, null) ?: Train(basicId = route.basicData.id)
        }
        // Фиксируем фактическую цель (создание нового поезда / fallback при удалённом запомненном).
        if (prefs.getString(KEY_TARGET, null) != train.trainId) saveTarget(train.trainId)

        // Название станции - единая функция (плечо и порядок станций поезда, радиус 1 км; без плеча - станция по рельсам
        // с сервера, запасной вариант - справочник).
        val name = withContext(Dispatchers.Default) {
            val dir = stationDirectory.stations()
            val points = StationDirectory.points(dir)
            val names = StationDirectory.names(dir)
            val phase = train.servicePhase?.let { sp ->
                val full = runCatching {
                    settingsUseCase.getUserSettingFlow().first().servicePhases.firstOrNull { it.id == sp.id }
                }.getOrNull() ?: sp
                stationDirectory.resolve(full)
            }
            val written = com.z_company.domain.util.ServicePhaseStationPicker.writtenNames(
                train.stations.map { it.stationName }, train.servicePhase != null,
            )
            if (stopNamer != null) {
                stopNamer.name(
                    phase, written, event.latitude, event.longitude, points, names,
                    onDiscovered = { stationDirectory.fillCoordinates(it.name, it.latitude, it.longitude) },
                )
            } else {
                com.z_company.domain.util.ServicePhaseStationPicker.pickForStop(
                    phase, written, event.latitude, event.longitude, points, names,
                )
            }
        }

        val open = editor
        if (open != null && open.trainId == train.trainId) {
            // Форма «Поезд» открыта на этом поезде: пишем через неё, иначе её автосохранение затрёт запись.
            if (withContext(Dispatchers.Main) { open.apply(event, name) }) return
        }
        val updated = AutoTrainWriter.apply(train, event, name) ?: return
        withContext(Dispatchers.Default) {
            trainUseCase.updateTrain(updated).first { it !is ResultState.Loading }
        }
    }

    private suspend fun currentRoute() =
        routeUseCase.getListRoutes().findCurrentRoute(nowMs(), settings)

    private fun recorderConfig(until: Long) = GpsRecorderConfig(
        notificationTitle = "LocoDriver",
        notificationText = "АВТО включено до ${formatTime(context, until)}",
        stopActionLabel = "Выключить",
        smallIconRes = R.drawable.ic_stat_auto,
        channelName = "Запись АВТО",
    )

    private fun showExpiredNotification() =
        notify(EXPIRED_ID, "АВТО выключено: прошло 12 часов", launchIntent(enable = false), emptyList())

    /** «АВТО включено, но текущего маршрута нет» — не чаще раза в 30 мин, с «Выключить» и «Открыть приложение». */
    private fun notifyNoRoute() {
        val now = nowMs()
        if (now - prefs.getLong(KEY_NO_ROUTE_AT, 0L) < NO_ROUTE_REPEAT_MS) return
        prefs.edit().putLong(KEY_NO_ROUTE_AT, now).apply()
        val off = PendingIntent.getBroadcast(
            context, 2,
            Intent(context, AutoRecordActionReceiver::class.java).setAction(AutoRecordActionReceiver.ACTION_DISABLE)
                .setPackage(context.packageName),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        notify(
            NO_ROUTE_ID, "АВТО включено, но текущего маршрута нет (работа окончена?)",
            launchIntent(enable = false),
            listOf("Выключить" to off, "Открыть приложение" to launchIntent(enable = false)),
        )
    }

    /**
     * Качество сигнала (SCREEN_SPECS §6.9): переход в LOST сбрасывает окна детектора; LOST дольше 30 с -
     * одноразовое «Потерян сигнал GPS», по возвращении - «Сигнал GPS восстановлен».
     */
    private fun onQuality(q: GnssQuality) {
        if (!_state.value.enabled) return
        val now = nowMs()
        when (q.level) {
            GnssLevel.LOST -> {
                if (lostSinceMs == null) {
                    lostSinceMs = now
                    detector.onSignalLost()
                }
                if (!lostNotified && now - (lostSinceMs ?: now) >= GnssQualityConfig.LOST_NOTIFY_AFTER_MS) {
                    lostNotified = true
                    notify(GPS_LOST_ID, "Потерян сигнал GPS. Записи стоянок и отправлений приостановлены", launchIntent(enable = false), emptyList())
                }
            }
            GnssLevel.GOOD, GnssLevel.WEAK -> {
                if (lostSinceMs != null) {
                    lostSinceMs = null
                    if (lostNotified) {
                        lostNotified = false
                        notify(GPS_LOST_ID, "Сигнал GPS восстановлен", launchIntent(enable = false), emptyList())
                    }
                }
            }
            GnssLevel.UNKNOWN -> Unit
        }
    }

    private fun cancelAlerts() {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        nm.cancel(GPS_LOST_ID)
        nm.cancel(INTERRUPTED_ID)
        nm.cancel(NO_ROUTE_ID)
    }

    private fun launchIntent(enable: Boolean): PendingIntent? =
        context.packageManager.getLaunchIntentForPackage(context.packageName)?.let {
            if (enable) it.putExtra("widget_auto_enable", true) // MainActivity включает АВТО при открытии
            PendingIntent.getActivity(
                context, if (enable) 3 else 4, it,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        }

    private fun notify(id: Int, text: String, content: PendingIntent?, actions: List<Pair<String, PendingIntent?>>) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(
                NotificationChannel(ALERT_CHANNEL, "АВТО: предупреждения", NotificationManager.IMPORTANCE_DEFAULT),
            )
        }
        val b = NotificationCompat.Builder(context, ALERT_CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_auto)
            .setContentTitle("LocoDriver")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setContentIntent(content)
        actions.forEach { (label, pi) -> b.addAction(0, label, pi) }
        runCatching { nm.notify(id, b.build()) } // без разрешения на уведомления — молча
    }

    companion object {
        const val AUTO_OFF_MS = 12 * 60 * 60 * 1000L
        const val PREFS_NAME = "auto_record"
        private const val KEY_UNTIL = "until_ms"
        private const val KEY_TARGET = "target_train_id"
        private const val KEY_BEAT = "heartbeat_ms"
        private const val KEY_INTERRUPTED = "interrupted"
        private const val KEY_STOPPED = "detector_stopped"
        private const val KEY_STOPPED_SINCE = "detector_stopped_since"
        private const val KEY_INTERRUPTED_UNTIL = "interrupted_until_ms"
        private const val RESUME_MAX_GAP_MS = 10 * 60_000L
        private const val KEY_TZ = "display_tz"
        private const val KEY_NO_ROUTE_AT = "no_route_notified_ms"
        const val KEY_BATTERY_ASKED = "battery_asked"
        private const val STALE_MS = 10 * 60_000L
        private const val BEAT_WRITE_MS = 20_000L
        private const val NO_ROUTE_REPEAT_MS = 30 * 60_000L
        private const val START_TIMEOUT_MS = 15_000L
        private const val ALERT_CHANNEL = "auto_record_alerts"
        private const val EXPIRED_ID = 7402
        private const val NO_ROUTE_ID = 7403
        private const val INTERRUPTED_ID = 7404
        private const val NO_CONSENT_ID = 7405
        private const val GPS_LOST_ID = 7406

        /** Для виджетов: момент автовыключения или 0, если АВТО выключено. */
        fun savedUntil(context: Context, now: Long = System.currentTimeMillis()): Long =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getLong(KEY_UNTIL, 0L)
                .takeIf { it > now } ?: 0L

        /**
         * «чч:мм» в часовом поясе отображения пользователя (§0.1: РФ/Беларусь — МСК, Казахстан — свой),
         * а не по часам устройства. Пояс сохраняется менеджером в prefs при смене настроек.
         */
        fun formatTime(context: Context, ms: Long): String {
            val id = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_TZ, null)
            val fmt = SimpleDateFormat("HH:mm", Locale.getDefault())
            if (id != null) fmt.timeZone = TimeZone.getTimeZone(id)
            return fmt.format(Date(ms))
        }
    }
}
