package com.z_company.route.util

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.z_company.route.viewmodel.StatTab
import com.z_company.route.viewmodel.StatisticsUiState
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Одностраничный PDF-отчёт статистики (по референсу design → StatsPdfDoc).
 * Рендерит уже посчитанный StatisticsUiState — тот же экран в виде листа A4.
 */
class StatisticsPdfGenerator(private val context: Context) {

    private val pageWidth = 595
    private val pageHeight = 842
    private val ml = 40f
    private val mr = 40f
    private val mt = 48f
    private val contentWidth = pageWidth - ml - mr

    private val accent = Color.rgb(0, 160, 245)
    private val ink = Color.rgb(26, 23, 20)
    private val sub = Color.rgb(107, 103, 96)
    private val line = Color.rgb(226, 222, 214)

    private val mono = Typeface.MONOSPACE
    private val sans = Typeface.SANS_SERIF
    private val sansBold = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    private val monoBold = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)

    private fun paint(size: Float, color: Int, tf: Typeface, letterSp: Float = 0f) = Paint().apply {
        this.textSize = size; this.color = color; this.typeface = tf; isAntiAlias = true; letterSpacing = letterSp
    }

    fun generate(state: StatisticsUiState): File {
        val doc = PdfDocument()
        val info = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = doc.startPage(info)
        val c = page.canvas
        var y = mt

        // ── Шапка ────────────────────────────────────────────────────
        val pLabel = paint(11f, sub, sans)
        c.drawText("Приложение «Машинист»", ml, y, pLabel)
        val meta = paint(9f, sub, mono)
        val today = SimpleDateFormat("dd.MM.yyyy", Locale("ru")).format(Date())
        c.drawText("сформирован $today", pageWidth - mr - meta.measureText("сформирован $today"), y, meta)
        y += 26f
        val title = when (state.tab) {
            StatTab.HISTORY -> "Статистика · всё время"
            else -> "Статистика · ${state.periodLabel}"
        }
        c.drawText(title, ml, y, paint(24f, ink, sansBold))
        y += 16f
        if (state.tab != StatTab.HISTORY && state.compareEnabled && state.compareTitle.isNotEmpty()) {
            c.drawText("в сравнении с ${state.compareTitle}", ml, y, paint(11f, sub, sans))
            y += 6f
        }
        y += 10f
        c.drawLine(ml, y, ml + contentWidth, y, Paint().apply { color = ink; strokeWidth = 1.5f })
        y += 26f

        if (state.tab == StatTab.HISTORY) {
            y = drawHistory(c, y, state)
        } else {
            y = drawKpiGrid(c, y, state)
            if (state.tab == StatTab.YEAR && state.yearBars.isNotEmpty()) {
                y = drawMonthlyChart(c, y + 8f, state)
            }
            if (state.topDirections.isNotEmpty()) {
                y = drawTopDirections(c, y + 16f, state)
            }
        }

        // ── Подвал ───────────────────────────────────────────────────
        val footY = pageHeight - 34f
        c.drawLine(ml, footY - 12f, ml + contentWidth, footY - 12f, Paint().apply { color = line; strokeWidth = 0.5f })
        val pf = paint(8f, sub, mono)
        c.drawText("Маршрутный лист · экспорт статистики", ml, footY, pf)
        val note = "Данные приложения · не является официальным документом"
        c.drawText(note, pageWidth - mr - pf.measureText(note), footY, pf)

        doc.finishPage(page)
        val file = File(context.cacheDir, fileName(state))
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        return file
    }

    private fun drawKpiGrid(c: android.graphics.Canvas, startY: Float, state: StatisticsUiState): Float {
        var y = startY
        c.drawText("ОСНОВНЫЕ ПОКАЗАТЕЛИ", ml, y, paint(10f, sub, sansBold, 0.08f))
        y += 16f
        val cols = 3
        val cellW = contentWidth / cols
        val cellH = 58f
        val rows = (state.metrics.size + cols - 1) / cols
        state.metrics.forEachIndexed { i, m ->
            val col = i % cols
            val row = i / cols
            val x = ml + col * cellW
            val cy = y + row * cellH
            c.drawRect(x, cy, x + cellW, cy + cellH, Paint().apply { color = line; strokeWidth = 0.5f; style = Paint.Style.STROKE })
            c.drawText(m.label, x + 12f, cy + 18f, paint(9.5f, sub, sans))
            val valueStr = if (m.unit.isNotEmpty()) m.value else m.value
            c.drawText(valueStr, x + 12f, cy + 38f, paint(18f, ink, monoBold))
            val note = buildString {
                m.delta?.pct?.let { append(it) }
                if (m.unit.isNotEmpty()) { if (isNotEmpty()) append("  "); append(m.unit) }
            }
            if (note.isNotEmpty()) c.drawText(note, x + 12f, cy + 52f, paint(9f, accent, mono))
        }
        return y + rows * cellH
    }

    private fun drawMonthlyChart(c: android.graphics.Canvas, startY: Float, state: StatisticsUiState): Float {
        var y = startY + 20f
        c.drawText("ОТРАБОТАНО ПО МЕСЯЦАМ · Ч", ml, y, paint(10f, sub, sansBold, 0.08f))
        y += 14f
        val chartH = 120f
        val bars = state.yearBars
        val max = (bars.maxOfOrNull { it.cur } ?: 1f).coerceAtLeast(1f) * 1.05f
        val slot = contentWidth / bars.size
        val barW = slot * 0.5f
        val baseY = y + chartH
        val valPaint = paint(7.5f, sub, mono)
        val labelPaint = paint(8f, sub, mono)
        bars.forEach { b ->
            val idx = bars.indexOf(b)
            val cx = ml + idx * slot + slot / 2f
            val h = chartH * (b.cur / max)
            c.drawRect(cx - barW / 2f, baseY - h, cx + barW / 2f, baseY,
                Paint().apply { color = accent; style = Paint.Style.FILL })
            if (b.cur > 0f) {
                val v = b.cur.toInt().toString()
                c.drawText(v, cx - valPaint.measureText(v) / 2f, baseY - h - 4f, valPaint)
            }
            c.drawText(b.label, cx - labelPaint.measureText(b.label) / 2f, baseY + 12f, labelPaint)
        }
        c.drawLine(ml, baseY, ml + contentWidth, baseY, Paint().apply { color = line; strokeWidth = 0.5f })
        return baseY + 20f
    }

    private fun drawTopDirections(c: android.graphics.Canvas, startY: Float, state: StatisticsUiState): Float {
        var y = startY
        c.drawText("ТОП НАПРАВЛЕНИЙ", ml, y, paint(10f, sub, sansBold, 0.08f))
        y += 16f
        // Заголовки
        val headPaint = paint(9f, sub, sans)
        c.drawText("Плечо обслуживания", ml, y, headPaint)
        c.drawText("Поездок", ml + contentWidth - 150f, y, headPaint)
        c.drawText("Часов", ml + contentWidth - 60f, y, headPaint)
        y += 6f
        c.drawLine(ml, y, ml + contentWidth, y, Paint().apply { color = ink; strokeWidth = 1f })
        y += 18f
        state.topDirections.forEach { d ->
            c.drawText("${d.from} → ${d.to}", ml, y, paint(11f, ink, sansBold))
            c.drawText("${d.trips}", ml + contentWidth - 150f, y, paint(11f, ink, mono))
            c.drawText(d.hours, ml + contentWidth - 60f, y, paint(11f, ink, mono))
            y += 8f
            c.drawLine(ml, y, ml + contentWidth, y, Paint().apply { color = line; strokeWidth = 0.5f })
            y += 18f
        }
        return y
    }

    private fun drawHistory(c: android.graphics.Canvas, startY: Float, state: StatisticsUiState): Float {
        var y = startY
        c.drawText(state.historyTotalCaption, ml, y, paint(10f, sub, sansBold, 0.08f))
        y += 34f
        c.drawText(state.historyTotal, ml, y, paint(40f, ink, monoBold))
        y += 40f
        c.drawText("ГОД ЗА ГОДОМ", ml, y, paint(10f, sub, sansBold, 0.08f))
        y += 18f
        state.historyRows.forEach { r ->
            c.drawText("${r.year}${r.note?.let { "  $it" } ?: ""}", ml, y, paint(12f, ink, sansBold))
            val vStr = if (r.unit.isNotEmpty()) "${r.value} ${r.unit}" else r.value
            c.drawText(vStr, ml + contentWidth - paint(12f, ink, mono).measureText(vStr), y, paint(12f, ink, mono))
            y += 8f
            val barH = 12f
            c.drawRect(ml, y, ml + contentWidth, y + barH, Paint().apply { color = line; style = Paint.Style.FILL })
            c.drawRect(ml, y, ml + contentWidth * r.fraction.coerceIn(0f, 1f), y + barH,
                Paint().apply { color = accent; style = Paint.Style.FILL })
            y += barH + 18f
        }
        return y
    }

    private fun fileName(state: StatisticsUiState): String {
        val base = when (state.tab) {
            StatTab.HISTORY -> "все_время"
            else -> state.periodLabel.replace(" ", "_")
        }
        return "Статистика_$base.pdf"
    }
}
