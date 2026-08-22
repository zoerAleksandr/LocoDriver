package com.z_company.loco_driver.widget

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.action.actionParametersOf
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.z_company.loco_driver.MainActivity
import com.z_company.loco_driver.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val SMALL_WIDGET_SIZE_NARROW = DpSize(110.dp, 80.dp)
// Порог заметно выше minWidth: на многих лаунчерах фактическая ширина даже
// дефолтного (нерастянутого) размещения ощутимо больше декларативного
// minWidth/targetCellWidth — если порог слишком низкий, Responsive-хост
// всегда выбирает «широкий» вариант, и кнопка видна даже без растягивания.
private val SMALL_WIDGET_SIZE_WIDE = DpSize(260.dp, 80.dp)

/**
 * Малый виджет (2×2): блок «Отработано / норма за месяц» —
 * mono-метрика, дельта и индикатор нормы. Тап по телу открывает приложение.
 *
 * При растягивании по ширине (см. [SizeMode.Responsive]) в верхнем правом
 * углу поверх контента появляется прямоугольная кнопка перехода: в раздел
 * поезда, если он есть у текущего маршрута; иначе в сам текущий маршрут;
 * иначе — в форму создания нового маршрута (см. [WKeys.SMALL_TARGET_KIND]).
 * Блок нормы при этом всегда занимает всю ширину виджета — кнопка лежит
 * поверх него, а не разъезжается по колонкам.
 */
class LocoDriverSmallWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition
    override val sizeMode: SizeMode =
        SizeMode.Responsive(setOf(SMALL_WIDGET_SIZE_NARROW, SMALL_WIDGET_SIZE_WIDE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        try {
            withContext(Dispatchers.IO) {
                WidgetDataLoader.loadAndPush(context)
            }
        } catch (e: Exception) {
            Log.w("SmallWidget", "Initial data load failed", e)
        }

        provideContent {
            GlanceTheme {
                SmallWidgetContent()
            }
        }
    }

    @Composable
    private fun SmallWidgetContent() {
        val prefs = currentState<Preferences>()
        val context = LocalContext.current
        val isWide = LocalSize.current.width >= SMALL_WIDGET_SIZE_WIDE.width

        val data = NormData(
            workedHm = prefs[WKeys.WORKED_HM] ?: "--:--",
            normHm = prefs[WKeys.NORM_HM] ?: "",
            deltaText = prefs[WKeys.DELTA_TEXT] ?: "",
            isOvertime = prefs[WKeys.IS_OVERTIME] ?: false,
            hasNorm = prefs[WKeys.HAS_NORM] ?: false,
            barMax = prefs[WKeys.BAR_MAX] ?: 100,
            barProgress = prefs[WKeys.BAR_PROGRESS] ?: 0,
            barSecondary = prefs[WKeys.BAR_SECONDARY] ?: 0,
        )

        val rv = RemoteViews(context.packageName, R.layout.widget_small_norm).also {
            WidgetRender.populateNorm(context, it, data, metricSizeSp = 38f)
        }

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(24.dp)
                .background(ImageProvider(R.drawable.widget_card_bg))
                .clickable(actionRunCallback<OpenAppActionCallback>()),
            contentAlignment = Alignment.TopEnd
        ) {
            // Блок нормы всегда на всю ширину/высоту — кнопка перехода (ниже)
            // лежит поверх него как оверлей в углу, а не отъедает место сбоку.
            AndroidRemoteViews(
                remoteViews = rv,
                modifier = GlanceModifier.fillMaxSize()
            )

            // Кнопка перехода — видна только когда виджет растянут по ширине
            // (см. SMALL_WIDGET_SIZE_WIDE), прижата к верхнему правому углу.
            if (isWide) {
                val targetKind = prefs[WKeys.SMALL_TARGET_KIND] ?: "new"
                val targetTrainId = prefs[WKeys.SMALL_TARGET_TRAIN_ID] ?: ""
                val targetBasicId = prefs[WKeys.SMALL_TARGET_BASIC_ID] ?: ""
                // Иконка меняется по тому, куда ведёт кнопка.
                val iconRes = when (targetKind) {
                    "train" -> R.drawable.widget_ic_train_on
                    "route" -> R.drawable.widget_ic_route_on
                    else -> R.drawable.widget_ic_add_on
                }

                // Отступ от края навешен на ЭТОТ обёрточный Box (fillMaxSize +
                // padding сужает область, внутри которой позиционируется
                // кнопка), а не на саму кнопку — padding прямо на кнопке в
                // RemoteViews уходит во внутренний inset вида, а не в отступ
                // от родителя, и кнопка визуально «прилипает» к краю.
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .padding(top = 20.dp, end = 20.dp),
                    contentAlignment = Alignment.TopEnd
                ) {
                    Box(
                        modifier = GlanceModifier
                            .width(68.dp)
                            .height(52.dp)
                            .cornerRadius(16.dp)
                            .background(ImageProvider(R.drawable.widget_small_go_btn_bg))
                            .clickable(
                                actionRunCallback<OpenTrainOrRouteActionCallback>(
                                    actionParametersOf(
                                        TargetKindKey to targetKind,
                                        TargetTrainIdKey to targetTrainId,
                                        TargetBasicIdKey to targetBasicId,
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            provider = ImageProvider(iconRes),
                            contentDescription = "Открыть",
                            modifier = GlanceModifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }

    companion object {
        suspend fun updateAllSmallWidgets(
            context: Context,
            data: NormData,
            targetKind: String = "new",
            targetTrainId: String = "",
            targetBasicId: String = "",
        ) {
            val manager = GlanceAppWidgetManager(context)
            val glanceIds = manager.getGlanceIds(LocoDriverSmallWidget::class.java)
            glanceIds.forEach { glanceId ->
                updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                    prefs.toMutablePreferences().apply {
                        this[WKeys.WORKED_HM] = data.workedHm
                        this[WKeys.NORM_HM] = data.normHm
                        this[WKeys.DELTA_TEXT] = data.deltaText
                        this[WKeys.IS_OVERTIME] = data.isOvertime
                        this[WKeys.HAS_NORM] = data.hasNorm
                        this[WKeys.BAR_MAX] = data.barMax
                        this[WKeys.BAR_PROGRESS] = data.barProgress
                        this[WKeys.BAR_SECONDARY] = data.barSecondary
                        this[WKeys.SMALL_TARGET_KIND] = targetKind
                        this[WKeys.SMALL_TARGET_TRAIN_ID] = targetTrainId
                        this[WKeys.SMALL_TARGET_BASIC_ID] = targetBasicId
                    }
                }
                LocoDriverSmallWidget().update(context, glanceId)
            }
        }
    }
}

private val TargetKindKey = ActionParameters.Key<String>("target_kind")
private val TargetTrainIdKey = ActionParameters.Key<String>("target_train_id")
private val TargetBasicIdKey = ActionParameters.Key<String>("target_basic_id")

/**
 * ActionCallback кнопки перехода малого виджета (видна при растягивании по
 * ширине). Ведёт по приоритету: раздел поезда → текущий маршрут → форма
 * нового маршрута (см. [WidgetDataLoader.loadAndPush], поле `targetKind`).
 */
class OpenTrainOrRouteActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val kind = parameters[TargetKindKey] ?: "new"
        val trainId = parameters[TargetTrainIdKey] ?: ""
        val basicId = parameters[TargetBasicIdKey] ?: ""

        val intent = Intent(context, MainActivity::class.java).apply {
            when {
                kind == "train" && trainId.isNotEmpty() && basicId.isNotEmpty() -> {
                    putExtra("widget_train_id", trainId)
                    putExtra("widget_basic_id", basicId)
                }
                kind == "route" && basicId.isNotEmpty() -> {
                    putExtra("widget_route_id", basicId)
                }
                else -> putExtra("widget_add_route", true)
            }
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        context.startActivity(intent)
    }
}
