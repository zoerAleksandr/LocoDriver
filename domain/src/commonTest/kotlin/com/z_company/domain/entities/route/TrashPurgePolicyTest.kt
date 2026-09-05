package com.z_company.domain.entities.route

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TrashPurgePolicyTest {
    @Test
    fun activeRouteCannotBePurged() {
        assertFalse(TrashPurgePolicy.canPurgeManually(route(isDeleted = false)))
    }

    @Test
    fun pendingRemoteDeletionCannotBePurged() {
        assertFalse(
            TrashPurgePolicy.canPurgeManually(
                route(isDeleted = true, remoteRouteId = "remote", remoteDeletionPending = true)
            )
        )
    }

    @Test
    fun unsentLocalTrashCanBePurged() {
        assertTrue(TrashPurgePolicy.canPurgeManually(route(isDeleted = true)))
    }

    @Test
    fun serverBackedTrashRequiresRemoteAcknowledgement() {
        assertFalse(
            TrashPurgePolicy.canPurgeManually(
                route(isDeleted = true, remoteRouteId = "remote")
            )
        )
        assertTrue(
            TrashPurgePolicy.canPurgeManually(
                route(isDeleted = true, remoteRouteId = "remote", remoteDeletedAt = 123L)
            )
        )
    }

    private fun route(
        isDeleted: Boolean,
        remoteRouteId: String? = null,
        remoteDeletionPending: Boolean = false,
        remoteDeletedAt: Long? = null,
    ) = Route(
        basicData = BasicData(
            isDeleted = isDeleted,
            remoteRouteId = remoteRouteId,
            remoteDeletionPending = remoteDeletionPending,
            remoteDeletedAt = remoteDeletedAt,
        )
    )
}
