package com.z_company.data_local.setting.type_converter

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object LongListToPrimitiveConverter {
    @TypeConverter
    fun stringToList(value: String): List<Long>? {
        val type = object : TypeToken<List<Long>>() {}.type
        return Gson().fromJson<List<Long>>(value.replace("_", ""), type)
    }

    @TypeConverter
    fun listToString(list: List<Long>): String {
        return Gson().toJson(list)
    }

}