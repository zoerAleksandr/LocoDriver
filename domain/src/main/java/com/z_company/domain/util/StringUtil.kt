package com.z_company.domain.util

inline fun String?.ifNullOrBlank(defaultValue: () -> String) =
    if (isNullOrBlank()) defaultValue() else this