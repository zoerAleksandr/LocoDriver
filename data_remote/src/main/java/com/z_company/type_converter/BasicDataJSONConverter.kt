package com.z_company.type_converter

import com.google.gson.GsonBuilder
import com.z_company.entity.BasicData

object BasicDataJSONConverter {
    private val gson = GsonBuilder().setDateFormat("MMM dd, yyyy HH:mm:ss").create()

    fun fromString(value: String): BasicData {
        return gson.fromJson(value, BasicData::class.java)
    }

    fun toString(basicData: BasicData): String {
        return gson.toJson(basicData)
    }
}