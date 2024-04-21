package com.z_company.type_converter

import com.google.gson.Gson
import com.z_company.entity.Station

object StationJSONConverter {
    private val gson = Gson()
    fun fromString(value: String): Station {
        return gson.fromJson(value, Station::class.java)
    }
    fun toString(station: Station): String {
        return gson.toJson(station)
    }
}