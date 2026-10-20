package com.z_company.repository.remote_rest.response

import com.z_company.domain.entities.norma_time.LocomotiveSeries
import com.z_company.domain.entities.norma_time.LocomotiveSeriesTombstone
import com.z_company.domain.entities.norma_time.SectionNumberingType
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
    val sectionNumberingType: SectionNumberingType = SectionNumberingType.NUMERIC,
    /**
     * Маркер нового клиента: сервер перезаписывает паспортные поля ниже, в том числе очищает null
     * (при `explicitNulls = false` null-поля не уходят — без маркера сервер считал бы их «не прислано»).
     * Старые клиенты полей не знают и маркера не шлют — сервер их значения не трогает.
     */
    val hasBrakeFields: Boolean? = null,
    val brakeVolumeL: Int? = null,
    val axleCount: Int? = null,
    val axleLoadTf: Double? = null,
    val weightT: Double? = null,
    val sectionCount: Int? = null,
    val updatedAt: Double,
    /** Надгробие; новый клиент шлёт поле всегда (true/false) — см. [NormaTimeStationResponse]. */
    val isDeleted: Boolean? = null,
) {
    fun toDomain(): LocomotiveSeries = LocomotiveSeries(
        seriesId = seriesId,
        name = name,
        type = LocoType.entries.find { it.name == type } ?: LocoType.ELECTRIC,
        acceptanceDurationMin = acceptanceDurationMin,
        deliveryDurationMin = deliveryDurationMin,
        acceptanceHandToHandMin = acceptanceHandToHandMin,
        deliveryHandToHandMin = deliveryHandToHandMin,
        sectionNumberingType = sectionNumberingType,
        brakeVolumeL = brakeVolumeL,
        axleCount = axleCount,
        axleLoadTf = axleLoadTf,
        weightT = weightT,
        sectionCount = sectionCount,
        updatedAt = updatedAt.toLong()
    )

    fun toTombstone(): LocomotiveSeriesTombstone = LocomotiveSeriesTombstone(
        seriesId = seriesId,
        name = name,
        type = LocoType.entries.find { it.name == type } ?: LocoType.ELECTRIC,
        deletedAt = updatedAt.toLong(),
    )

    companion object {
        fun fromTombstone(t: LocomotiveSeriesTombstone): NormaTimeLocomotiveResponse =
            NormaTimeLocomotiveResponse(
                seriesId = t.seriesId,
                name = t.name,
                type = t.type.name,
                updatedAt = t.deletedAt.toDouble(),
                isDeleted = true,
            )

        fun fromDomain(series: LocomotiveSeries): NormaTimeLocomotiveResponse =
            NormaTimeLocomotiveResponse(
                seriesId = series.seriesId,
                name = series.name,
                type = series.type.name,
                acceptanceDurationMin = series.acceptanceDurationMin,
                deliveryDurationMin = series.deliveryDurationMin,
                acceptanceHandToHandMin = series.acceptanceHandToHandMin,
                deliveryHandToHandMin = series.deliveryHandToHandMin,
                sectionNumberingType = series.sectionNumberingType,
                hasBrakeFields = true,
                brakeVolumeL = series.brakeVolumeL,
                axleCount = series.axleCount,
                axleLoadTf = series.axleLoadTf,
                weightT = series.weightT,
                sectionCount = series.sectionCount,
                updatedAt = series.updatedAt.toDouble(),
                isDeleted = false,
            )
    }
}
