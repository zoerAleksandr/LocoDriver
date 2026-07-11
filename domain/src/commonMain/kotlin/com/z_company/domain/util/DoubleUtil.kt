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

/** Убирает хвостовые нули; для чисел в научной нотации раскрывает вручную (KMP-safe) */
fun Double?.str(): String {
    if (this == null) return ""
    return if (this % 1.0 == 0.0) {
        this.toLong().toString()
    } else {
        val s = this.toString()
        if ('E' in s || 'e' in s) expandScientificNotation(this)
        else s
    }
}

private fun expandScientificNotation(value: Double): String {
    val negative = value < 0
    val abs = if (negative) -value else value
    val intPart = abs.toLong()
    var frac = abs - intPart
    val sb = StringBuilder()
    if (negative) sb.append('-')
    sb.append(intPart)
    if (frac > 0.0) {
        sb.append('.')
        repeat(10) {
            frac *= 10
            val digit = frac.toInt()
            sb.append(digit)
            frac -= digit
        }
    }
    return sb.toString().trimEnd('0').trimEnd('.')
}

/**
 * Форматирует денежную сумму по российской локали: разряды через неразрывный
 * пробел, копейки через запятую — «69 928,32». KMP-safe (без java.util.Formatter).
 * Неразрывный пробел ( ) не даёт сумме переноситься по строкам.
 */
private fun formatFixed2(value: Double): String {
    val negative = value < 0
    val abs = if (negative) -value else value
    val rounded = kotlin.math.round(abs * 100).toLong()
    val intPart = rounded / 100
    val fracPart = (rounded % 100).toInt()
    val intGrouped = intPart.toString()
        .reversed()
        .chunked(3)
        .joinToString(" ")
        .reversed()
    return buildString {
        if (negative) append('-')
        append(intGrouped)
        append(',')
        append(fracPart.toString().padStart(2, '0'))
    }
}

fun Double?.str2decimalSign(): String {
    return if (this == null) "" else formatFixed2(this)
}

fun Double?.toMoneyString(currency: String = "₽"): String {
    return if (this == null) "0 $currency" else "${formatFixed2(this)} $currency"
}
