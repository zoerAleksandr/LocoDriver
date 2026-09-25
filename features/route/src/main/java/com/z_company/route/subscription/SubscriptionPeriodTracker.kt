package com.z_company.route.subscription

import com.z_company.domain.repositories.SharedPreferencesRepositories
import com.z_company.repository.SecureTokenStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first

/**
 * Общая (singleton) точка между экраном Подписки и глобальным диалогом
 * «Срок подписки изменён» ([com.z_company.route.viewmodel.SubscriptionNoticesViewModel]).
 *
 * Экран Подписки сам показывает «Платёж принят!» — в этом случае глобальный
 * диалог о сроке не нужен. Пока идёт поллинг оплаты, [paymentCheckInProgress]
 * = true и глобальная проверка молчит; при успехе экран вызывает
 * [acknowledge] ДО снятия флага — новый срок уже «увиден».
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
