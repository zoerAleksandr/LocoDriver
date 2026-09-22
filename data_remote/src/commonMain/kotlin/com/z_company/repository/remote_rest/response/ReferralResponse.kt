package com.z_company.repository.remote_rest.response

import kotlinx.serialization.Serializable

@Serializable
data class ReferralStatusResponse(
    val code: String,
    val canApplyCode: Boolean,
    val canInvite: Boolean = false,
    val appliedCode: String? = null,
    val appliedStatus: String? = null,
    val invitedCount: Int = 0,
    val rewardedCount: Int = 0,
    // Суммарные подаренные дни этому пользователю за всё время — исторический
    // счётчик, не уменьшается (для метрики «Начислено» на экране рефералки).
    val awardedDays: Int = 0,
    // Сколько из ОСТАВШИХСЯ дней подписки бонусные — убывает и обнуляется,
    // если подписка когда-либо истекла (для «Из них N дн.» на экране подписки).
    val remainingBonusDays: Int = 0,
)

@Serializable
data class ApplyReferralCodeRequest(val code: String)

@Serializable
data class ApplyReferralCodeResponse(val status: String, val code: String)
