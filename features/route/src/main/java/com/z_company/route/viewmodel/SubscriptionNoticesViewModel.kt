package com.z_company.route.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.domain.repositories.SharedPreferencesRepositories
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.repository.SecureTokenStorage
import com.z_company.repository.remote_rest.RemoteRestApi
import com.z_company.route.subscription.SubscriptionNoticesPolicy
import com.z_company.route.subscription.SubscriptionPeriodTracker
import com.z_company.route.subscription.asSubscriptionSeenStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
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
 * [SubscriptionPeriodTracker] — там уже показан «Платёж принят!». Вход в
 * аккаунт тоже идёт под трекером: срок нового аккаунта приходит раньше его
 * userId.
 *
 * «Увиденное» хранится по userId, поэтому проверка перезапускается и при
 * смене токена/userId (выход, вход под другим аккаунтом), а не только при
 * изменении срока.
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

    private val policy = SubscriptionNoticesPolicy(sharedPrefs.asSubscriptionSeenStore())

    init {
        viewModelScope.launch {
            combine(
                secureTokenStorage.getAuthBearerTokenFlow()
                    .map { it.orEmpty() }
                    .distinctUntilChanged(),
                secureTokenStorage.getUserIdFlow()
                    .map { it.orEmpty() }
                    .distinctUntilChanged(),
                settingsUseCase.getUserSettingFlow()
                    .map { it.subscriptionPeriod }
                    .distinctUntilChanged(),
                tracker.periodUpdatesInProgress
                    .map { it > 0 }
                    .distinctUntilChanged(),
            ) { token, userId, period, inProgress -> CheckInput(token, userId, period, inProgress) }
                // Смена аккаунта посреди сетевого запроса — старая проверка
                // уже не про текущего пользователя.
                .collectLatest { input ->
                    // Во время поллинга оплаты или входа решение за тем экраном;
                    // когда счётчик обнулится, combine перепроверит текущий срок.
                    if (!input.inProgress) check(input)
                }
        }
    }

    private data class CheckInput(
        val token: String,
        val userId: String,
        val period: Long,
        val inProgress: Boolean,
    )

    private suspend fun check(input: CheckInput) {
        if (input.token.isBlank()) return // не вошёл — «увиденное» не трогаем
        if (input.userId.isBlank()) {
            // Ключ аккаунта неизвестен (профиль ещё не загружался или не
            // загрузился при входе). Узнаём сами; сохранение id перезапустит
            // проверку уже под верным ключом.
            resolveUserId(input.token)?.let { secureTokenStorage.saveUserId(it) }
            return
        }
        val awardedDays = fetchAwardedDays(input.token) ?: return // офлайн — проверим при следующем изменении/запуске
        val notices = policy.check(input.userId, input.period, awardedDays)
        notices.bonusDays?.let { days -> _bonusAwardedDays.value = (_bonusAwardedDays.value ?: 0) + days }
        notices.periodChange?.let { _periodChange.value = it }
    }

    private suspend fun resolveUserId(token: String): String? = try {
        remoteRestApi.getUserProfile("Bearer $token").user.id.takeIf { it.isNotBlank() }
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        null
    }

    private suspend fun fetchAwardedDays(token: String): Int? = try {
        remoteRestApi.getReferralStatus("Bearer $token").awardedDays
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        null
    }

    fun dismissBonusAwardedDialog() {
        _bonusAwardedDays.value = null
    }

    fun dismissPeriodChangeDialog() {
        _periodChange.value = null
    }
}
