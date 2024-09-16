package com.z_company.domain.util

inline fun String?.ifNullOrBlank(defaultValue: () -> String) =
    if (isNullOrBlank()) defaultValue() else this

fun String.splitBySpaceAndComma(): List<String> {
    return this.split(" ", ",")
}

fun String.toDoubleOrZero(): Double {
    return this.toDoubleOrNull() ?: 0.0
}