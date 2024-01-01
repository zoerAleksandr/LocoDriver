package com.example.data_local.route.entity_converters

import com.example.domain.entities.route.BasicData
import com.example.data_local.route.entity.BasicData as BasicDataEntity

internal object BasicDataConverter {
    fun fromData(basicData: BasicData) = BasicDataEntity(
        id = basicData.id,
        number = basicData.number,
        timeStartWork = basicData.timeStartWork,
        timeEndWork = basicData.timeEndWork
    )

    fun toData(entity: BasicDataEntity) = BasicData().apply {
        entity.id
        entity.number
        entity.timeStartWork
        entity.timeEndWork
    }
}