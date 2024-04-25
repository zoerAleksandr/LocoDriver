package com.z_company.type_converter

import com.google.gson.Gson
import com.z_company.entity.SectionElectric

object SectionElectricJSONConverter {
    private val gson = Gson()
    fun fromString(value: String): SectionElectric {
        return gson.fromJson(value, SectionElectric::class.java)
    }
    fun toString(sectionElectric: SectionElectric): String {
        return gson.toJson(sectionElectric)
    }
}