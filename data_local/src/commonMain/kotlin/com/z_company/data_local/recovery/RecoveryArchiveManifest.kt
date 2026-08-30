package com.z_company.data_local.recovery

import kotlinx.serialization.Serializable

@Serializable
data class RecoveryArchiveManifest(
    val archiveFormatVersion: Int,
    val sourceDbVersion: Int,
    val appBuild: Int,
    val createdAt: Long,
    val installationId: String,
    val accountIdHash: String? = null,
    val sections: List<RecoveryArchiveSection>,
)

@Serializable
data class RecoveryArchiveSection(
    val name: String,
    val itemCount: Long,
    val sha256: String,
)

data class RecoveryArchiveSectionDigest(
    val itemCount: Long,
    val sha256: String,
)

enum class RecoveryArchiveValidationCode {
    UNSUPPORTED_FORMAT,
    INVALID_SOURCE_VERSION,
    INVALID_APP_BUILD,
    INVALID_CREATED_AT,
    INVALID_INSTALLATION_ID,
    INVALID_SECTION_NAME,
    DUPLICATE_SECTION,
    MISSING_REQUIRED_SECTION,
    INVALID_ITEM_COUNT,
    INVALID_SHA256,
    SECTION_DIGEST_MISMATCH,
}

class RecoveryArchiveValidationException(
    val code: RecoveryArchiveValidationCode,
) : IllegalArgumentException(code.name)

object RecoveryArchiveContract {
    const val CURRENT_FORMAT_VERSION: Int = 1

    val REQUIRED_SECTIONS: Set<String> = setOf(
        "routes.ndjson",
        "settings.json",
        "salary-settings.json",
        "norms.json",
        "attachments-manifest.json",
    )
}
