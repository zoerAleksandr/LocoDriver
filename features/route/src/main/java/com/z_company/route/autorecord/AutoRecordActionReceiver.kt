package com.z_company.route.autorecord

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.koin.core.context.GlobalContext

/** Действие «Выключить» из уведомлений АВТО, не связанных с foreground service. */
class AutoRecordActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_DISABLE) return
        runCatching { GlobalContext.getOrNull()?.get<AutoRecordManager>()?.disable() }
    }

    companion object {
        const val ACTION_DISABLE = "com.z_company.route.autorecord.DISABLE"
    }
}
