package com.z_company.loco_driver.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.z_company.loco_driver.MainActivity
import com.z_company.loco_driver.R
import com.z_company.route.autorecord.AutoRecordManager
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

/**
 * Кнопка «АВТО» в виджетах (SCREEN_SPECS §6.2 п.6а). Состояние читается из SharedPreferences
 * менеджера при каждой отрисовке; менеджер обновляет виджеты при смене состояния.
 * Пульсации в RemoteViews нет — точка статичная. Времени окончания на кнопке нет.
 */

/** Кнопка рядом с кнопками записи (развёрнутый виджет). [wide] — отдельная строка на всю ширину. */
@Composable
fun AutoButton(modifier: GlanceModifier, wide: Boolean = false) {
    val context = LocalContext.current
    val until = AutoRecordManager.savedUntil(context)
    val on = until > 0
    Box(
        modifier = modifier
            .height(if (wide) 40.dp else 52.dp)
            .cornerRadius(14.dp)
            .background(ImageProvider(if (on) R.drawable.widget_auto_btn_on else R.drawable.widget_auto_btn_off))
            .clickable(actionRunCallback<AutoToggleActionCallback>()),
        contentAlignment = Alignment.Center,
    ) {
        // Цвета из ресурсов (values / values-night): включено — зелёный, выключено — серый.
        val fg = ColorProvider(if (on) R.color.widget_auto_on_fg else R.color.widget_auto_off_fg)
        Text(
            text = "АВТО",
            style = TextStyle(color = fg, fontWeight = FontWeight.Bold, fontSize = 13.sp, textAlign = TextAlign.Center),
        )
    }
}

/** Точка-индикатор малого виджета; тап переключает. */
@Composable
fun AutoDot(modifier: GlanceModifier) {
    val context = LocalContext.current
    val on = AutoRecordManager.savedUntil(context) > 0
    Box(
        modifier = modifier.clickable(actionRunCallback<AutoToggleActionCallback>()),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = GlanceModifier
                .size(14.dp)
                .background(ImageProvider(if (on) R.drawable.widget_auto_dot_on else R.drawable.widget_auto_dot_off)),
        ) {}
    }
}

/**
 * Тап по «АВТО» в виджете: выключение — сразу; включение — через приложение (службу геолокации
 * нельзя стартовать из фона), MainActivity включает АВТО сам (extra `widget_auto_enable`).
 */
class AutoToggleActionCallback : ActionCallback, KoinComponent {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        if (AutoRecordManager.savedUntil(context) > 0) {
            get<AutoRecordManager>().disable()
        } else {
            context.startActivity(
                Intent(context, MainActivity::class.java).apply {
                    putExtra("widget_auto_enable", true)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                },
            )
        }
    }
}
