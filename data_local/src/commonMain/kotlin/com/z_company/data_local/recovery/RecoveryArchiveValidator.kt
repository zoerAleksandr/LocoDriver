package com.z_company.data_local.recovery

object RecoveryArchiveValidator {
    private val sectionName = Regex("^[a-z0-9][a-z0-9._-]{0,63}$")
    private val sha256 = Regex("^[a-fA-F0-9]{64}$")

    fun validateManifest(manifest: RecoveryArchiveManifest) {
        requireValid(
            manifest.archiveFormatVersion == RecoveryArchiveContract.CURRENT_FORMAT_VERSION,
            RecoveryArchiveValidationCode.UNSUPPORTED_FORMAT,
        )
        requireValid(manifest.sourceDbVersion > 0, RecoveryArchiveValidationCode.INVALID_SOURCE_VERSION)
        requireValid(manifest.appBuild > 0, RecoveryArchiveValidationCode.INVALID_APP_BUILD)
        requireValid(manifest.createdAt > 0L, RecoveryArchiveValidationCode.INVALID_CREATED_AT)
        requireValid(
            manifest.installationId.isNotBlank() && manifest.installationId.length <= 128,
            RecoveryArchiveValidationCode.INVALID_INSTALLATION_ID,
        )

        val names = mutableSetOf<String>()
        manifest.sections.forEach { section ->
            requireValid(sectionName.matches(section.name), RecoveryArchiveValidationCode.INVALID_SECTION_NAME)
            requireValid(names.add(section.name), RecoveryArchiveValidationCode.DUPLICATE_SECTION)
            requireValid(section.itemCount >= 0L, RecoveryArchiveValidationCode.INVALID_ITEM_COUNT)
            requireValid(sha256.matches(section.sha256), RecoveryArchiveValidationCode.INVALID_SHA256)
        }
        requireValid(
            names.containsAll(RecoveryArchiveContract.REQUIRED_SECTIONS),
            RecoveryArchiveValidationCode.MISSING_REQUIRED_SECTION,
        )
    }

    fun validatePayloadDigests(
        manifest: RecoveryArchiveManifest,
        actual: Map<String, RecoveryArchiveSectionDigest>,
    ) {
        validateManifest(manifest)
        manifest.sections.forEach { expected ->
            val digest = actual[expected.name]
            requireValid(
                digest != null &&
                    digest.itemCount == expected.itemCount &&
                    digest.sha256.equals(expected.sha256, ignoreCase = true),
                RecoveryArchiveValidationCode.SECTION_DIGEST_MISMATCH,
            )
        }
    }

    private fun requireValid(condition: Boolean, code: RecoveryArchiveValidationCode) {
        if (!condition) throw RecoveryArchiveValidationException(code)
    }
}
