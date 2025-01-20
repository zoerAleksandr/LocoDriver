package com.z_company.type_converter

import com.google.gson.GsonBuilder
import com.z_company.entity.Train

object TrainJSONConverter {
    private val gson = GsonBuilder().setDateFormat("MMM dd, yyyy HH:mm:ss").create()
    fun fromString(value: String): Train {
        return gson.fromJson(value, Train::class.java)
    }
    fun toString(train: Train): String {
        return gson.toJson(train)
    }
}