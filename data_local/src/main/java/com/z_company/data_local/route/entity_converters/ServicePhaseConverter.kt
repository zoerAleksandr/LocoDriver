package com.z_company.data_local.route.entity_converters

import androidx.room.TypeConverter
import com.google.gson.GsonBuilder
import com.z_company.domain.entities.ServicePhase

internal object ServicePhaseConverter {
    private val gson = GsonBuilder().setDateFormat("MMM dd, yyyy HH:mm:ss").create()

    @TypeConverter
    fun fromString(value: String?): ServicePhase? {
        return gson.fromJson(value, ServicePhase::class.java)
    }

    @TypeConverter
    fun toString(entity: ServicePhase?): String? {
        return gson.toJson(entity)
    }
}