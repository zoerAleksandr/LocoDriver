package com.z_company.domain.util

/** Возвращает true если this < other && (this == null && other == null)
 * */
fun Double?.lessThan(other: Double?): Boolean {
    return if (this == null || other == null) false
    else this < other
}

/** Возвращает true если this > other && (this == null && other == null)
 * */
fun Double?.moreThan(other: Double?): Boolean {
    return if (this == null || other == null) false
    else this > other
}

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
    val s = this.toString()
    val eIdx = s.indexOf('E')
    val dotIdx = s.indexOf('.')
    return when {
        dotIdx < 0 -> 0
        eIdx > 0 -> eIdx - dotIdx - 1
        else -> s.length - dotIdx - 1
    }
}

fun Double?.str(): String {
    if (this == null) return ""
    return if (this % 1.0 == 0.0) {
        this.toLong().toString()
    } else {
        val s = this.toString()
        if ('E' in s || 'e' in s) "%.10f".format(this).trimEnd('0').trimEnd('.')
        else s
    }
}

@Suppress("DefaultLocale")
fun Double?.str2decimalSign(): String {
    return if (this == null) {
        ""
    } else {
        String.format("%.2f", this)
    }
}

fun Double?.toMoneyString(): String {
    return if (this == null) {
        "0 ₽"
    } else {
        "${String.format("% .2f", this)} ₽"
    }
}
