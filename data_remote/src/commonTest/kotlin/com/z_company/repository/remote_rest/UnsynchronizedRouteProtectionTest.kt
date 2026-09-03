package com.z_company.repository.remote_rest

import com.z_company.domain.entities.route.BasicData
import com.z_company.domain.entities.route.Route
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UnsynchronizedRouteProtectionTest {

    @Test
    fun protectsNewUnsynchronizedRouteFromPullOverwrite() {
        val local = Route(
            basicData = BasicData(
                id = "local-only",
                isSynchronized = false,
                remoteRouteId = null,
            )
        )

        assertFalse(canDeleteLocalRouteMissingFromServer(local))
    }

    @Test
    fun protectsEditedCloudRouteFromPullOverwrite() {
        val local = Route(
            basicData = BasicData(
                id = "edited-cloud-route",
                isSynchronized = false,
                remoteRouteId = "edited-cloud-route",
            )
        )

        assertFalse(canDeleteLocalRouteMissingFromServer(local))
    }

    @Test
    fun allowsPullToReplaceCleanSynchronizedRoute() {
        val local = Route(
            basicData = BasicData(
                id = "clean-cloud-route",
                isSynchronized = true,
                remoteRouteId = "clean-cloud-route",
            )
        )

        assertTrue(canDeleteLocalRouteMissingFromServer(local))
    }

    @Test
    fun deletedRouteUsesDedicatedDeletionFlow() {
        val local = Route(
            basicData = BasicData(
                id = "deleted-route",
                isSynchronized = false,
                isDeleted = true,
            )
        )

        assertFalse(canDeleteLocalRouteMissingFromServer(local))
    }
}
