package com.z_company.data_local.setting.type_converter

import androidx.room.TypeConverter
import com.google.gson.GsonBuilder
import com.z_company.data_local.setting.entity.ServicePhase

object ServicePhaseToPrimitiveConverter {
    private val gson = GsonBuilder().setDateFormat("MMM dd, yyyy HH:mm:ss").create()
    @TypeConverter
    fun fromString(value: String): List<ServicePhase> {
        return gson.fromJson(value, Array<ServicePhase>::class.java).toList()
    }

    @TypeConverter
    fun toString(list: List<ServicePhase>): String {
        return gson.toJson(list)
    }
}