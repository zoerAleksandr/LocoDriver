package com.z_company.data_local.setting.type_converter

import androidx.room.TypeConverter
import com.google.gson.GsonBuilder
import com.z_company.data_local.setting.entity.MonthOfYear

object MonthOfYearToPrimitiveConverter {
    private val gson = GsonBuilder().setDateFormat("MMM dd, yyyy HH:mm:ss").create()
    @TypeConverter
    fun fromString(value: String): MonthOfYear {
        return gson.fromJson(value, MonthOfYear::class.java)
    }

    @TypeConverter
    fun toString(time: MonthOfYear): String {
        return gson.toJson(time)
    }
}