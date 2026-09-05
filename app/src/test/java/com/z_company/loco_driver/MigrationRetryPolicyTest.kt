package com.z_company.loco_driver

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MigrationRetryPolicyTest {
    @Test
    fun `same failed build blocks automatic retry`() {
        assertTrue(
            MigrationRetryPolicy.shouldBlockAutomaticAttempt(
                failedBuild = 80,
                currentBuild = 80,
            )
        )
    }

    @Test
    fun `new build is allowed to retry fixed migration`() {
        assertFalse(
            MigrationRetryPolicy.shouldBlockAutomaticAttempt(
                failedBuild = 80,
                currentBuild = 81,
            )
        )
    }

    @Test
    fun `first launch is allowed to attempt migration`() {
        assertFalse(
            MigrationRetryPolicy.shouldBlockAutomaticAttempt(
                failedBuild = null,
                currentBuild = 80,
            )
        )
    }
}
