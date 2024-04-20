package com.z_company.type_converter

import com.google.gson.Gson
import com.z_company.entity.SectionDiesel

object SectionDieselJSONConverter {
    private val gson = Gson()
    fun fromString(value: String): SectionDiesel {
        return gson.fromJson(value, SectionDiesel::class.java)
    }
    fun toString(sectionDiesel: SectionDiesel): String {
        return gson.toJson(sectionDiesel)
    }
}