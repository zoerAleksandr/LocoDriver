package com.z_company.domain.util

import java.time.Instant
import java.time.LocalDateTime


fun Long?.lessThan(other: Long?): Boolean {
    return if (this == null || other == null) false
    else this < other
}
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
