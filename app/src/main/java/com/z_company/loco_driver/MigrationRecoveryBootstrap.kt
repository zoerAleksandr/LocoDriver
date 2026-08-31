package com.z_company.loco_driver

import android.content.Context
import com.z_company.data_local.DatabaseDriverFactory
import com.z_company.data_local.recovery.AndroidPreMigrationRecoveryCoordinator

internal enum class MigrationBootstrapState {
    READY,
    RECOVERY_REQUIRED,
}

internal object MigrationRetryPolicy {
    fun shouldBlockAutomaticAttempt(failedBuild: Int?, currentBuild: Int): Boolean =
        failedBuild == currentBuild
}

/**
 * Runs before the main dependency graph is created, so a broken Route.db migration
 * cannot turn into a Koin crash loop.
 */
internal class MigrationRecoveryBootstrap(
    private val context: Context,
    private val appBuild: Int,
) {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    fun prepare(allowRetry: Boolean = false): MigrationBootstrapState {
        if (!allowRetry && failedInCurrentBuild()) {
            return MigrationBootstrapState.RECOVERY_REQUIRED
        }

        return try {
            AndroidPreMigrationRecoveryCoordinator(context, appBuild).prepareIfNeeded()
            DatabaseDriverFactory(context).createRouteDriver().close()
            preferences.edit().clear().commit()
            MigrationBootstrapState.READY
        } catch (error: Throwable) {
            preferences.edit()
                .putInt(KEY_FAILED_BUILD, appBuild)
                .putString(KEY_ERROR_CODE, error::class.simpleName ?: "MigrationError")
                .commit()
            MigrationBootstrapState.RECOVERY_REQUIRED
        }
    }

    fun currentState(): MigrationBootstrapState =
        if (failedInCurrentBuild()) {
            MigrationBootstrapState.RECOVERY_REQUIRED
        } else {
            MigrationBootstrapState.READY
        }

    fun errorCode(): String? = preferences.getString(KEY_ERROR_CODE, null)

    private fun failedInCurrentBuild(): Boolean =
        MigrationRetryPolicy.shouldBlockAutomaticAttempt(
            failedBuild = preferences.getInt(KEY_FAILED_BUILD, NO_BUILD).takeUnless { it == NO_BUILD },
            currentBuild = appBuild,
        )

    private companion object {
        const val PREFERENCES = "migration_recovery_bootstrap"
        const val KEY_FAILED_BUILD = "failed_build"
        const val KEY_ERROR_CODE = "error_code"
        const val NO_BUILD = -1
    }
}
