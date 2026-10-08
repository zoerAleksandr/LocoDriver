package com.z_company.route.component.servicephase

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.z_company.route.R
import kotlin.math.roundToInt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Значок текущего положения на карте поезда (SCREEN_SPECS §6.9 и §17.5). У каждого значка два вектора:
 * [topRes] — вид сверху (на карте, «нос» вверх, поворачивается по курсу, белый ореол + тёмный контур читаются
 * на светлой и тёмной подложке) и [isoRes] — изометрия (только превью в списках выбора, не поворачивается).
 * Исходные SVG — docs/design/position_icons/. [mapHeightDp] — длина значка на карте.
 */
enum class PositionIcon(
    val key: String,
    val label: String,
    @DrawableRes internal val topRes: Int,
    @DrawableRes internal val isoRes: Int,
    internal val mapHeightDp: Float,
) {
    ARROW("arrow", "Стрелка", R.drawable.ic_posicon_arrow_top, R.drawable.ic_posicon_arrow_iso, 44f),
    VL10("vl10", "ВЛ-10", R.drawable.ic_posicon_vl10_top, R.drawable.ic_posicon_vl10_iso, 56f),
    ES6("es6", "2ЭС6 «Синара»", R.drawable.ic_posicon_es6_top, R.drawable.ic_posicon_es6_iso, 56f),
    TE25KM("te25km", "2ТЭ25КМ «Витязь»", R.drawable.ic_posicon_te25km_top, R.drawable.ic_posicon_te25km_iso, 56f),
    SWALLOW("swallow", "Ласточка", R.drawable.ic_posicon_swallow_top, R.drawable.ic_posicon_swallow_iso, 56f),
    STEAM("steam", "Паровоз", R.drawable.ic_posicon_steam_top, R.drawable.ic_posicon_steam_iso, 56f);

    companion object {
        /** Старые значения (до замены набора): паровоз → Паровоз, тепловоз → 2ТЭ25КМ, электровоз → ВЛ-10, электропоезд → Ласточка, остальное → Стрелка. */
        private val LEGACY = mapOf("diesel" to TE25KM, "electric" to VL10, "emu" to SWALLOW)

        fun fromKey(key: String?): PositionIcon =
            entries.firstOrNull { it.key == key } ?: LEGACY[key] ?: ARROW
    }
}

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

/** Растр вида сверху высотой [heightPx] (ширина по пропорциям вектора) для слоя карты. */
internal fun renderPositionIcon(context: Context, icon: PositionIcon, heightPx: Int): Bitmap {
    val d = ContextCompat.getDrawable(context, icon.topRes)!!.mutate()
    val w = (heightPx * d.intrinsicWidth.toFloat() / d.intrinsicHeight).roundToInt().coerceAtLeast(1)
    val bmp = Bitmap.createBitmap(w, heightPx, Bitmap.Config.ARGB_8888)
    d.setBounds(0, 0, w, heightPx)
    d.draw(Canvas(bmp))
    return bmp
}

/** Изометрическое превью на светлой плитке: настройки и быстрый выбор на карте (не поворачивается). */
@androidx.compose.runtime.Composable
fun PositionIconTile(icon: PositionIcon, size: androidx.compose.ui.unit.Dp = 48.dp, modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier) {
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .size(size)
            .background(androidx.compose.ui.graphics.Color(0xFFEEF1F4), androidx.compose.foundation.shape.RoundedCornerShape(12.dp)),
        contentAlignment = androidx.compose.ui.Alignment.Center,
    ) {
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(icon.isoRes),
            contentDescription = icon.label,
            modifier = androidx.compose.ui.Modifier.size(size - 4.dp),
        )
    }
}
