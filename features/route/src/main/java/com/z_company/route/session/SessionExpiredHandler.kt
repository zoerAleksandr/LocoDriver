package com.z_company.route.session

import com.z_company.domain.repositories.SharedPreferencesRepositories
import com.z_company.repository.SecureTokenStorage
import com.z_company.repository.remote_rest.SessionExpiredNotifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Единственное место, где приложение закрывает истёкшую сессию.
 *
 * Bearer-токен живёт 180 дней и не продлевается. Когда сервер начинает
 * отвечать 401, Ktor-клиент шлёт сигнал в [SessionExpiredNotifier] с любого
 * запроса — синхронизации с главного экрана, фонового воркера, профиля. Здесь
 * по этому сигналу сессия закрывается так же, как при явном выходе
 * (`ProfileViewModel.logOut`): стухший токен не остаётся в хранилище и не
 * порождает «залогинен → форма входа» при следующем открытии профиля.
 *
 * Сообщение пользователю здесь не показывается: его даёт тот экран, где
 * случился 401 (диалог синхронизации, snackbar фонового sync, профиль) —
 * иначе одно и то же «Сессия истекла» приходило бы дважды.
 *
 * Идемпотентно: десяток 401 подряд (по запросу на каждый маршрут) и
 * параллельный вызов из экрана профиля чистят хранилище один раз.
 */
class SessionExpiredHandler(
    private val secureTokenStorage: SecureTokenStorage,
    private val sharedPrefs: SharedPreferencesRepositories,
) {
    private val mutex = Mutex()

    /** Подписка на сигнал сетевого слоя; [scope] живёт столько же, сколько приложение. */
    fun start(scope: CoroutineScope) {
        SessionExpiredNotifier.events
            .onEach { logOutExpiredSession() }
            .launchIn(scope)
    }

    /** Закрыть сессию, если она ещё считалась живой. */
    suspend fun logOutExpiredSession() = mutex.withLock {
        val token = secureTokenStorage.getAuthBearerTokenFlow().first()
        if (token.isNullOrBlank()) return@withLock
        secureTokenStorage.saveAuthToken("")
        secureTokenStorage.saveVkId("")
        // Курсор дельта-синхронизации привязан к аккаунту — см. logOut.
        sharedPrefs.setRouteSyncCursor(null)
    }
}
