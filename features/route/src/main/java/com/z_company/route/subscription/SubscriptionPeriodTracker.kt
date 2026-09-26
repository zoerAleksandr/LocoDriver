package com.z_company.route.subscription

import com.z_company.domain.repositories.SharedPreferencesRepositories
import com.z_company.repository.SecureTokenStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update

/**
 * Общая (singleton) точка между экранами, которые сами меняют срок подписки,
 * и глобальными диалогами ([com.z_company.route.viewmodel.SubscriptionNoticesViewModel]).
 *
 * Экран Подписки сам показывает «Платёж принят!» — в этом случае глобальный
 * диалог о сроке не нужен. Вход в аккаунт подтягивает срок нового аккаунта
 * до того, как известен его userId, — сравнивать его с «увиденным» нельзя.
 * Пока идёт хоть одно такое обновление ([runPeriodUpdate]),
 * [periodUpdatesInProgress] > 0 и глобальная проверка молчит. Счётчик, а не
 * флаг: первый завершившийся поллинг не должен открыть проверку, пока второй
 * ещё идёт. При успешной оплате экран вызывает [acknowledge] ДО выхода из
 * [runPeriodUpdate] — новый срок уже «увиден».
 */
class SubscriptionPeriodTracker(
    private val secureTokenStorage: SecureTokenStorage,
    private val sharedPrefs: SharedPreferencesRepositories,
) {
    private val _periodUpdatesInProgress = MutableStateFlow(0)
    val periodUpdatesInProgress: StateFlow<Int> = _periodUpdatesInProgress.asStateFlow()

    suspend fun <T> runPeriodUpdate(block: suspend () -> T): T {
        _periodUpdatesInProgress.update { it + 1 }
        try {
            return block()
        } finally {
            _periodUpdatesInProgress.update { it - 1 }
        }
    }

    /**
     * Запомнить [period] как увиденный пользователем текущего аккаунта.
     * Бонус рефералки за эту же оплату (у приглашённого) уже внутри срока —
     * учтённые бонусные дни сбрасываем в «неизвестно», глобальная проверка
     * возьмёт текущий awardedDays как базу.
     */
    suspend fun acknowledge(period: Long) {
        val userId = secureTokenStorage.getUserIdFlow().first()?.takeIf { it.isNotBlank() } ?: return
        sharedPrefs.setLastSeenSubscriptionPeriod(userId, period)
        sharedPrefs.setSubscriptionPeriodReferralDays(userId, -1)
    }
}
