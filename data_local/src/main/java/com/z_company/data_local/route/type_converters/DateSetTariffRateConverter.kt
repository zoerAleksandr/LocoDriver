package com.z_company.data_local.route.type_converters

import androidx.room.TypeConverter
import com.google.gson.GsonBuilder
import com.z_company.domain.entities.DateSetTariffRate

object DateSetTariffRateConverter {
    private val gson = GsonBuilder().setDateFormat("MMM dd, yyyy HH:mm:ss").create()

    @TypeConverter
    fun fromString(value: String): DateSetTariffRate {
        return gson.fromJson(value, DateSetTariffRate::class.java)
    }

    @TypeConverter
    fun toString(obj: DateSetTariffRate): String {
        return gson.toJson(obj)
    }

}