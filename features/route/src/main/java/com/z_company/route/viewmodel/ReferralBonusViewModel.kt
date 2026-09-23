package com.z_company.route.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.domain.repositories.SharedPreferencesRepositories
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.repository.SecureTokenStorage
import com.z_company.repository.remote_rest.RemoteRestApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Глобальный, на весь app-session (Activity-scoped, создаётся в LocoDriverApp
 * вне NavHost), источник диалога «Бонус начислен!». Раньше проверка жила
 * внутри PurchasesViewModel и диалог показывался только на экране Подписки —
 * теперь событие должно всплывать на ЛЮБОМ экране, как только данные
 * обновились (subscriptionPeriod меняется, когда бонус реально начислен —
 * и приглашающему, и приглашённому за один и тот же платёж друга).
 */
class ReferralBonusViewModel : ViewModel(), KoinComponent {
    private val secureTokenStorage: SecureTokenStorage by inject()
    private val remoteRestApi: RemoteRestApi by inject()
    private val sharedPrefs: SharedPreferencesRepositories by inject()
    private val settingsUseCase: SettingsUseCase by inject()

    private val _bonusAwardedDays = MutableStateFlow<Int?>(null)
    val bonusAwardedDays = _bonusAwardedDays.asStateFlow()

    init {
        viewModelScope.launch { checkBonus() }
        viewModelScope.launch {
            settingsUseCase.getUserSettingFlow()
                .map { it.subscriptionPeriod }
                .distinctUntilChanged()
                .drop(1)
                .collect { checkBonus() }
        }
    }

    /**
     * Сравнивает свежий awardedDays с последним увиденным на устройстве ДЛЯ
     * ЭТОГО userId (см. checkNewReferralBonus в PWA — тот же принцип). -1 в
     * хранилище — первый запуск трекинга: просто запоминаем базу, без
     * диалога.
     */
    private suspend fun checkBonus() {
        try {
            val userId = secureTokenStorage.getUserIdFlow().first() ?: return
            val token = secureTokenStorage.getAuthBearerTokenFlow().first()
            val status = remoteRestApi.getReferralStatus("Bearer $token")
            val lastSeen = sharedPrefs.getLastSeenReferralAwardedDays(userId)
            if (lastSeen in 0 until status.awardedDays) {
                _bonusAwardedDays.value = status.awardedDays - lastSeen
            }
            sharedPrefs.setLastSeenReferralAwardedDays(userId, status.awardedDays)
        } catch (_: Exception) {
            // офлайн/не авторизован — просто проверим при следующем обновлении данных
        }
    }

    fun dismissBonusAwardedDialog() {
        _bonusAwardedDays.value = null
    }
}
