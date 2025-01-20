package com.z_company.type_converter

import com.google.gson.GsonBuilder
import com.z_company.entity.SectionDiesel

object SectionDieselJSONConverter {
    private val gson = GsonBuilder().setDateFormat("MMM dd, yyyy HH:mm:ss").create()
    fun fromString(value: String): SectionDiesel {
        return gson.fromJson(value, SectionDiesel::class.java)
    }
    fun toString(sectionDiesel: SectionDiesel): String {
        return gson.toJson(sectionDiesel)
    }
}