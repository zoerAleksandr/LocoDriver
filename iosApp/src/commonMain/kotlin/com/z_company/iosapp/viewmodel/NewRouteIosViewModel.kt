@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.z_company.iosapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.repository.SecureTokenStorage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Clock

/**
 * iOS-аналог `RouteActionsHelper.newRouteClick()` — гейт кнопки «+» (SCREEN_SPECS §3.4).
 *
 * - подписка активна (`subscriptionPeriod ≠ 0` и `subscriptionPeriod + 24 ч ≥ now`) → "open";
 * - иначе считаются ВСЕ локальные маршруты, включая корзину (`listRouteWithDeleting`):
 *   `≥ FREE_ROUTES_LIMIT` → "limit" («Бесплатный лимит исчерпан»),
 *   `< FREE_ROUTES_LIMIT` → "trial" («Пробный период», осталось N из 20);
 * - ошибка → "error" (ничего не происходит).
 */
class NewRouteIosViewModel(
    private val routeUseCase: RouteUseCase,
    private val settingsUseCase: SettingsUseCase,
    private val secureTokenStorage: SecureTokenStorage,
) : ViewModel() {

    companion object {
        /** Лимит бесплатных маршрутов без подписки (как `RouteActionsHelper.FREE_ROUTES_LIMIT`). */
        const val FREE_ROUTES_LIMIT = 20
        private const val GRACE_PERIOD_MS = 24L * 3_600_000L
    }

    fun onNewRouteClick(callback: (NewRouteIosDecision) -> Unit) {
        viewModelScope.launch {
            val decision = try {
                decide()
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                NewRouteIosDecision(type = "error", freeRoutesLeft = 0, isAuthorized = false)
            }
            callback(decision)
        }
    }

    private suspend fun decide(): NewRouteIosDecision {
        val now = Clock.System.now().toEpochMilliseconds()
        val setting = settingsUseCase.getUserSettingFlow().first()
        val time = setting.subscriptionPeriod
        val subscriptionActive = time != 0L && time + GRACE_PERIOD_MS >= now
        val token = secureTokenStorage.getAuthBearerTokenFlow().first()
        val isAuthorized = !token.isNullOrBlank()
        if (subscriptionActive) {
            return NewRouteIosDecision(type = "open", freeRoutesLeft = 0, isAuthorized = isAuthorized)
        }
        val routesSize = withContext(Dispatchers.Default) {
            routeUseCase.listRouteWithDeleting().size
        }
        return if (routesSize >= FREE_ROUTES_LIMIT) {
            NewRouteIosDecision(type = "limit", freeRoutesLeft = 0, isAuthorized = isAuthorized)
        } else {
            NewRouteIosDecision(
                type = "trial",
                freeRoutesLeft = (FREE_ROUTES_LIMIT - routesSize).coerceAtLeast(0),
                isAuthorized = isAuthorized,
            )
        }
    }
}
