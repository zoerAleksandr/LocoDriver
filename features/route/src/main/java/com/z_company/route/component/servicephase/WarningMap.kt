package com.z_company.route.component.servicephase

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.text.TextPaint
import com.z_company.domain.track.FlatMetric
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.style.layers.LineLayer
import kotlin.math.hypot
import kotlin.math.sqrt

/**
 * Привязка предупреждений на карте к настоящим рельсам подложки. Ось участка редкая (точки через километры), поэтому линия по
 * ней уходит с путей; рельсовые линии берутся из слоёв стиля (то, что нарисовано на экране) и линия предупреждения проецируется
 * на ближайшую из них. Нет рельсов на экране (тайлы не загружены, мелкий масштаб) - остаётся линия по оси.
 */
internal object RailSnap {
    /** Рельсовые ломаные подложки на экране, точки `(lat, lon)`. */
    fun railLines(m: MapLibreMap, width: Int, height: Int): List<List<Pair<Double, Double>>> {
        val style = m.style ?: return emptyList()
        if (width <= 0 || height <= 0) return emptyList()
        return try {
            val ids = style.layers
                .filter { it is LineLayer && !it.id.startsWith("train-map-") && it.id.contains("rail", ignoreCase = true) && !it.id.contains("transit", ignoreCase = true) }
                .map { it.id }.toTypedArray()
            if (ids.isEmpty()) return emptyList()
            val out = ArrayList<List<Pair<Double, Double>>>()
            m.queryRenderedFeatures(RectF(0f, 0f, width.toFloat(), height.toFloat()), *ids).forEach { f ->
                when (val g = f.geometry()) {
                    is org.maplibre.geojson.LineString -> out += g.coordinates().map { it.latitude() to it.longitude() }
                    is org.maplibre.geojson.MultiLineString -> g.coordinates().forEach { line -> out += line.map { it.latitude() to it.longitude() } }
                    else -> {}
                }
            }
            out
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Линия [path] (lat, lon, с шагом около [stepM]), прижатая к ближайшим рельсам не дальше [maxOffsetM]. Возвращает куски
     * (разрывы - где рельсов поблизости нет или точка перескочила на чужой путь); пусто - привязать нечего.
     */
    fun snap(
        path: List<Pair<Double, Double>>,
        rails: List<List<Pair<Double, Double>>>,
        stepM: Double,
        maxOffsetM: Double = 250.0,
    ): List<List<Pair<Double, Double>>> {
        if (path.size < 2 || rails.isEmpty()) return emptyList()
        val lat0 = path[0].first
        val ky = FlatMetric.M_PER_DEG_LAT
        val kx = FlatMetric.kx(lat0)
        class Seg(val ax: Double, val ay: Double, val bx: Double, val by: Double)
        val segs = ArrayList<Seg>()
        for (line in rails) for (i in 0 until line.size - 1) {
            val a = line[i]
            val b = line[i + 1]
            segs += Seg(a.second * kx, a.first * ky, b.second * kx, b.first * ky)
        }
        if (segs.isEmpty()) return emptyList()
        val result = ArrayList<List<Pair<Double, Double>>>()
        var cur = ArrayList<Pair<Double, Double>>()
        var prevX = 0.0
        var prevY = 0.0
        var hasPrev = false
        val jumpLimit = stepM * 3 + 150.0
        for ((lat, lon) in path) {
            val px = lon * kx
            val py = lat * ky
            var best = Double.MAX_VALUE
            var bx = 0.0
            var by = 0.0
            for (s in segs) {
                val dx = s.bx - s.ax
                val dy = s.by - s.ay
                val len2 = dx * dx + dy * dy
                val t = if (len2 < 1e-9) 0.0 else (((px - s.ax) * dx + (py - s.ay) * dy) / len2).coerceIn(0.0, 1.0)
                val qx = s.ax + t * dx
                val qy = s.ay + t * dy
                val d = hypot(px - qx, py - qy)
                if (d < best) { best = d; bx = qx; by = qy }
            }
            val ok = best <= maxOffsetM && (!hasPrev || hypot(bx - prevX, by - prevY) <= jumpLimit)
            if (ok) {
                cur += (by / ky) to (bx / kx)
                prevX = bx; prevY = by; hasPrev = true
            } else {
                if (cur.size >= 2) result += cur
                cur = ArrayList()
                hasPrev = false
                // точка, которая не прошла по скачку, начинает новый кусок, если сама лежит у рельсов
                if (best <= maxOffsetM) { cur += (by / ky) to (bx / kx); prevX = bx; prevY = by; hasPrev = true }
            }
        }
        if (cur.size >= 2) result += cur
        return result
    }

    /** Длина ломаной, м. */
    fun lengthM(line: List<Pair<Double, Double>>): Double {
        if (line.size < 2) return 0.0
        val ky = FlatMetric.M_PER_DEG_LAT
        val kx = FlatMetric.kx(line[0].first)
        var sum = 0.0
        for (i in 0 until line.size - 1) {
            val dx = (line[i + 1].second - line[i].second) * kx
            val dy = (line[i + 1].first - line[i].first) * ky
            sum += sqrt(dx * dx + dy * dy)
        }
        return sum
    }

    /** Точка на ломаной на половине её длины. */
    fun midpoint(line: List<Pair<Double, Double>>): Pair<Double, Double> {
        val half = lengthM(line) / 2
        val ky = FlatMetric.M_PER_DEG_LAT
        val kx = FlatMetric.kx(line[0].first)
        var acc = 0.0
        for (i in 0 until line.size - 1) {
            val dx = (line[i + 1].second - line[i].second) * kx
            val dy = (line[i + 1].first - line[i].first) * ky
            val seg = sqrt(dx * dx + dy * dy)
            if (acc + seg >= half && seg > 1e-6) {
                val t = (half - acc) / seg
                return (line[i].first + (line[i + 1].first - line[i].first) * t) to
                    (line[i].second + (line[i + 1].second - line[i].second) * t)
            }
            acc += seg
        }
        return line.last()
    }
}

/**
 * Карточка-«диалог» предупреждения для карты: белая плашка с жёлтой рамкой и выступом-указателем вниз; сверху название
 * («Ограничение 40 км/ч»), ниже место («138 км 1 пк — 139 км 5 пк»). Точка привязки значка - низ указателя.
 */
internal fun renderWarningCallout(context: Context, title: String, subtitle: String, density: Float): Bitmap {
    val dp = density
    val pad = 8f * dp
    val pointer = 7f * dp
    val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 13f * dp; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); color = 0xFF1B1400.toInt()
    }
    val subPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 11f * dp; typeface = Typeface.DEFAULT; color = 0xFF5B5233.toInt()
    }
    val w = maxOf(titlePaint.measureText(title), subPaint.measureText(subtitle)) + pad * 2
    val textH = titlePaint.fontSpacing + if (subtitle.isNotEmpty()) subPaint.fontSpacing else 0f
    val h = textH + pad * 2
    val bmp = Bitmap.createBitmap(w.toInt() + 2, (h + pointer).toInt() + 2, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    val card = RectF(1f, 1f, w + 1f, h + 1f)
    val shape = Path().apply {
        addRoundRect(card, 9f * dp, 9f * dp, Path.Direction.CW)
        moveTo(card.centerX() - pointer, card.bottom - 1f)
        lineTo(card.centerX(), card.bottom + pointer)
        lineTo(card.centerX() + pointer, card.bottom - 1f)
        close()
    }
    c.drawPath(shape, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt() })
    c.drawPath(shape, Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 2f * dp; color = 0xFFFFD60A.toInt() })
    c.drawText(title, card.left + pad, card.top + pad - titlePaint.ascent(), titlePaint)
    if (subtitle.isNotEmpty()) c.drawText(subtitle, card.left + pad, card.top + pad + titlePaint.fontSpacing - subPaint.ascent(), subPaint)
    return bmp
}
