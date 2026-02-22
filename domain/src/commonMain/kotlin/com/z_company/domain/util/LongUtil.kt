package com.z_company.domain.util

/** Возвращает true если this < other && (this == null && other == null)
 * */
fun Long?.lessThan(other: Long?): Boolean {
    return if (this == null || other == null) false
    else this < other
}

/** Возвращает true если this > other && (this == null && other == null)
 * */
fun Long?.moreThan(other: Long?): Boolean {
    return if (this == null || other == null) false
    else this > other
}
fun Long?.compareWithNullable(other: Long?): Boolean {
    return if (this == null || other == null) true
    else this < other
}
fun Long.ifNotZero(): Long? =
    if (this == 0L) null
    else this
