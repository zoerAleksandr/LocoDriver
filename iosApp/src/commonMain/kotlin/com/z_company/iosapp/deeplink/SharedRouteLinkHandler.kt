package com.z_company.iosapp.deeplink

import com.z_company.core.AppError
import com.z_company.core.ResultState
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.reidentifyForImport
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.repository.remote_rest.ShareRouteManager
import com.z_company.repository.remote_rest.toAppError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Обработчик deep-link `locodriver://share/{id}` на iOS.
 *
 * Singleton (Kotlin `object` → в Swift доступен как `SharedRouteLinkHandler.shared`).
 * Swift-слой (iOSApp.swift) вызывает [handle] при получении URL через `onOpenURL`.
 * После успешной загрузки и сохранения маршрута в локальную БД эмитится
 * `basicData.id` в [pendingFormRouteId] — `AppNavHost` подхватывает его и навигирует
 * на `FormRoute`.
 */
object SharedRouteLinkHandler : KoinComponent {
    private val shareRouteManager: ShareRouteManager by inject()
    private val routeUseCase: RouteUseCase by inject()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _pendingFormRouteId = MutableStateFlow<String?>(null)
    val pendingFormRouteId: StateFlow<String?> = _pendingFormRouteId.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _appError = MutableStateFlow<AppError?>(null)
    val appError: StateFlow<AppError?> = _appError.asStateFlow()

    /** Вызывать из Swift: `SharedRouteLinkHandler.shared.handle(urlString: url.absoluteString)`. */
    fun handle(urlString: String) {
        val shareId = ShareRouteManager.parseShareId(urlString) ?: return
        scope.launch {
            try {
                shareRouteManager.getSharedRoute(shareId).collect { result ->
                    when (result) {
                        is ResultState.Success -> saveImported(result.data.reidentifyForImport())
                        is ResultState.Error -> {
                            _errorMessage.value =
                                result.entity.message ?: "Не удалось загрузить общий маршрут"
                            _appError.value = result.entity.appError
                        }
                        is ResultState.Loading -> Unit
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Ошибка загрузки общего маршрута"
                _appError.value = e.toAppError()
            }
        }
    }

    fun clearPendingFormRouteId() { _pendingFormRouteId.value = null }
    fun clearError() {
        _errorMessage.value = null
        _appError.value = null
    }

    // ── watch helpers для Swift (по тому же паттерну, что у iOS-ViewModel'ей) ──
    fun watchErrorMessage(callback: (String?) -> Unit) {
        scope.launch { errorMessage.collect { callback(it) } }
    }

    fun watchAppError(callback: (AppError?) -> Unit) {
        scope.launch { appError.collect { callback(it) } }
    }

    fun watchPendingFormRouteId(callback: (String?) -> Unit) {
        scope.launch { pendingFormRouteId.collect { callback(it) } }
    }

    private suspend fun saveImported(route: Route) {
        val newBasicId = route.basicData.id
        try {
            routeUseCase.saveRoute(route).collect { state ->
                when (state) {
                    is ResultState.Success -> _pendingFormRouteId.value = newBasicId
                    is ResultState.Error -> {
                        _errorMessage.value =
                            state.entity.message ?: "Ошибка импорта маршрута"
                        _appError.value = state.entity.appError
                    }
                    is ResultState.Loading -> Unit
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _errorMessage.value = e.message ?: "Ошибка импорта маршрута"
            _appError.value = e.toAppError()
        }
    }
}
