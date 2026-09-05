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

    fun restore(routeId: String) {
        if (_uiState.value.restoringRouteId != null || _uiState.value.isRestoringAll) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(restoringRouteId = routeId, message = null)
            routeUseCase.restoreFromTrash(routeId).collect { result ->
                when (result) {
                    is ResultState.Success -> {
                        refresh()
                        _uiState.value = _uiState.value.copy(restoringRouteId = null, message = "Маршрут восстановлен")
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
            routeIds.forEach { routeId ->
                routeUseCase.restoreFromTrash(routeId).collect { result ->
                    if (result is ResultState.Error) failed = true
                }
            }
            refresh()
            _uiState.value = _uiState.value.copy(
                isRestoringAll = false,
                message = if (failed) "Часть маршрутов восстановить не удалось" else "Все маршруты восстановлены",
            )
        }
    }

    fun confirmPendingRemoteDeletions() {
        val pendingIds = _uiState.value.routes
            .filter { it.basicData.remoteDeletionPending }
            .map { it.basicData.id }
        if (pendingIds.isEmpty()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRestoringAll = true, message = null)
            val now = kotlin.time.Clock.System.now().toEpochMilliseconds()
            var failed = false
            pendingIds.forEach { routeId ->
                routeUseCase.acknowledgeRemoteDeletion(routeId, now).collect { result ->
                    if (result is ResultState.Error) failed = true
                }
            }
            refresh()
            _uiState.value = _uiState.value.copy(
                isRestoringAll = false,
                message = if (failed) "Часть подтверждений сохранить не удалось"
                else "Удаление принято. Локальные копии останутся в корзине",
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
