package com.z_company.loco_driver.widget

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.z_company.loco_driver.R
import kotlin.math.ceil

/**
 * Ключи Glance-состояния для виджетов и хелперы построения RemoteViews.
 * Дизайн — из design/src/widgets.jsx: карточка, mono-метрика, индикатор нормы,
 * блок текущего маршрута с быстрой записью прибытия/отправления.
 *
 * Цвета, зависящие от темы, резолвятся ресурсами @color (values / values-night)
 * автоматически на стороне хоста для обычных XML-атрибутов. Цвета, которые
 * выставляются в рантайме (Bitmap-числа, знак дельты) — через
 * `RemoteViews.setColorInt(..., notNight, night)`, см. [WidgetRender.forcedNightColor].
 */
object WKeys {
    // ─── Блок нормы (общий для малого и развёрнутого) ───
    val WORKED_HM = stringPreferencesKey("w_worked_hm")          // "187:20"
    val NORM_HM = stringPreferencesKey("w_norm_hm")              // "176:00"
    val DELTA_TEXT = stringPreferencesKey("w_delta_text")        // "+11:20" / "−11:20"
    val IS_OVERTIME = booleanPreferencesKey("w_is_overtime")
    val HAS_NORM = booleanPreferencesKey("w_has_norm")
    val BAR_MAX = intPreferencesKey("w_bar_max")
    val BAR_PROGRESS = intPreferencesKey("w_bar_progress")
    val BAR_SECONDARY = intPreferencesKey("w_bar_secondary")

    // ─── Блок текущего маршрута (только развёрнутый) ───
    val HAS_CURRENT_ROUTE = booleanPreferencesKey("w_has_route")
    val ROUTE_NUMBER = stringPreferencesKey("w_route_number")    // "№147" / ""
    val STATION_FROM = stringPreferencesKey("w_station_from")
    val STATION_TO = stringPreferencesKey("w_station_to")
    val TRAIN_LINE = stringPreferencesKey("w_train_line")        // "№2654 · КАНСК..."
    val IS_DEPARTURE_NEXT = booleanPreferencesKey("w_is_departure_next")
    val STAMP_DONE_TIME = stringPreferencesKey("w_stamp_done_time") // "23:48" / ""
    // Поезд ещё не добавлен, ИЛИ уже записано прибытие на конечную станцию
    // плеча — кнопкам «Прибытие/Отправление» уже нечего делать.
    val SHOW_STAMP_BUTTONS = booleanPreferencesKey("w_show_stamp_buttons")
    // Поезд добавлен И у него есть хотя бы одна станция. Поезд без станций
    // равнозначен «поезда нет» — показывать по нему нечего.
    val CURRENT_HAS_TRAIN = booleanPreferencesKey("w_current_has_train")
    val CURRENT_REPORT_TIME = stringPreferencesKey("w_current_report_time") // явка, "20:00"
    val CURRENT_REPORT_DATE = stringPreferencesKey("w_current_report_date") // "16.07.26"
    // id текущего маршрута — для перехода по тапу на блок «ТЕКУЩИЙ МАРШРУТ»
    val CURRENT_ROUTE_ID = stringPreferencesKey("w_current_route_id")

    // ─── Состояние нижнего блока развёрнутого виджета ───
    // "current" | "upcoming" | "upcoming_unknown" | "rest" | "home_rest" | "none"
    val STATE = stringPreferencesKey("w_state")

    // upcoming — следующий маршрут
    val UP_TIME = stringPreferencesKey("w_up_time")       // "20:00"
    val UP_DATE = stringPreferencesKey("w_up_date")       // "16.07.26"
    val UP_NUMBER = stringPreferencesKey("w_up_number")   // "№148" / ""
    val UP_FROM = stringPreferencesKey("w_up_from")
    val UP_TO = stringPreferencesKey("w_up_to")

    // rest — отдых в ПО
    val REST_SHORT_DUR = stringPreferencesKey("w_rest_short_dur")  // "3ч"
    val REST_SHORT_END = stringPreferencesKey("w_rest_short_end")  // "до 25.04 15:16"
    val REST_FULL_DUR = stringPreferencesKey("w_rest_full_dur")
    val REST_FULL_END = stringPreferencesKey("w_rest_full_end")

    // home_rest — домашний отдых
    val HR_DUR = stringPreferencesKey("w_hr_dur")         // "16ч 15м"
    val HR_END = stringPreferencesKey("w_hr_end")         // "16.07 · 18:45"
    val HR_MIN_END = stringPreferencesKey("w_hr_min_end") // "16.07 · 12:45"

    // ─── Малый виджет: кнопка перехода при растягивании по ширине ───
    // "train" (есть поезд) | "route" (есть маршрут без поезда) | "new" (маршрута нет).
    val SMALL_TARGET_KIND = stringPreferencesKey("w_small_target_kind")
    val SMALL_TARGET_TRAIN_ID = stringPreferencesKey("w_small_target_train_id")
    val SMALL_TARGET_BASIC_ID = stringPreferencesKey("w_small_target_basic_id")
}

/** Данные блока нормы. */
data class NormData(
    val workedHm: String,
    val normHm: String,
    val deltaText: String,
    val isOvertime: Boolean,
    val hasNorm: Boolean,
    val barMax: Int,
    val barProgress: Int,
    val barSecondary: Int,
)

object WidgetRender {

    // Кастомные шрифты (@font) не применяются в RemoteViews-виджетах, поэтому
    // крупные mono-числа рисуем в Bitmap реальным JetBrains Mono — как на
    // главном экране приложения.
    private val typefaceCache = HashMap<Int, Typeface>()

    private fun mono(context: Context, fontRes: Int): Typeface =
        typefaceCache.getOrPut(fontRes) {
            ResourcesCompat.getFont(context, fontRes) ?: Typeface.DEFAULT_BOLD
        }

    /**
     * `MainActivity.applyApplicationNightMode()` красит ВЕСЬ процесс приложения
     * (`UiModeManager.setApplicationNightMode`, API 31+) под ВЫБРАННУЮ в
     * приложении тему, и это сохраняется на уровне ОС между запусками. Поэтому
     * обычный `ContextCompat.getColor(context, ...)` в этом процессе может
     * резолвить цвет по теме приложения, а не по теме устройства — виджет же
     * должен краситься под тему домашнего экрана независимо от того, что
     * выбрано в самом приложении.
     *
     * Обойти это напрямую (узнать «текущую реальную» тему устройства изнутри
     * процесса) — ненадёжно: `Resources.getSystem()` не отслеживает live-тему
     * (у неё всегда пустой/неопределённый uiMode, отсюда прошлый баг — виджет
     * стал всегда светлым). Правильный путь — не гадать «какая тема сейчас»,
     * а форсированно резолвить ОБА варианта цвета (light и dark), передать их
     * оба через `RemoteViews.setColorInt(viewId, method, notNight, night)`
     * (API 31+) и дать ХОСТУ (лаунчеру) выбрать нужный по СВОЕЙ, настоящей
     * Configuration — она этим оверрайдом не затронута.
     * `createConfigurationContext` с форсированным uiMode резолвит ресурс
     * детерминированно, независимо от текущего (возможно испорченного)
     * состояния `context`.
     */
    private fun forcedNightColor(context: Context, colorRes: Int, night: Boolean): Int {
        val bit = if (night) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
        val config = Configuration(context.resources.configuration).apply {
            uiMode = bit or (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv())
        }
        return ContextCompat.getColor(context.createConfigurationContext(config), colorRes)
    }

    /** Красит TextView под реальную тему устройства (см. [forcedNightColor]). */
    private fun setThemedTextColor(context: Context, rv: RemoteViews, viewId: Int, colorRes: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            rv.setColorInt(
                viewId, "setTextColor",
                forcedNightColor(context, colorRes, night = false),
                forcedNightColor(context, colorRes, night = true),
            )
        } else {
            // До API 31 `setApplicationNightMode` не вызывается (гейт в
            // MainActivity), процесс не перекрашен — прямой резолв безопасен.
            rv.setTextColor(viewId, ContextCompat.getColor(context, colorRes))
        }
    }

    /** Рендерит mono-число в Bitmap. fontRes — вес (extrabold/bold/semibold). */
    fun renderMonoBitmap(
        context: Context,
        text: String,
        sizeSp: Float,
        colorRes: Int,
        fontRes: Int = R.font.jetbrains_mono_extrabold,
    ): Bitmap {
        val density = context.resources.displayMetrics.density
        // На API 31+ красим глиф нейтральным непрозрачным цветом — важна
        // только альфа (форма), реальный цвет накладывается позже через
        // setColorInt/setColorFilter (см. bindMonoBitmap), по теме ХОСТА.
        // На API < 31 риска перекраса процесса нет — резолвим сразу.
        val color = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Color.WHITE
        } else {
            ContextCompat.getColor(context, colorRes)
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = mono(context, fontRes)
            textSize = sizeSp * density
            this.color = color
            letterSpacing = -0.03f
        }
        val fm = paint.fontMetrics
        val w = ceil(paint.measureText(text)).toInt().coerceAtLeast(1)
        val h = ceil(fm.descent - fm.ascent).toInt().coerceAtLeast(1)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        Canvas(bmp).drawText(text, 0f, -fm.ascent, paint)
        return bmp
    }

    /**
     * Ставит mono-Bitmap в ImageView и красит его под реальную тему устройства
     * (см. [renderMonoBitmap] / [forcedNightColor]).
     */
    private fun bindMonoBitmap(
        context: Context,
        rv: RemoteViews,
        viewId: Int,
        text: String,
        sizeSp: Float,
        colorRes: Int,
        fontRes: Int = R.font.jetbrains_mono_extrabold,
    ) {
        rv.setImageViewBitmap(viewId, renderMonoBitmap(context, text, sizeSp, colorRes, fontRes))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            rv.setColorInt(
                viewId, "setColorFilter",
                forcedNightColor(context, colorRes, night = false),
                forcedNightColor(context, colorRes, night = true),
            )
        }
    }

    /**
     * Заполняет layout блока нормы (widget_small_norm / widget_norm_block).
     * Оба layout'а содержат одинаковые id: metric_image, delta_text, delta_label,
     * norm_bar, norm_value.
     * @param metricSizeSp размер отработанного времени (44 — развёрнутый, 38 — малый).
     */
    fun populateNorm(context: Context, rv: RemoteViews, d: NormData, metricSizeSp: Float) {
        bindMonoBitmap(
            context, rv, R.id.metric_image,
            d.workedHm, metricSizeSp, R.color.widget_text, R.font.jetbrains_mono_extrabold
        )
        rv.setTextViewText(R.id.norm_value, d.normHm)

        if (d.hasNorm) {
            rv.setViewVisibility(R.id.delta_text, View.VISIBLE)
            rv.setViewVisibility(R.id.delta_label, View.VISIBLE)
            rv.setTextViewText(R.id.delta_text, d.deltaText)
            setThemedTextColor(
                context, rv, R.id.delta_text,
                if (d.isOvertime) R.color.widget_warning else R.color.widget_success
            )
            rv.setTextViewText(
                R.id.delta_label,
                if (d.isOvertime) "сверх нормы" else "до нормы"
            )
        } else {
            rv.setViewVisibility(R.id.delta_text, View.GONE)
            rv.setViewVisibility(R.id.delta_label, View.GONE)
        }

        val max = if (d.barMax > 0) d.barMax else 100
        rv.setProgressBar(R.id.norm_bar, max, d.barProgress, false)
        rv.setInt(R.id.norm_bar, "setSecondaryProgress", d.barSecondary)
    }

    /**
     * Заполняет хедер текущего маршрута (widget_route_header).
     * @param showCaption показывать строку «ТЕКУЩИЙ МАРШРУТ» + номер.
     * `false`, когда под хедером ещё показаны кнопки записи — так
     * экономится высота, а номер маршрута и так виден в приложении.
     */
    fun populateRouteHeader(
        rv: RemoteViews,
        routeNumber: String,
        from: String,
        to: String,
        trainLine: String,
        showCaption: Boolean = true,
    ) {
        if (showCaption) {
            rv.setViewVisibility(R.id.header_caption_row, View.VISIBLE)
            if (routeNumber.isNotEmpty()) {
                rv.setViewVisibility(R.id.route_number, View.VISIBLE)
                rv.setTextViewText(R.id.route_number, routeNumber)
            } else {
                rv.setViewVisibility(R.id.route_number, View.GONE)
            }
        } else {
            rv.setViewVisibility(R.id.header_caption_row, View.GONE)
        }
        // Направление: скрываем целиком, если станции неизвестны.
        if (from.isEmpty() && to.isEmpty()) {
            rv.setViewVisibility(R.id.route_direction, View.GONE)
        } else {
            rv.setViewVisibility(R.id.route_direction, View.VISIBLE)
            rv.setTextViewText(R.id.station_from, from)
            rv.setTextViewText(R.id.station_to, to)
            val hasTo = to.isNotEmpty()
            rv.setViewVisibility(R.id.route_arrow, if (hasTo) View.VISIBLE else View.GONE)
            rv.setViewVisibility(R.id.station_to, if (hasTo) View.VISIBLE else View.GONE)
        }
        if (trainLine.isNotEmpty()) {
            rv.setViewVisibility(R.id.train_line, View.VISIBLE)
            rv.setTextViewText(R.id.train_line, trainLine)
        } else {
            rv.setViewVisibility(R.id.train_line, View.GONE)
        }
    }

    /**
     * Блок явки (widget_upcoming) — «Следующий маршрут» либо, с
     * `caption = "ТЕКУЩИЙ МАРШРУТ"`, текущий маршрут без добавленного поезда
     * (когда показать нечего, кроме номера и времени/даты явки).
     */
    fun buildUpcoming(
        context: Context,
        number: String, time: String, date: String,
        from: String, to: String, known: Boolean,
        caption: String = "СЛЕДУЮЩИЙ МАРШРУТ",
    ): RemoteViews {
        val rv = RemoteViews(context.packageName, R.layout.widget_upcoming)
        rv.setTextViewText(R.id.up_caption, caption)
        if (number.isNotEmpty()) {
            rv.setViewVisibility(R.id.up_number, View.VISIBLE)
            rv.setTextViewText(R.id.up_number, number)
        } else {
            rv.setViewVisibility(R.id.up_number, View.GONE)
        }
        bindMonoBitmap(context, rv, R.id.up_time_image, time, 30f, R.color.widget_text)
        rv.setTextViewText(R.id.up_date, date)
        if (known && (from.isNotEmpty() || to.isNotEmpty())) {
            rv.setViewVisibility(R.id.up_path_section, View.VISIBLE)
            rv.setTextViewText(R.id.up_from, from)
            rv.setTextViewText(R.id.up_to, to)
        } else {
            rv.setViewVisibility(R.id.up_path_section, View.GONE)
        }
        return rv
    }

    /** Состояние «Отдых в ПО» (widget_rest_po). */
    fun buildRestPo(
        context: Context,
        shortDur: String, shortEnd: String,
        fullDur: String, fullEnd: String,
    ): RemoteViews {
        val rv = RemoteViews(context.packageName, R.layout.widget_rest_po)
        bindMonoBitmap(context, rv, R.id.rest_short_dur, shortDur, 24f, R.color.widget_text)
        rv.setTextViewText(R.id.rest_short_end, shortEnd)
        bindMonoBitmap(context, rv, R.id.rest_full_dur, fullDur, 24f, R.color.widget_text)
        rv.setTextViewText(R.id.rest_full_end, fullEnd)
        return rv
    }

    /** Состояние «Домашний отдых» (widget_home_rest). Кнопка — в Glance. */
    fun buildHomeRest(context: Context, dur: String, end: String, minEnd: String): RemoteViews {
        val rv = RemoteViews(context.packageName, R.layout.widget_home_rest)
        bindMonoBitmap(context, rv, R.id.hr_duration, dur, 26f, R.color.widget_text)
        rv.setTextViewText(R.id.hr_end, end)
        rv.setTextViewText(R.id.hr_min_end, minEnd)
        return rv
    }

    /** Заполняет блок состояния «без маршрута» (widget_rest_state). */
    fun populateRestState(
        rv: RemoteViews,
        line1: String, line2: String, line3: String,
        line4: String, line5: String, nextReport: String,
    ) {
        fun bind(id: Int, text: String) {
            if (text.isNotEmpty()) {
                rv.setViewVisibility(id, View.VISIBLE)
                rv.setTextViewText(id, text)
            } else {
                rv.setViewVisibility(id, View.GONE)
            }
        }
        bind(R.id.state_line1, line1)
        bind(R.id.state_line2, line2)
        bind(R.id.state_line3, line3)
        bind(R.id.state_line4, line4)
        bind(R.id.state_line5, line5)
        bind(R.id.next_report, nextReport)
    }

    /**
     * Строит RemoteViews кнопки быстрой записи.
     * @param isActive true → «Записать» (следующее действие); false → записанное время.
     * @param isDeparture true → «Отправление» (стрелка вверх); false → «Прибытие».
     * @param doneTime "HH:MM" записанного события (для неактивной кнопки).
     */
    fun stampButton(
        context: Context,
        isActive: Boolean,
        isDeparture: Boolean,
        doneTime: String,
    ): RemoteViews {
        val rv = RemoteViews(context.packageName, R.layout.widget_stamp_button)

        rv.setInt(
            R.id.stamp_root, "setBackgroundResource",
            if (isActive) R.drawable.widget_stamp_active_bg else R.drawable.widget_stamp_done_bg
        )
        rv.setInt(
            R.id.stamp_icon_box, "setBackgroundResource",
            if (isActive) R.drawable.widget_stamp_icon_pending else R.drawable.widget_stamp_icon_done
        )
        val iconRes = when {
            isDeparture && isActive -> R.drawable.widget_ic_arrow_up
            isDeparture && !isActive -> R.drawable.widget_ic_arrow_up_on
            !isDeparture && isActive -> R.drawable.widget_ic_arrow_down
            else -> R.drawable.widget_ic_arrow_down_on
        }
        rv.setImageViewResource(R.id.stamp_icon, iconRes)
        rv.setTextViewText(R.id.stamp_label, if (isDeparture) "Отправление" else "Прибытие")

        if (isActive) {
            rv.setViewVisibility(R.id.stamp_action, View.VISIBLE)
            rv.setViewVisibility(R.id.stamp_done_row, View.GONE)
        } else {
            rv.setViewVisibility(R.id.stamp_action, View.GONE)
            rv.setViewVisibility(R.id.stamp_done_row, View.VISIBLE)
            rv.setTextViewText(R.id.stamp_time, doneTime.ifEmpty { "—:—" })
        }
        return rv
    }
}
