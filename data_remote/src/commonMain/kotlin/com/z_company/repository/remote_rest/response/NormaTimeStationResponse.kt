package com.z_company.repository.remote_rest.response

import com.z_company.domain.entities.norma_time.StationNorm
import com.z_company.domain.entities.norma_time.StationTombstone
import com.z_company.domain.entities.setting.ServicePhaseStationKind
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
    /**
     * Маркер нового клиента: сервер перезаписывает геополя (null-поля при
     * `explicitNulls = false` не уходят — без маркера сервер считал бы их «не прислано»).
     */
    val hasGeoFields: Boolean? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val esr: String? = null,
    val osmId: Long? = null,
    /** "station" | "halt". */
    val kind: String? = null,
    val axisKm: Int? = null,
    val axisPk: Int? = null,
    val updatedAt: Double,
    /**
     * Надгробие: станцию удалили. Новый клиент шлёт поле всегда (true/false) — по нему сервер
     * понимает, что это слияние, а не full-replace старого клиента. GET отдаёт поле только
     * при `includeDeleted=true`.
     */
    val isDeleted: Boolean? = null,
) {
    fun toDomain(): StationNorm = StationNorm(
        stationId = stationId,
        name = name,
        appearanceToStartMin = appearanceToStartMin,
        endToBarrierMin = endToBarrierMin,
        barrierToStartMin = barrierToStartMin,
        endToWorkEndMin = endToWorkEndMin,
        latitude = latitude?.takeIf { longitude != null },
        longitude = longitude?.takeIf { latitude != null },
        esr = esr,
        osmId = osmId,
        kind = when (kind) {
            "station" -> ServicePhaseStationKind.STATION
            "halt" -> ServicePhaseStationKind.HALT
            else -> null
        },
        axisKm = axisKm,
        axisPicket = axisPk,
        updatedAt = updatedAt.toLong()
    )

    fun toTombstone(): StationTombstone =
        StationTombstone(stationId = stationId, name = name, deletedAt = updatedAt.toLong())

    companion object {
        fun fromTombstone(t: StationTombstone): NormaTimeStationResponse =
            NormaTimeStationResponse(
                stationId = t.stationId,
                name = t.name,
                updatedAt = t.deletedAt.toDouble(),
                isDeleted = true,
            )

        fun fromDomain(station: StationNorm): NormaTimeStationResponse =
            NormaTimeStationResponse(
                stationId = station.stationId,
                name = station.name,
                appearanceToStartMin = station.appearanceToStartMin,
                endToBarrierMin = station.endToBarrierMin,
                barrierToStartMin = station.barrierToStartMin,
                endToWorkEndMin = station.endToWorkEndMin,
                hasGeoFields = true,
                latitude = station.latitude,
                longitude = station.longitude,
                esr = station.esr,
                osmId = station.osmId,
                kind = when (station.kind) {
                    ServicePhaseStationKind.STATION -> "station"
                    ServicePhaseStationKind.HALT -> "halt"
                    null -> null
                },
                axisKm = station.axisKm,
                axisPk = station.axisPicket,
                updatedAt = station.updatedAt.toDouble(),
                isDeleted = false,
            )
    }
}
