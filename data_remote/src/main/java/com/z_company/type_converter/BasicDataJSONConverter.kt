package com.z_company.type_converter

import com.google.gson.Gson
import com.z_company.entity.BasicData

object BasicDataJSONConverter {
    private val gson = Gson()

    fun fromString(value: String): BasicData {
        return gson.fromJson(value, BasicData::class.java)
    }

    fun toString(basicData: BasicData): String {
        return gson.toJson(basicData)
    }
}