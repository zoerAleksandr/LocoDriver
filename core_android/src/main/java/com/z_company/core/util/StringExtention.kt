package com.z_company.core.util

import android.text.TextUtils

fun String.isEmailValid(): Boolean {
    return !TextUtils.isEmpty(this) && android.util.Patterns.EMAIL_ADDRESS.matcher(this).matches()
}

fun String.splitBySpaceAndComma(): List<String> {
    return this.split(" ", ",")
}