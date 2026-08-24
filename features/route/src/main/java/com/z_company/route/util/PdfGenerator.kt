package com.z_company.route.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.z_company.core.util.ConverterLongToTime
import com.z_company.core.util.TimeManager
import com.z_company.domain.entities.Day
import com.z_company.domain.entities.ReleaseType
import com.z_company.domain.entities.TagForDay
import com.z_company.domain.entities.WorkScheduleProfile
import com.z_company.domain.entities.route.LocoType
import com.z_company.domain.entities.route.Route
import com.z_company.route.viewmodel.SalaryCalculationUIState
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class PdfGenerator(private val context: Context) {

    private val pageWidth = 595
    private val pageHeight = 842
    private val ml = 36f
    private val mr = 36f
    private val mt = 52f
    private val mb = 40f
    private val contentWidth = pageWidth - ml - mr
    private val tz = TimeZone.getTimeZone("GMT+3")

    private val timeManager = TimeManager("GMT+3")

    private val paintHeader = Paint().apply {
        color = Color.DKGRAY; textSize = 7f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC); isAntiAlias = true
    }
    private val paintHeaderLine = Paint().apply {
        color = Color.LTGRAY; strokeWidth = 0.5f; style = Paint.Style.STROKE
    }
    private val paintTitle = Paint().apply {
        color = Color.BLACK; textSize = 13f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true
    }
    private val paintSection = Paint().apply {
        color = Color.BLACK; textSize = 10f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true
    }
    private val paintBody = Paint().apply {
        color = Color.BLACK; textSize = 9f; typeface = Typeface.DEFAULT; isAntiAlias = true
    }
    private val paintBodyBold = Paint().apply {
        color = Color.BLACK; textSize = 9f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true
    }
    private val paintSmall = Paint().apply {
        color = Color.DKGRAY; textSize = 7f; typeface = Typeface.DEFAULT; isAntiAlias = true
    }
    private val paintTiny = Paint().apply {
        color = Color.DKGRAY; textSize = 7.5f; typeface = Typeface.DEFAULT; isAntiAlias = true
    }
    private val paintTableBorder = Paint().apply {
        color = Color.GRAY; strokeWidth = 0.5f; style = Paint.Style.STROKE
    }
    private val paintFill = Paint().apply { style = Paint.Style.FILL }
    private val paintSectionFill = Paint().apply {
        color = Color.argb(25, 0, 0, 0); style = Paint.Style.FILL
    }

    // ─── Russian month names ─────────────────────────────────────────────────────

    private val russianMonths = mapOf(
        "январь" to 0, "февраль" to 1, "март" to 2, "апрель" to 3,
        "май" to 4, "июнь" to 5, "июль" to 6, "август" to 7,
        "сентябрь" to 8, "октябрь" to 9, "ноябрь" to 10, "декабрь" to 11
    )

    private fun parseMonthYear(monthLabel: String): Pair<Int, Int>? {
        val parts = monthLabel.trim().split(" ").filter { it.isNotBlank() }
        if (parts.size < 2) return null
        val month = russianMonths[parts[0].lowercase(Locale("ru"))] ?: return null
        val year = parts[1].toIntOrNull() ?: return null
        return Pair(month, year)
    }

    // ─── Page manager ────────────────────────────────────────────────────────────

    inner class PageManager(private val document: PdfDocument) {
        private var currentPage: PdfDocument.Page? = null
        var canvas: Canvas = Canvas()
        private var pageNumber = 0
        var y = mt

        fun newPage(): Canvas {
            currentPage?.let { document.finishPage(it) }
            pageNumber++
            val info = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            currentPage = document.startPage(info)
            canvas = currentPage!!.canvas
            y = mt
            drawPageHeader(canvas)
            return canvas
        }

        fun finish() { currentPage?.let { document.finishPage(it) } }

        fun needNewPage(h: Float = 20f) = y + h > pageHeight - mb

        fun checkNewPage(h: Float = 20f) { if (needNewPage(h)) newPage() }

        fun sectionTitle(text: String) {
            checkNewPage(26f)
            y += 14f  // достаточный отступ от предыдущего разделителя
            canvas.drawRect(ml, y - 11f, ml + contentWidth, y + 3f, paintSectionFill)
            canvas.drawText(text, ml + 4f, y, paintSection)
            y += 14f
        }

        /** Draw a separator line with proper spacing (no text overlap) */
        fun separator() {
            y += 3f
            val paint = Paint().apply { color = Color.LTGRAY; strokeWidth = 0.5f; style = Paint.Style.STROKE }
            canvas.drawLine(ml, y, ml + contentWidth, y, paint)
            y += 5f
        }

        /** Draw text and advance y */
        fun text(str: String, paint: Paint, indent: Float = 0f) {
            if (str.isBlank()) return
            val x = ml + indent
            val maxW = contentWidth - indent
            // simple word-wrap
            var line = ""
            for (word in str.split(" ")) {
                val test = if (line.isEmpty()) word else "$line $word"
                if (paint.measureText(test) > maxW && line.isNotEmpty()) {
                    checkNewPage(paint.textSize + 3f)
                    canvas.drawText(line, x, y, paint)
                    y += paint.textSize + 3f
                    line = word
                } else line = test
            }
            if (line.isNotEmpty()) {
                checkNewPage(paint.textSize + 3f)
                canvas.drawText(line, x, y, paint)
                y += paint.textSize + 3f
            }
        }
    }

    // ─── Table helpers ───────────────────────────────────────────────────────────

    private val col1W = contentWidth * 0.38f
    private val col2W = contentWidth * 0.62f

    /** Draw a 2-column table row: label | value */
    private fun PageManager.tableRow(label: String, value: String?, isHeaderRow: Boolean = false, valueBold: Boolean = false) {
        if (value.isNullOrBlank()) return
        val rowH = 14f
        checkNewPage(rowH + 2f)
        val top = y - 10f
        val bot = y + 4f
        if (isHeaderRow) {
            canvas.drawRect(ml, top, ml + contentWidth, bot, paintSectionFill)
        }
        canvas.drawRect(ml, top, ml + col1W, bot, paintTableBorder)
        canvas.drawRect(ml + col1W, top, ml + contentWidth, bot, paintTableBorder)
        val lPaint = if (isHeaderRow) paintBodyBold else paintSmall
        canvas.drawText(label, ml + 3f, y, lPaint)
        // value may need clipping
        val valPaint = if (isHeaderRow || valueBold) paintBodyBold else paintBody
        val maxValW = col2W - 6f
        var valText = value ?: ""
        while (valPaint.measureText(valText) > maxValW && valText.length > 3) {
            valText = valText.dropLast(1)
        }
        canvas.drawText(valText, ml + col1W + 3f, y, valPaint)
        y += rowH
    }

    /** Salary table: 4 columns */
    private val sColW = floatArrayOf(
        contentWidth * 0.50f, contentWidth * 0.16f, contentWidth * 0.14f, contentWidth * 0.20f
    )

    private fun PageManager.salaryRow(desc: String, hours: String, pct: String, amount: String, bold: Boolean = false) {
        val rowH = 13f
        checkNewPage(rowH + 2f)
        val top = y - 9f; val bot = y + 3f
        if (bold) canvas.drawRect(ml, top, ml + contentWidth, bot, paintSectionFill)
        var x = ml
        val p = if (bold) paintBodyBold else paintBody
        sColW.forEachIndexed { i, w ->
            canvas.drawRect(x, top, x + w, bot, paintTableBorder)
            val txt = when (i) { 0 -> desc; 1 -> hours; 2 -> pct; else -> amount }
            val tx = if (i == 0) x + 3f else x + w - p.measureText(txt) - 3f
            canvas.drawText(txt, tx, y, p)
            x += w
        }
        y += rowH
    }

    private fun PageManager.salaryHeader() {
        checkNewPage(16f)
        salaryRow("Вид выплаты", "Часы", "%", "Сумма", bold = true)
    }

    /** Таблица удержаний: 3 колонки (без Часы) */
    private val rColW = floatArrayOf(
        contentWidth * 0.66f, contentWidth * 0.14f, contentWidth * 0.20f
    )

    private fun PageManager.retentionRow(desc: String, pct: String, amount: String, bold: Boolean = false) {
        val rowH = 13f
        checkNewPage(rowH + 2f)
        val top = y - 9f; val bot = y + 3f
        if (bold) canvas.drawRect(ml, top, ml + contentWidth, bot, paintSectionFill)
        var x = ml
        val p = if (bold) paintBodyBold else paintBody
        val texts = arrayOf(desc, pct, amount)
        rColW.forEachIndexed { i, w ->
            canvas.drawRect(x, top, x + w, bot, paintTableBorder)
            val txt = texts[i]
            val tx = if (i == 0) x + 3f else x + w - p.measureText(txt) - 3f
            canvas.drawText(txt, tx, y, p)
            x += w
        }
        y += rowH
    }

    private fun PageManager.retentionHeader() {
        checkNewPage(16f)
        retentionRow("Вид удержания", "%", "Сумма", bold = true)
    }

    // ─── Formatters ──────────────────────────────────────────────────────────────

    // 0L трактуется как отсутствующее значение (старый код мог сохранять 0 вместо null)
    private fun fmtTime(millis: Long?) = millis?.takeIf { it > 0L }?.let { timeManager.formatTime(it) }
    private fun fmtDate(millis: Long?) = millis?.takeIf { it > 0L }?.let { timeManager.formatDate(it) }
    private fun fmtDT(millis: Long?) = millis?.takeIf { it > 0L }?.let { "${timeManager.formatDate(it)} ${timeManager.formatTime(it)}" }
    private fun fmtDur(ms: Long?) = ms?.let { ConverterLongToTime.getTimeInStringFormat(it) }
    // Значения, которые после округления до 2 знаков дают 0.00, не отображаем
    // Валюта расчётного листа — задаётся в drawSalary() по стране из настроек.
    private var payCurrency: String = "₽"
    private fun fmtMoney(v: Double?) = if (v == null || kotlin.math.abs(v) < 0.005) "" else "${"%.2f".format(v)} $payCurrency"
    private fun fmtHours(v: Long?) = if (v == null || v == 0L) "" else ConverterLongToTime.getTimeInStringFormat(v)
    private fun fmtPct(v: Double?) = if (v == null || v == 0.0) "" else "${v}%"
    private fun fmtPctStr(v: String?) = if (v.isNullOrBlank() || v == "0" || v == "0.0") "" else "$v%"

    // ─── Public entry point ──────────────────────────────────────────────────────

    fun generatePdf(
        routes: List<Route>,
        salaryState: SalaryCalculationUIState?,
        monthLabel: String,
        sections: PdfSections,
        calendarDays: List<Day> = emptyList(),
        workScheduleProfile: WorkScheduleProfile = WorkScheduleProfile.standard(),
    ): File {
        // Удаляем старые PDF-файлы из кеша
        context.cacheDir.listFiles { f -> f.name.startsWith("Машинист_") && f.name.endsWith(".pdf") }
            ?.forEach { it.delete() }

        val document = PdfDocument()
        val pm = PageManager(document)

        if (sections.includeRouteDetails) {
            pm.newPage()
            drawRouteDetails(pm, routes, monthLabel)
        }
        if (sections.includeSchedule) {
            pm.newPage()
            drawSchedule(pm, routes, monthLabel, calendarDays, workScheduleProfile)
        }
        if (sections.includeSalary) {
            pm.newPage()
            if (salaryState != null) {
                drawSalary(pm, salaryState, monthLabel)
            } else {
                pm.text("Расчётный листок", paintTitle)
                pm.y += 4f
                pm.text(
                    "Данные расчёта недоступны. Откройте «Расчёт заработной платы» и повторите формирование.",
                    paintBody
                )
            }
        }

        pm.finish()

        // Уникальное имя вида "Машинист_Апрель_2026.pdf"
        val labelParts = monthLabel.trim().split(" ")
        val monthPart = labelParts.getOrNull(0)?.replaceFirstChar { it.uppercase(Locale.ROOT) } ?: "Месяц"
        val yearPart = labelParts.getOrNull(1) ?: "Год"
        val file = File(context.cacheDir, "Машинист_${monthPart}_${yearPart}.pdf")
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()
        return file
    }

    // ─── Page header ─────────────────────────────────────────────────────────────

    private fun drawPageHeader(canvas: Canvas) {
        canvas.drawText("Файл сформирован в приложении «Машинист»", ml, 20f, paintHeader)
        canvas.drawLine(ml, 26f, pageWidth - mr, 26f, paintHeaderLine)
    }

    // ─── Route details ───────────────────────────────────────────────────────────

    private fun drawRouteDetails(pm: PageManager, routes: List<Route>, monthLabel: String) {
        pm.text("Поездки за $monthLabel", paintTitle)
        pm.y += 2f
        pm.separator()

        if (routes.isEmpty()) {
            pm.text("Нет маршрутов за период.", paintBody)
            return
        }

        // Сортируем по времени явки (timeStartWork) по возрастанию.
        // Маршруты без явки (null) уходят в конец списка.
        val sortedRoutes = routes.sortedWith(
            compareBy(nullsLast()) { it.basicData.timeStartWork }
        )

        sortedRoutes.forEach { route ->
            val needed = minOf(estimateRouteHeight(route), (pageHeight - mt - mb) * 0.85f)
            if (pm.needNewPage(needed)) pm.newPage()
            drawRoute(pm, route)
        }
    }

    private fun estimateRouteHeight(route: Route): Float {
        var h = 10f + 16f  // spacing + header
        h += 14f * 2       // явка/сдача header + data
        val bd = route.basicData
        if ((bd.timeStartBreak ?: 0L) > 0L || (bd.timeEndBreak ?: 0L) > 0L) h += 14f * 2
        if (bd.restPointOfTurnover || bd.isOnePersonOperation || !bd.notes.isNullOrBlank()) h += 15f
        route.locomotives.forEach { loco ->
            h += 4f + 14f   // spacing + header
            h += 14f * 2    // acceptance/delivery
            if (loco.heatingCounterAccepted != null || loco.heatingCounterDelivery != null) h += 28f
            if (loco.auxiliaryCounterAccepted != null || loco.auxiliaryCounterDelivery != null) h += 28f
            val secCount = if (loco.type == LocoType.ELECTRIC) loco.electricSectionList.size else loco.dieselSectionList.size
            h += secCount * 56f
            h += 42f // итого
        }
        route.trains.forEach { train ->
            h += 4f + 14f
            h += (train.stations.size + 1) * 14f
        }
        route.passengers.forEach { _ -> h += 4f + 14f * 3 }
        h += 8f
        return h
    }

    // ─── Multi-column row helper ─────────────────────────────────────────────────

    /** Draw a row with N cells of given widths. If [bold] — draws background tint. */
    private fun PageManager.multiRow(
        cells: List<String>,
        ws: FloatArray,
        paint: Paint = paintBody,
        bold: Boolean = false,
        rowH: Float = 14f
    ) {
        checkNewPage(rowH + 2f)
        val top = y - rowH + 4f
        val bot = y + 4f
        if (bold) canvas.drawRect(ml, top, ml + contentWidth, bot, paintSectionFill)
        var x = ml
        cells.forEachIndexed { i, text ->
            val w = ws.getOrElse(i) { 0f }
            canvas.drawRect(x, top, x + w, bot, paintTableBorder)
            if (text.isNotBlank()) {
                val p = if (bold) paintBodyBold else paint
                val avail = w - 6f
                var t = text
                while (p.measureText(t) > avail && t.length > 3) t = t.dropLast(1)
                canvas.drawText(t, x + 3f, y, p)
            }
            x += w
        }
        y += rowH
    }

    private fun drawRoute(pm: PageManager, route: Route) {
        val bd = route.basicData
        val num = bd.number?.takeIf { it.isNotBlank() } ?: "б/н"
        val dateStr = fmtDate(bd.timeStartWork) ?: "—"
        pm.checkNewPage(60f)
        pm.y += 10f
        pm.multiRow(listOf("Маршрут №$num   $dateStr"), floatArrayOf(contentWidth), bold = true, rowH = 16f)

        // ── Явка / Сдача / Рабочее время — 3 равные колонки
        val w3 = floatArrayOf(contentWidth / 3f, contentWidth / 3f, contentWidth / 3f)
        val breakMs = if (bd.timeStartBreak != null && bd.timeEndBreak != null &&
            bd.timeEndBreak!! > bd.timeStartBreak!!)
            (bd.timeEndBreak!! - bd.timeStartBreak!!) else 0L
        val workDur = if (bd.timeStartWork != null && bd.timeEndWork != null)
            fmtDur(maxOf(0L, bd.timeEndWork!! - bd.timeStartWork!! - breakMs)) else "—"

        pm.multiRow(listOf("Явка", "Сдача", "Рабочее время"), w3, paint = paintSmall, bold = true)
        pm.multiRow(
            listOf(
                fmtDT(bd.timeStartWork) ?: "—",
                fmtDT(bd.timeEndWork) ?: "—",
                workDur ?: "—"
            ), w3, paint = paintBodyBold
        )

        // ── Перерыв (если задан и != 0 — 0L означает «не задан» в старом коде)
        if ((bd.timeStartBreak ?: 0L) > 0L || (bd.timeEndBreak ?: 0L) > 0L) {
            val w2h = floatArrayOf(contentWidth / 2f, contentWidth / 2f)
            pm.multiRow(listOf("Начало перерыва", "Окончание перерыва"), w2h, paint = paintSmall, bold = true)
            pm.multiRow(
                listOf(
                    fmtDT(bd.timeStartBreak) ?: "—",
                    fmtDT(bd.timeEndBreak) ?: "—"
                ), w2h
            )
        }

        // Дополнительные флаги
        if (bd.restPointOfTurnover) {
            pm.y += 2f
            pm.text("• Отдых в пункте оборота", paintSmall)
        }
        if (bd.isOnePersonOperation) pm.text("• Работа в одно лицо", paintSmall)
        if (!bd.notes.isNullOrBlank()) pm.text("Примечание: ${bd.notes}", paintSmall)

        // ── Локомотивы
        route.locomotives.forEachIndexed { idx, loco ->
            pm.checkNewPage(50f)
            pm.y += 4f
            val locoType = if (loco.type == LocoType.ELECTRIC) "Электровоз" else "Тепловоз"
            val locoSeries = loco.series?.takeIf { it.isNotBlank() } ?: "—"
            val locoNum   = loco.number?.takeIf { it.isNotBlank() } ?: "—"
            // "Локомотив" | серия | номер — 3 колонки
            val wLoco = floatArrayOf(contentWidth * 0.35f, contentWidth * 0.40f, contentWidth * 0.25f)
            pm.multiRow(listOf("Локомотив ${idx + 1} ($locoType)", locoSeries, locoNum), wLoco, bold = true)

            // Приёмка / сдача — 4 колонки
            val w4 = floatArrayOf(contentWidth / 4f, contentWidth / 4f, contentWidth / 4f, contentWidth / 4f)
            pm.multiRow(listOf("Нач. приёмки", "Оконч. приёмки", "Нач. сдачи", "Оконч. сдачи"), w4, paint = paintSmall, bold = true)
            pm.multiRow(
                listOf(
                    fmtDT(loco.timeStartOfAcceptance) ?: "—",
                    fmtDT(loco.timeEndOfAcceptance) ?: "—",
                    fmtDT(loco.timeStartOfDelivery) ?: "—",
                    fmtDT(loco.timeEndOfDelivery) ?: "—"
                ), w4
            )

            // Отопление счётчик
            val hcA = loco.heatingCounterAccepted; val hcD = loco.heatingCounterDelivery
            if (hcA != null || hcD != null) {
                pm.multiRow(listOf("Отопление (счётчик)", "Принял", "Сдал", "Расход"), w4, paint = paintSmall, bold = true)
                val hcRes = if (hcA != null && hcD != null) "${"%.1f".format(hcA - hcD)}" else "—"
                pm.multiRow(listOf("", hcA?.let { "${"%.1f".format(it)}" } ?: "—", hcD?.let { "${"%.1f".format(it)}" } ?: "—", hcRes), w4)
            }
            // Собственные нужды
            val acA = loco.auxiliaryCounterAccepted; val acD = loco.auxiliaryCounterDelivery
            if (acA != null || acD != null) {
                pm.multiRow(listOf("Собственные нужды", "Принял", "Сдал", "Расход"), w4, paint = paintSmall, bold = true)
                val acRes = if (acA != null && acD != null) "${"%.1f".format(acA - acD)}" else "—"
                pm.multiRow(listOf("", acA?.let { "${"%.1f".format(it)}" } ?: "—", acD?.let { "${"%.1f".format(it)}" } ?: "—", acRes), w4)
            }

            // ── Секции
            if (loco.type == LocoType.ELECTRIC) {
                val hasNorma1 = loco.normaElectricCurrent1 != null
                val hasNorma2 = loco.normaElectricCurrent2 != null
                val hasAnyNorma = hasNorma1 || hasNorma2
                val wSec: FloatArray = if (hasAnyNorma) floatArrayOf(
                    contentWidth * 0.28f, contentWidth * 0.13f, contentWidth * 0.13f,
                    contentWidth * 0.13f, contentWidth * 0.16f, contentWidth * 0.17f
                ) else w4

                val sectionsWithElecData = loco.electricSectionList.filter { sec ->
                    listOf(sec.acceptedEnergy, sec.deliveryEnergy, sec.acceptedRecovery, sec.deliveryRecovery).any { (it ?: 0.0) != 0.0 }
                }
                val singleElecSection = sectionsWithElecData.size == 1

                loco.electricSectionList.forEachIndexed { si, sec ->
                    val hasData = listOf(sec.acceptedEnergy, sec.deliveryEnergy,
                        sec.acceptedRecovery, sec.deliveryRecovery).any { (it ?: 0.0) != 0.0 }
                    if (!hasData) return@forEachIndexed
                    pm.checkNewPage(30f)
                    pm.y += 2f
                    val secHeader = if (hasAnyNorma)
                        listOf("Секция ${si + 1}", "Принял", "Сдал", "Итого", "Норма", "Результат")
                    else
                        listOf("Секция ${si + 1}", "Принял", "Сдал", "Итого")
                    pm.multiRow(secHeader, wSec, paint = paintSmall, bold = true)
                    if ((sec.acceptedEnergy ?: 0.0) != 0.0 || (sec.deliveryEnergy ?: 0.0) != 0.0) {
                        val accept = sec.acceptedEnergy?.let { "%.1f".format(it) } ?: "—"
                        val deliv  = sec.deliveryEnergy?.let { "%.1f".format(it) } ?: "—"
                        val total  = sec.deliveryEnergy?.let { d -> sec.acceptedEnergy?.let { a -> "%.1f".format(d - a) } } ?: "—"
                        val cells = mutableListOf("Расход тяги", accept, deliv, total)
                        if (hasAnyNorma && singleElecSection) {
                            cells.add(loco.normaElectricCurrent1?.let { "%.1f".format(it) } ?: "")
                            cells.add(loco.normaElectricCurrent1?.let { n ->
                                val diff = (sec.deliveryEnergy ?: 0.0) - (sec.acceptedEnergy ?: 0.0) - n
                                "%.1f".format(diff)
                            } ?: "")
                        } else if (hasAnyNorma) { cells.add(""); cells.add("") }
                        pm.multiRow(cells, wSec)
                    }
                    if ((sec.acceptedRecovery ?: 0.0) != 0.0 || (sec.deliveryRecovery ?: 0.0) != 0.0) {
                        val accept = sec.acceptedRecovery?.let { "%.1f".format(it) } ?: "—"
                        val deliv  = sec.deliveryRecovery?.let { "%.1f".format(it) } ?: "—"
                        val total  = sec.deliveryRecovery?.let { d -> sec.acceptedRecovery?.let { a -> "%.1f".format(d - a) } } ?: "—"
                        val cells = mutableListOf("Рекуперация", accept, deliv, total)
                        if (hasAnyNorma && singleElecSection) {
                            cells.add(loco.normaElectricCurrent2?.let { "%.1f".format(it) } ?: "")
                            cells.add(loco.normaElectricCurrent2?.let { n ->
                                val diff = (sec.deliveryRecovery ?: 0.0) - (sec.acceptedRecovery ?: 0.0) - n
                                "%.1f".format(diff)
                            } ?: "")
                        } else if (hasAnyNorma) { cells.add(""); cells.add("") }
                        pm.multiRow(cells, wSec)
                    }
                }
                // Итого по электросекциям: показываем если > 1 секция, или если 1 секция без нормы
                val showElecTotals = sectionsWithElecData.size > 1 || (sectionsWithElecData.size == 1 && !hasAnyNorma)
                if (showElecTotals) {
                    val totalExpend = loco.electricSectionList.sumOf { sec ->
                        (sec.deliveryEnergy ?: 0.0) - (sec.acceptedEnergy ?: 0.0)
                    }
                    val totalRecov = loco.electricSectionList.sumOf { sec ->
                        (sec.deliveryRecovery ?: 0.0) - (sec.acceptedRecovery ?: 0.0)
                    }
                    if (totalExpend != 0.0 || totalRecov != 0.0) {
                        pm.y += 2f
                        val itHeader = if (hasAnyNorma && sectionsWithElecData.size > 1)
                            listOf("Итого", "", "", "", "Норма", "Результат")
                        else
                            listOf("Итого", "", "", "")
                        pm.multiRow(itHeader, wSec, paint = paintSmall, bold = true)
                        if (totalExpend != 0.0) {
                            val totalStr = "%.1f".format(totalExpend)
                            val cells = mutableListOf("Расход тяги", "", "", totalStr)
                            if (hasAnyNorma && sectionsWithElecData.size > 1) {
                                cells.add(loco.normaElectricCurrent1?.let { "%.1f".format(it) } ?: "")
                                cells.add(loco.normaElectricCurrent1?.let { n -> "%.1f".format(totalExpend - n) } ?: "")
                            }
                            pm.multiRow(cells, wSec)
                        }
                        if (totalRecov != 0.0) {
                            val totalStr = "%.1f".format(totalRecov)
                            val cells = mutableListOf("Рекуперация", "", "", totalStr)
                            if (hasAnyNorma && sectionsWithElecData.size > 1) {
                                cells.add(loco.normaElectricCurrent2?.let { "%.1f".format(it) } ?: "")
                                cells.add(loco.normaElectricCurrent2?.let { n -> "%.1f".format(totalRecov - n) } ?: "")
                            }
                            pm.multiRow(cells, wSec)
                        }
                    }
                }
            } else {
                val normaDieselVal = loco.normaDiesel?.trim()?.toDoubleOrNull()
                val hasNormaDiesel = normaDieselVal != null
                val wSec: FloatArray = if (hasNormaDiesel) floatArrayOf(
                    contentWidth * 0.28f, contentWidth * 0.13f, contentWidth * 0.13f,
                    contentWidth * 0.13f, contentWidth * 0.16f, contentWidth * 0.17f
                ) else w4

                val dieselSectionsWithData = loco.dieselSectionList.filter { (it.acceptedFuel ?: 0.0) != 0.0 || (it.deliveryFuel ?: 0.0) != 0.0 }
                val singleDieselSection = dieselSectionsWithData.size == 1

                loco.dieselSectionList.forEachIndexed { si, sec ->
                    val hasData = (sec.acceptedFuel ?: 0.0) != 0.0 || (sec.deliveryFuel ?: 0.0) != 0.0
                    if (!hasData) return@forEachIndexed
                    pm.checkNewPage(28f)
                    pm.y += 2f
                    val secHeader = if (hasNormaDiesel)
                        listOf("Секция ${si + 1}", "Принял", "Сдал", "Итого", "Норма", "Результат")
                    else
                        listOf("Секция ${si + 1}", "Принял", "Сдал", "Итого")
                    pm.multiRow(secHeader, wSec, paint = paintSmall, bold = true)
                    val accept = sec.acceptedFuel?.let { "${"%.2f".format(it)} л" } ?: "—"
                    val deliv  = sec.deliveryFuel?.let { "${"%.2f".format(it)} л" } ?: "—"
                    val expendLitres = sec.deliveryFuel?.let { d -> sec.acceptedFuel?.let { a -> a - d } }
                    val total  = expendLitres?.let { "${"%.2f".format(it)} л" } ?: "—"
                    val cells = mutableListOf("Топливо", accept, deliv, total)
                    if (hasNormaDiesel && singleDieselSection) {
                        cells.add(normaDieselVal?.let { "${"%.2f".format(it)} л" } ?: "")
                        cells.add(normaDieselVal?.let { n ->
                            val diff = (expendLitres ?: 0.0) - n
                            "${"%.2f".format(diff)} л"
                        } ?: "")
                    } else if (hasNormaDiesel) { cells.add(""); cells.add("") }
                    pm.multiRow(cells, wSec)
                    sec.coefficient?.let { coeff ->
                        val expendKg = expendLitres?.let { it * coeff }
                        if (expendKg != null && expendKg > 0.0) {
                            val acceptKg = sec.acceptedFuel?.let { "${"%.1f".format(it * coeff)} кг" } ?: "—"
                            val delivKg  = sec.deliveryFuel?.let { "${"%.1f".format(it * coeff)} кг" } ?: "—"
                            val resKg    = "${"%.1f".format(expendKg)} кг"
                            val kgCells = mutableListOf("", acceptKg, delivKg, resKg)
                            if (hasNormaDiesel) { kgCells.add(""); kgCells.add("") }
                            pm.multiRow(kgCells, wSec)
                        }
                    }
                }
                val showDieselTotals = dieselSectionsWithData.size > 1 || (dieselSectionsWithData.size == 1 && !hasNormaDiesel)
                if (showDieselTotals) {
                    val totalExpend = loco.dieselSectionList.sumOf { sec ->
                        (sec.acceptedFuel ?: 0.0) - (sec.deliveryFuel ?: 0.0)
                    }
                    if (totalExpend > 0.0) {
                        pm.y += 2f
                        val itHeader = if (hasNormaDiesel && dieselSectionsWithData.size > 1)
                            listOf("Итого", "", "", "", "Норма", "Результат")
                        else
                            listOf("Итого", "", "", "")
                        pm.multiRow(itHeader, wSec, paint = paintSmall, bold = true)
                        val totalStr = "${"%.2f".format(totalExpend)} л"
                        val cells = mutableListOf("Топливо", "", "", totalStr)
                        if (hasNormaDiesel && dieselSectionsWithData.size > 1) {
                            cells.add(normaDieselVal?.let { "${"%.2f".format(it)} л" } ?: "")
                            cells.add(normaDieselVal?.let { n -> "${"%.2f".format(totalExpend - n)} л" } ?: "")
                        }
                        pm.multiRow(cells, wSec)
                    }
                }
            }
        }

        // ── Поезда
        route.trains.forEachIndexed { idx, train ->
            pm.checkNewPage(50f)
            pm.y += 4f
            // Строка-заголовок поезда: Поезд N | № | Вес | Оси | у.д.
            val wTrain = floatArrayOf(
                contentWidth * 0.22f, contentWidth * 0.20f,
                contentWidth * 0.20f, contentWidth * 0.18f, contentWidth * 0.20f
            )
            pm.multiRow(
                listOf(
                    "Поезд ${idx + 1}",
                    train.number?.let { "№$it" } ?: "б/н",
                    train.weight?.let { "Вес $it т" } ?: "",
                    train.axle?.let { "Оси $it" } ?: "",
                    train.conditionalLength?.let { raw ->
                        val fmt = raw.toDoubleOrNull()
                            ?.let { d -> if (d == kotlin.math.floor(d)) d.toLong().toString() else raw }
                            ?: raw
                        "у.д. $fmt"
                    } ?: ""
                ), wTrain, bold = true
            )

            // Станции: Станция | Прибытие | Отправление — 3 колонки
            val wSt = floatArrayOf(contentWidth * 0.36f, contentWidth * 0.32f, contentWidth * 0.32f)
            if (train.stations.isNotEmpty()) {
                pm.multiRow(listOf("Станция", "Прибытие", "Отправление"), wSt, paint = paintSmall, bold = true)
                train.stations.forEach { station ->
                    pm.multiRow(
                        listOf(
                            station.stationName ?: "—",
                            fmtDT(station.timeArrival) ?: "—",
                            fmtDT(station.timeDeparture) ?: "—"
                        ), wSt
                    )
                }
            }
        }

        // ── Пассажиры
        route.passengers.forEachIndexed { idx, passenger ->
            pm.checkNewPage(50f)
            pm.y += 4f
            // Заголовок: Пассажир N | номер поезда
            val wPass2 = floatArrayOf(contentWidth * 0.5f, contentWidth * 0.5f)
            pm.multiRow(
                listOf("Пассажиром ${idx + 1}", passenger.trainNumber?.let { "Поезд №$it" } ?: ""),
                wPass2, bold = true
            )
            // Отправление | Прибытие | В пути — 3 колонки
            val wPass3 = floatArrayOf(contentWidth / 3f, contentWidth / 3f, contentWidth / 3f)
            pm.multiRow(listOf("Отправление", "Прибытие", "В пути"), wPass3, paint = paintSmall, bold = true)
            val passTime = if (passenger.timeDeparture != null && passenger.timeArrival != null)
                fmtDur(maxOf(0L, passenger.timeArrival!! - passenger.timeDeparture!!)) else "—"
            pm.multiRow(
                listOf(
                    fmtDT(passenger.timeDeparture) ?: "—",
                    fmtDT(passenger.timeArrival) ?: "—",
                    passTime ?: "—"
                ), wPass3
            )
            // Станции
            val stFrom = passenger.stationDeparture?.takeIf { it.isNotBlank() }
            val stTo   = passenger.stationArrival?.takeIf { it.isNotBlank() }
            if (stFrom != null || stTo != null) {
                pm.multiRow(listOf("Откуда", "Куда", ""), wPass3, paint = paintSmall, bold = true)
                pm.multiRow(listOf(stFrom ?: "—", stTo ?: "—", ""), wPass3)
            }
            if (!passenger.notes.isNullOrBlank()) pm.text("Примечание: ${passenger.notes}", paintSmall)
        }

        pm.y += 8f
    }

    // ─── Schedule section ─────────────────────────────────────────────────────────

    /** Info shown under the day number in a schedule cell */
    private data class ScheduleEntry(
        val line1: String   // e.g. "17:01-05:01 (12:00)" or "←-09:20 (09:20)"
    )

    /** Overlap of route's break with [from, to) window (in ms). 0L treated as null. */
    private fun calcBreakOverlap(route: Route, from: Long, to: Long): Long {
        val bs = route.basicData.timeStartBreak?.takeIf { it > 0L } ?: return 0L
        val be = route.basicData.timeEndBreak?.takeIf { it > 0L } ?: return 0L
        if (be <= bs) return 0L
        val bFrom = maxOf(bs, from)
        val bTo = minOf(be, to)
        return if (bTo > bFrom) bTo - bFrom else 0L
    }

    private fun drawSchedule(
        pm: PageManager,
        routes: List<Route>,
        monthLabel: String,
        calendarDays: List<Day> = emptyList(),
        workScheduleProfile: WorkScheduleProfile = WorkScheduleProfile.standard(),
    ) {
        pm.text("График за $monthLabel", paintTitle)
        pm.y += 2f
        pm.separator()

        val (month, year) = parseMonthYear(monthLabel) ?: run {
            pm.text("Не удалось определить период из «$monthLabel».", paintBody)
            return
        }

        // Build month calendar
        val firstDayCal = Calendar.getInstance(tz).apply {
            set(year, month, 1, 0, 0, 0); set(Calendar.MILLISECOND, 0)
        }
        val daysInMonth = firstDayCal.getActualMaximum(Calendar.DAY_OF_MONTH)

        // Month boundaries in MSK for transition route clipping
        val monthStartMs = dayStartMs(year, month, 1)
        val nextMonthStartMs = Calendar.getInstance(tz).apply {
            set(year, month, 1, 0, 0, 0); set(Calendar.MILLISECOND, 0)
            add(Calendar.MONTH, 1)
        }.timeInMillis

        // Day → presence flags, schedule entries, passenger durations
        val dayRoute = BooleanArray(daysInMonth + 1)
        val dayPassenger = BooleanArray(daysInMonth + 1)
        val dayEntries = Array<MutableList<ScheduleEntry>>(daysInMonth + 1) { mutableListOf() }
        val dayPassDurations = Array<MutableList<String>>(daysInMonth + 1) { mutableListOf() }

        // Дни отвлечений из календаря
        val dayRelease = Array<ReleaseType?>(daysInMonth + 1) { null }
        calendarDays.forEach { day ->
            if (day.isReleaseDay && day.dayOfMonth in 1..daysInMonth) {
                dayRelease[day.dayOfMonth] = day.releaseType
            }
        }

        // startFrac..endFrac within the day (0f=midnight, 1f=next midnight)
        data class WorkInterval(val startFrac: Float, val endFrac: Float)
        val dayWorkIntervals = Array<MutableList<WorkInterval>>(daysInMonth + 1) { mutableListOf() }

        routes.forEach { route ->
            val start = route.basicData.timeStartWork ?: return@forEach
            val endRaw = route.basicData.timeEndWork
            val endForColoring = endRaw ?: (start + 86_400_000L)

            val startCal = Calendar.getInstance(tz).apply { timeInMillis = start }
            val startMonth = startCal.get(Calendar.MONTH)
            val startYear = startCal.get(Calendar.YEAR)

            // ── Work intervals per day for proportional shading (only when endWork is known)
            if (endRaw != null) {
                for (day in 1..daysInMonth) {
                    val dayStart = dayStartMs(year, month, day)
                    val dayEnd = dayStart + 86_400_000L
                    val clippedStart = maxOf(start, dayStart)
                    val clippedEnd = minOf(endRaw, dayEnd)
                    if (clippedEnd > clippedStart) {
                        val startFrac = (clippedStart - dayStart).toFloat() / 86_400_000f
                        val endFrac = (clippedEnd - dayStart).toFloat() / 86_400_000f
                        dayWorkIntervals[day].add(WorkInterval(startFrac.coerceIn(0f, 1f), endFrac.coerceIn(0f, 1f)))
                    }
                }
            }

            // ── Cell entry for явка day (if in current month)
            if (startMonth == month && startYear == year) {
                val d = startCal.get(Calendar.DAY_OF_MONTH)
                if (d in 1..daysInMonth) {
                    val startStr = timeManager.formatTime(start)
                    val isTransitionOut = endRaw != null && run {
                        val ec = Calendar.getInstance(tz).apply { timeInMillis = endRaw }
                        ec.get(Calendar.MONTH) != month || ec.get(Calendar.YEAR) != year
                    }
                    val endStr = when {
                        endRaw == null -> ""
                        isTransitionOut -> "→"
                        else -> timeManager.formatTime(endRaw)
                    }
                    val durStr = if (endRaw != null) {
                        val effectiveEnd = if (isTransitionOut) nextMonthStartMs else endRaw
                        val brk = calcBreakOverlap(route, start, effectiveEnd)
                        val workedMs = maxOf(0L, effectiveEnd - start - brk)
                        if (workedMs > 0) " (${fmtDur(workedMs)})" else ""
                    } else ""
                    dayEntries[d].add(ScheduleEntry(line1 = startStr))
                }
            }

            // ── Cell entry for сдача day when явка was in previous month (transition in)
            if (endRaw != null) {
                val endCal = Calendar.getInstance(tz).apply { timeInMillis = endRaw }
                if ((endCal.get(Calendar.MONTH) == month && endCal.get(Calendar.YEAR) == year) &&
                    (startMonth != month || startYear != year)
                ) {
                    val endDay = endCal.get(Calendar.DAY_OF_MONTH).coerceIn(1, daysInMonth)
                    val endStr = timeManager.formatTime(endRaw)
                    val clippedStart = maxOf(start, monthStartMs)
                    val brk = calcBreakOverlap(route, clippedStart, endRaw)
                    val workedMs = maxOf(0L, endRaw - clippedStart - brk)
                    val durStr = if (workedMs > 0) " (${fmtDur(workedMs)})" else ""
                    dayEntries[endDay].add(ScheduleEntry(line1 = "← $endStr"))
                }
            }

            // ── Mark route days (for cell coloring)
            for (day in 1..daysInMonth) {
                val dayStart = dayStartMs(year, month, day)
                val dayEnd = dayStart + 86_400_000L
                if (start < dayEnd && endForColoring > dayStart) dayRoute[day] = true
            }

            // ── Passengers
            route.passengers.forEach { p ->
                val ps = p.timeDeparture?.takeIf { it > 0L } ?: return@forEach
                val pe = p.timeArrival?.takeIf { it > 0L } ?: (ps + 3_600_000L)
                for (day in 1..daysInMonth) {
                    val dayStart = dayStartMs(year, month, day)
                    val dayEnd = dayStart + 86_400_000L
                    if (ps < dayEnd && pe > dayStart) dayPassenger[day] = true
                }
                // Show duration on departure day
                val psCal = Calendar.getInstance(tz).apply { timeInMillis = ps }
                if (psCal.get(Calendar.MONTH) == month && psCal.get(Calendar.YEAR) == year) {
                    val d = psCal.get(Calendar.DAY_OF_MONTH)
                    if (d in 1..daysInMonth) {
                        val dur = fmtDur(pe - ps) ?: ""
                        if (dur.isNotBlank()) dayPassDurations[d].add(dur)
                    }
                }
            }
        }

        // Grid
        val cellW = contentWidth / 7f
        val cellH = 48f
        val headerH = 18f

        val totalRows = ((daysInMonth + firstDow(firstDayCal) - 1) / 7) + 1
        val gridHeight = headerH + totalRows * cellH + 4f
        pm.checkNewPage(gridHeight + 60f)
        val gridTop = pm.y

        // Day-of-week headers
        listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс").forEachIndexed { col, label ->
            val x = ml + col * cellW
            pm.canvas.drawRect(x, gridTop, x + cellW, gridTop + headerH, paintTableBorder)
            pm.canvas.drawRect(x, gridTop, x + cellW, gridTop + headerH, paintSectionFill)
            pm.canvas.drawText(
                label,
                x + cellW / 2f - paintSmall.measureText(label) / 2f,
                gridTop + headerH - 5f,
                paintBodyBold
            )
        }

        // Paint for centered явка time in cell
        val paintCellTime = Paint().apply {
            color = Color.BLACK; textSize = 11f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true
        }

        var col = firstDow(firstDayCal)
        var row = 0
        for (day in 1..daysInMonth) {
            val x = ml + col * cellW
            val cellY = gridTop + headerH + row * cellH

            pm.canvas.drawRect(x, cellY, x + cellW, cellY + cellH, paintTableBorder)

            // Пропорциональная заливка по времени работы (горизонтальная, голубая)
            dayWorkIntervals[day].forEach { interval ->
                val fillLeft = x + interval.startFrac * cellW
                val fillRight = x + interval.endFrac * cellW
                paintFill.color = Color.argb(70, 33, 150, 243)
                pm.canvas.drawRect(fillLeft.coerceAtLeast(x + 1f), cellY + 1f, fillRight.coerceAtMost(x + cellW - 1f), cellY + cellH - 1f, paintFill)
            }
            // Заливка для отвлечений из календаря (фиолетовая)
            if (dayRelease[day] != null) {
                paintFill.color = Color.argb(60, 156, 39, 176)
                pm.canvas.drawRect(x + 1f, cellY + 1f, x + cellW - 1f, cellY + cellH - 1f, paintFill)
            }
            // Оранжевая заливка для дней следования пассажиром (только если нет явки в этот день)
            if (dayPassenger[day] && dayEntries[day].isEmpty()) {
                paintFill.color = Color.argb(70, 255, 152, 0)
                pm.canvas.drawRect(x + 1f, cellY + 1f, x + cellW - 1f, cellY + cellH - 1f, paintFill)
            }

            // Day number — top-left
            pm.canvas.drawText(day.toString(), x + 3f, cellY + 10f, paintBody)

            // Отображение содержимого ячейки
            val entries = dayEntries[day]
            when {
                entries.size == 1 -> {
                    // Одна явка — по центру, крупно
                    val text = entries[0].line1
                    val textW = paintCellTime.measureText(text)
                    val textX = (x + (cellW - textW) / 2f).coerceAtLeast(x + 2f)
                    val textY = cellY + cellH / 2f + paintCellTime.textSize / 2f - 2f
                    pm.canvas.drawText(text, textX, textY, paintCellTime)
                }
                entries.size >= 2 -> {
                    // Несколько явок — в столбик по центру, тот же шрифт
                    val spacing = paintCellTime.textSize + 3f
                    val totalH = entries.size * spacing
                    var textY = cellY + (cellH - totalH) / 2f + paintCellTime.textSize
                    entries.forEach { e ->
                        val textW = paintCellTime.measureText(e.line1)
                        val textX = (x + (cellW - textW) / 2f).coerceAtLeast(x + 2f)
                        pm.canvas.drawText(e.line1, textX, textY, paintCellTime)
                        textY += spacing
                    }
                }
                else -> {
                    // Нет явки
                    // Показываем тип отвлечения (если есть)
                    dayRelease[day]?.let { rt ->
                        val label = when (rt) {
                            is ReleaseType.Vacation -> "Отпуск"
                            is ReleaseType.SickLeave -> "Больн."
                            is ReleaseType.Courses -> "Курсы"
                            is ReleaseType.Donor -> "Донор"
                            is ReleaseType.ChildCare -> "Уход"
                            is ReleaseType.BusinessTrip -> "Команд."
                            is ReleaseType.TechnicalStudy -> "Техзан."
                            else -> "Отвл."
                        }
                        val textW = paintTiny.measureText(label)
                        val textX = (x + (cellW - textW) / 2f).coerceAtLeast(x + 2f)
                        val textY = cellY + cellH / 2f + paintTiny.textSize / 2f - 2f
                        pm.canvas.drawText(label, textX, textY, paintTiny)
                    } ?: run {
                        // Показываем длительность следования пассажиром
                        dayPassDurations[day].firstOrNull()?.let { pDur ->
                            val textW = paintTiny.measureText(pDur)
                            val textX = (x + (cellW - textW) / 2f).coerceAtLeast(x + 2f)
                            val textY = cellY + cellH / 2f + paintTiny.textSize / 2f - 2f
                            pm.canvas.drawText(pDur, textX, textY, paintTiny)
                        }
                    }
                }
            }

            col++
            if (col == 7) { col = 0; row++ }
        }

        pm.y = gridTop + headerH + (row + if (col > 0) 1 else 0) * cellH + 8f

        pm.y += 8f

        // Summary — total work time with month clipping for transitions
        val totalWorkMs = routes.sumOf { r ->
            val s = r.basicData.timeStartWork ?: return@sumOf 0L
            val e = r.basicData.timeEndWork ?: return@sumOf 0L
            val sCal = Calendar.getInstance(tz).apply { timeInMillis = s }
            val eCal = Calendar.getInstance(tz).apply { timeInMillis = e }
            val isTransition = sCal.get(Calendar.MONTH) != month || sCal.get(Calendar.YEAR) != year ||
                eCal.get(Calendar.MONTH) != month || eCal.get(Calendar.YEAR) != year
            if (isTransition) {
                val clippedStart = maxOf(s, monthStartMs)
                val clippedEnd = minOf(e, nextMonthStartMs)
                if (clippedEnd > clippedStart) {
                    val brk = calcBreakOverlap(r, clippedStart, clippedEnd)
                    maxOf(0L, clippedEnd - clippedStart - brk)
                } else 0L
            } else {
                val brk = calcBreakOverlap(r, s, e)
                maxOf(0L, e - s - brk)
            }
        }
        val totalPassMs = routes.sumOf { r ->
            r.passengers.sumOf { p ->
                val s = p.timeDeparture ?: return@sumOf 0L
                val e = p.timeArrival ?: return@sumOf 0L
                maxOf(0L, e - s)
            }
        }

        // Норма на месяц из календарных данных
        val standardNormaHours = calendarDays.sumOf { day ->
            workScheduleProfile.effectiveHours(
                kotlinx.datetime.LocalDate(year, month + 1, day.dayOfMonth),
                day.tag,
            )
        }
        // Норму уменьшают не все отвлечения: «Выходной» — перенос выходного дня,
        // а «Технические занятия» оплачиваются отдельно по среднему часу
        // (см. UtilForMonthOfYear.reducesNorma / NormaUseCase).
        val detachmentHours = calendarDays.filter {
            it.isReleaseDay &&
                it.releaseType != ReleaseType.DayOff &&
                it.releaseType != ReleaseType.TechnicalStudy
        }.sumOf { day ->
            workScheduleProfile.effectiveHours(
                kotlinx.datetime.LocalDate(year, month + 1, day.dayOfMonth),
                day.tag,
            )
        }
        val personalNormaHours = standardNormaHours - detachmentHours

        if (standardNormaHours > 0) pm.tableRow("Норма на месяц", "$standardNormaHours ч.")
        if (personalNormaHours > 0 && personalNormaHours != standardNormaHours) pm.tableRow("Личная норма", "$personalNormaHours ч.")
        pm.tableRow("Маршрутов в месяце", "${routes.size}")
        pm.tableRow("Отработано в месяце", fmtDur(totalWorkMs))
        if (totalPassMs > 0) pm.tableRow("Следование пассажиром", fmtDur(totalPassMs))
        if (detachmentHours > 0) pm.tableRow("Отвлечения", "$detachmentHours ч.")
        val detachmentMs = detachmentHours * 3_600_000L
        if (totalWorkMs > 0 || detachmentMs > 0) pm.tableRow("Всего", fmtDur(totalWorkMs + detachmentMs))
        pm.y += 4f
        pm.separator()
    }

    private fun dayStartMs(year: Int, month: Int, day: Int): Long =
        Calendar.getInstance(tz).apply {
            set(year, month, day, 0, 0, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    /** firstDow: 0=Mon … 6=Sun */
    private fun firstDow(cal: Calendar): Int {
        val dow = cal.get(Calendar.DAY_OF_WEEK)  // 1=Sun … 7=Sat
        return (dow - 2 + 7) % 7
    }

    // ─── Salary section ───────────────────────────────────────────────────────────

    private fun drawSalary(pm: PageManager, s: SalaryCalculationUIState, monthLabel: String = "") {
        payCurrency = s.currency   // валюта расчётного листа — по стране из настроек (₽ / ₸ / Br)
        val monthTitle = monthLabel.ifBlank { s.month }
        pm.text("Расчётный лист${if (monthTitle.isNotBlank()) " за $monthTitle" else ""}", paintTitle)
        pm.y += 2f
        pm.separator()

        // Начисления
        pm.checkNewPage(28f)
        pm.y += 8f
        pm.canvas.drawRect(ml, pm.y - 12f, ml + contentWidth, pm.y + 2f, paintSectionFill)
        pm.canvas.drawRect(ml, pm.y - 12f, ml + contentWidth, pm.y + 2f, paintTableBorder)
        pm.canvas.drawText("Начисления", ml + 4f, pm.y, paintSection)
        pm.y += 14f
        pm.salaryHeader()

        fun row(desc: String, h: Long?, pct: Double?, money: Double?) {
            val ms = fmtMoney(money)
            if (ms.isBlank()) return   // скрываем строки с нулевой/пустой суммой
            pm.salaryRow(desc, fmtHours(h), fmtPct(pct), ms)
        }

        row("Оплата по тарифу", s.paymentAtTariffHours, null, s.paymentAtTariffMoney)
        row("Ночные часы", s.paymentNightTimeHours, s.paymentNightTimePercent, s.paymentNightTimeMoney)
        row("Пассажиром", s.paymentAtPassengerHours, null, s.paymentAtPassengerMoney)
        row("Резервом", s.paymentAtSingleLocomotiveHours, null, s.paymentAtSingleLocomotiveMoney)
        row("Праздничные", s.paymentHolidayHours, null, s.paymentHolidayMoney)
        row("По среднему", s.averagePaymentHours, null, s.averagePaymentMoney)
        row("По уходу за ребёнком-инвалидом", s.caringForDisableChildrenHours, null, s.caringForDisableChildrenMoney)
        row("Командировка (по среднему)", s.businessTripHours, null, s.businessTripMoney)
        row("Технические занятия", s.technicalStudyHours, null, s.technicalStudyMoney)

        // Percentage-only surcharges
        fun rowPct(desc: String, pct: Double?, money: Double?) {
            val ms = fmtMoney(money)
            if (ms.isBlank()) return   // скрываем строки с нулевой/пустой суммой
            pm.salaryRow(desc, "", fmtPct(pct), ms)
        }
        rowPct("Зональная надбавка", s.zonalSurchargePercent, s.zonalSurchargeMoney)
        rowPct("Надбавка за класс квалификации", s.surchargeQualificationClassPercent, s.surchargeQualificationClassMoney)
        s.linearMileageAccruals.forEach { accrual ->
            pm.salaryRow(
                "Пробег ${accrual.phaseName} (${"%.2f".format(accrual.rate)} ₽/км)",
                "",
                "",
                fmtMoney(accrual.money),
            )
        }
        row("В одно лицо (груз.)", s.onePersonOperationHours, s.onePersonOperationPercent, s.onePersonOperationMoney)
        row("В одно лицо (пас.)", s.onePersonOperationPassengerTrainHours, s.onePersonOperationPassengerTrainPercent, s.onePersonOperationPassengerTrainMoney)
        rowPct("Вредность", s.harmfulnessSurchargePercent, s.harmfulnessSurchargeMoney)
        rowPct("Районный коэффициент", s.districtSurchargeCoefficient, s.districtSurchargeMoney)
        rowPct("Северная надбавка", s.nordicSurchargePercent, s.nordicSurchargeMoney)

        s.surchargeExtendedServicePhaseHour.forEachIndexed { i, h ->
            val m = fmtMoney(s.surchargeExtendedServicePhaseMoney.getOrNull(i))
            if (m.isBlank()) return@forEachIndexed
            pm.salaryRow("Удлинённое плечо ${i + 1}", fmtHours(h), fmtPctStr(s.surchargeExtendedServicePhasePercent.getOrNull(i)), m)
        }
        s.surchargeHeavyTransHour.forEachIndexed { i, h ->
            val m = fmtMoney(s.surchargeHeavyTransMoney.getOrNull(i))
            if (m.isBlank()) return@forEachIndexed
            pm.salaryRow("Тяжёлые поезда ${i + 1}", fmtHours(h), fmtPctStr(s.surchargeHeavyTransPercent.getOrNull(i)), m)
        }
        s.surchargeLongTrainHour.forEachIndexed { i, h ->
            val m = fmtMoney(s.surchargeLongTrainMoney.getOrNull(i))
            if (m.isBlank()) return@forEachIndexed
            pm.salaryRow("Длинносост. поезда ${i + 1}", fmtHours(h), fmtPctStr(s.surchargeLongTrainPercent.getOrNull(i)), m)
        }
        row(
            "Доплата за ПДМ (6000 т. и 350 осей)",
            s.surchargeHeavyLongDistanceTrainsHours,
            s.surchargeHeavyLongDistanceTrainsPercent,
            s.surchargeHeavyLongDistanceTrainsMoney
        )
        fmtMoney(s.surchargeDoubledTrainFirstMoney).takeIf { it.isNotBlank() }?.let { m ->
            pm.salaryRow("Сдвоенные (30%)", fmtHours(s.surchargeDoubledTrainFirstHours), "30%", m)
        }
        fmtMoney(s.surchargeDoubledTrainSecondMoney).takeIf { it.isNotBlank() }?.let { m ->
            pm.salaryRow("Сдвоенные (15%)", fmtHours(s.surchargeDoubledTrainSecondHours), "15%", m)
        }
        row("Сверхурочные", s.paymentAtOvertimeHours, null, s.paymentAtOvertimeMoney)
        fmtMoney(s.surchargeAtOvertime05Money).takeIf { it.isNotBlank() }?.let { m ->
            pm.salaryRow("Доп. сверхурочные (50%)", fmtHours(s.surchargeAtOvertime05Hours), "50%", m)
        }
        fmtMoney(s.surchargeAtOvertimeMoney).takeIf { it.isNotBlank() }?.let { m ->
            pm.salaryRow("Доп. сверхурочные (100%)", fmtHours(s.surchargeAtOvertimeHours), "100%", m)
        }
        row("Переотдых", s.restInExcessOfTheNormTime, null, s.restInExcessOfTheNormMoney)
        fmtMoney(s.otherSurchargeMoney).takeIf { it.isNotBlank() }?.let { m ->
            pm.salaryRow("Прочие надбавки", "", fmtPct(s.otherSurchargePercent), m)
        }

        // Total charged
        pm.salaryRow("Итого начислено", "", "", fmtMoney(s.totalChargedMoney), bold = true)
        pm.y += 4f

        // Удержания
        pm.checkNewPage(28f)
        pm.y += 8f
        pm.canvas.drawRect(ml, pm.y - 12f, ml + contentWidth, pm.y + 2f, paintSectionFill)
        pm.canvas.drawRect(ml, pm.y - 12f, ml + contentWidth, pm.y + 2f, paintTableBorder)
        pm.canvas.drawText("Удержания", ml + 4f, pm.y, paintSection)
        pm.y += 14f
        pm.retentionHeader()
        if (fmtMoney(s.retentionNdfl).isNotBlank()) pm.retentionRow("НДФЛ (13%)", "13%", fmtMoney(s.retentionNdfl))
        if (fmtMoney(s.unionistsRetention).isNotBlank()) pm.retentionRow("Профсоюз", "", fmtMoney(s.unionistsRetention))
        if (fmtMoney(s.otherRetention).isNotBlank()) pm.retentionRow("Прочие удержания", "", fmtMoney(s.otherRetention))
        if (fmtMoney(s.welfareRetention).isNotBlank()) pm.retentionRow("Благосостояние", "", fmtMoney(s.welfareRetention))
        if (fmtMoney(s.alimonyRetention).isNotBlank()) pm.retentionRow("Алименты", "", fmtMoney(s.alimonyRetention))
        pm.retentionRow("Всего удержано", "", fmtMoney(s.totalRetention), bold = true)

        pm.y += 8f
        pm.checkNewPage(20f)
        pm.text("К выдаче: ${fmtMoney(s.toBeCredited)}", paintTitle)
        pm.y += 4f
        pm.separator()
    }
}
