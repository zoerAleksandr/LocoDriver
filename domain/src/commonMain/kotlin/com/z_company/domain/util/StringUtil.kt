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
