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
import com.z_company.repository.SecureTokenStorage
import com.z_company.repository.remote_rest.tracks.AltitudeContributor
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.TimeUnit

/**
 * Фоновая отправка обезличенных вкладов высот (согласие №2, SCREEN_SPECS §6.9): после сеанса «АВТО» и при старте
 * приложения. Только при сети; сбои повторяются с экспоненциальной паузой. Без входа в аккаунт и без согласия
 * №2 ничего не отправляет. Координаты не логируются.
 */
class AltitudeContribWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params), KoinComponent {

    private val contributor: AltitudeContributor by inject()
    private val tokenStorage: SecureTokenStorage by inject()

    override suspend fun doWork(): Result {
        val token = tokenStorage.getAuthBearerTokenFlow().first()
        // Без входа отправлять нечего (точки остаются до недели); отозванное согласие обрабатывает сам contributor.
        if (token.isNullOrBlank()) return Result.success()
        val report = contributor.run("Bearer $token")
        return when (report.outcome) {
            AltitudeContributor.Outcome.RETRY_LATER ->
                if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.success()
            else -> Result.success()
        }
    }

    private companion object {
        const val MAX_ATTEMPTS = 8
    }
}

/** Постановка [AltitudeContribWorker] в WorkManager. */
object AltitudeContribScheduler {
    private const val NAME = "altitude_contrib"

    fun enqueue(context: Context) {
        val request = OneTimeWorkRequestBuilder<AltitudeContribWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(NAME, ExistingWorkPolicy.KEEP, request)
    }

    /** Согласие №2 отозвано: отменить ожидающую отправку. */
    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(NAME)
    }
}
