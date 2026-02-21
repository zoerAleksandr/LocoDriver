package com.z_company.domain.entities

import java.util.Calendar
import java.util.UUID
import kotlinx.serialization.Serializable
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable
enum class TagForDay {
    WORKING_DAY, NON_WORKING_DAY, SHORTENED_DAY, HOLIDAY,
}

@Serializable
data class MonthOfYear(
    var id: String = UUID.randomUUID().toString(),
    var year: Int = Calendar.getInstance().get(Calendar.YEAR),
    var month: Int = Calendar.getInstance().get(Calendar.MONTH),
    val days: List<Day> = listOf(),
    val tariffRate: Double = 0.0,
    val dateSetTariffRate: DateSetTariffRate? = null
)

// ReleasePeriod содержит List<Calendar> — не сериализуется через kotlinx.serialization.
// Используется только локально (не отправляется на сервер).
data class ReleasePeriod(
    val id: String = UUID.randomUUID().toString(),
    val days: List<Calendar> = listOf(),
    val type: ReleaseType? = null
)

/**
 * Кастомный сериализатор для sealed class ReleaseType.
 * Заменяет Gson TypeAdapter (ReleaseTypeAdapter).
 * Сериализует как строку — значение поля text.
 */
object ReleaseTypeSerializer : KSerializer<ReleaseType> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("ReleaseType", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ReleaseType) {
        encoder.encodeString(value.text)
    }

    override fun deserialize(decoder: Decoder): ReleaseType {
        return when (decoder.decodeString()) {
            "Донорские" -> ReleaseType.Donor
            "Курсы" -> ReleaseType.Courses
            "Больничный" -> ReleaseType.SickLeave
            "Отпуск" -> ReleaseType.Vacation
            "По уходу за ребенком-инвалидом" -> ReleaseType.ChildCare
            else -> ReleaseType.Other
        }
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
    val id: String = UUID.randomUUID().toString(),
    val dateNewRate: Int,
    val oldRate: Double,
)