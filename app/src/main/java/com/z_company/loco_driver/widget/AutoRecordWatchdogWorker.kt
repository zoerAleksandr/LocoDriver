package com.z_company.loco_driver.widget

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.z_company.route.autorecord.AutoRecordManager
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

/**
 * Периодическая проверка живучести «АВТО» (SCREEN_SPECS §6.2 п.6а): включено, а службы/точек нет —
 * перезапуск или «прервано» с уведомлением. Планируется менеджером, пока АВТО включено.
 */
class AutoRecordWatchdogWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params), KoinComponent {
    override suspend fun doWork(): Result {
        runCatching { get<AutoRecordManager>().checkAlive() }
        return Result.success()
    }
}
