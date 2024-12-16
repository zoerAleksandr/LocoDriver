package com.z_company.data_local.route.type_converters

import androidx.room.TypeConverter
import com.google.gson.GsonBuilder
import java.util.Date

object DateTypeConverter {
    private val gson = GsonBuilder().setDateFormat("MMM dd, yyyy HH:mm:ss").create()
    @TypeConverter
    fun fromString(value: String): Date {
        return gson.fromJson(value, Date::class.java)
    }

    @TypeConverter
    fun toString(date: Date): String {
        return gson.toJson(date)
    }
}