package com.z_company.type_converter

import com.google.gson.GsonBuilder
import com.z_company.entity.Locomotive

object LocomotiveJSONConverter {
    private val gson = GsonBuilder().setDateFormat("MMM dd, yyyy HH:mm:ss").create()

    fun fromString(value: String): Locomotive {
        return gson.fromJson(value, Locomotive::class.java)
    }

    fun toString(locomotive: Locomotive): String {
        return gson.toJson(locomotive)
    }
}