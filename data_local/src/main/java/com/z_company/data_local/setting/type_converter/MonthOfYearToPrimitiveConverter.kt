package com.z_company.data_local.setting.type_converter

import androidx.room.TypeConverter
import com.google.gson.GsonBuilder
import com.z_company.data_local.setting.entity.MonthOfYear
import com.google.gson.JsonDeserializer
import com.google.gson.JsonSerializer
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.z_company.domain.entities.ReleaseType
import java.lang.reflect.Type

object MonthOfYearToPrimitiveConverter {
    private val gson = GsonBuilder()
        .setDateFormat("MMM dd, yyyy HH:mm:ss")
        .registerTypeAdapter(ReleaseType::class.java, ReleaseTypeAdapter())
        .create()
//    private val gson = GsonBuilder().setDateFormat("MMM dd, yyyy HH:mm:ss").create()

    @TypeConverter
    fun fromString(value: String): MonthOfYear {
        return gson.fromJson(value, MonthOfYear::class.java)
    }

    @TypeConverter
    fun toString(time: MonthOfYear): String {
        return gson.toJson(time)
    }
}

class ReleaseTypeAdapter : JsonSerializer<ReleaseType>, JsonDeserializer<ReleaseType> {

    override fun serialize(src: ReleaseType?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
        val json = JsonObject()
        json.addProperty("type", src?.javaClass?.simpleName)
        // Нет нужды сериализовать text и color, так как они фиксированы в object
        return json
    }

    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): ReleaseType? {
        if (json == null || json.isJsonNull) return null
        val obj = json.asJsonObject ?: return null
        val typeName = obj.get("type")?.asString ?: return null  // Изменено: return null вместо throw
        return when (typeName) {
            "Vacation" -> ReleaseType.Vacation
            "SickLeave" -> ReleaseType.SickLeave
            "Courses" -> ReleaseType.Courses
            "Donor" -> ReleaseType.Donor
            "ChildCare" -> ReleaseType.ChildCare
            "Other" -> ReleaseType.Other
            else -> null  // Изменено: return null вместо throw для неизвестного типа
        }
    }
}