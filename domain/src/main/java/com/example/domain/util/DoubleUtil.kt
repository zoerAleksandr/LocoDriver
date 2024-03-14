package com.example.domain.util

import java.math.BigDecimal

operator fun Double?.minus(other: Double?): Double? =
    if (this != null && other != null) {
        this - other
    } else {
        null
    }

operator fun Double?.times(other: Double?): Double? =
    if (this != null && other != null) {
        this * other
    } else {
        null
    }

operator fun Double.plus(other: Double?): Double {
    return if (other == null) {
        this
    } else {
        this + other
    }
}

operator fun Double?.plus(other: Double?): Double? =
    if (this != null && other != null) {
        this + other
    } else if (this == null && other != null) {
        other
    } else if (this != null && other == null) {
        this
    } else {
        null
    }

fun Double?.plusNullableValue(other: Double?): Double? {
    return if (this == null) {
        null
    } else {
        this + other
    }
}

fun Double.countCharsAfterDecimalPoint(): Int {
    return BigDecimal.valueOf(this).scale()
}

fun Double.str(): String {
    return if (this % 1.0 == 0.0) {
        this.toString().dropLast(2)
    } else {
        this.toString()
    }
}