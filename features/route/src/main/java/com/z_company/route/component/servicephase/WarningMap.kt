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
     * Линия [path] (lat, lon, с шагом около [stepM]), прижатая к рельсам не дальше [maxOffsetM]. Идём по ОДНОМУ пути: параллельные
     * пути и стрелки не смешиваются - пока текущий путь рядом (не дальше лучшего на [STICK_M]), остаёмся на нём; сменить его
     * приходится только на конце куска (тайл, разветвление), тогда берём тот из ближайших, чья проекция ближе к прежней точке.
     * Возвращает куски (разрыв - где рельсов поблизости нет); пусто - привязать нечего.
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
        val lines = rails.filter { it.size >= 2 }
        if (lines.isEmpty()) return emptyList()
        val n = path.size
        val k = lines.size
        // Для каждой точки пути и каждого пути: расстояние и ближайшая точка на нём.
        val dist = Array(n) { DoubleArray(k) { Double.MAX_VALUE } }
        val projX = Array(n) { DoubleArray(k) }
        val projY = Array(n) { DoubleArray(k) }
        for (j in 0 until k) {
            val line = lines[j]
            for (i in 0 until n) {
                val px = path[i].second * kx
                val py = path[i].first * ky
                var best = Double.MAX_VALUE
                var qx0 = 0.0
                var qy0 = 0.0
                for (t in 0 until line.size - 1) {
                    val ax = line[t].second * kx; val ay = line[t].first * ky
                    val dx = line[t + 1].second * kx - ax
                    val dy = line[t + 1].first * ky - ay
                    val len2 = dx * dx + dy * dy
                    val u = if (len2 < 1e-9) 0.0 else (((px - ax) * dx + (py - ay) * dy) / len2).coerceIn(0.0, 1.0)
                    val qx = ax + u * dx
                    val qy = ay + u * dy
                    val d = hypot(px - qx, py - qy)
                    if (d < best) { best = d; qx0 = qx; qy0 = qy }
                }
                dist[i][j] = best; projX[i][j] = qx0; projY[i][j] = qy0
            }
        }
        // Общая «близость» каждого пути к линии зоны (для выбора главного пути в начале).
        val total = DoubleArray(k) { j -> (0 until n).sumOf { i -> minOf(dist[i][j], maxOffsetM) } }
        val result = ArrayList<List<Pair<Double, Double>>>()
        var cur = ArrayList<Pair<Double, Double>>()
        var line = -1
        var prevX = 0.0
        var prevY = 0.0
        var hasPrev = false
        for (i in 0 until n) {
            val bestD = (0 until k).minOf { dist[i][it] }
            if (bestD > maxOffsetM) {
                if (cur.size >= 2) result += cur
                cur = ArrayList(); hasPrev = false; line = -1
                continue
            }
            var pick = -1
            if (line >= 0 && dist[i][line] <= bestD + STICK_M && dist[i][line] <= maxOffsetM) {
                pick = line
            } else {
                val near = (0 until k).filter { dist[i][it] <= bestD + STICK_M }
                pick = if (hasPrev) near.minByOrNull { hypot(projX[i][it] - prevX, projY[i][it] - prevY) }!!
                else near.minByOrNull { total[it] }!!
            }
            val x = projX[i][pick]
            val y = projY[i][pick]
            if (hasPrev && hypot(x - prevX, y - prevY) > stepM * 3 + 150.0) {
                if (cur.size >= 2) result += cur
                cur = ArrayList()
            }
            line = pick
            cur += (y / ky) to (x / kx)
            prevX = x; prevY = y; hasPrev = true
        }
        if (cur.size >= 2) result += cur
        return result
    }

    private const val STICK_M = 40.0

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
