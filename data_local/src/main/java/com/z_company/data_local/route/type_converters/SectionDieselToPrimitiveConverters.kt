package com.z_company.data_local.route.type_converters

import androidx.room.TypeConverter
import com.z_company.data_local.route.entity.SectionDiesel
import com.google.gson.GsonBuilder

internal object SectionDieselToPrimitiveConverters {
    private val gson = GsonBuilder().setDateFormat("MMM dd, yyyy HH:mm:ss").create()

    @TypeConverter
    fun fromString(value: String): List<SectionDiesel> {
        return gson.fromJson(value, Array<SectionDiesel>::class.java).toList()
    }

    @TypeConverter
    fun toString(list: List<SectionDiesel>): String {
        return gson.toJson(list)
    }
}