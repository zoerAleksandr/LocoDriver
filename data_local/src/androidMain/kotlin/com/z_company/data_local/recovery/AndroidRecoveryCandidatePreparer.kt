package com.z_company.data_local.recovery

import android.content.Context
import java.io.File

data class PreparedRecoveryCandidate(
    val routeDatabase: File,
    val settingsDatabase: File,
    val salaryDatabase: File,
    val routeCount: Long,
    val settingsRowCount: Long,
    val salaryRowCount: Long,
)

/** Builds disposable current-schema databases. It never replaces any live database. */
class AndroidRecoveryCandidatePreparer(private val context: Context) {
    fun prepare(archiveDirectory: File): PreparedRecoveryCandidate {
        val manifest = AndroidRecoveryArchiveInspector().inspect(archiveDirectory)
        clearCandidates()
        val digests = manifest.sections.associate { section ->
            section.name to RecoveryArchiveSectionDigest(section.itemCount, section.sha256)
        }
        try {
            val routes = AndroidRouteRecoveryImporter(context).importToFreshDatabase(
                File(archiveDirectory, ROUTES_FILE),
                requireNotNull(digests[ROUTES_FILE]),
                ROUTE_CANDIDATE,
            )
            val settingsImporter = AndroidSettingsRecoveryImporter(context)
            val settings = settingsImporter.importSettingsAndNormsToFreshDatabase(
                File(archiveDirectory, SETTINGS_FILE),
                requireNotNull(digests[SETTINGS_FILE]),
                File(archiveDirectory, NORMS_FILE),
                requireNotNull(digests[NORMS_FILE]),
                SETTINGS_CANDIDATE,
            )
            val salary = settingsImporter.importSalaryToFreshDatabase(
                File(archiveDirectory, SALARY_FILE),
                requireNotNull(digests[SALARY_FILE]),
                SALARY_CANDIDATE,
            )
            return PreparedRecoveryCandidate(
                routes.databaseFile,
                settings.databaseFile,
                salary.databaseFile,
                routes.routeCount,
                settings.itemCount,
                salary.itemCount,
            )
        } catch (error: Throwable) {
            clearCandidates()
            throw error
        }
    }

    fun clearCandidates() {
        context.deleteDatabase(ROUTE_CANDIDATE)
        context.deleteDatabase(SETTINGS_CANDIDATE)
        context.deleteDatabase(SALARY_CANDIDATE)
    }

    companion object {
        const val ROUTE_CANDIDATE = "Route.recovery.candidate.db"
        const val SETTINGS_CANDIDATE = "Settings.recovery.candidate.db"
        const val SALARY_CANDIDATE = "SalarySetting.recovery.candidate.db"
        private const val ROUTES_FILE = "routes.ndjson"
        private const val SETTINGS_FILE = "settings.json"
        private const val SALARY_FILE = "salary-settings.json"
        private const val NORMS_FILE = "norms.json"
    }
}
