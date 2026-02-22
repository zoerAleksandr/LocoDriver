package com.z_company.domain.entities.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

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
