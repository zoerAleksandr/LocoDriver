package com.z_company.data_local.recovery

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RecoveryRawSnapshotManifestTest {
    @Test
    fun validManifestRoundTrips() {
        val source = RecoveryRawSnapshotManifestV1(
            1,
            listOf(RecoveryRawSnapshotFileV1("Route.snapshot.db", 1L, "a".repeat(64), 12)),
        )
        assertEquals(
            source,
            RecoveryRawSnapshotManifestJson.decodeAndValidate(
                RecoveryRawSnapshotManifestJson.encode(source)
            ),
        )
    }

    @Test
    fun pathTraversalIsRejected() {
        val content = RecoveryRawSnapshotManifestJson.encode(
            RecoveryRawSnapshotManifestV1(
                1,
                listOf(RecoveryRawSnapshotFileV1("../Route.db", 1L, "a".repeat(64), 12)),
            )
        )
        assertFailsWith<IllegalArgumentException> {
            RecoveryRawSnapshotManifestJson.decodeAndValidate(content)
        }
    }
}
