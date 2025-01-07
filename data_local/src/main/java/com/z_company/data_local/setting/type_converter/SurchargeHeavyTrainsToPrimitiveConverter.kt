package com.z_company.data_local.setting.type_converter

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.z_company.data_local.setting.entity.SurchargeHeavyTrains

internal object SurchargeHeavyTrainsToPrimitiveConverter {
    private val gson = Gson()
    @TypeConverter
    fun fromString(value: String): List<SurchargeHeavyTrains> {
        return gson.fromJson(value, Array<SurchargeHeavyTrains>::class.java).toList()
    }

    @TypeConverter
    fun toString(list: List<SurchargeHeavyTrains>): String {
        return gson.toJson(list)
    }
}