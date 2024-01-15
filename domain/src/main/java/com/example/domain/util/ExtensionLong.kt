package com.example.domain.util

fun Long?.compareWithNullable(other: Long?): Boolean {
    return if (this == null || other == null) true
    else this < other
}