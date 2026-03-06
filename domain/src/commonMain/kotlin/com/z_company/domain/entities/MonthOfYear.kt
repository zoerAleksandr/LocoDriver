package com.z_company.domain.entities

import com.z_company.domain.util.generateId
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.datetime.LocalDate

@Serializable
enum class TagForDay {
    WORKING_DAY, NON_WORKING_DAY, SHORTENED_DAY, HOLIDAY,
}

@Serializable
data class MonthOfYear(
    var id: String = generateId(),
    var year: Int = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).year,
    var month: Int = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).monthNumber - 1,
    val days: List<Day> = listOf(),
    val tariffRate: Double = 0.0,
    val dateSetTariffRate: DateSetTariffRate? = null
)

// ReleasePeriod содержит List<LocalDate> — используется только локально (не отправляется на сервер).
data class ReleasePeriod(
    val id: String = generateId(),
    val days: List<LocalDate> = listOf(),
    val type: ReleaseType? = null
)

/**
 * Кастомный сериализатор для sealed class ReleaseType.
 * Заменяет Gson TypeAdapter (ReleaseTypeAdapter).
 * Сериализует как строку — значение поля text.
 *
 * Десериализация поддерживает два формата:
 * - Новый (kotlinx.serialization): примитивная строка "Отпуск"
 * - Старый (Gson/Room): JSON-объект {"type":"Vacation"}
 */
object ReleaseTypeSerializer : KSerializer<ReleaseType> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("ReleaseType", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ReleaseType) {
        encoder.encodeString(value.text)
    }

    override fun deserialize(decoder: Decoder): ReleaseType {
        // Поддержка legacy Gson-формата {"type":"Vacation"} из старой Room БД
        if (decoder is JsonDecoder) {
            return when (val element = decoder.decodeJsonElement()) {
                is JsonPrimitive -> fromText(element.content)
                is JsonObject -> fromGsonType(element["type"]?.jsonPrimitive?.content)
                else -> ReleaseType.Other
            }
        }
        // Fallback для не-JSON декодеров
        return fromText(decoder.decodeString())
    }

    /** Маппинг по русскому тексту (новый формат kotlinx.serialization) */
    private fun fromText(text: String): ReleaseType = when (text) {
        "Донорские" -> ReleaseType.Donor
        "Курсы" -> ReleaseType.Courses
        "Больничный" -> ReleaseType.SickLeave
        "Отпуск" -> ReleaseType.Vacation
        "По уходу за ребенком-инвалидом" -> ReleaseType.ChildCare
        else -> ReleaseType.Other
    }

    /** Маппинг по английскому имени класса (legacy Gson-формат из Room) */
    private fun fromGsonType(type: String?): ReleaseType = when (type) {
        "Vacation" -> ReleaseType.Vacation
        "SickLeave" -> ReleaseType.SickLeave
        "Courses" -> ReleaseType.Courses
        "Donor" -> ReleaseType.Donor
        "ChildCare" -> ReleaseType.ChildCare
        else -> ReleaseType.Other
    }
}

@Serializable(with = ReleaseTypeSerializer::class)
sealed class ReleaseType(val text: String) {
    object Vacation : ReleaseType("Отпуск")
    object SickLeave : ReleaseType("Больничный")
    object Courses : ReleaseType("Курсы")
    object Donor : ReleaseType("Донорские")
    object ChildCare : ReleaseType("По уходу за ребенком-инвалидом")
    object Other : ReleaseType("Прочее")
}

@Serializable
data class Day(
    val dayOfMonth: Int,
    val tag: TagForDay,
    val isReleaseDay: Boolean = false,
    val releaseType: ReleaseType? = null
)

@Serializable
data class DateSetTariffRate(
    val id: String = generateId(),
    val dateNewRate: Int,
    val oldRate: Double,
)
