package com.example.domain.util


fun Long?.lessThan(other: Long?): Boolean {
    return if (this == null || other == null) false
    else this < other
}
fun Long?.moreThan(other: Long?): Boolean {
    return if (this == null || other == null) false
    else this > other
}

