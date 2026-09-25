package com.z_company.route.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.route.subscription.SubscriptionPeriodTracker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/** Срок подписки сменился: был [previous], стал [current] (ms epoch). */
data class SubscriptionPeriodChange(val previous: Long, val current: Long) {
    val isExtended: Boolean get() = current > previous
}

/**
 * Глобальный (Activity-scoped, создаётся в LocoDriverApp вне NavHost)
 * источник диалога «Подписка продлена / Срок подписки изменён» — всплывает
 * на любом экране, когда `subscriptionPeriod` поменялся, а пользователь
 * этого ещё не видел: рекуррентное автосписание, оплата на другой платформе,
 * изменение с сервера. Оплата на экране Подписки гасит диалог через
 * [SubscriptionPeriodTracker] — там уже показан «Платёж принят!».
 */
class SubscriptionPeriodViewModel : ViewModel(), KoinComponent {
    private val settingsUseCase: SettingsUseCase by inject()
    private val tracker: SubscriptionPeriodTracker by inject()

    private val _change = MutableStateFlow<SubscriptionPeriodChange?>(null)
    val change = _change.asStateFlow()

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
                    if (inProgress) return@collect
                    val previous = tracker.consumeChange(period) ?: return@collect
                    _change.value = SubscriptionPeriodChange(previous, period)
                }
        }
    }

    fun dismiss() {
        _change.value = null
    }
}
