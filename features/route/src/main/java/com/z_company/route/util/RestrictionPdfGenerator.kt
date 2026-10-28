package com.z_company.route.util

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.z_company.domain.track.RestrictionReport
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * PDF «Отчёт о проследовании ограничений» (SCREEN_SPECS, «Журнал проследования»): шапка (поезд, дата формирования),
 * карточка на каждое ограничение (заход головой, выход всем составом, максимальная скорость, превышение), итог.
 * Многостраничный A4. В файл попадают только время, скорость, км/пикет и подписи ограничений: координат нет, аккаунта
 * и почты нет. Файл лежит в `cacheDir` (отдаётся через FileProvider), имя без персональных данных.
 */
class RestrictionPdfGenerator(private val context: Context) {
    private val pageWidth = 595
    private val pageHeight = 842
    private val ml = 40f
    private val mr = 40f
    private val mt = 48f
    private val mb = 56f
    private val contentWidth = pageWidth - ml - mr

    private val accent = Color.rgb(0, 160, 245)
    private val danger = Color.rgb(211, 47, 47)
    private val ink = Color.rgb(26, 23, 20)
    private val sub = Color.rgb(107, 103, 96)
    private val line = Color.rgb(226, 222, 214)
    private val sans = Typeface.SANS_SERIF
    private val sansBold = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    private val mono = Typeface.MONOSPACE

    private fun paint(size: Float, color: Int, tf: Typeface) = Paint().apply {
        textSize = size; this.color = color; typeface = tf; isAntiAlias = true
    }

    /** [trainTitle] - подпись поезда («Поезд 2034», «Лужская — Гатчина»); [rows] - из [RestrictionReport.rows]. */
    fun generate(trainTitle: String, rows: List<RestrictionReport.Row>): File {
        val doc = PdfDocument()
        var pageNo = 1
        var page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNo).create())
        var c = page.canvas
        var y = mt

        fun footer() {
            val pf = paint(8f, sub, mono)
            c.drawLine(ml, pageHeight - mb + 14f, ml + contentWidth, pageHeight - mb + 14f, Paint().apply { color = line; strokeWidth = 0.5f })
            c.drawText("Приложение «Машинист» · не является официальным документом", ml, pageHeight - mb + 28f, pf)
            val n = "стр. $pageNo"
            c.drawText(n, pageWidth - mr - pf.measureText(n), pageHeight - mb + 28f, pf)
        }

        fun newPage() {
            footer()
            doc.finishPage(page)
            pageNo++
            page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNo).create())
            c = page.canvas
            y = mt
        }

        // Шапка
        val today = SimpleDateFormat("dd.MM.yyyy", Locale("ru")).format(Date())
        c.drawText("Приложение «Машинист»", ml, y, paint(11f, sub, sans))
        val meta = paint(9f, sub, mono)
        val made = "сформирован $today"
        c.drawText(made, pageWidth - mr - meta.measureText(made), y, meta)
        y += 28f
        c.drawText("Проследование ограничений", ml, y, paint(22f, ink, sansBold))
        y += 20f
        c.drawText(trainTitle, ml, y, paint(12f, sub, sans))
        y += 12f
        c.drawLine(ml, y, ml + contentWidth, y, Paint().apply { color = ink; strokeWidth = 1.5f })
        y += 22f

        val titleP = paint(12f, ink, sansBold)
        val textP = paint(10f, ink, sans)
        val subP = paint(9f, sub, sans)
        val cardH = 92f
        rows.forEachIndexed { i, r ->
            if (y + cardH > pageHeight - mb) newPage()
            val top = y
            c.drawText("${i + 1}. ${r.title}", ml, y + 12f, titleP)
            if (r.exceeded) {
                val tag = "ПРЕВЫШЕНИЕ"
                val tp = paint(9f, danger, sansBold)
                c.drawText(tag, ml + contentWidth - tp.measureText(tag), y + 12f, tp)
            }
            var ly = y + 27f
            val where = listOfNotNull(r.place.ifBlank { null }, r.kmText).joinToString(" · ")
            if (where.isNotEmpty()) { c.drawText(where, ml, ly, subP); ly += 14f }
            c.drawText("Заход головой:", ml, ly, subP); c.drawText(r.enter, ml + 92f, ly, textP); ly += 14f
            c.drawText("Выход составом:", ml, ly, subP); c.drawText(r.exit, ml + 92f, ly, textP); ly += 14f
            val lim = r.limit?.let { "ограничение $it · " }.orEmpty()
            c.drawText("Скорость:", ml, ly, subP); c.drawText(lim + "максимальная " + r.maxSpeed, ml + 92f, ly, if (r.exceeded) paint(10f, danger, sansBold) else textP)
            y = maxOf(ly + 12f, top + cardH - 10f)
            c.drawLine(ml, y, ml + contentWidth, y, Paint().apply { color = line; strokeWidth = 0.5f })
            y += 14f
        }

        if (y + 40f > pageHeight - mb) newPage()
        val exceeded = rows.count { it.exceeded }
        c.drawText("Всего ограничений: ${rows.size}", ml, y + 10f, paint(11f, ink, sansBold))
        c.drawText(
            if (exceeded == 0) "Превышений нет" else "Превышений скорости: $exceeded",
            ml, y + 26f, paint(10f, if (exceeded == 0) accent else danger, sansBold),
        )
        footer()
        doc.finishPage(page)
        val file = File(context.cacheDir, "restrictions_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())}.pdf")
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        return file
    }
}
