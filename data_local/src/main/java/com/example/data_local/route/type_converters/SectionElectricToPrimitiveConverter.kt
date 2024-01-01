package com.example.data_local.route.type_converters

import androidx.room.TypeConverter
import com.example.data_local.route.entity.SectionElectric
import com.google.gson.Gson

internal object SectionElectricToPrimitiveConverter {
    private val gson = Gson()
    @TypeConverter
    fun fromString(value: String): List<SectionElectric> {
        return gson.fromJson(value, Array<SectionElectric>::class.java).toList()
    }

    @TypeConverter
    fun toString(list: List<SectionElectric>): String {
        return gson.toJson(list)
    }
}