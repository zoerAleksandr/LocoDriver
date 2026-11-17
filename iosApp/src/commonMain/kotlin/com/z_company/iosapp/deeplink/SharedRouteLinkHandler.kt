package com.z_company.iosapp.deeplink

import com.z_company.core.ResultState
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.reidentifyForImport
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.repository.remote_rest.ShareRouteManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Обработчик deep-link `locodriver://share/{id}` на iOS.
 *
 * Singleton (Kotlin `object` → в Swift доступен как `SharedRouteLinkHandler.shared`).
 * Swift-слой (iOSApp.swift) вызывает [handle] при получении URL через `onOpenURL`.
 * Одиночная и общая ссылки загружаются в SwiftUI-шторку импорта; локальная запись
 * выполняется только по действию пользователя.
 */
object SharedRouteLinkHandler : KoinComponent {
    private val shareRouteManager: ShareRouteManager by inject()
    private val routeUseCase: RouteUseCase by inject()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _pendingRoutes = MutableStateFlow<List<Route>>(emptyList())
    private val _isBatchLoading = MutableStateFlow(false)
    private val _isBatchSaving = MutableStateFlow(false)
    private val _batchErrorMessage = MutableStateFlow<String?>(null)
    private var batchJob: Job? = null

    /** Вызывать из Swift: `SharedRouteLinkHandler.shared.handle(urlString: url.absoluteString)`. */
    fun handle(urlString: String) {
        val shareIds = ShareRouteManager.parseShareIds(urlString)
        if (shareIds.isEmpty()) return
        loadBatch(shareIds)
    }

    /** SwiftUI подписывается на пачку маршрутов через callback. */
    fun watchPendingRoutes(callback: (List<Route>) -> Unit) {
        scope.launch { _pendingRoutes.collect { callback(it) } }
    }

    fun watchBatchLoading(callback: (Boolean) -> Unit) {
        scope.launch { _isBatchLoading.collect { callback(it) } }
    }

    fun watchBatchSaving(callback: (Boolean) -> Unit) {
        scope.launch { _isBatchSaving.collect { callback(it) } }
    }

    fun watchBatchErrorMessage(callback: (String?) -> Unit) {
        scope.launch { _batchErrorMessage.collect { callback(it) } }
    }

    /** Сохранить всю загруженную пачку. Ошибочные записи остаются в шторке для повтора. */
    fun savePendingRoutes() {
        if (_isBatchLoading.value || _isBatchSaving.value || _pendingRoutes.value.isEmpty()) return
        val routes = _pendingRoutes.value
        _isBatchSaving.value = true
        _batchErrorMessage.value = null
        scope.launch {
            val failedRoutes = mutableListOf<Route>()
            routes.forEach { route ->
                try {
                    val result = routeUseCase.saveRoute(route).first { it !is ResultState.Loading }
                    if (result !is ResultState.Success) failedRoutes += route
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    failedRoutes += route
                    _batchErrorMessage.value = e.message ?: "Ошибка сохранения маршрутов"
                }
            }
            _pendingRoutes.value = failedRoutes
            _isBatchSaving.value = false
            if (failedRoutes.isEmpty()) {
                _batchErrorMessage.value = null
            } else if (_batchErrorMessage.value == null) {
                _batchErrorMessage.value = "Не удалось сохранить маршрутов: ${failedRoutes.size}"
            }
        }
    }

    /** Закрыть шторку без сохранения. Загруженные маршруты останутся только в памяти. */
    fun cancelPendingRoutes() {
        if (_isBatchSaving.value) return
        batchJob?.cancel()
        _pendingRoutes.value = emptyList()
        _isBatchLoading.value = false
        _batchErrorMessage.value = null
    }

    private fun loadBatch(shareIds: List<String>) {
        batchJob?.cancel()
        _pendingRoutes.value = emptyList()
        _batchErrorMessage.value = null
        _isBatchLoading.value = true
        batchJob = scope.launch {
            val routes = mutableListOf<Route>()
            var failedCount = 0
            shareIds.distinct().forEach { shareId ->
                var route: Route? = null
                val failuresBeforeRequest = failedCount
                try {
                    shareRouteManager.getSharedRoute(shareId).collect { result ->
                        when (result) {
                            is ResultState.Success -> route = result.data.reidentifyForImport()
                            is ResultState.Error -> failedCount++
                            is ResultState.Loading -> Unit
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    failedCount++
                    _batchErrorMessage.value = e.message ?: "Ошибка загрузки маршрутов"
                }
                if (route != null) routes += route!!
                else if (failedCount == failuresBeforeRequest) failedCount++
            }
            _pendingRoutes.value = routes
            _isBatchLoading.value = false
            if (routes.isEmpty()) {
                _batchErrorMessage.value = _batchErrorMessage.value
                    ?: "Не удалось загрузить общие маршруты"
            } else if (failedCount > 0) {
                _batchErrorMessage.value = "Не удалось загрузить маршрутов: $failedCount"
            }
        }
    }

}
