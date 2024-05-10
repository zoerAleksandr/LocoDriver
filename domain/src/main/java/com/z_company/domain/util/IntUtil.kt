package com.z_company.domain.util

operator fun Int?.minus(other: Int?): Int? =
    if (this != null && other != null) {
        this - other
    } else {
        null
    }

operator fun Int?.plus(other: Int?): Int? =
    if (this != null && other != null) {
        this + other
    } else if (this == null && other != null) {
        other
    } else this

fun Int?.plusNullableValue(other: Int?): Int? {
    return if (this == null) {
        null
    } else {
        this + other
    }
}

 fun Int?.str(): String {
     return this?.toString() ?: ""
 }