package com.z_company.data_local.route.mapping

import com.zcompany.datalocal.route.db.Locomotive as LocomotiveRow
import com.z_company.domain.entities.route.Locomotive
import com.z_company.domain.entities.route.LocoType
import com.z_company.domain.entities.route.SectionDiesel
import com.z_company.domain.entities.route.SectionElectric
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true }

internal object LocomotiveMapper {

    fun encodeElectricSections(list: List<SectionElectric>): String =
        json.encodeToString(list)

    fun encodeDieselSections(list: List<SectionDiesel>): String =
        json.encodeToString(list)

    private fun decodeElectricSections(value: String): List<SectionElectric> =
        runCatching { json.decodeFromString<List<SectionElectric>>(value) }.getOrElse { e ->
            println("LocomotiveMapper: Failed to decode electric sections: ${e.message}")
            emptyList()
        }

    private fun decodeDieselSections(value: String): List<SectionDiesel> =
        runCatching { json.decodeFromString<List<SectionDiesel>>(value) }.getOrElse { e ->
            println("LocomotiveMapper: Failed to decode diesel sections: ${e.message}")
            emptyList()
        }

    fun toData(row: LocomotiveRow): Locomotive = Locomotive(
        locoId = row.locoId,
        basicId = row.basicId,
        series = row.series,
        number = row.number,
        type = LocoType.entries.getOrElse(row.type.toInt()) { LocoType.ELECTRIC },
        electricSectionList = decodeElectricSections(row.electricSectionList).toMutableList(),
        dieselSectionList = decodeDieselSections(row.dieselSectionList).toMutableList(),
        timeStartOfAcceptance = row.timeStartOfAcceptance,
        timeEndOfAcceptance = row.timeEndOfAcceptance,
        timeStartOfDelivery = row.timeStartOfDelivery,
        timeEndOfDelivery = row.timeEndOfDelivery,
        normaElectricCurrent1 = row.normaElectricCurrent1?.toInt(),
        normaElectricCurrent2 = row.normaElectricCurrent2?.toInt(),
        normaDiesel = row.normaDiesel,
        heatingCounterAccepted = row.heatingCounterAccepted?.toDoubleOrNull(),
        heatingCounterDelivery = row.heatingCounterDelivery?.toDoubleOrNull(),
        auxiliaryCounterAccepted = row.auxiliaryCounterAccepted?.toDoubleOrNull(),
        auxiliaryCounterDelivery = row.auxiliaryCounterDelivery?.toDoubleOrNull()
    )
}
