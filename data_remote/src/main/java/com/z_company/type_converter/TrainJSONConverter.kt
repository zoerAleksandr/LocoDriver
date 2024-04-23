package com.z_company.type_converter

import com.google.gson.Gson
import com.z_company.entity.Train

object TrainJSONConverter {
    private val gson = Gson()
    fun fromString(value: String): Train {
        return gson.fromJson(value, Train::class.java)
    }
    fun toString(train: Train): String {
        return gson.toJson(train)
    }
}