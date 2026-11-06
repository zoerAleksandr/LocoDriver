package com.z_company.iosapp.session

import com.z_company.domain.repositories.SharedPreferencesRepositories
import com.z_company.repository.SecureTokenStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Единый источник истины авторизации на iOS (IOS_PLAN С4).
 *
 * Раньше `isAuthorized` каждый экран получал «извне» (гейт покупок — от вызывающего
 * экрана), и состояния расходились. Теперь токен читается только здесь:
 * `SecureTokenStorage.getAuthBearerTokenFlow()` собирается один раз, любой экран
 * подписывается через [watchIsAuthorized].
 *
 * Семантика повторяет Android `AccountSwitcher` / `SessionExpiredHandler`:
 * - [beginAccountSwitch] — вход/регистрация под другим аккаунтом: прежний `userId`
 *   стирается ДО сохранения нового токена (ключ «увиденного» диалогов подписки);
 * - [clearSession] — выход и 401 «Сессия истекла» (§18.5): токен, vk_id, userId
 *   и курсор дельта-синхронизации маршрутов очищаются.
 */
class AuthSessionHolder(
    private val secureTokenStorage: SecureTokenStorage,
    private val sharedPrefs: SharedPreferencesRepositories,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _isAuthorized = MutableStateFlow(false)
    val isAuthorized: StateFlow<Boolean> = _isAuthorized.asStateFlow()

    /** In-memory копия токена: Keychain-запись может не пройти (нет подписи/entitlement),
     *  и тогда чтение из хранилища вернул бы пустоту прямо во время сеанса. */
    private var cachedToken: String? = null

    init {
        scope.launch {
            secureTokenStorage.getAuthBearerTokenFlow().collect { token ->
                if (!token.isNullOrBlank()) cachedToken = token
                _isAuthorized.value = !token.isNullOrBlank()
            }
        }
    }

    /** Подписка для Swift (watchX-паттерн). */
    fun watchIsAuthorized(callback: (Boolean) -> Unit) {
        scope.launch { _isAuthorized.collect { callback(it) } }
    }

    /** Синхронная проверка для гейтов (§3.2): без ожидания асинхронной подписки. */
    fun isAuthorizedNow(): Boolean = _isAuthorized.value || !cachedToken.isNullOrBlank()

    /** Сырой bearer-токен без префикса; null — сессии нет. */
    suspend fun currentToken(): String? =
        cachedToken?.takeIf { it.isNotBlank() } ?: secureTokenStorage.getAuthBearerTokenFlow().first()

    /** Токен в формате HTTP `Authorization`; null — сессии нет. */
    suspend fun currentBearer(): String? = currentToken()?.let { "Bearer $it" }

    suspend fun currentUserId(): String? = secureTokenStorage.getUserIdFlow().first()

    suspend fun currentVkId(): String? = secureTokenStorage.getVkIdFlow().first()

    suspend fun saveToken(token: String) {
        secureTokenStorage.saveAuthToken(token)
        cachedToken = token.takeIf { it.isNotBlank() }
        _isAuthorized.value = token.isNotBlank()
    }

    suspend fun saveUserId(userId: String) {
        secureTokenStorage.saveUserId(userId)
    }

    suspend fun saveVkId(vkId: String) {
        secureTokenStorage.saveVkId(vkId)
    }

    /**
     * Смена аккаунта на устройстве (§18.0 «Результаты входа/регистрации»):
     * прежний userId стирается до записи нового токена. Срок подписки и userId
     * нового аккаунта подтягивает вызывающий (см. ProfileIosViewModel.logInAs).
     */
    suspend fun beginAccountSwitch(token: String, vkId: String? = null) {
        secureTokenStorage.saveUserId("")
        secureTokenStorage.saveAuthToken(token)
        vkId?.let { secureTokenStorage.saveVkId(it) }
        // Токен-флоу одноразовый (cold flow) — флаг обновляем здесь, иначе
        // isAuthorized навсегда остался бы значением с последнего чтения.
        cachedToken = token.takeIf { it.isNotBlank() }
        _isAuthorized.value = token.isNotBlank()
    }

    /**
     * Сессия закрывается (выход или 401 «Сессия истекла», §18.5): токен, vk_id,
     * userId и курсор дельта-синхронизации маршрутов. Локальные маршруты и
     * настройки не удаляются.
     */
    suspend fun clearSession() {
        secureTokenStorage.saveAuthToken("")
        secureTokenStorage.saveVkId("")
        secureTokenStorage.saveUserId("")
        sharedPrefs.setRouteSyncCursor(null)
        cachedToken = null
        _isAuthorized.value = false
    }
}
