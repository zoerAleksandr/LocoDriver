package com.z_company.domain.util

inline fun String?.ifNullOrBlank(defaultValue: () -> String) =
    if (isNullOrBlank()) defaultValue() else this

fun String.splitBySpaceAndComma(): List<String> {
    return this.split(" ", ",")
}

fun String.toDoubleOrZero(): Double {
    return this.toDoubleOrNull() ?: 0.0
}

fun String?.toIntOrZero(): Int {
    if (this.isNullOrBlank()) return 0
    // Сначала пробуем как Int ("114"), потом как Double ("114.0", "57.5") с округлением вниз.
    // Сервер хранит conditionalLength/weight/distance как Float и отдаёт с ".0",
    // что ломает прямой Int.parse. Этот fallback решает проблему на стороне клиента.
    return this.toIntOrNull() ?: this.toDoubleOrNull()?.toInt() ?: 0
}