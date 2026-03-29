package com.z_company.loco_driver.widget

import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class LocoDriverWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = LocoDriverWidget()

    private val scope = MainScope()

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        // После обновления APK старые PendingIntent-ы не содержат EXTRA_CALLBACK_CLASS —
        // ActionTrampolineActivity крашится с "List adapter activity trampoline invoked
        // without specifying target intent". Принудительно обновляем виджет, чтобы
        // Glance пересоздал свежие PendingIntent-ы.
        if (intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            scope.launch {
                glanceAppWidget.updateAll(context)
            }
        }
    }
}
