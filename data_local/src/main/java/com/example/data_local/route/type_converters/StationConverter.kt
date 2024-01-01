package com.example.data_local.route.type_converters

import androidx.room.TypeConverter
import com.example.data_local.route.entity.Station
import com.google.gson.Gson

internal object StationConverter {
    private val gson = Gson()

    @TypeConverter
    fun fromString(value: String): List<Station> {
        return gson.fromJson(value, Array<Station>::class.java).toList()
    }

    @TypeConverter
    fun toString(list: List<Station>): String {
        return gson.toJson(list)
    }
}