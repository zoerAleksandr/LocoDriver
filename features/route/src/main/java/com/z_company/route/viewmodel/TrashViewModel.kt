@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.z_company.route.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.core.ResultState
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.PhysicalDeletionReason
import com.z_company.domain.entities.route.TrashPurgePolicy
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.repository.SecureTokenStorage
import com.z_company.repository.remote_rest.SyncManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

data class TrashUiState(
    val routes: List<Route> = emptyList(),
    val restoringRouteId: String? = null,
    val isRestoringAll: Boolean = false,
    val isPurging: Boolean = false,
    val isSyncing: Boolean = false,
    val message: String? = null,
)

class TrashViewModel : ViewModel(), KoinComponent {
    private val routeUseCase: RouteUseCase by inject()
    private val syncManager: SyncManager by inject()
    private val secureTokenStorage: SecureTokenStorage by inject()
    private val routeActionsHelper: RouteActionsHelper by inject()
    private val _uiState = MutableStateFlow(TrashUiState())
    val uiState = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        _uiState.value = _uiState.value.copy(routes = routeUseCase.listTrash())
    }

    fun onScreenOpened() {
        refresh()
        purgeExpiredRoutes()
        if (_uiState.value.isSyncing || syncManager.isSyncInProgress()) return
        if (!syncManager.shouldRunAutomaticSync(cooldownMillis = TRASH_SYNC_COOLDOWN_MILLIS)) return
        viewModelScope.launch {
            if (!routeActionsHelper.hasActiveSubscription()) return@launch
            val token = secureTokenStorage.getAuthBearerTokenFlow().first()
            if (token.isNullOrBlank()) return@launch
            _uiState.value = _uiState.value.copy(isSyncing = true)
            var errorMessage: String? = null
            try {
                syncManager.syncBidirectional("Bearer $token").collect { result ->
                    if (result is ResultState.Error) {
                        errorMessage = result.entity.message ?: "Не удалось синхронизировать корзину"
                    }
                }
            } catch (error: kotlinx.coroutines.CancellationException) {
                throw error
            } catch (error: Exception) {
                errorMessage = error.message ?: "Не удалось синхронизировать корзину"
            } finally {
                refresh()
                _uiState.value = _uiState.value.copy(isSyncing = false, message = errorMessage)
            }
        }
    }

    private fun purgeExpiredRoutes() {
        viewModelScope.launch {
            val cutoff = kotlin.time.Clock.System.now().toEpochMilliseconds() -
                30L * 24L * 60L * 60L * 1000L
            _uiState.value.routes.filter { route ->
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
     * Отправляет восстановленный маршрут на сервер сразу, не дожидаясь
     * следующей полной синхронизации.
     *
     * Восстановление само по себе локальное, а на сервере маршрут остаётся
     * удалённым, и там уже лежит отметка об удалении. Пока восстановленный
     * маршрут не уехал обратно, другое устройство при синхронизации получит эту
     * отметку и снова уберёт свою копию — восстановление будет выглядеть как
     * не сработавшее.
     *
     * Неудача отправки восстановление не отменяет: маршрут остаётся
     * несинхронизированным и уедет следующей синхронизацией. Возвращает текст
     * ошибки или null.
     */
    private suspend fun pushRestoredRoute(routeId: String): String? {
        if (!routeActionsHelper.hasActiveSubscription()) return null
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
        if (_uiState.value.restoringRouteId != null || _uiState.value.isRestoringAll) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(restoringRouteId = routeId, message = null)
            routeUseCase.restoreFromTrash(routeId).collect { result ->
                when (result) {
                    is ResultState.Success -> {
                        val pushError = pushRestoredRoute(routeId)
                        refresh()
                        _uiState.value = _uiState.value.copy(
                            restoringRouteId = null,
                            message = pushError ?: "Маршрут восстановлен",
                        )
                    }
                    is ResultState.Error -> _uiState.value = _uiState.value.copy(
                        restoringRouteId = null,
                        message = result.entity.message ?: "Не удалось восстановить маршрут",
                    )
                    else -> Unit
                }
            }
        }
    }

    fun restoreAll() {
        val routeIds = _uiState.value.routes.map { it.basicData.id }
        if (routeIds.isEmpty() || _uiState.value.restoringRouteId != null || _uiState.value.isRestoringAll) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRestoringAll = true, message = null)
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
            _uiState.value = _uiState.value.copy(
                isRestoringAll = false,
                message = when {
                    failed -> "Часть маршрутов восстановить не удалось"
                    pushFailed -> "Маршруты восстановлены, часть не отправлена на сервер"
                    else -> "Все маршруты восстановлены"
                },
            )
        }
    }

    fun emptyTrash() {
        val purgeable = _uiState.value.routes.filter(TrashPurgePolicy::canPurgeManually)
        if (purgeable.isEmpty() || _uiState.value.isRestoringAll || _uiState.value.isPurging) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isPurging = true, message = null)
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
            val waitingForServer = _uiState.value.routes.count { route ->
                route.basicData.isDeleted &&
                    !route.basicData.remoteDeletionPending &&
                    !route.basicData.remoteRouteId.isNullOrBlank() &&
                    route.basicData.remoteDeletedAt == null
            }
            _uiState.value = _uiState.value.copy(
                isPurging = false,
                message = when {
                    failed == 0 && waitingForServer > 0 ->
                        "Удалено: $purged. Ожидают удаления с сервера: $waitingForServer"
                    failed == 0 -> "Корзина очищена: $purged"
                    purged == 0 -> "Не удалось очистить корзину"
                    else -> "Удалено: $purged, не удалось удалить: $failed"
                },
            )
        }
    }

    fun consumeMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    private companion object {
        const val TRASH_SYNC_COOLDOWN_MILLIS = 5L * 60L * 1000L
    }
}
