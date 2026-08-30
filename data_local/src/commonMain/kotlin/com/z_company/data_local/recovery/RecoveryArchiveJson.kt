package com.z_company.data_local.recovery

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object RecoveryArchiveJson {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = true
    }

    fun encodeManifest(manifest: RecoveryArchiveManifest): String =
        json.encodeToString(manifest)

    fun decodeManifest(value: String): RecoveryArchiveManifest =
        if (value.encodeToByteArray().size <= RecoveryArchiveContract.MAX_MANIFEST_BYTES) {
            json.decodeFromString(value)
        } else {
            throw RecoveryArchiveValidationException(
                RecoveryArchiveValidationCode.MANIFEST_TOO_LARGE
            )
        }
}
