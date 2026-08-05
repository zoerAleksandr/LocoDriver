package com.z_company.repository.remote_rest.response

import com.z_company.domain.entities.partner.Partner
import kotlinx.serialization.Serializable

/**
 * DTO записи справочника напарников (`/v1/partners/`).
 * По образцу [NormaTimeStationResponse]: updatedAt приходит с сервера как Double
 * (например 1779032766922.0), конвертируем в Long.
 */
@Serializable
data class PartnerResponse(
    val partnerId: String,
    val fullName: String,
    val tabNumber: String? = null,
    val notes: String? = null,
    val updatedAt: Double
) {
    fun toDomain(): Partner = Partner(
        partnerId = partnerId,
        fullName = fullName,
        tabNumber = tabNumber,
        notes = notes,
        updatedAt = updatedAt.toLong()
    )

    companion object {
        fun fromDomain(partner: Partner): PartnerResponse =
            PartnerResponse(
                partnerId = partner.partnerId,
                fullName = partner.fullName,
                tabNumber = partner.tabNumber,
                notes = partner.notes,
                updatedAt = partner.updatedAt.toDouble()
            )
    }
}
