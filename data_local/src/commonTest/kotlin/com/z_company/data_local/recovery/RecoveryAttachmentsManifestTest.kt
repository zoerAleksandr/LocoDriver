package com.z_company.data_local.recovery

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RecoveryAttachmentsManifestTest {
    @Test
    fun remoteAttachmentRoundTrips() {
        val source = RecoveryAttachmentsManifestV1(
            1,
            listOf(RecoveryRemoteAttachmentV1("photo-1", "route-1", null, "https://host/p", 1L)),
        )

        assertEquals(
            source,
            RecoveryAttachmentsManifestJson.decodeAndValidate(
                RecoveryAttachmentsManifestJson.encode(source)
            ),
        )
    }

    @Test
    fun localUriIsNotAcceptedAsBackedUpContent() {
        val content = RecoveryAttachmentsManifestJson.encode(
            RecoveryAttachmentsManifestV1(
                1,
                listOf(
                    RecoveryRemoteAttachmentV1(
                        "photo-1",
                        "route-1",
                        null,
                        "content://provider/image",
                        1L,
                    )
                ),
            )
        )

        val error = assertFailsWith<RecoveryAttachmentsValidationException> {
            RecoveryAttachmentsManifestJson.decodeAndValidate(content)
        }

        assertEquals(RecoveryAttachmentsValidationCode.UNSAFE_REMOTE_URL, error.code)
    }

    @Test
    fun embeddedUrlCredentialsAreRejected() {
        assertEquals(
            false,
            RecoveryAttachmentsManifestJson.isSafeRemoteUrl("https://token@host/image"),
        )
    }
}
