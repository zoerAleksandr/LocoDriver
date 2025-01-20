package com.z_company.type_converter

import com.google.gson.GsonBuilder
import com.z_company.entity.Photo

object PhotoJSONConverter {
    private val gson = GsonBuilder().setDateFormat("MMM dd, yyyy HH:mm:ss").create()
    fun fromString(value: String): Photo {
        return gson.fromJson(value, Photo::class.java)
    }

    fun toString(photo: Photo): String {
        return gson.toJson(photo)
    }
}