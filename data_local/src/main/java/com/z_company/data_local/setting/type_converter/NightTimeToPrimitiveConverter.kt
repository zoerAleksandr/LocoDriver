package com.z_company.data_local.setting.type_converter

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.z_company.data_local.setting.entity.NightTime

object NightTimeToPrimitiveConverter {
    private val gson = Gson()
    @TypeConverter
    fun fromString(value: String): NightTime {
        return gson.fromJson(value, NightTime::class.java)
    }

    @TypeConverter
    fun toString(time: NightTime): String {
        return gson.toJson(time)
    }
}