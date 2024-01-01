package com.example.data_local.route.type_converters

import androidx.room.TypeConverter
import com.example.data_local.route.entity.SectionDiesel
import com.google.gson.Gson

internal object SectionDieselToPrimitiveConverters {
    private val gson = Gson()

    @TypeConverter
    fun fromString(value: String): List<SectionDiesel> {
        return gson.fromJson(value, Array<SectionDiesel>::class.java).toList()
    }

    @TypeConverter
    fun toString(list: List<SectionDiesel>): String {
        return gson.toJson(list)
    }
}