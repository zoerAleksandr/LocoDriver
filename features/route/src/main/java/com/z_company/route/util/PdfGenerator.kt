package com.z_company.route.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.z_company.core.util.ConverterLongToTime
import com.z_company.core.util.TimeManager
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
            checkNewPage(22f)
            y += 4f
            // background behind section title
            canvas.drawRect(ml, y - 11f, ml + contentWidth, y + 3f, paintSectionFill)
            canvas.drawText(text, ml + 4f, y, paintSection)
            y += 14f  // достаточный отступ чтобы таблица не перекрывала заголовок
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

    private fun fmtTime(millis: Long?) = millis?.let { timeManager.formatTime(it) }
    private fun fmtDate(millis: Long?) = millis?.let { timeManager.formatDate(it) }
    private fun fmtDT(millis: Long?) = millis?.let { "${timeManager.formatDate(it)} ${timeManager.formatTime(it)}" }
    private fun fmtDur(ms: Long?) = ms?.let { ConverterLongToTime.getTimeInStringFormat(it) }
    private fun fmtMoney(v: Double?) = if (v == null || v == 0.0) "" else "${"%.2f".format(v)} ₽"
    private fun fmtHours(v: Long?) = if (v == null || v == 0L) "" else ConverterLongToTime.getTimeInStringFormat(v)
    private fun fmtPct(v: Double?) = if (v == null || v == 0.0) "" else "${v}%"
    private fun fmtPctStr(v: String?) = if (v.isNullOrBlank() || v == "0" || v == "0.0") "" else "$v%"

    // ─── Public entry point ──────────────────────────────────────────────────────

    fun generatePdf(
        routes: List<Route>,
        salaryState: SalaryCalculationUIState?,
        monthLabel: String,
        sections: PdfSections
    ): File {
        val document = PdfDocument()
        val pm = PageManager(document)

        if (sections.includeRouteDetails) {
            pm.newPage()
            drawRouteDetails(pm, routes, monthLabel)
        }
        if (sections.includeSchedule) {
            pm.newPage()
            drawSchedule(pm, routes, monthLabel)
        }
        if (sections.includeSalary) {
            pm.newPage()
            if (salaryState != null) {
                drawSalary(pm, salaryState)
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

        val file = File(context.cacheDir, "mashinist_${monthLabel.replace(" ", "_")}.pdf")
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

        routes.forEach { route -> drawRoute(pm, route) }
    }

    private fun drawRoute(pm: PageManager, route: Route) {
        val bd = route.basicData
        val num = bd.number?.takeIf { it.isNotBlank() } ?: "б/н"
        val dateStr = fmtDT(bd.timeStartWork) ?: "—"
        pm.checkNewPage(50f)
        pm.sectionTitle("Маршрут №$num   $dateStr")

        // ── BasicData table
        val workDur = if (bd.timeStartWork != null && bd.timeEndWork != null) {
            val breakMs = if (bd.timeStartBreak != null && bd.timeEndBreak != null)
                (bd.timeEndBreak!! - bd.timeStartBreak!!) else 0L
            fmtDur(maxOf(0L, bd.timeEndWork!! - bd.timeStartWork!! - breakMs))
        } else null

        pm.tableRow("Явка", fmtDT(bd.timeStartWork))
        pm.tableRow("Сдача", fmtDT(bd.timeEndWork))
        pm.tableRow("Время в работе", workDur, valueBold = true)
        pm.tableRow("Начало перерыва", fmtDT(bd.timeStartBreak))
        pm.tableRow("Окончание перерыва", fmtDT(bd.timeEndBreak))
        if (bd.restPointOfTurnover) pm.tableRow("Отдых в пункте оборота", "Да")
        if (bd.isOnePersonOperation) pm.tableRow("Работа в одно лицо", "Да")
        pm.tableRow("Примечание", bd.notes)

        // ── Locomotives
        route.locomotives.forEachIndexed { idx, loco ->
            pm.checkNewPage(40f)
            pm.y += 3f
            val locoType = if (loco.type == LocoType.ELECTRIC) "Электровоз" else "Тепловоз"
            val locoId = buildString {
                loco.series?.takeIf { it.isNotBlank() }?.let { append(it).append(" ") }
                loco.number?.takeIf { it.isNotBlank() }?.let { append(it).append(" ") }
                append("($locoType)")
            }
            pm.tableRow("Локомотив ${idx + 1}", locoId, isHeaderRow = true)
            pm.tableRow("Начало приёмки", fmtDT(loco.timeStartOfAcceptance))
            pm.tableRow("Конец приёмки", fmtDT(loco.timeEndOfAcceptance))
            pm.tableRow("Начало сдачи", fmtDT(loco.timeStartOfDelivery))
            pm.tableRow("Конец сдачи", fmtDT(loco.timeEndOfDelivery))

            if (loco.type == LocoType.ELECTRIC) {
                loco.electricSectionList.forEachIndexed { si, sec ->
                    val vals = buildList {
                        sec.acceptedEnergy?.takeIf { it != 0.0 }?.let { add("Тяга: пр ${"%.1f".format(it)}") }
                        sec.deliveryEnergy?.takeIf { it != 0.0 }?.let { add("сд ${"%.1f".format(it)} кВт⋅ч") }
                        sec.acceptedRecovery?.takeIf { it != 0.0 }?.let { add("Рек: пр ${"%.1f".format(it)}") }
                        sec.deliveryRecovery?.takeIf { it != 0.0 }?.let { add("сд ${"%.1f".format(it)} кВт⋅ч") }
                    }.joinToString("  ")
                    if (vals.isNotBlank()) pm.tableRow("  Секция ${si + 1}", vals)
                }
            } else {
                loco.dieselSectionList.forEachIndexed { si, sec ->
                    val vals = buildList {
                        sec.acceptedFuel?.takeIf { it != 0.0 }?.let { add("пр ${"%.2f".format(it)} л") }
                        sec.deliveryFuel?.takeIf { it != 0.0 }?.let { add("сд ${"%.2f".format(it)} л") }
                    }.joinToString("  ")
                    if (vals.isNotBlank()) pm.tableRow("  Секция ${si + 1} (топливо)", vals)
                }
            }
        }

        // ── Trains
        route.trains.forEachIndexed { idx, train ->
            pm.checkNewPage(40f)
            pm.y += 3f
            val trainId = buildString {
                train.number?.takeIf { it.isNotBlank() }?.let { append("№$it") }
                train.weight?.takeIf { it.isNotBlank() }?.let { append("  $it т") }
                train.axle?.takeIf { it.isNotBlank() }?.let { append("  ${it} ос.") }
            }.ifBlank { "—" }
            pm.tableRow("Поезд ${idx + 1}", trainId, isHeaderRow = true)
            train.distance?.takeIf { it.isNotBlank() }?.let { pm.tableRow("Расстояние", "$it км") }

            train.stations.forEach { station ->
                val name = station.stationName ?: "—"
                val arr = fmtTime(station.timeArrival) ?: "—"
                val dep = fmtTime(station.timeDeparture) ?: "—"
                pm.tableRow("  $name", "приб. $arr  отпр. $dep")
            }
        }

        // ── Passengers
        route.passengers.forEachIndexed { idx, passenger ->
            pm.checkNewPage(36f)
            pm.y += 3f
            pm.tableRow("Пассажир ${idx + 1}", "", isHeaderRow = true)
            pm.tableRow("Поезд", passenger.trainNumber)
            pm.tableRow("Откуда", passenger.stationDeparture)
            pm.tableRow("Куда", passenger.stationArrival)
            pm.tableRow("Отправление", fmtDT(passenger.timeDeparture))
            pm.tableRow("Прибытие", fmtDT(passenger.timeArrival))
            pm.tableRow("Примечание", passenger.notes)
        }

        pm.y += 4f
        pm.separator()
    }

    // ─── Schedule section ─────────────────────────────────────────────────────────

    private fun drawSchedule(pm: PageManager, routes: List<Route>, monthLabel: String) {
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

        // Day → (hasRoute, hasPassenger, appearance times)
        val dayRoute = BooleanArray(daysInMonth + 1)
        val dayPassenger = BooleanArray(daysInMonth + 1)
        val dayAppearanceTimes = Array<MutableList<String>>(daysInMonth + 1) { mutableListOf() }

        routes.forEach { route ->
            val start = route.basicData.timeStartWork ?: return@forEach
            val end = route.basicData.timeEndWork ?: (start + 86_400_000L)
            // Явка — день начала маршрута
            val startCal = Calendar.getInstance(tz).apply { timeInMillis = start }
            if (startCal.get(Calendar.MONTH) == month && startCal.get(Calendar.YEAR) == year) {
                val d = startCal.get(Calendar.DAY_OF_MONTH)
                if (d in 1..daysInMonth) dayAppearanceTimes[d].add(timeManager.formatTime(start))
            }
            for (day in 1..daysInMonth) {
                val dayStart = dayStartMs(year, month, day)
                val dayEnd = dayStart + 86_400_000L
                if (start < dayEnd && end > dayStart) dayRoute[day] = true
            }
            route.passengers.forEach { p ->
                val ps = p.timeDeparture ?: return@forEach
                val pe = p.timeArrival ?: (ps + 3_600_000L)
                for (day in 1..daysInMonth) {
                    val dayStart = dayStartMs(year, month, day)
                    val dayEnd = dayStart + 86_400_000L
                    if (ps < dayEnd && pe > dayStart) dayPassenger[day] = true
                }
            }
        }

        // Grid
        val cellW = contentWidth / 7f
        val cellH = 34f
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

        var col = firstDow(firstDayCal)
        var row = 0
        for (day in 1..daysInMonth) {
            val x = ml + col * cellW
            val cellY = gridTop + headerH + row * cellH

            pm.canvas.drawRect(x, cellY, x + cellW, cellY + cellH, paintTableBorder)

            when {
                dayRoute[day] && dayPassenger[day] -> {
                    paintFill.color = Color.argb(70, 33, 150, 243)
                    pm.canvas.drawRect(x + 1f, cellY + 1f, x + cellW / 2f, cellY + cellH - 1f, paintFill)
                    paintFill.color = Color.argb(70, 255, 152, 0)
                    pm.canvas.drawRect(x + cellW / 2f, cellY + 1f, x + cellW - 1f, cellY + cellH - 1f, paintFill)
                }
                dayRoute[day] -> {
                    paintFill.color = Color.argb(70, 33, 150, 243)
                    pm.canvas.drawRect(x + 1f, cellY + 1f, x + cellW - 1f, cellY + cellH - 1f, paintFill)
                }
                dayPassenger[day] -> {
                    paintFill.color = Color.argb(70, 255, 152, 0)
                    pm.canvas.drawRect(x + 1f, cellY + 1f, x + cellW - 1f, cellY + cellH - 1f, paintFill)
                }
            }

            // Число — в верхнем левом углу
            val dayStr = day.toString()
            val dp = if (dayRoute[day] || dayPassenger[day]) paintBodyBold else paintBody
            pm.canvas.drawText(dayStr, x + 3f, cellY + 11f, dp)

            // Времена явки — одно под другим в нижней части ячейки
            dayAppearanceTimes[day].forEachIndexed { i, t ->
                pm.canvas.drawText(t, x + 3f, cellY + 22f + i * 9f, paintSmall)
            }

            col++
            if (col == 7) { col = 0; row++ }
        }

        pm.y = gridTop + headerH + (row + if (col > 0) 1 else 0) * cellH + 8f

        // Legend
        pm.y += 4f
        paintFill.color = Color.argb(80, 33, 150, 243)
        pm.canvas.drawRect(ml, pm.y - 8f, ml + 12f, pm.y + 2f, paintFill)
        pm.canvas.drawText("— Явка", ml + 16f, pm.y, paintSmall)
        pm.y += 12f
        paintFill.color = Color.argb(80, 255, 152, 0)
        pm.canvas.drawRect(ml, pm.y - 8f, ml + 12f, pm.y + 2f, paintFill)
        pm.canvas.drawText("— Отвлечение", ml + 16f, pm.y, paintSmall)
        pm.y += 14f

        // Summary
        val totalWorkMs = routes.sumOf { r ->
            val s = r.basicData.timeStartWork ?: return@sumOf 0L
            val e = r.basicData.timeEndWork ?: return@sumOf 0L
            val brk = if (r.basicData.timeStartBreak != null && r.basicData.timeEndBreak != null)
                (r.basicData.timeEndBreak!! - r.basicData.timeStartBreak!!) else 0L
            maxOf(0L, e - s - brk)
        }
        val totalPassMs = routes.sumOf { r ->
            r.passengers.sumOf { p ->
                val s = p.timeDeparture ?: return@sumOf 0L
                val e = p.timeArrival ?: return@sumOf 0L
                maxOf(0L, e - s)
            }
        }

        pm.tableRow("Маршрутов в месяце", "${routes.size}")
        pm.tableRow("Отработано", fmtDur(totalWorkMs))
        if (totalPassMs > 0) pm.tableRow("Время в отвлечениях", fmtDur(totalPassMs))
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

    private fun drawSalary(pm: PageManager, s: SalaryCalculationUIState) {
        pm.text("Расчётный листок${if (s.month.isNotBlank()) " — ${s.month}" else ""}", paintTitle)
        pm.y += 2f
        pm.separator()

        // Начисления
        pm.checkNewPage(28f)
        pm.canvas.drawRect(ml, pm.y - 12f, ml + contentWidth, pm.y + 2f, paintSectionFill)
        pm.canvas.drawText("Начисления", ml + 4f, pm.y, paintSection)
        pm.y += 14f
        pm.salaryHeader()

        fun row(desc: String, h: Long?, pct: Double?, money: Double?) {
            val hs = fmtHours(h); val ps = fmtPct(pct); val ms = fmtMoney(money)
            if (hs.isBlank() && ps.isBlank() && ms.isBlank()) return
            pm.salaryRow(desc, hs, ps, ms)
        }

        row("Оплата по тарифу", s.paymentAtTariffHours, null, s.paymentAtTariffMoney)
        row("Ночные часы", s.paymentNightTimeHours, s.paymentNightTimePercent, s.paymentNightTimeMoney)
        row("Пассажиром", s.paymentAtPassengerHours, null, s.paymentAtPassengerMoney)
        row("Резервом", s.paymentAtSingleLocomotiveHours, null, s.paymentAtSingleLocomotiveMoney)
        row("Праздничные", s.paymentHolidayHours, null, s.paymentHolidayMoney)
        row("По среднему", s.averagePaymentHours, null, s.averagePaymentMoney)
        row("По уходу за ребёнком-инвалидом", s.caringForDisableChildrenHours, null, s.caringForDisableChildrenMoney)

        // Percentage-only surcharges
        fun rowPct(desc: String, pct: Double?, money: Double?) {
            val ps = fmtPct(pct); val ms = fmtMoney(money)
            if (ps.isBlank() && ms.isBlank()) return
            pm.salaryRow(desc, "", ps, ms)
        }
        rowPct("Зональная надбавка", s.zonalSurchargePercent, s.zonalSurchargeMoney)
        rowPct("Надбавка за класс квалификации", s.surchargeQualificationClassPercent, s.surchargeQualificationClassMoney)
        rowPct("В одно лицо (груз.)", s.onePersonOperationPercent, s.onePersonOperationMoney)
        rowPct("В одно лицо (пас.)", s.onePersonOperationPassengerTrainPercent, s.onePersonOperationPassengerTrainMoney)
        rowPct("Вредность", s.harmfulnessSurchargePercent, s.harmfulnessSurchargeMoney)
        rowPct("Районный коэффициент", s.districtSurchargeCoefficient, s.districtSurchargeMoney)
        rowPct("Северная надбавка", s.nordicSurchargePercent, s.nordicSurchargeMoney)

        s.surchargeExtendedServicePhaseHour.forEachIndexed { i, h ->
            val p = fmtPctStr(s.surchargeExtendedServicePhasePercent.getOrNull(i))
            val m = fmtMoney(s.surchargeExtendedServicePhaseMoney.getOrNull(i))
            if (fmtHours(h).isBlank() && p.isBlank() && m.isBlank()) return@forEachIndexed
            pm.salaryRow("Удлинённое плечо ${i + 1}", fmtHours(h), p, m)
        }
        s.surchargeHeavyTransHour.forEachIndexed { i, h ->
            val p = fmtPctStr(s.surchargeHeavyTransPercent.getOrNull(i))
            val m = fmtMoney(s.surchargeHeavyTransMoney.getOrNull(i))
            if (fmtHours(h).isBlank() && p.isBlank() && m.isBlank()) return@forEachIndexed
            pm.salaryRow("Тяжёлые поезда ${i + 1}", fmtHours(h), p, m)
        }
        s.surchargeLongTrainHour.forEachIndexed { i, h ->
            val p = fmtPctStr(s.surchargeLongTrainPercent.getOrNull(i))
            val m = fmtMoney(s.surchargeLongTrainMoney.getOrNull(i))
            if (fmtHours(h).isBlank() && p.isBlank() && m.isBlank()) return@forEachIndexed
            pm.salaryRow("Длинносост. поезда ${i + 1}", fmtHours(h), p, m)
        }
        if (fmtHours(s.surchargeDoubledTrainFirstHours).isNotBlank() || fmtMoney(s.surchargeDoubledTrainFirstMoney).isNotBlank())
            pm.salaryRow("Сдвоенные (30%)", fmtHours(s.surchargeDoubledTrainFirstHours), "30%", fmtMoney(s.surchargeDoubledTrainFirstMoney))
        if (fmtHours(s.surchargeDoubledTrainSecondHours).isNotBlank() || fmtMoney(s.surchargeDoubledTrainSecondMoney).isNotBlank())
            pm.salaryRow("Сдвоенные (15%)", fmtHours(s.surchargeDoubledTrainSecondHours), "15%", fmtMoney(s.surchargeDoubledTrainSecondMoney))
        row("Сверхурочные", s.paymentAtOvertimeHours, null, s.paymentAtOvertimeMoney)
        if (fmtHours(s.surchargeAtOvertime05Hours).isNotBlank() || fmtMoney(s.surchargeAtOvertime05Money).isNotBlank())
            pm.salaryRow("Доп. сверхурочные (50%)", fmtHours(s.surchargeAtOvertime05Hours), "50%", fmtMoney(s.surchargeAtOvertime05Money))
        if (fmtHours(s.surchargeAtOvertimeHours).isNotBlank() || fmtMoney(s.surchargeAtOvertimeMoney).isNotBlank())
            pm.salaryRow("Доп. сверхурочные (100%)", fmtHours(s.surchargeAtOvertimeHours), "100%", fmtMoney(s.surchargeAtOvertimeMoney))
        row("Переотдых", s.restInExcessOfTheNormTime, null, s.restInExcessOfTheNormMoney)
        if (fmtMoney(s.otherSurchargeMoney).isNotBlank())
            pm.salaryRow("Прочие надбавки", "", fmtPct(s.otherSurchargePercent), fmtMoney(s.otherSurchargeMoney))

        // Total charged
        pm.salaryRow("Итого начислено", "", "", fmtMoney(s.totalChargedMoney), bold = true)
        pm.y += 4f

        // Удержания
        pm.checkNewPage(28f)
        pm.canvas.drawRect(ml, pm.y - 12f, ml + contentWidth, pm.y + 2f, paintSectionFill)
        pm.canvas.drawText("Удержания", ml + 4f, pm.y, paintSection)
        pm.y += 14f
        pm.retentionHeader()
        if (fmtMoney(s.retentionNdfl).isNotBlank()) pm.retentionRow("НДФЛ (13%)", "13%", fmtMoney(s.retentionNdfl))
        if (fmtMoney(s.unionistsRetention).isNotBlank()) pm.retentionRow("Профсоюз", "", fmtMoney(s.unionistsRetention))
        if (fmtMoney(s.otherRetention).isNotBlank()) pm.retentionRow("Прочие удержания", "", fmtMoney(s.otherRetention))
        pm.retentionRow("Всего удержано", "", fmtMoney(s.totalRetention), bold = true)

        pm.y += 8f
        pm.checkNewPage(20f)
        pm.text("К выдаче: ${fmtMoney(s.toBeCredited)}", paintTitle)
        pm.y += 4f
        pm.separator()
    }
}
