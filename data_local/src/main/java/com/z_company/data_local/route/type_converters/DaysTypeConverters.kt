package com.z_company.data_local.route.type_converters

import androidx.room.TypeConverter
import com.google.gson.GsonBuilder
import com.z_company.data_local.setting.type_converter.ReleaseTypeAdapter
import com.z_company.domain.entities.Day
import com.z_company.domain.entities.ReleaseType

internal object DaysTypeConverters {
    private val gson = GsonBuilder()
        .setDateFormat("MMM dd, yyyy HH:mm:ss")
        .registerTypeAdapter(ReleaseType::class.java, ReleaseTypeAdapter())
        .create()

//    private val gson = GsonBuilder().setDateFormat("MMM dd, yyyy HH:mm:ss").create()
    @TypeConverter
    fun fromString(value: String): List<Day> {
        return gson.fromJson(value, Array<Day>::class.java).toList()
    }

    @TypeConverter
    fun toString(list: List<Day>): String {
        return gson.toJson(list)
    }
}