package com.z_company.data_local.route.type_converters

import androidx.room.TypeConverter
import com.z_company.data_local.route.entity.Photo
import com.google.gson.GsonBuilder

internal object PhotosConverter {
    private val gson = GsonBuilder().setDateFormat("MMM dd, yyyy HH:mm:ss").create()

    @TypeConverter
    fun fromString(value: String): List<Photo> {
        return gson.fromJson(value, Array<Photo>::class.java).toList()
    }

    @TypeConverter
    fun toString(list: List<Photo>): String {
        return gson.toJson(list)
    }
}