package com.z_company.data_local.route.type_converters

import androidx.room.TypeConverter
import com.z_company.data_local.route.entity.SectionElectric
import com.google.gson.GsonBuilder

internal object SectionElectricToPrimitiveConverter {
    private val gson = GsonBuilder().setDateFormat("MMM dd, yyyy HH:mm:ss").create()
    @TypeConverter
    fun fromString(value: String): List<SectionElectric> {
        return gson.fromJson(value, Array<SectionElectric>::class.java).toList()
    }

    @TypeConverter
    fun toString(list: List<SectionElectric>): String {
        return gson.toJson(list)
    }
}