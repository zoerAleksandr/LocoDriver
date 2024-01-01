package com.example.data_local.route.type_converters

import androidx.room.TypeConverter
import com.example.data_local.route.entity.Station
import com.google.gson.Gson

internal object PhotosConverter {
    private val gson = Gson()

    @TypeConverter
    fun fromString(value: String): List<String> {
        return gson.fromJson(value, Array<String>::class.java).toList()
    }

    @TypeConverter
    fun toString(list: List<String>): String {
        return gson.toJson(list)
    }
}