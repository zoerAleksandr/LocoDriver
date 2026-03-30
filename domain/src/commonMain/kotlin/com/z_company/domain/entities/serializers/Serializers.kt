package com.z_company.domain.entities.serializers

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull

/**
 * Сериализатор для updatedAt: Long.
 * Сервер присылает дату строкой "Feb 17, 2026 17:24:45" (формат Gson DateSerializer),
 * а локально хранится как Long epoch millis. Обрабатывает оба формата.
 */
object DateAsLongSerializer : KSerializer<Long> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("DateAsLong", PrimitiveKind.LONG)

    override fun serialize(encoder: Encoder, value: Long) = encoder.encodeLong(value)

    override fun deserialize(decoder: Decoder): Long {
        if (decoder is JsonDecoder) {
            val element = decoder.decodeJsonElement()
            if (element is JsonPrimitive) {
                element.longOrNull?.let { return it }
                element.content.let { return parseDateString(it) }
            }
        }
        return decoder.decodeLong()
    }

    private val months = mapOf(
        "Jan" to 1, "Feb" to 2, "Mar" to 3, "Apr" to 4,
        "May" to 5, "Jun" to 6, "Jul" to 7, "Aug" to 8,
        "Sep" to 9, "Oct" to 10, "Nov" to 11, "Dec" to 12
    )

    private fun parseDateString(dateStr: String): Long {
        return runCatching {
            // "Feb 17, 2026 17:24:45"
            val parts = dateStr.split(" ", ":")
            val month = months[parts[0]] ?: 1
            val day = parts[1].trimEnd(',').toInt()
            val year = parts[2].toInt()
            val hour = parts[3].toInt()
            val minute = parts[4].toInt()
            val second = parts[5].toInt()
            LocalDateTime(year, month, day, hour, minute, second)
                .toInstant(TimeZone.UTC)
                .toEpochMilliseconds()
        }.getOrElse {
            println("DateAsLongSerializer: Failed to parse '$dateStr': ${it.message}")
            Clock.System.now().toEpochMilliseconds()
        }
    }
}

/**
 * Сериализатор для Int-полей, которые сервер может прислать как Double (напр. 30.0).
 * Обрабатывает Int, Double и Long из JSON, конвертируя в Int.
 */
object IntAsDoubleSerializer : KSerializer<Int> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("IntAsDouble", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: Int) = encoder.encodeInt(value)

    override fun deserialize(decoder: Decoder): Int {
        if (decoder is JsonDecoder) {
            val element = decoder.decodeJsonElement()
            if (element is JsonPrimitive) {
                element.intOrNull?.let { return it }
                element.doubleOrNull?.let { return it.toInt() }
                element.longOrNull?.let { return it.toInt() }
            }
        }
        return decoder.decodeInt()
    }
}

/**
 * Сериализатор для полей нормы электровоза: принимает Int или Double от сервера, возвращает Double.
 * Отправляет на сервер без дробной части если она равна нулю (12.0 → 12), иначе с дробной (12.5).
 */
object NumberAsDoubleSerializer : KSerializer<Double> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("NumberAsDouble", PrimitiveKind.DOUBLE)

    override fun serialize(encoder: Encoder, value: Double) {
        if (encoder is JsonEncoder && value % 1.0 == 0.0) {
            encoder.encodeLong(value.toLong())
        } else {
            encoder.encodeDouble(value)
        }
    }

    override fun deserialize(decoder: Decoder): Double {
        if (decoder is JsonDecoder) {
            val element = decoder.decodeJsonElement()
            if (element is JsonPrimitive) {
                element.doubleOrNull?.let { return it }
                element.longOrNull?.let { return it.toDouble() }
                element.intOrNull?.let { return it.toDouble() }
            }
        }
        return decoder.decodeDouble()
    }
}

/**
 * Сериализатор для Double → String (для совместимости с сервером, который присылает числа как строки).
 * Используется через @Contextual в Locomotive, SectionElectric.
 * Заменяет BigDecimalAsStringSerializer.
 */
object DoubleAsStringSerializer : KSerializer<Double> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("DoubleAsString", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Double) {
        val s = if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
        encoder.encodeString(s)
    }

    override fun deserialize(decoder: Decoder): Double {
        return decoder.decodeString().toDouble()
    }
}
