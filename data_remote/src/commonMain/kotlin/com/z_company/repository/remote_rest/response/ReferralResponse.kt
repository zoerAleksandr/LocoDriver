package com.z_company.repository.remote_rest.response

import kotlinx.serialization.Serializable

@Serializable
data class ReferralStatusResponse(
    val code: String,
    val canApplyCode: Boolean,
    val appliedCode: String? = null,
    val appliedStatus: String? = null,
    val invitedCount: Int = 0,
    val rewardedCount: Int = 0,
)

@Serializable
data class ApplyReferralCodeRequest(val code: String)

@Serializable
data class ApplyReferralCodeResponse(val status: String, val code: String)
