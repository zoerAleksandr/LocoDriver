@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.z_company.iosapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.core.ResultState
import com.z_company.domain.entities.route.PhysicalDeletionReason
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.TrashPurgePolicy
import com.z_company.domain.track.TrackRepository
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.repository.SecureTokenStorage
import com.z_company.repository.remote_rest.SyncManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlin.time.Clock

// ═════════════════════════════════════════════════════════════════════════════
// Модели корзины (§9.4).
// ═════════════════════════════════════════════════════════════════════════════

data class TrashRouteUi(
    val basicId: String,
    /** «dd.MM.yyyy · HH:mm» явки или «Дата и время не указаны». */
    val dateText: String,
    /** «Удалён dd.MM.yyyy в HH:mm». */
    val deletedAtText: String,
    /** «Осталось N день/дня/дней» или «Срок хранения истёк». */
    val daysLeftText: String,
    /** Ожидает удаления с сервера (tombstone ещё не подтверждён). */
    val isWaitingServer: Boolean,
    /** «Проверяем статус» — remoteDeletionPending. */
    val isPendingRemote: Boolean,
    /** Можно удалить физически (TrashPurgePolicy.canPurgeManually). */
    val canPurge: Boolean,
)

data class TrashUi(
    val routes: List<TrashRouteUi> = emptyList(),
    /** id маршрута, который сейчас восстанавливается (кнопки остальных неактивны). */
    val restoringRouteId: String? = null,
    val isRestoringAll: Boolean = false,
    val isPurging: Boolean = false,
    val isSyncing: Boolean = false,
    /** «Очистить» активна, только если есть физически удаляемые записи. */
    val canClear: Boolean = false,
    /** «Проверяем статус: N» (remoteDeletionPending). */
    val pendingCheckCount: Int = 0,
    /** «Ожидают синхронизации: N». */
    val waitingSyncCount: Int = 0,
)

// ═════════════════════════════════════════════════════════════════════════════
// ViewModel корзины маршрутов — SCREEN_SPECS §9.4 (порт Android `TrashViewModel`).
// ═════════════════════════════════════════════════════════════════════════════

class TrashIosViewModel(
    private val routeUseCase: RouteUseCase,
    private val settingsUseCase: SettingsUseCase,
    private val syncManager: SyncManager,
    private val secureTokenStorage: SecureTokenStorage,
    private val trackRepository: TrackRepository? = null,
) : ViewModel() {

    private val _ui = MutableStateFlow(TrashUi())
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 16)

    private var rawRoutes: List<Route> = emptyList()

    init {
        refresh()
    }

    fun watchUi(callback: (TrashUi) -> Unit) {
        viewModelScope.launch { _ui.collect { callback(it) } }
    }

    fun watchMessages(callback: (String) -> Unit) {
        viewModelScope.launch { _messages.collect { callback(it) } }
    }

    fun refresh() {
        rawRoutes = routeUseCase.listTrash()
        publish()
    }

    /** Открытие экрана: список + автопрокат срока + тихая синхронизация (§9.4). */
    fun onScreenOpened() {
        refresh()
        purgeExpiredRoutes()
        if (_ui.value.isSyncing || syncManager.isSyncInProgress()) return
        if (!syncManager.shouldRunAutomaticSync(cooldownMillis = TRASH_SYNC_COOLDOWN_MILLIS)) return
        viewModelScope.launch {
            if (!hasActiveSubscriptionNow()) return@launch
            val token = secureTokenStorage.getAuthBearerTokenFlow().first()
            if (token.isNullOrBlank()) return@launch
            _ui.value = _ui.value.copy(isSyncing = true)
            var errorMessage: String? = null
            try {
                syncManager.syncBidirectional("Bearer $token").collect { result ->
                    if (result is ResultState.Error) {
                        errorMessage = result.entity.message ?: "Не удалось синхронизировать корзину"
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (error: Exception) {
                errorMessage = error.message ?: "Не удалось синхронизировать корзину"
            } finally {
                refresh()
                _ui.value = _ui.value.copy(isSyncing = false)
                errorMessage?.let { _messages.tryEmit(it) }
            }
        }
    }

    /** Подтверждённые записи старше 30 дней удаляются автоматически (§9.4). */
    private fun purgeExpiredRoutes() {
        viewModelScope.launch(Dispatchers.Default) {
            val cutoff = Clock.System.now().toEpochMilliseconds() - 30L * 24L * 60L * 60L * 1000L
            rawRoutes.filter { route ->
                val deletedAt = route.basicData.deletedAt
                deletedAt != null && deletedAt <= cutoff && TrashPurgePolicy.canPurgeManually(route)
            }.forEach { route ->
                routeUseCase.purgeRoute(route, PhysicalDeletionReason.TRASH_RETENTION_EXPIRED)
                    .collect { }
            }
            refresh()
        }
    }

    /**
     * Отправляет восстановленный маршрут на сервер сразу (§9.4). Неудача отправки
     * восстановление не отменяет. Возвращает текст ошибки или null.
     */
    private suspend fun pushRestoredRoute(routeId: String): String? {
        if (!hasActiveSubscriptionNow()) return null
        val token = secureTokenStorage.getAuthBearerTokenFlow().first()
        if (token.isNullOrBlank()) return null
        var errorMessage: String? = null
        syncManager.syncRoute(routeId, "Bearer $token").collect { result ->
            if (result is ResultState.Error) {
                errorMessage = result.entity.message
                    ?: "Маршрут восстановлен, но отправить его на сервер не удалось"
            }
        }
        return errorMessage
    }

    fun restore(routeId: String) {
        if (_ui.value.restoringRouteId != null || _ui.value.isRestoringAll) return
        viewModelScope.launch {
            _ui.value = _ui.value.copy(restoringRouteId = routeId)
            routeUseCase.restoreFromTrash(routeId).collect { result ->
                when (result) {
                    is ResultState.Success -> {
                        val pushError = pushRestoredRoute(routeId)
                        refresh()
                        _ui.value = _ui.value.copy(restoringRouteId = null)
                        _messages.tryEmit(pushError ?: "Маршрут восстановлен")
                    }

                    is ResultState.Error -> {
                        _ui.value = _ui.value.copy(restoringRouteId = null)
                        _messages.tryEmit(
                            result.entity.message ?: "Не удалось восстановить маршрут"
                        )
                    }

                    else -> Unit
                }
            }
        }
    }

    fun restoreAll() {
        val routeIds = rawRoutes.map { it.basicData.id }
        if (routeIds.isEmpty() || _ui.value.restoringRouteId != null || _ui.value.isRestoringAll) {
            return
        }
        viewModelScope.launch {
            _ui.value = _ui.value.copy(isRestoringAll = true)
            var failed = false
            var pushFailed = false
            routeIds.forEach { routeId ->
                var restored = false
                routeUseCase.restoreFromTrash(routeId).collect { result ->
                    if (result is ResultState.Error) failed = true
                    if (result is ResultState.Success) restored = true
                }
                if (restored && pushRestoredRoute(routeId) != null) pushFailed = true
            }
            refresh()
            _ui.value = _ui.value.copy(isRestoringAll = false)
            _messages.tryEmit(
                when {
                    failed -> "Часть маршрутов восстановить не удалось"
                    pushFailed -> "Маршруты восстановлены, часть не отправлена на сервер"
                    else -> "Все маршруты восстановлены"
                }
            )
        }
    }

    /** Физическое удаление только для записей без потери tombstone (§9.4). */
    fun emptyTrash() {
        val purgeable = rawRoutes.filter(TrashPurgePolicy::canPurgeManually)
        if (purgeable.isEmpty() || _ui.value.isRestoringAll || _ui.value.isPurging) return
        viewModelScope.launch {
            _ui.value = _ui.value.copy(isPurging = true)
            var purged = 0
            var failed = 0
            purgeable.forEach { route ->
                routeUseCase.purgeRoute(route, PhysicalDeletionReason.USER_EMPTIED_TRASH)
                    .collect { result ->
                        when (result) {
                            is ResultState.Success -> purged++
                            is ResultState.Error -> failed++
                            else -> Unit
                        }
                    }
            }
            refresh()
            val waitingForServer = rawRoutes.count { route ->
                route.basicData.isDeleted &&
                    !route.basicData.remoteDeletionPending &&
                    !route.basicData.remoteRouteId.isNullOrBlank() &&
                    route.basicData.remoteDeletedAt == null
            }
            _ui.value = _ui.value.copy(isPurging = false)
            _messages.tryEmit(
                when {
                    failed == 0 && waitingForServer > 0 ->
                        "Удалено: $purged. Ожидают удаления с сервера: $waitingForServer"

                    failed == 0 -> "Корзина очищена: $purged"
                    purged == 0 -> "Не удалось очистить корзину"
                    else -> "Удалено: $purged, не удалось удалить: $failed"
                }
            )
        }
    }

    // ── Строки подтверждений (§9.4) ─────────────────────────────────────────

    /**
     * Текст диалога очистки. BUG-33 (паритет с Android): слово «маршрутов»
     * не склоняется — переносим как есть.
     */
    fun emptyTrashConfirmText(count: Int): String =
        "Будут безвозвратно удалены $count маршрутов."

    /** Строка «Будут удалены и записанные треки.» — если у удаляемых поездов есть треки. */
    fun purgeTracksNote(): String? {
        if (!hasRecordedTracksForPurgeable()) return null
        return "Будут удалены и записанные треки."
    }

    fun hasRecordedTracksForPurgeable(): Boolean {
        val repo = trackRepository ?: return false
        val purgeable = rawRoutes.filter(TrashPurgePolicy::canPurgeManually)
        return runCatching {
            purgeable.any { route ->
                route.trains.any { train -> repo.tracksForTrain(train.trainId).isNotEmpty() }
            }
        }.getOrDefault(false)
    }

    fun dispose() {
        // Синхронизацию не отменяем: она идёт общая, а экран — только наблюдатель.
    }

    // ── Внутреннее ──────────────────────────────────────────────────────────

    private fun publish() {
        // BUG-32 (паритет с Android): даты корзины форматируются в поясе
        // устройства (SimpleDateFormat), а не в поясе отображения (§0.1).
        val deviceTz = TimeZone.currentSystemDefault()
        val now = Clock.System.now().toEpochMilliseconds()
        val items = rawRoutes
            .sortedByDescending { it.basicData.deletedAt ?: 0L }
            .map { route ->
                val deletedAt = route.basicData.deletedAt
                val daysLeftMs = deletedAt?.let {
                    it + 30L * 24L * 60L * 60L * 1000L - now
                }
                val daysLeft = daysLeftMs?.let { ms ->
                    if (ms <= 0L) 0L else (ms + 24L * 60L * 60L * 1000L - 1) / (24L * 60L * 60L * 1000L)
                }
                TrashRouteUi(
                    basicId = route.basicData.id,
                    dateText = route.basicData.timeStartWork?.let {
                        "${Fmt.dateFull(it, deviceTz)} · ${Fmt.time(it, deviceTz)}"
                    } ?: "Дата и время не указаны",
                    deletedAtText = deletedAt?.let {
                        "Удалён ${Fmt.dateFull(it, deviceTz)} в ${Fmt.time(it, deviceTz)}"
                    } ?: "",
                    daysLeftText = when {
                        daysLeft == null -> ""
                        daysLeft <= 0L -> "Срок хранения истёк"
                        else -> "Осталось $daysLeft ${Fmt.daysWord(daysLeft)}"
                    },
                    isWaitingServer = route.basicData.isDeleted &&
                        !route.basicData.remoteDeletionPending &&
                        !route.basicData.remoteRouteId.isNullOrBlank() &&
                        route.basicData.remoteDeletedAt == null,
                    isPendingRemote = route.basicData.remoteDeletionPending,
                    canPurge = TrashPurgePolicy.canPurgeManually(route),
                )
            }
        _ui.value = _ui.value.copy(
            routes = items,
            canClear = items.any { it.canPurge },
            pendingCheckCount = items.count { it.isPendingRemote },
            waitingSyncCount = items.count { it.isWaitingServer },
        )
    }

    private suspend fun hasActiveSubscriptionNow(): Boolean =
        settingsUseCase.getUserSettingFlow().first().subscriptionPeriod >
            Clock.System.now().toEpochMilliseconds()

    private companion object {
        const val TRASH_SYNC_COOLDOWN_MILLIS = 5L * 60L * 1000L
    }
}
