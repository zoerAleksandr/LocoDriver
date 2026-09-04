package com.z_company.domain.util

inline fun String?.ifNullOrBlank(defaultValue: () -> String) =
    if (isNullOrBlank()) defaultValue() else this

fun String.splitBySpaceAndComma(): List<String> {
    return this.split(" ", ",")
}

fun String.toDoubleOrZero(): Double {
    return toFiniteDoubleOrNull() ?: 0.0
}

fun String.toFiniteDoubleOrNull(): Double? {
    val normalized = filterNot(Char::isWhitespace).replace(',', '.')
    return normalized.toDoubleOrNull()?.takeIf(Double::isFinite)
}

/** Денежное/процентное значение настройки: пустое поле означает 0. */
fun String.toNonNegativeFiniteDoubleOrNull(): Double? {
    if (isBlank()) return 0.0
    return toFiniteDoubleOrNull()?.takeIf { it >= 0.0 }
}

fun String.toExactIntOrNull(): Int? {
    val value = toFiniteDoubleOrNull() ?: return null
    if (value % 1.0 != 0.0 || value < Int.MIN_VALUE || value > Int.MAX_VALUE) return null
    return value.toInt()
}

fun String?.toIntOrZero(): Int {
    if (this.isNullOrBlank()) return 0
    // Сначала пробуем как Int ("114"), потом как Double ("114.0", "57.5") с округлением вниз.
    // Сервер хранит conditionalLength/weight/distance как Float и отдаёт с ".0",
    // что ломает прямой Int.parse. Этот fallback решает проблему на стороне клиента.
    val normalized = filterNot(Char::isWhitespace).replace(',', '.')
    val value = normalized.toDoubleOrNull()?.takeIf(Double::isFinite) ?: return 0
    if (value < Int.MIN_VALUE || value > Int.MAX_VALUE) return 0
    return value.toInt()
}

/**
 * Оставляет от пользовательского ввода ведущее число: цифры и, если
 * [allowDecimal], один десятичный разделитель (запятая приводится к точке).
 *
 * Числовая клавиатура Android показывает вспомогательный ряд с «( ) - , .»,
 * поэтому на сервер уходили значения вида "4623(" (промах по скобке рядом с
 * цифрами), "13-" и "29,13". Разбор останавливается на первом недопустимом
 * символе: "4623 (60)" должно дать 4623, а не склейку 462360.
 */
fun String.sanitizeNumericInput(allowDecimal: Boolean = false): String {
    val result = StringBuilder()
    var hasSeparator = false
    for (char in trim()) {
        when {
            // Именно ASCII-цифры: Char.isDigit() пропустит арабо-индийские,
            // которые сервер потом не разберёт.
            char in '0'..'9' -> result.append(char)
            allowDecimal && !hasSeparator && (char == ',' || char == '.') -> {
                result.append('.')
                hasSeparator = true
            }
            else -> return result.toString()
        }
    }
    return result.toString()
}
