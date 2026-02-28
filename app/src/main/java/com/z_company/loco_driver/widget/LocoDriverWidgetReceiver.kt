package com.z_company.loco_driver.widget

import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.z_company.route.viewmodel.home_view_model.HomeViewModel
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class LocoDriverWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = LocoDriverWidget()

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_GO_CLICKED) {
            // Trigger widget update — the actual onGoClicked logic
            // requires HomeViewModel which is Activity-scoped.
            // Open the app instead so the user can tap play/stop there.
            val launchIntent = context.packageManager
                .getLaunchIntentForPackage(context.packageName)
                ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (launchIntent != null) {
                context.startActivity(launchIntent)
            }
        }
    }

    companion object {
        const val ACTION_GO_CLICKED = "com.z_company.loco_driver.widget.ACTION_GO_CLICKED"
    }
}
