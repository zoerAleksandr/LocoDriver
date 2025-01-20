package com.z_company.type_converter

import com.google.gson.GsonBuilder
import com.z_company.entity.Passenger

object PassengerJSONConverter {
    private val gson = GsonBuilder().setDateFormat("MMM dd, yyyy HH:mm:ss").create()
    fun fromString(value: String): Passenger {
        return gson.fromJson(value, Passenger::class.java)
    }
    fun toString(passenger: Passenger): String {
        return gson.toJson(passenger)
    }
}