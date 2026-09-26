package com.z_company.route.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.domain.repositories.SharedPreferencesRepositories
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.repository.SecureTokenStorage
import com.z_company.repository.remote_rest.RemoteRestApi
import com.z_company.route.subscription.SubscriptionPeriodTracker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/** Подписка продлена: срок вырос с [previous] до [current] (ms epoch). */
data class SubscriptionPeriodChange(val previous: Long, val current: Long)

/**
 * Глобальный (Activity-scoped, создаётся в LocoDriverApp вне NavHost)
 * источник двух разных диалогов, которые всплывают на любом экране:
 *
 * - «Бонус начислен!» — выросли бонусные дни реферальной программы
 *   (`awardedDays` из `GET /v1/referrals/me`);
 * - «Подписка продлена» — `subscriptionPeriod` вырос НЕ за счёт бонуса и
 *   пользователь этого ещё не видел (автопродление, оплата на другой
 *   платформе, правка на сервере). Уменьшение срока диалогом не сообщаем:
 *   пользователь может не помнить прежний срок, и «срок изменён» читался бы
 *   двусмысленно.
 *
 * Бонус сервер кладёт в тот же `subscriptionPeriod`, поэтому оба решения
 * принимаются в одном месте: из изменения срока вычитаются бонусные дни,
 * пришедшие с прошлого увиденного срока, и о сроке сообщаем, только если
 * остаётся что-то кроме бонуса.
 *
 * Оплата на экране Подписки гасит диалог о сроке через
 * [SubscriptionPeriodTracker] — там уже показан «Платёж принят!».
 */
class SubscriptionNoticesViewModel : ViewModel(), KoinComponent {
    private val settingsUseCase: SettingsUseCase by inject()
    private val tracker: SubscriptionPeriodTracker by inject()
    private val secureTokenStorage: SecureTokenStorage by inject()
    private val remoteRestApi: RemoteRestApi by inject()
    private val sharedPrefs: SharedPreferencesRepositories by inject()

    private val _bonusAwardedDays = MutableStateFlow<Int?>(null)
    val bonusAwardedDays = _bonusAwardedDays.asStateFlow()

    private val _periodChange = MutableStateFlow<SubscriptionPeriodChange?>(null)
    val periodChange = _periodChange.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                settingsUseCase.getUserSettingFlow()
                    .map { it.subscriptionPeriod }
                    .distinctUntilChanged(),
                tracker.paymentCheckInProgress,
            ) { period, inProgress -> period to inProgress }
                .collect { (period, inProgress) ->
                    // Во время поллинга оплаты решение за экраном Подписки;
                    // когда флаг снимется, combine перепроверит текущий срок.
                    if (!inProgress) check(period)
                }
        }
    }

    private suspend fun check(period: Long) {
        val userId = secureTokenStorage.getUserIdFlow().first()?.takeIf { it.isNotBlank() } ?: return
        val awardedDays = fetchAwardedDays() ?: return // офлайн — проверим при следующем изменении/запуске
        checkReferralBonus(userId, awardedDays)
        checkPeriod(userId, period, awardedDays)
    }

    private suspend fun fetchAwardedDays(): Int? = try {
        val token = secureTokenStorage.getAuthBearerTokenFlow().first()
        remoteRestApi.getReferralStatus("Bearer $token").awardedDays
    } catch (_: Exception) {
        null
    }

    /**
     * Сравнивает свежий awardedDays с последним увиденным ДЛЯ ЭТОГО userId
     * (см. checkNewReferralBonus в PWA). -1 — первый запуск трекинга: только
     * запоминаем базу, без диалога.
     */
    private fun checkReferralBonus(userId: String, awardedDays: Int) {
        val lastSeen = sharedPrefs.getLastSeenReferralAwardedDays(userId)
        if (lastSeen in 0 until awardedDays) {
            _bonusAwardedDays.value = (_bonusAwardedDays.value ?: 0) + awardedDays - lastSeen
        }
        sharedPrefs.setLastSeenReferralAwardedDays(userId, awardedDays)
    }

    /**
     * Не сообщаем: при `period <= 0` (пустые настройки до загрузки — не
     * запоминаем); при первом отслеживании аккаунта на устройстве (только
     * база — иначе вход с оплаченной подпиской выглядел бы «продлением»);
     * при уменьшении срока; если рост целиком объясняется бонусом рефералки.
     */
    private fun checkPeriod(userId: String, period: Long, awardedDays: Int) {
        if (period <= 0L) return
        val lastSeen = sharedPrefs.getLastSeenSubscriptionPeriod(userId)
        val referralDaysInLastSeen = sharedPrefs.getSubscriptionPeriodReferralDays(userId)
        if (period == lastSeen) {
            // Срок подтверждён экраном Подписки — бонус, пришедший вместе с
            // этой оплатой, уже внутри него.
            if (referralDaysInLastSeen < 0) {
                sharedPrefs.setSubscriptionPeriodReferralDays(userId, awardedDays)
            }
            return
        }
        sharedPrefs.setLastSeenSubscriptionPeriod(userId, period)
        sharedPrefs.setSubscriptionPeriodReferralDays(userId, awardedDays)
        if (lastSeen <= 0L) return

        val bonusDays = if (referralDaysInLastSeen >= 0) {
            (awardedDays - referralDaysInLastSeen).coerceAtLeast(0)
        } else {
            0
        }
        if (!isExtendedBeyondBonus(lastSeen, period, bonusDays)) return
        _periodChange.value = SubscriptionPeriodChange(lastSeen, period)
    }

    /**
     * Сервер начисляет бонус как `max(срок, сейчас) + бонус`. Без бонуса —
     * любой рост. С бонусом — только если срок ушёл дальше, чем мог увести
     * один бонус (запас [TOLERANCE_MS] на округление awardedDays вниз).
     */
    private fun isExtendedBeyondBonus(lastSeen: Long, period: Long, bonusDays: Int): Boolean {
        if (period <= lastSeen) return false
        if (bonusDays == 0) return true
        val bonusBase = maxOf(lastSeen, System.currentTimeMillis())
        return period - bonusDays * DAY_MS > bonusBase + TOLERANCE_MS
    }

    fun dismissBonusAwardedDialog() {
        _bonusAwardedDays.value = null
    }

    fun dismissPeriodChangeDialog() {
        _periodChange.value = null
    }

    private companion object {
        const val DAY_MS = 86_400_000L
        const val TOLERANCE_MS = DAY_MS
    }
}
