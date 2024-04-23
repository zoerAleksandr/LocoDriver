package com.z_company.type_converter

import com.google.gson.Gson
import com.z_company.entity.Photo

object PhotoJSONConverter {
    private val gson = Gson()
    fun fromString(value: String): Photo {
        return gson.fromJson(value, Photo::class.java)
    }

    fun toString(photo: Photo): String {
        return gson.toJson(photo)
    }
}