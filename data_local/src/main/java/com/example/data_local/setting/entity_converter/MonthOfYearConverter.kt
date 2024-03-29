package com.example.data_local.setting.entity_converter

import com.example.domain.entities.MonthOfYear
import com.example.data_local.setting.entity.MonthOfYear as MonthOfYearEntity

object MonthOfYearConverter {
    private fun fromData(monthOfYear: MonthOfYear) = MonthOfYearEntity(
        id = monthOfYear.id,
        year = monthOfYear.year,
        month = monthOfYear.month,
        normaHours = monthOfYear.normaHours
    )

    fun toData(entity: MonthOfYearEntity) = MonthOfYear(
        id = entity.id,
        year = entity.year,
        month = entity.month,
        normaHours = entity.normaHours
    )

    fun fromDataList(list: List<MonthOfYear>): MutableList<MonthOfYearEntity> {
        return list.map {
            fromData(it)
        }.toMutableList()
    }

    fun toDataList(listEntity: List<MonthOfYearEntity>): MutableList<MonthOfYear> {
        return listEntity.map {
            toData(it)
        }.toMutableList()
    }
}