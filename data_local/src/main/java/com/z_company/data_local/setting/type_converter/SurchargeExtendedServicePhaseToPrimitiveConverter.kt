package com.z_company.data_local.setting.type_converter

import androidx.room.TypeConverter
import com.google.gson.GsonBuilder
import com.z_company.data_local.setting.entity.SurchargeExtendedServicePhase

internal object SurchargeExtendedServicePhaseToPrimitiveConverter {
    private val gson = GsonBuilder().setDateFormat("MMM dd, yyyy HH:mm:ss").create()
    @TypeConverter
    fun fromString(value: String): List<SurchargeExtendedServicePhase> {
        return gson.fromJson(value, Array<SurchargeExtendedServicePhase>::class.java).toList()
    }

    @TypeConverter
    fun toString(list: List<SurchargeExtendedServicePhase>): String {
        return gson.toJson(list)
    }
}
