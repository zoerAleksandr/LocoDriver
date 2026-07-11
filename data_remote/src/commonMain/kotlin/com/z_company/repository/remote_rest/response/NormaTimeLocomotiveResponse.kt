package com.z_company.repository.remote_rest.response

import com.z_company.domain.entities.norma_time.LocomotiveSeries
import com.z_company.domain.entities.route.LocoType
import kotlinx.serialization.Serializable

// updatedAt приходит с сервера как Double (например 1779032766922.0), конвертируем в Long.
@Serializable
data class NormaTimeLocomotiveResponse(
    val seriesId: String,
    val name: String,
    val type: String,
    val acceptanceDurationMin: Int? = null,
    val deliveryDurationMin: Int? = null,
    val acceptanceHandToHandMin: Int? = null,
    val deliveryHandToHandMin: Int? = null,
    val updatedAt: Double
) {
    fun toDomain(): LocomotiveSeries = LocomotiveSeries(
        seriesId = seriesId,
        name = name,
        type = LocoType.entries.find { it.name == type } ?: LocoType.ELECTRIC,
        acceptanceDurationMin = acceptanceDurationMin,
        deliveryDurationMin = deliveryDurationMin,
        acceptanceHandToHandMin = acceptanceHandToHandMin,
        deliveryHandToHandMin = deliveryHandToHandMin,
        updatedAt = updatedAt.toLong()
    )

    companion object {
        fun fromDomain(series: LocomotiveSeries): NormaTimeLocomotiveResponse =
            NormaTimeLocomotiveResponse(
                seriesId = series.seriesId,
                name = series.name,
                type = series.type.name,
                acceptanceDurationMin = series.acceptanceDurationMin,
                deliveryDurationMin = series.deliveryDurationMin,
                acceptanceHandToHandMin = series.acceptanceHandToHandMin,
                deliveryHandToHandMin = series.deliveryHandToHandMin,
                updatedAt = series.updatedAt.toDouble()
            )
    }
}
