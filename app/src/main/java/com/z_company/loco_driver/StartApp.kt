package com.z_company.loco_driver

import android.app.Application
import android.util.Log
import com.my.tracker.MyTracker
import com.my.tracker.MyTrackerConfig.LocationTrackingMode
import com.z_company.route.session.SessionExpiredHandler
import com.z_company.core.initSentry
import com.vk.id.VKID
import com.z_company.data_local.route.di.sqlDelightRouteModule
import com.z_company.data_local.setting.di.sqlDelightSettingsModule
import com.z_company.loco_driver.di.repositoryModule
import com.z_company.loco_driver.di.resourcesModule
import com.z_company.loco_driver.di.updateModule
import com.z_company.loco_driver.di.useCaseModule
import com.z_company.loco_driver.di.viewModelModule
import com.z_company.repository.remote_rest.RemoteRestClient
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import java.util.Locale
import androidx.work.Constraints
import androidx.work.BackoffPolicy
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.z_company.work_manager.SyncWorker
import com.z_company.work_manager.DiagnosticWorker
import com.z_company.loco_driver.recovery.RecoverySnapshotUploadWorker
import com.z_company.loco_driver.recovery.RecoverySnapshotRestoreCoordinator
import com.z_company.loco_driver.recovery.RecoveryTelemetryQueue
import com.z_company.loco_driver.recovery.RecoveryTelemetryReason
import com.z_company.loco_driver.recovery.RecoveryTelemetryType
import com.z_company.repository.SecureTokenStorage
import com.z_company.repository.remote_rest.recovery.RecoveryCloudHttpException
import com.z_company.domain.repositories.DiagnosticRepository
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first

internal sealed interface CloudRecoveryOutcome {
    data object Success : CloudRecoveryOutcome
    data object AuthorizationRequired : CloudRecoveryOutcome
    data object SnapshotUnavailable : CloudRecoveryOutcome
    data class Failed(val errorCode: String) : CloudRecoveryOutcome
}

class StartApp : Application() {

    private val mainGraphStarted = AtomicBoolean(false)
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var migrationBootstrap: MigrationRecoveryBootstrap

    @Volatile
    internal var migrationBootstrapState: MigrationBootstrapState = MigrationBootstrapState.READY
        private set

    override fun onCreate() {
        super.onCreate()
        // До первого сетевого запроса: в debug адрес API подменяется на
        // локальный бэкенд (BuildConfig.API_BASE_URL), в release строка пустая
        // и остаётся прод.
        RemoteRestClient.useBaseUrl(BuildConfig.API_BASE_URL)
        if (BuildConfig.DEBUG) {
            Log.i("StartApp", "API: ${RemoteRestClient.BASE_URL}")
        }
        initSentry(BuildConfig.SENTRY_DSN)
        VKID.init(this)
        VKID.instance.setLocale(Locale("ru"))
        val myTrackerConfig = MyTracker.getTrackerConfig()
        myTrackerConfig.locationTrackingMode = LocationTrackingMode.CACHED
        MyTracker.setDebugMode(true)
        MyTracker.initTracker(getString(R.string.my_tracer_sdk_key), this)

        migrationBootstrap = MigrationRecoveryBootstrap(this, BuildConfig.VERSION_CODE)
        migrationBootstrapState = migrationBootstrap.prepare()
        if (migrationBootstrapState != MigrationBootstrapState.READY) return

        startMainGraphAndWorkers()
    }

    fun retryRouteMigration(): Boolean {
        val telemetry = RecoveryTelemetryQueue(this)
        telemetry.record(RecoveryTelemetryType.RECOVERY_SOURCE_SELECTED, RecoveryTelemetryReason.LOCAL)
        migrationBootstrapState = migrationBootstrap.prepare(allowRetry = true)
        if (migrationBootstrapState != MigrationBootstrapState.READY) {
            telemetry.record(RecoveryTelemetryType.RECOVERY_FAILED, RecoveryTelemetryReason.MIGRATION_FAILED)
            return false
        }
        telemetry.record(RecoveryTelemetryType.RECOVERY_SUCCEEDED, RecoveryTelemetryReason.LOCAL)
        startMainGraphAndWorkers()
        return true
    }

    fun migrationErrorCode(): String? = migrationBootstrap.errorCode()

    internal fun cloudRecoveryEnabled(): Boolean = BuildConfig.RECOVERY_CLOUD_ENABLED

    internal suspend fun restoreLatestCloudSnapshot(): CloudRecoveryOutcome {
        if (!BuildConfig.RECOVERY_CLOUD_ENABLED) {
            return CloudRecoveryOutcome.Failed("CLOUD_RECOVERY_DISABLED")
        }
        val token = SecureTokenStorage(this).getAuthBearerTokenFlow().first()
        val telemetry = RecoveryTelemetryQueue(this)
        telemetry.record(RecoveryTelemetryType.RECOVERY_SOURCE_SELECTED, RecoveryTelemetryReason.CLOUD)
        if (token.isNullOrBlank()) {
            telemetry.record(
                RecoveryTelemetryType.RECOVERY_FAILED,
                RecoveryTelemetryReason.AUTHORIZATION_REQUIRED,
            )
            return CloudRecoveryOutcome.AuthorizationRequired
        }
        return try {
            RecoverySnapshotRestoreCoordinator(this).restoreLatest(token)
            migrationBootstrapState = migrationBootstrap.prepare(allowRetry = true)
            if (migrationBootstrapState == MigrationBootstrapState.READY) {
                telemetry.record(RecoveryTelemetryType.RECOVERY_SUCCEEDED, RecoveryTelemetryReason.CLOUD)
                startMainGraphAndWorkers()
                CloudRecoveryOutcome.Success
            } else {
                telemetry.record(
                    RecoveryTelemetryType.RECOVERY_FAILED,
                    RecoveryTelemetryReason.VALIDATION_FAILED,
                )
                CloudRecoveryOutcome.Failed(migrationErrorCode() ?: "RESTORED_DATABASE_REJECTED")
            }
        } catch (error: RecoveryCloudHttpException) {
            when (error.statusCode) {
                401, 403 -> {
                    telemetry.record(
                        RecoveryTelemetryType.RECOVERY_FAILED,
                        RecoveryTelemetryReason.AUTHORIZATION_REQUIRED,
                    )
                    CloudRecoveryOutcome.AuthorizationRequired
                }
                404 -> {
                    telemetry.record(
                        RecoveryTelemetryType.RECOVERY_FAILED,
                        RecoveryTelemetryReason.SNAPSHOT_NOT_FOUND,
                    )
                    CloudRecoveryOutcome.SnapshotUnavailable
                }
                else -> {
                    telemetry.record(
                        RecoveryTelemetryType.RECOVERY_FAILED,
                        RecoveryTelemetryReason.NETWORK_OR_SERVER,
                    )
                    CloudRecoveryOutcome.Failed("RECOVERY_HTTP_${error.statusCode}")
                }
            }
        } catch (error: Exception) {
            telemetry.record(
                RecoveryTelemetryType.RECOVERY_FAILED,
                RecoveryTelemetryReason.VALIDATION_FAILED,
            )
            CloudRecoveryOutcome.Failed(error::class.simpleName ?: "CloudRecoveryError")
        }
    }

    private fun startMainGraphAndWorkers() {
        if (!mainGraphStarted.compareAndSet(false, true)) return
        val koinApplication = startKoin {
            androidContext(this@StartApp)
            modules(
                viewModelModule,
                sqlDelightRouteModule,
                sqlDelightSettingsModule,
                repositoryModule,
                useCaseModule,
                resourcesModule,
                updateModule
            )
        }
        // Истёкший bearer-токен ловится на любом запросе (см. SessionExpiredHandler);
        // без этой подписки о 401 узнавал бы только экран «Профиль».
        koinApplication.koin.get<SessionExpiredHandler>().start(appScope)
        runCatching {
            RecoveryTelemetryQueue(this).drainTo(
                diagnostics = koinApplication.koin.get<DiagnosticRepository>(),
                appVersion = BuildConfig.VERSION_NAME,
                appBuild = BuildConfig.VERSION_CODE.toLong(),
            )
        }

        // KEEP гарантирует что дубликат не создастся если задача уже запланирована.
        // Убран блокирующий .get() на главном потоке — вызывал ANR на Android 16.
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodicWorkRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            repeatInterval = 36,
            repeatIntervalTimeUnit = TimeUnit.HOURS
        )
            .setInitialDelay(6, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        val periodicDiagnosticsRequest = PeriodicWorkRequestBuilder<DiagnosticWorker>(
            repeatInterval = 12,
            repeatIntervalTimeUnit = TimeUnit.HOURS,
        )
            .setConstraints(constraints)
            .build()

        val periodicRecoveryRequest = PeriodicWorkRequestBuilder<RecoverySnapshotUploadWorker>(
            repeatInterval = 12,
            repeatIntervalTimeUnit = TimeUnit.HOURS,
        )
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
            .build()

        // UPDATE (вместо KEEP): обновляет параметры задачи при каждом запуске приложения.
        // Это позволяет применять изменения конфигурации (интервал, constraints) после
        // обновления приложения без переустановки. Расписание при этом сохраняется.
        WorkManager.getInstance(this)
            .enqueueUniqueWork(
                "diagnostics_upload_now",
                ExistingWorkPolicy.KEEP,
                OneTimeWorkRequestBuilder<DiagnosticWorker>()
                    .setConstraints(constraints)
                    .build(),
            )
        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork(
                "diagnostics_upload_periodic",
                ExistingPeriodicWorkPolicy.UPDATE,
                periodicDiagnosticsRequest,
            )
        if (BuildConfig.RECOVERY_CLOUD_ENABLED) {
            WorkManager.getInstance(this)
                .enqueueUniqueWork(
                    "recovery_snapshot_upload_now",
                    ExistingWorkPolicy.KEEP,
                    OneTimeWorkRequestBuilder<RecoverySnapshotUploadWorker>()
                        .setConstraints(constraints)
                        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
                        .build(),
                )
            WorkManager.getInstance(this)
                .enqueueUniquePeriodicWork(
                    "recovery_snapshot_upload_periodic",
                    ExistingPeriodicWorkPolicy.UPDATE,
                    periodicRecoveryRequest,
                )
        } else {
            WorkManager.getInstance(this).cancelUniqueWork("recovery_snapshot_upload_now")
            WorkManager.getInstance(this).cancelUniqueWork("recovery_snapshot_upload_periodic")
        }
        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork(
                "sync_work",
                ExistingPeriodicWorkPolicy.UPDATE,
                periodicWorkRequest
            )
    }
}
