package com.z_company.route.session

import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.repository.SecureTokenStorage
import com.z_company.repository.remote_rest.AuthManager
import com.z_company.repository.remote_rest.GetUserProfileState
import com.z_company.repository.remote_rest.SettingManager
import com.z_company.route.subscription.SubscriptionPeriodTracker
import kotlinx.coroutines.flow.first

/** Хранилище и сеть, которые трогает смена аккаунта (отдельно — ради тестов). */
interface AccountSwitchGateway {
    suspend fun saveUserId(userId: String)
    suspend fun saveAuthToken(token: String)
    suspend fun saveVkId(vkId: String)

    /** Срок подписки аккаунта на сервере; 0 — подписки нет, null — сервер не ответил. */
    suspend fun fetchSubscriptionPeriod(token: String): Long?
    suspend fun setLocalSubscriptionPeriod(period: Long)

    /** Серверный `user.id`; null — профиль не загрузился. */
    suspend fun fetchUserId(token: String): String?
}

/**
 * Вход/регистрация под аккаунтом на устройстве, где мог быть другой аккаунт.
 *
 * Привязанное к аккаунту локальное состояние заменяется, а не наследуется:
 * - `userId` — ключ «увиденного» глобальных диалогов подписки (SCREEN_SPECS
 *   §32.1a) и `user_id` оплаты Robokassa. Прежний стираем ДО смены токена,
 *   новый сохраняем сразу, не дожидаясь открытия профиля;
 * - срок подписки в локальных настройках. Все синхронизации сливают его как
 *   `max(локальный, серверный)`, поэтому срок прежнего аккаунта, оставшийся
 *   после выхода, иначе навсегда достался бы новому (платные функции без
 *   подписки). Ставим серверный срок нового аккаунта; без ответа сервера — 0:
 *   свой срок вернёт первый же успешный restore/синк, а чужой так не утечёт.
 *
 * Всё идёт под [SubscriptionPeriodTracker.runPeriodUpdate]: глобальная
 * проверка не должна увидеть срок нового аккаунта под ключом прежнего.
 */
class AccountSwitcher(
    private val gateway: AccountSwitchGateway,
    private val tracker: SubscriptionPeriodTracker,
) {
    /**
     * @param beforeBind шаги регистрации, которые должны пройти после сброса
     *   срока, но до сохранения нового userId.
     * @return false — срок подписки с сервера не загрузился.
     */
    suspend fun switchTo(
        token: String,
        vkId: String? = null,
        beforeBind: suspend () -> Unit = {},
    ): Boolean = tracker.runPeriodUpdate {
        gateway.saveUserId("")
        gateway.saveAuthToken(token)
        vkId?.let { gateway.saveVkId(it) }
        val period = gateway.fetchSubscriptionPeriod(token)
        gateway.setLocalSubscriptionPeriod(period ?: 0L)
        beforeBind()
        // Без сети id останется пустым — его узнает профиль или
        // SubscriptionNoticesViewModel при следующей проверке.
        gateway.fetchUserId(token)?.takeIf { it.isNotBlank() }?.let { gateway.saveUserId(it) }
        period != null
    }
}

class RemoteAccountSwitchGateway(
    private val secureTokenStorage: SecureTokenStorage,
    private val settingManager: SettingManager,
    private val settingsUseCase: SettingsUseCase,
    private val authManager: AuthManager,
) : AccountSwitchGateway {
    override suspend fun saveUserId(userId: String) = secureTokenStorage.saveUserId(userId)
    override suspend fun saveAuthToken(token: String) = secureTokenStorage.saveAuthToken(token)
    override suspend fun saveVkId(vkId: String) = secureTokenStorage.saveVkId(vkId)

    override suspend fun fetchSubscriptionPeriod(token: String): Long? =
        settingManager.getAccountSubscriptionPeriod("Bearer $token")

    override suspend fun setLocalSubscriptionPeriod(period: Long) {
        settingsUseCase.updateSubscriptionPeriod(period).collect {}
    }

    override suspend fun fetchUserId(token: String): String? {
        val profile = authManager.getUserProfile("Bearer $token")
            .first { it !is GetUserProfileState.Loading }
        return (profile as? GetUserProfileState.Success)?.user?.id
    }
}
