package com.z_company.repository.remote_rest.response

import com.z_company.domain.entities.norma_time.StationNorm
import kotlinx.serialization.Serializable

// updatedAt приходит с сервера как Double (например 1779032766922.0), конвертируем в Long.
@Serializable
data class NormaTimeStationResponse(
    val stationId: String,
    val name: String,
    val appearanceToStartMin: Int? = null,
    val endToBarrierMin: Int? = null,
    val barrierToStartMin: Int? = null,
    val endToWorkEndMin: Int? = null,
    val updatedAt: Double
) {
    fun toDomain(): StationNorm = StationNorm(
        stationId = stationId,
        name = name,
        appearanceToStartMin = appearanceToStartMin,
        endToBarrierMin = endToBarrierMin,
        barrierToStartMin = barrierToStartMin,
        endToWorkEndMin = endToWorkEndMin,
        updatedAt = updatedAt.toLong()
    )

    companion object {
        fun fromDomain(station: StationNorm): NormaTimeStationResponse =
            NormaTimeStationResponse(
                stationId = station.stationId,
                name = station.name,
                appearanceToStartMin = station.appearanceToStartMin,
                endToBarrierMin = station.endToBarrierMin,
                barrierToStartMin = station.barrierToStartMin,
                endToWorkEndMin = station.endToWorkEndMin,
                updatedAt = station.updatedAt.toDouble()
            )
    }
}
