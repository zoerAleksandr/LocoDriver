package com.z_company.domain.entities.route

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class BasicDataTrashSerializationTest {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun oldPayloadUsesSafeTrashDefaults() {
        val decoded = json.decodeFromString<BasicData>(
            """{"id":"legacy","isDeleted":true,"updatedAt":1000}"""
        )

        assertEquals("legacy", decoded.id)
        assertEquals(null, decoded.deletedAt)
        assertEquals(null, decoded.deletionReason)
        assertFalse(decoded.remoteDeletionPending)
        assertEquals(null, decoded.remoteDeletedAt)
    }

    @Test
    fun localTrashMetadataIsNeverSerialized() {
        val encoded = json.encodeToString(
            BasicData(
                id = "route",
                isDeleted = true,
                deletedAt = 2000,
                deletionReason = "USER_REQUESTED",
                remoteDeletionPending = true,
                remoteDeletedAt = 3000,
            )
        )

        assertFalse(encoded.contains("deletedAt"))
        assertFalse(encoded.contains("deletionReason"))
        assertFalse(encoded.contains("remoteDeletionPending"))
        assertFalse(encoded.contains("remoteDeletedAt"))
    }
}
