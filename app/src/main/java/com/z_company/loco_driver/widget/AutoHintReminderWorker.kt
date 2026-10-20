package com.z_company.loco_driver.widget

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.z_company.domain.autorecord.TrainOrdering
import com.z_company.domain.onboarding.AutoReminderRules
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.loco_driver.MainActivity
import com.z_company.route.autorecord.AutoRecordManager
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import java.util.TimeZone

/**
 * Мягкое напоминание про «АВТО» (docs/ONBOARDING_MATERIALS.md §4, SCREEN_SPECS §6.9): раз в сутки проверяет правило
 * [AutoReminderRules] и, если пользователь неделю ездит без «АВТО», показывает одно уведомление - не чаще раза в 30 дней.
 * Уведомление идёт в своём канале (его можно отключить в системных настройках); без разрешения на уведомления
 * не показывается и не засчитывается. Нажатие открывает последний поезд.
 */
class AutoHintReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params), KoinComponent {

    override suspend fun doWork(): Result {
        runCatching { remindIfNeeded() }
        return Result.success()
    }

    private fun remindIfNeeded() {
        val ctx = applicationContext
        val now = System.currentTimeMillis()
        val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val manager: AutoRecordManager = get()
        val routes = get<RouteUseCase>().getListRoutes().filter { !it.basicData.isDeleted }
        val starts = routes.mapNotNull { it.basicData.timeStartWork }
        val offset = TimeZone.getDefault().getOffset(now).toLong()
        val should = AutoReminderRules.shouldRemind(
            nowMs = now,
            lastRemindedMs = prefs.getLong(KEY_LAST_REMINDED, 0L),
            lastAutoEnabledMs = AutoRecordManager.lastEnabledMs(ctx),
            tripStartsMs = starts,
            autoActive = manager.state.value.enabled,
            zoneOffsetMs = offset,
        )
        if (!should) return
        if (!NotificationManagerCompat.from(ctx).areNotificationsEnabled()) return

        // Последний поезд: самый поздний по началу работы рейс, где есть поезда.
        val lastTrain = routes.filter { it.trains.isNotEmpty() }
            .maxByOrNull { it.basicData.timeStartWork ?: 0L }
            ?.let { TrainOrdering.pickAutoTarget(it.trains) }

        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, "Подсказки про «АВТО»", NotificationManager.IMPORTANCE_LOW),
            )
        }
        val intent = Intent(ctx, MainActivity::class.java).apply {
            if (lastTrain != null) {
                putExtra("widget_train_id", lastTrain.trainId)
                putExtra("widget_basic_id", lastTrain.basicId)
            }
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            ctx, REQUEST_CODE, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(com.z_company.route.R.drawable.ic_stat_auto)
            .setContentTitle(AutoReminderRules.TITLE)
            .setContentText(AutoReminderRules.TEXT)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()
        // Без разрешения (Android 13+) notify молча не сработает; срок засчитываем только при успешной постановке.
        if (runCatching { nm.notify(NOTIFICATION_ID, notification) }.isSuccess) {
            prefs.edit().putLong(KEY_LAST_REMINDED, now).apply()
        }
    }

    companion object {
        const val UNIQUE_NAME = "auto_hint_reminder"
        private const val PREFS_NAME = "onboarding_reminder"
        private const val KEY_LAST_REMINDED = "auto_hint_last_ms"
        private const val CHANNEL = "auto_hint"
        private const val NOTIFICATION_ID = 7410
        private const val REQUEST_CODE = 7410
    }
}
