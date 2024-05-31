package com.z_company.data_local.setting.entity_converter

import com.z_company.domain.entities.NightTime
import com.z_company.data_local.setting.entity.NightTime as NightTimeEntity

internal object NightTimeConverter {
    fun fromData(nightTime: NightTime) = NightTimeEntity(
        startNightHour = nightTime.startNightHour,
        startNightMinute = nightTime.startNightMinute,
        endNightHour = nightTime.endNightHour,
        endNightMinute = nightTime.endNightMinute
    )

    fun toData(entity: NightTimeEntity) = NightTime(
        startNightHour = entity.startNightHour,
        startNightMinute = entity.startNightMinute,
        endNightHour = entity.endNightHour,
        endNightMinute = entity.endNightMinute
    )
}