package com.z_company.loco_driver

import com.z_company.data_local.RouteMigrationStoragePolicy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteMigrationStoragePolicyTest {
    @Test
    fun requiresThreeDatabaseCopiesAndFixedReserve() {
        val databaseSize = 10L * 1024L * 1024L
        val required = RouteMigrationStoragePolicy.requiredAvailableBytes(databaseSize)

        assertFalse(RouteMigrationStoragePolicy.hasEnoughSpace(databaseSize, required - 1L))
        assertTrue(RouteMigrationStoragePolicy.hasEnoughSpace(databaseSize, required))
    }

    @Test
    fun negativeDatabaseSizeCannotReduceReserve() {
        assertTrue(
            RouteMigrationStoragePolicy.hasEnoughSpace(
                databaseSizeBytes = -1L,
                availableBytes = RouteMigrationStoragePolicy.FIXED_RESERVE_BYTES,
            )
        )
    }
}
