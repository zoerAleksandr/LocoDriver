package com.z_company.data_local.recovery

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class RecoveryArchiveValidatorTest {
    @Test
    fun validVersionOneManifestAndPayloadPass() {
        val manifest = validManifest()

        RecoveryArchiveValidator.validatePayloadDigests(
            manifest,
            manifest.sections.associate { section ->
                section.name to RecoveryArchiveSectionDigest(section.itemCount, section.sha256)
            },
        )
    }

    @Test
    fun pathTraversalSectionIsRejected() {
        val manifest = validManifest().copy(
            sections = validManifest().sections +
                RecoveryArchiveSection("../Route.db", 1L, HASH),
        )

        assertValidationCode(RecoveryArchiveValidationCode.INVALID_SECTION_NAME) {
            RecoveryArchiveValidator.validateManifest(manifest)
        }
    }

    @Test
    fun missingRequiredSectionIsRejected() {
        val manifest = validManifest().copy(
            sections = validManifest().sections.filterNot { it.name == "routes.ndjson" },
        )

        assertValidationCode(RecoveryArchiveValidationCode.MISSING_REQUIRED_SECTION) {
            RecoveryArchiveValidator.validateManifest(manifest)
        }
    }

    @Test
    fun duplicateSectionIsRejected() {
        val manifest = validManifest().let { it.copy(sections = it.sections + it.sections.first()) }

        assertValidationCode(RecoveryArchiveValidationCode.DUPLICATE_SECTION) {
            RecoveryArchiveValidator.validateManifest(manifest)
        }
    }

    @Test
    fun digestMismatchIsRejected() {
        val manifest = validManifest()
        val actual = manifest.sections.associate { section ->
            section.name to RecoveryArchiveSectionDigest(section.itemCount, section.sha256)
        }.toMutableMap().apply {
            this["routes.ndjson"] = RecoveryArchiveSectionDigest(2L, HASH)
        }

        assertValidationCode(RecoveryArchiveValidationCode.SECTION_DIGEST_MISMATCH) {
            RecoveryArchiveValidator.validatePayloadDigests(manifest, actual)
        }
    }

    @Test
    fun unknownJsonFieldsAreIgnoredForForwardCompatibility() {
        val encoded = RecoveryArchiveJson.encodeManifest(validManifest())
        val withFutureField = encoded.dropLast(1) + ",\"futureField\":true}"

        val decoded = RecoveryArchiveJson.decodeManifest(withFutureField)

        assertEquals(validManifest(), decoded)
        assertTrue(RecoveryArchiveJson.encodeManifest(decoded).contains("archiveFormatVersion"))
    }

    @Test
    fun oversizedManifestIsRejectedBeforeDecoding() {
        val oversized = " ".repeat(RecoveryArchiveContract.MAX_MANIFEST_BYTES + 1)

        assertValidationCode(RecoveryArchiveValidationCode.MANIFEST_TOO_LARGE) {
            RecoveryArchiveJson.decodeManifest(oversized)
        }
    }

    @Test
    fun excessiveSectionCountIsRejected() {
        val base = validManifest()
        val extra = (0..RecoveryArchiveContract.MAX_SECTIONS).map { index ->
            RecoveryArchiveSection("future-$index.bin", 0L, HASH)
        }

        assertValidationCode(RecoveryArchiveValidationCode.TOO_MANY_SECTIONS) {
            RecoveryArchiveValidator.validateManifest(base.copy(sections = base.sections + extra))
        }
    }

    private fun assertValidationCode(
        expected: RecoveryArchiveValidationCode,
        block: () -> Unit,
    ) {
        val error = assertFailsWith<RecoveryArchiveValidationException>(block = block)
        assertEquals(expected, error.code)
    }

    private fun validManifest(): RecoveryArchiveManifest = RecoveryArchiveManifest(
        archiveFormatVersion = 1,
        sourceDbVersion = 12,
        appBuild = 80,
        createdAt = 1_777_000_000_000L,
        installationId = "fixture-installation",
        sections = RecoveryArchiveContract.REQUIRED_SECTIONS.mapIndexed { index, name ->
            RecoveryArchiveSection(name, index.toLong(), HASH)
        },
    )

    private companion object {
        const val HASH = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
    }
}
