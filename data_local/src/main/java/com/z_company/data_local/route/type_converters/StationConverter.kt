package com.z_company.data_local.route.type_converters

import androidx.room.TypeConverter
import com.z_company.data_local.route.entity.Station
import com.google.gson.GsonBuilder

internal object StationConverter {
    private val gson = GsonBuilder().setDateFormat("MMM dd, yyyy HH:mm:ss").create()

    @TypeConverter
    fun fromString(value: String): List<Station> {
        return gson.fromJson(value, Array<Station>::class.java).toList()
    }

    @TypeConverter
    fun toString(list: List<Station>): String {
        return gson.toJson(list)
    }
}