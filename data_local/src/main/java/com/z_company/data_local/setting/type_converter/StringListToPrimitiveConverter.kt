package com.z_company.data_local.setting.type_converter

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object StringListToPrimitiveConverter {

//    @TypeConverter
//    fun listToJson(value: List<String>?): String = Gson().toJson(value)
//
//    @TypeConverter
//    fun jsonToList(value: String): List<String> =
//        Gson().fromJson(value, Array<String>::class.java).toList()

    @TypeConverter
    fun stringToList(value: String): List<String>? {
        val type = object : TypeToken<List<String>>() {}.type
        return Gson().fromJson<List<String>>(value, type)
    }

    @TypeConverter
    fun listToString(list: List<String>): String {
        return Gson().toJson(list)
    }

}