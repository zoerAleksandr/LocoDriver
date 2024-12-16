package com.z_company.entity_converter

import com.z_company.domain.entities.route.BasicData
import com.z_company.entity.BasicData as BasicDataRemote
import java.util.Date

object BasicDataConverter {
    fun fromData(basicData: BasicData) = BasicDataRemote(
        id = basicData.id,
        isSynchronized = basicData.isSynchronized,
        remoteObjectId = basicData.remoteObjectId ?: "",
        updatedAt = Date(),
        number = basicData.number,
        timeStartWork = basicData.timeStartWork,
        timeEndWork = basicData.timeEndWork,
        restPointOfTurnover = basicData.restPointOfTurnover,
        notes = basicData.notes
    )

    fun toData(entity: BasicDataRemote) = BasicData(
        id = entity.id,
        isSynchronized = entity.isSynchronized,
        remoteObjectId = entity.remoteObjectId,
        updatedAt = entity.updatedAt,
        number = entity.number,
        timeStartWork = entity.timeStartWork,
        timeEndWork = entity.timeEndWork,
        restPointOfTurnover = entity.restPointOfTurnover,
        notes = entity.notes
    )
}