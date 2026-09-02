package com.z_company.core

fun initSentry(dsn: String) {
    platformInitSentry(dsn)
}

internal expect fun platformInitSentry(dsn: String)
