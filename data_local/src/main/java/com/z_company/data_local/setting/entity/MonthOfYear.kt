package com.z_company.data_local.setting.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.z_company.data_local.route.type_converters.DaysTypeConverters
import com.z_company.domain.entities.Day

@TypeConverters(
    DaysTypeConverters::class
)
@Entity
data class MonthOfYear(
    @PrimaryKey
    val id: String,
    val year: Int,
    val month: Int,
    val days: List<Day>
)
