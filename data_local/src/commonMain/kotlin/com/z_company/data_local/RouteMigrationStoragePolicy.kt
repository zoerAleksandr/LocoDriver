package com.z_company.data_local

object RouteMigrationStoragePolicy {
    const val FIXED_RESERVE_BYTES: Long = 16L * 1024L * 1024L

    fun requiredAvailableBytes(databaseSizeBytes: Long): Long {
        val safeSize = databaseSizeBytes.coerceAtLeast(0L)
        val maximumSafeSize = (Long.MAX_VALUE - FIXED_RESERVE_BYTES) / 3L
        return if (safeSize > maximumSafeSize) {
            Long.MAX_VALUE
        } else {
            safeSize * 3L + FIXED_RESERVE_BYTES
        }
    }

    fun hasEnoughSpace(databaseSizeBytes: Long, availableBytes: Long): Boolean =
        availableBytes >= requiredAvailableBytes(databaseSizeBytes)
}
