package com.z_company.route.component.servicephase

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.core.graphics.PathParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Значок текущего положения на карте поезда (SCREEN_SPECS §6.9 и §17.5): вид сверху, «нос» вверх, на карте
 * поворачивается по курсу. Векторные контуры в сетке 24×24: белая заливка корпуса с тёмным контуром и белым
 * ореолом читается на светлой и тёмной подложке. Стрелка (по умолчанию) — синяя с белой обводкой.
 */
enum class PositionIcon(val key: String, val label: String, internal val shapes: List<IconShape>) {
    ARROW(
        "arrow", "Стрелка",
        listOf(IconShape("M12 2 L19.5 21.5 L12 17 L4.5 21.5 Z", IconStyle.ACCENT)),
    ),
    STEAM(
        "steam", "Паровоз",
        listOf(
            // котёл с передним скотосбрасывателем, будка и тендер
            IconShape("M12 1.5 L8.8 4.8 H15.2 Z", IconStyle.BODY),
            IconShape("M8.8 4.8 H15.2 V14 H8.8 Z", IconStyle.BODY),
            IconShape("M7.5 14 H16.5 V18 H7.5 Z", IconStyle.BODY),
            IconShape("M8.5 18.8 H15.5 V23 H8.5 Z", IconStyle.BODY),
            IconShape("M12 8 m-1.4 0 a1.4 1.4 0 1 0 2.8 0 a1.4 1.4 0 1 0 -2.8 0", IconStyle.LINE),
            IconShape("M8.8 11 H15.2 M9.4 16 H14.6", IconStyle.LINE),
        ),
    ),
    DIESEL(
        "diesel", "Тепловоз",
        listOf(
            IconShape("M8 4.5 Q8 1.8 10.7 1.8 H13.3 Q16 1.8 16 4.5 V19.5 Q16 22.2 13.3 22.2 H10.7 Q8 22.2 8 19.5 Z", IconStyle.BODY),
            IconShape("M9.2 6.2 H14.8 M9.2 18.2 H14.8", IconStyle.LINE),
            IconShape("M12 10.8 m-2 0 a2 2 0 1 0 4 0 a2 2 0 1 0 -4 0", IconStyle.LINE),
            IconShape("M12 15 m-2 0 a2 2 0 1 0 4 0 a2 2 0 1 0 -4 0", IconStyle.LINE),
        ),
    ),
    ELECTRIC(
        "electric", "Электровоз",
        listOf(
            IconShape("M8 4.5 Q8 1.8 10.7 1.8 H13.3 Q16 1.8 16 4.5 V19.5 Q16 22.2 13.3 22.2 H10.7 Q8 22.2 8 19.5 Z", IconStyle.BODY),
            IconShape("M9.2 6.2 H14.8 M9.2 18.2 H14.8", IconStyle.LINE),
            // токоприёмник (пантограф)
            IconShape("M9.4 12 L12 9.2 L14.6 12 L12 14.8 Z M9 12 H15", IconStyle.LINE),
        ),
    ),
    EMU(
        "emu", "Электропоезд",
        listOf(
            IconShape("M9 3.5 Q9 1.5 11 1.5 H13 Q15 1.5 15 3.5 V8.2 H9 Z", IconStyle.BODY),
            IconShape("M9 9.2 H15 V14.8 H9 Z", IconStyle.BODY),
            IconShape("M9 15.8 H15 V20.5 Q15 22.5 13 22.5 H11 Q9 22.5 9 20.5 Z", IconStyle.BODY),
            IconShape("M10 4.4 H14 M10 19.6 H14", IconStyle.LINE),
            IconShape("M10.2 12 L12 10.4 L13.8 12 L12 13.6 Z", IconStyle.LINE),
        ),
    ),
    COACH(
        "coach", "Пассажирский вагон",
        listOf(
            IconShape("M8.8 2 H15.2 V22 H8.8 Z", IconStyle.BODY),
            // окна по бокам
            IconShape("M10.4 4 V6.6 M13.6 4 V6.6 M10.4 8.4 V11 M13.6 8.4 V11 M10.4 12.8 V15.4 M13.6 12.8 V15.4 M10.4 17.2 V19.8 M13.6 17.2 V19.8", IconStyle.LINE),
            IconShape("M12 1 V2 M12 22 V23", IconStyle.LINE),
        ),
    ),
    FREIGHT(
        "freight", "Грузовой состав",
        listOf(
            // локомотив впереди и два вагона
            IconShape("M8.5 4 Q8.5 1.6 10.9 1.6 H13.1 Q15.5 1.6 15.5 4 V7.8 H8.5 Z", IconStyle.BODY),
            IconShape("M8.5 9.4 H15.5 V15.4 H8.5 Z", IconStyle.BODY),
            IconShape("M8.5 17 H15.5 V23 H8.5 Z", IconStyle.BODY),
            IconShape("M12 8.6 V9.4 M12 15.4 V17", IconStyle.LINE),
            IconShape("M9.4 4.3 H14.6", IconStyle.LINE),
            IconShape("M9.5 10.4 L14.5 14.4 M14.5 10.4 L9.5 14.4 M9.5 18 L14.5 22 M14.5 18 L9.5 22", IconStyle.LINE),
        ),
    );

    companion object {
        fun fromKey(key: String?): PositionIcon = entries.firstOrNull { it.key == key } ?: ARROW
    }
}

internal enum class IconStyle { ACCENT, BODY, LINE }

internal class IconShape(val d: String, val style: IconStyle)

/** Выбранный значок положения: хранится локально на устройстве (SharedPreferences), на сервер не уходит. */
object PositionIconStore {
    private const val PREFS = "train_map_prefs"
    private const val KEY = "position_icon"

    private val _selected = MutableStateFlow(PositionIcon.ARROW)
    private var loaded = false

    /** Текущий выбор; при первом обращении читается из хранилища. */
    fun selected(context: Context): StateFlow<PositionIcon> {
        ensureLoaded(context)
        return _selected.asStateFlow()
    }

    fun set(context: Context, icon: PositionIcon) {
        ensureLoaded(context)
        runCatching {
            context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putString(KEY, icon.key).apply()
        }
        _selected.value = icon
    }

    @Synchronized
    private fun ensureLoaded(context: Context) {
        if (loaded) return
        loaded = true
        _selected.value = PositionIcon.fromKey(
            runCatching {
                context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
            }.getOrNull()
        )
    }
}

private const val ICON_ACCENT = "#00A0F5"
private const val ICON_INK = "#0A0E14"

/** Растровая отрисовка значка [sizePx] × [sizePx] (для слоя карты и предпросмотра в настройках). */
internal fun renderPositionIcon(icon: PositionIcon, sizePx: Int): Bitmap {
    val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    val k = sizePx / 24f
    c.scale(k, k)
    val paths: List<Pair<IconShape, Path>> = icon.shapes.map { it to (PathParser.createPathFromPathData(it.d)) }
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.strokeJoin = Paint.Join.ROUND
    paint.strokeCap = Paint.Cap.ROUND
    // Ореол: широкая белая обводка под всем значком, чтобы контур читался на любой подложке.
    paint.style = Paint.Style.STROKE
    paint.color = Color.WHITE
    paint.strokeWidth = 3.4f
    for ((s, p) in paths) if (s.style != IconStyle.LINE) c.drawPath(p, paint)
    for ((s, p) in paths) {
        when (s.style) {
            IconStyle.ACCENT -> {
                paint.style = Paint.Style.FILL
                paint.color = Color.parseColor(ICON_ACCENT)
                c.drawPath(p, paint)
                paint.style = Paint.Style.STROKE
                paint.color = Color.WHITE
                paint.strokeWidth = 1.8f
                c.drawPath(p, paint)
            }
            IconStyle.BODY -> {
                paint.style = Paint.Style.FILL
                paint.color = Color.WHITE
                c.drawPath(p, paint)
                paint.style = Paint.Style.STROKE
                paint.color = Color.parseColor(ICON_INK)
                paint.strokeWidth = 1.5f
                c.drawPath(p, paint)
            }
            IconStyle.LINE -> {
                paint.style = Paint.Style.STROKE
                paint.color = Color.parseColor(ICON_INK)
                paint.strokeWidth = 1.2f
                c.drawPath(p, paint)
            }
        }
    }
    return bmp
}

/** Значок на светлой плитке (карта всегда светлая): предпросмотр в настройках и быстрый выбор на карте. */
@androidx.compose.runtime.Composable
fun PositionIconTile(icon: PositionIcon, size: androidx.compose.ui.unit.Dp = 48.dp, modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier) {
    val px = with(androidx.compose.ui.platform.LocalDensity.current) { (size - 8.dp).roundToPx() }
    val bmp = androidx.compose.runtime.remember(icon, px) { renderPositionIcon(icon, px).asImageBitmap() }
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .size(size)
            .background(androidx.compose.ui.graphics.Color(0xFFEEF1F4), androidx.compose.foundation.shape.RoundedCornerShape(12.dp)),
        contentAlignment = androidx.compose.ui.Alignment.Center,
    ) {
        androidx.compose.foundation.Image(bitmap = bmp, contentDescription = icon.label, modifier = androidx.compose.ui.Modifier.size(size - 8.dp))
    }
}
