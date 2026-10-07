package com.z_company.loco_driver.track

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.z_company.domain.track.TrackPacking
import com.z_company.repository.SecureTokenStorage
import com.z_company.repository.remote_rest.tracks.TrackUploader
import com.z_company.route.autorecord.TrackRouteResolver
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.TimeUnit

/**
 * Фоновая отправка треков на сервер (SCREEN_SPECS §6.9): после сеанса «АВТО», при старте приложения,
 * при смене согласия №1. Только при сети; сбои повторяются с экспоненциальной паузой. Без входа в аккаунт
 * и без согласия ничего не отправляет. Координаты не логируются.
 */
class TrackUploadWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params), KoinComponent {

    private val uploader: TrackUploader by inject()
    private val resolver: TrackRouteResolver by inject()
    private val tokenStorage: SecureTokenStorage by inject()

    override suspend fun doWork(): Result {
        // Сироты — до отправки: их удаление ставится в очередь и доставляется этим же прогоном.
        runCatching { resolver.cleanOrphans() }
        val token = tokenStorage.getAuthBearerTokenFlow().first()
        if (token.isNullOrBlank()) return Result.success()
        val report = uploader.run("Bearer $token")
        if (report.waitingForQuiet) {
            TrackUploadScheduler.enqueue(applicationContext, delayMs = TrackPacking.QUIET_MS + 30_000L, later = true)
        }
        return when (report.outcome) {
            TrackUploader.Outcome.RETRY_LATER ->
                if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.success()
            else -> Result.success()
        }
    }

    private companion object {
        const val MAX_ATTEMPTS = 8
    }
}

/** Постановка [TrackUploadWorker] в WorkManager. */
object TrackUploadScheduler {
    private const val NOW = "track_upload"
    private const val LATER = "track_upload_later"

    /**
     * @param delayMs пауза перед запуском (ждём, пока трек «затихнет»)
     * @param later отдельное имя для отложенного запуска: не отменяет воркер, из которого вызван
     */
    fun enqueue(context: Context, delayMs: Long = 0L, later: Boolean = false) {
        val request = OneTimeWorkRequestBuilder<TrackUploadWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
            .apply { if (delayMs > 0) setInitialDelay(delayMs, TimeUnit.MILLISECONDS) }
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            if (later) LATER else NOW,
            if (later) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP,
            request,
        )
    }
}
