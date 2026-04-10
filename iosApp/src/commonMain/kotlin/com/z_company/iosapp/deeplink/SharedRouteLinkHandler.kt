package com.z_company.iosapp.deeplink

import com.z_company.core.ResultState
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.reidentifyForImport
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.repository.remote_rest.ShareRouteManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    /** Вызывать из Swift: `SharedRouteLinkHandler.shared.handle(urlString: url.absoluteString)`. */
    fun handle(urlString: String) {
        val shareId = ShareRouteManager.parseShareId(urlString) ?: return
        scope.launch {
            try {
                shareRouteManager.getSharedRoute(shareId).collect { result ->
                    when (result) {
                        is ResultState.Success -> saveImported(result.data.reidentifyForImport())
                        is ResultState.Error -> _errorMessage.value =
                            result.entity.message ?: "Не удалось загрузить общий маршрут"
                        is ResultState.Loading -> Unit
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Ошибка загрузки общего маршрута"
            }
        }
    }

    fun clearPendingFormRouteId() { _pendingFormRouteId.value = null }
    fun clearError() { _errorMessage.value = null }

    private suspend fun saveImported(route: Route) {
        val newBasicId = route.basicData.id
        try {
            routeUseCase.saveRoute(route).collect { state ->
                when (state) {
                    is ResultState.Success -> _pendingFormRouteId.value = newBasicId
                    is ResultState.Error -> _errorMessage.value =
                        state.entity.message ?: "Ошибка импорта маршрута"
                    is ResultState.Loading -> Unit
                }
            }
        } catch (e: Exception) {
            _errorMessage.value = e.message ?: "Ошибка импорта маршрута"
        }
    }
}
