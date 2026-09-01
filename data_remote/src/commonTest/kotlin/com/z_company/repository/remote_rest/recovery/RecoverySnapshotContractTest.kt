package com.z_company.repository.remote_rest.recovery

import com.z_company.repository.remote_rest.RemoteRestClient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertTrue

class RecoverySnapshotContractTest {
    @Test
    fun contractRoundTripsAndIgnoresFutureFields() {
        val json = """{
            "snapshotId":"snapshot-1","status":"READY","archiveFormatVersion":1,
            "sourceDbVersion":12,"appBuild":80,"sizeBytes":123,"sha256":"${"a".repeat(64)}",
            "createdAt":1,"futureField":"safe"
        }""".trimIndent()
        val response = RemoteRestClient.appJson.decodeFromString<RecoverySnapshotResponse>(json)
        RecoverySnapshotContractValidator.validate(response)
        assertEquals("snapshot-1", response.snapshotId)
    }

    @Test
    fun invalidSizeAndChecksumAreRejectedBeforeNetwork() {
        assertFails {
            RecoverySnapshotContractValidator.validate(
                RecoverySnapshotCreateRequest("key", 1, 12, 80, "installation", 0, "bad")
            )
        }
    }

    @Test
    fun recoveryEndpointIsHttps() {
        assertTrue(RECOVERY_API_BASE_URL.startsWith("https://"))
    }
}
