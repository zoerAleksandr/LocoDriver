package com.z_company.route.subscription

import com.z_company.domain.repositories.SharedPreferencesRepositories
import com.z_company.repository.SecureTokenStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first

/**
 * Общая (singleton) точка между экраном Подписки и глобальным диалогом
 * «Срок подписки изменён» ([com.z_company.route.viewmodel.SubscriptionPeriodViewModel]).
 *
 * Экран Подписки сам показывает «Платёж принят!» — в этом случае глобальный
 * диалог не нужен. Пока идёт поллинг оплаты, [paymentCheckInProgress] = true
 * и глобальная проверка молчит; при успехе экран вызывает [acknowledge] ДО
 * снятия флага — новый срок уже «увиден», и глобальный диалог не всплывёт.
 */
class SubscriptionPeriodTracker(
    private val secureTokenStorage: SecureTokenStorage,
    private val sharedPrefs: SharedPreferencesRepositories,
) {
    private val _paymentCheckInProgress = MutableStateFlow(false)
    val paymentCheckInProgress: StateFlow<Boolean> = _paymentCheckInProgress.asStateFlow()

    fun setPaymentCheckInProgress(value: Boolean) {
        _paymentCheckInProgress.value = value
    }

    /** Запомнить [period] как увиденный пользователем текущего аккаунта. */
    suspend fun acknowledge(period: Long) {
        val userId = currentUserId() ?: return
        sharedPrefs.setLastSeenSubscriptionPeriod(userId, period)
    }

    /**
     * Сравнить [period] с последним увиденным и запомнить его. Возвращает
     * предыдущий увиденный срок, если об изменении нужно сообщить, иначе null.
     *
     * Не сообщаем: без аккаунта; при `period <= 0` (пустые настройки до
     * загрузки — не запоминаем, чтобы следующий реальный срок не выглядел
     * «изменением»); при первом отслеживании аккаунта на устройстве (-1 —
     * только база, иначе вход с уже оплаченной подпиской дал бы ложный диалог).
     */
    suspend fun consumeChange(period: Long): Long? {
        if (period <= 0L) return null
        val userId = currentUserId() ?: return null
        val lastSeen = sharedPrefs.getLastSeenSubscriptionPeriod(userId)
        if (lastSeen == period) return null
        sharedPrefs.setLastSeenSubscriptionPeriod(userId, period)
        return lastSeen.takeIf { it > 0L }
    }

    private suspend fun currentUserId(): String? =
        secureTokenStorage.getUserIdFlow().first()?.takeIf { it.isNotBlank() }
}
