package com.z_company.data_local.route.type_converters

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.z_company.domain.entities.Day

internal object DaysTypeConverters {
    private val gson = Gson()
    @TypeConverter
    fun fromString(value: String): List<Day> {
        return gson.fromJson(value, Array<Day>::class.java).toList()
    }

    @TypeConverter
    fun toString(list: List<Day>): String {
        return gson.toJson(list)
    }
}