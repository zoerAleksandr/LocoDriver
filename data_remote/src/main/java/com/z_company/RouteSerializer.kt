package com.z_company

import com.z_company.domain.entities.route.Route
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModule
import java.math.BigDecimal
import java.util.Date
// Описание: Утилита для сериализации и десериализации объекта Route в JSON.
// Для чего: Используется для создания JSON-файла при шаринге маршрута и парсинга при импорте.
// Поддерживает вложенные структуры и списки автоматически благодаря @Serializable.

import java.io.File

object RouteSerializer {
    private val customSerializersModule = SerializersModule {
        contextual(Date::class, DateSerializer)
        contextual(BigDecimal::class, BigDecimalSerializer)
    }
    private val json = Json {
        prettyPrint = true  // Для читаемого JSON (опционально, можно убрать для компактности)
        encodeDefaults = true  // Сериализует дефолтные значения
        ignoreUnknownKeys = true  // Игнорирует неизвестные ключи при десериализации (для совместимости)
        serializersModule = customSerializersModule
    }

    // Функция: Сериализует Route в JSON-строку.
    // Для чего: Для записи в файл или передачи.
    fun serialize(route: Route): String {
        return json.encodeToString(serializer<Route>(), route)
    }

    // Функция: Десериализует JSON-строку в Route.
    // Для чего: Для импорта из файла.
    fun deserialize(jsonString: String): Route {
        return json.decodeFromString(serializer<Route>(), jsonString)
    }

    // Пример: Запись в файл (используй в shareRoute).
    // Для чего: Создаёт временный файл для шаринга.
    fun saveToFile(route: Route, file: File) {
        file.writeText(serialize(route))
    }

    // Пример: Чтение из файла (используй в checkIntent для импорта).
    // Для чего: Читает и десериализует файл.
    fun loadFromFile(file: File): Route {
        val jsonString = file.readText()
        return deserialize(jsonString)
    }
}

// Описание: Кастомный сериализатор для java.util.Date.
// Для чего: Стандартный сериализатор не обрабатывает Date напрямую; мы преобразуем его в Long (timestamp в миллисекундах) для JSON, чтобы избежать сложных объектов и обеспечить совместимость.
@OptIn(ExperimentalSerializationApi::class)
@Serializer(forClass = Date::class)
object DateSerializer : KSerializer<Date> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Date", PrimitiveKind.LONG)

    override fun serialize(encoder: Encoder, value: Date) {
        encoder.encodeLong(value.time)  // Сериализуем как Long (миллисекунды)
    }

    override fun deserialize(decoder: Decoder): Date {
        return Date(decoder.decodeLong())  // Десериализуем из Long обратно в Date
    }
}

// Описание: Кастомный сериализатор для java.math.BigDecimal.
// Для чего: BigDecimal не сериализуется по умолчанию как число без потери точности; мы преобразуем в String для точного представления (например, "123.456" вместо неточного double).
@OptIn(ExperimentalSerializationApi::class)
@Serializer(forClass = BigDecimal::class)
object BigDecimalSerializer : KSerializer<BigDecimal> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("BigDecimal", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: BigDecimal) {
        encoder.encodeString(value.toPlainString())  // Сериализуем как String без экспоненты
    }

    override fun deserialize(decoder: Decoder): BigDecimal {
        return BigDecimal(decoder.decodeString())  // Десериализуем из String обратно в BigDecimal
    }
}