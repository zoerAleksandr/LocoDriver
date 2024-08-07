package com.z_company.data_local.route.entity_converters

import com.z_company.domain.entities.route.BasicData
import java.util.Date
import com.z_company.data_local.route.entity.BasicData as BasicDataEntity

internal object BasicDataConverter {
    fun fromData(basicData: BasicData) = BasicDataEntity(
        id = basicData.id,
        isSynchronized = basicData.isSynchronized,
        isDeleted = basicData.isDeleted,
        remoteObjectId = basicData.remoteObjectId,
        updatedAt = Date(),
        number = basicData.number,
        timeStartWork = basicData.timeStartWork,
        timeEndWork = basicData.timeEndWork,
        restPointOfTurnover = basicData.restPointOfTurnover,
        notes = basicData.notes
    )

    fun toData(entity: BasicDataEntity) = BasicData(
        id = entity.id,
        isSynchronized = entity.isSynchronized,
        isDeleted = entity.isDeleted,
        remoteObjectId = entity.remoteObjectId,
        updatedAt = entity.updatedAt,
        number = entity.number,
        timeStartWork = entity.timeStartWork,
        timeEndWork = entity.timeEndWork,
        restPointOfTurnover = entity.restPointOfTurnover,
        notes = entity.notes
    )
}