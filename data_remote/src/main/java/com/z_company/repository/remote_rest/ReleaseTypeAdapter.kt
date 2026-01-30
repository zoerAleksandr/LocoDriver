package com.z_company.repository.remote_rest

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import com.z_company.domain.entities.ReleaseType

class ReleaseTypeAdapter : TypeAdapter<ReleaseType>() {
    override fun write(out: JsonWriter, value: ReleaseType?) {
        if (value == null) {
            out.nullValue()
            return
        }
        out.beginObject()
        out.name("text")
        out.value(value.text)
        out.endObject()
    }

    override fun read(`in`: JsonReader): ReleaseType? {
        if (`in`.peek() == com.google.gson.stream.JsonToken.NULL) {
            `in`.nextNull()
            return null
        }
        `in`.beginObject()
        var text: String? = null
        while (`in`.hasNext()) {
            when (`in`.nextName()) {
                "text" -> text = `in`.nextString()
            }
        }
        `in`.endObject()
        return when (text) {
            "Донорские" -> ReleaseType.Donor
            "Курсы" -> ReleaseType.Courses
            "Больничный" -> ReleaseType.SickLeave
            "Отпуск" -> ReleaseType.Vacation
            "По уходу за ребенком-инвалидом" -> ReleaseType.ChildCare
            "Прочее" -> ReleaseType.Other
            else -> ReleaseType.Other
        }
    }
}