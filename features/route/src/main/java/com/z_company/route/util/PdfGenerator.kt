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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class PdfGenerator(private val context: Context) {

    private val pageWidth = 595
    private val pageHeight = 842
    private val ml = 36f    // margin left
    private val mr = 36f    // margin right
    private val mt = 52f    // margin top (below header)
    private val mb = 36f    // margin bottom
    private val contentWidth = pageWidth - ml - mr

    private val timeManager = TimeManager("GMT+3")

    // Paints
    private val paintHeader = Paint().apply {
        color = Color.DKGRAY
        textSize = 7f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        isAntiAlias = true
    }
    private val paintHeaderLine = Paint().apply {
        color = Color.LTGRAY
        strokeWidth = 0.5f
        style = Paint.Style.STROKE
    }
    private val paintTitle = Paint().apply {
        color = Color.BLACK
        textSize = 14f
        typeface = Typeface.DEFAULT_BOLD
        isAntiAlias = true
    }
    private val paintSection = Paint().apply {
        color = Color.BLACK
        textSize = 11f
        typeface = Typeface.DEFAULT_BOLD
        isAntiAlias = true
    }
    private val paintBody = Paint().apply {
        color = Color.BLACK
        textSize = 9f
        typeface = Typeface.DEFAULT
        isAntiAlias = true
    }
    private val paintBodyBold = Paint().apply {
        color = Color.BLACK
        textSize = 9f
        typeface = Typeface.DEFAULT_BOLD
        isAntiAlias = true
    }
    private val paintSmall = Paint().apply {
        color = Color.DKGRAY
        textSize = 7f
        typeface = Typeface.DEFAULT
        isAntiAlias = true
    }
    private val paintLine = Paint().apply {
        color = Color.LTGRAY
        strokeWidth = 0.5f
        style = Paint.Style.STROKE
    }
    private val paintFill = Paint().apply {
        style = Paint.Style.FILL
    }
    private val paintTableBorder = Paint().apply {
        color = Color.GRAY
        strokeWidth = 0.5f
        style = Paint.Style.STROKE
    }

    // ─── Page manager ───────────────────────────────────────────────────────────

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

        fun needNewPage(nextBlockHeight: Float = 20f): Boolean =
            y + nextBlockHeight > pageHeight - mb

        fun checkNewPage(nextBlockHeight: Float = 20f) {
            if (needNewPage(nextBlockHeight)) newPage()
        }

        /** Draws a line and advances y */
        fun drawLine() {
            canvas.drawLine(ml, y, pageWidth - mr, y, paintLine)
            y += 4f
        }

        /** Draws text with line wrap and advances y */
        fun drawText(text: String, paint: Paint, indent: Float = 0f): Float {
            val x = ml + indent
            val maxWidth = contentWidth - indent
            val words = text.split(" ")
            val sb = StringBuilder()
            for (word in words) {
                val test = if (sb.isEmpty()) word else "$sb $word"
                if (paint.measureText(test) > maxWidth && sb.isNotEmpty()) {
                    checkNewPage(paint.textSize + 4)
                    canvas.drawText(sb.toString(), x, y, paint)
                    y += paint.textSize + 2f
                    sb.clear()
                    sb.append(word)
                } else {
                    if (sb.isNotEmpty()) sb.append(" ")
                    sb.append(word)
                }
            }
            if (sb.isNotEmpty()) {
                checkNewPage(paint.textSize + 4)
                canvas.drawText(sb.toString(), x, y, paint)
                y += paint.textSize + 2f
            }
            return y
        }

        /** Key–value row */
        fun drawKV(key: String, value: String?, indent: Float = 8f) {
            if (value.isNullOrBlank()) return
            checkNewPage(12f)
            val keyWidth = paintBodyBold.measureText("$key: ")
            canvas.drawText("$key: ", ml + indent, y, paintBodyBold)
            canvas.drawText(value, ml + indent + keyWidth, y, paintBody)
            y += 12f
        }
    }

    // ─── Public entry point ─────────────────────────────────────────────────────

    fun generatePdf(
        routes: List<Route>,
        salaryState: SalaryCalculationUIState?,
        monthLabel: String,
        sections: PdfSections
    ): File {
        val document = PdfDocument()
        val pm = PageManager(document)
        pm.newPage()

        if (sections.includeRouteDetails && routes.isNotEmpty()) {
            drawRouteDetailsSection(pm, routes, monthLabel)
        }
        if (sections.includeSchedule) {
            drawScheduleSection(pm, routes, monthLabel)
        }
        if (sections.includeSalary && salaryState != null) {
            drawSalarySection(pm, salaryState)
        } else if (sections.includeSalary && salaryState == null) {
            pm.checkNewPage(60f)
            pm.drawText("Расчётный листок", paintTitle)
            pm.y += 6f
            pm.drawText(
                "Данные расчёта заработной платы недоступны. " +
                "Откройте экран «Расчёт заработной платы» и повторите формирование PDF.",
                paintBody
            )
        }

        pm.finish()

        val file = File(context.cacheDir, "mashinist_$monthLabel.pdf")
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()
        return file
    }

    // ─── Page header ─────────────────────────────────────────────────────────────

    private fun drawPageHeader(canvas: Canvas) {
        val text = "Файл сформирован в приложении «Машинист»"
        canvas.drawText(text, ml, 20f, paintHeader)
        canvas.drawLine(ml, 26f, pageWidth - mr, 26f, paintHeaderLine)
    }

    // ─── Routes detail section ───────────────────────────────────────────────────

    private fun drawRouteDetailsSection(pm: PageManager, routes: List<Route>, monthLabel: String) {
        pm.drawText("Поездки за $monthLabel", paintTitle)
        pm.y += 4f
        pm.drawLine()

        routes.forEach { route -> drawSingleRoute(pm, route) }
    }

    private fun drawSingleRoute(pm: PageManager, route: Route) {
        pm.checkNewPage(40f)

        // ── Route title
        val bd = route.basicData
        val routeNum = bd.number?.takeIf { it.isNotBlank() } ?: "б/н"
        val start = bd.timeStartWork?.let { timeManager.formatDate(it) } ?: "—"
        pm.drawText("Маршрут №$routeNum   $start", paintSection)
        pm.y += 2f

        // ── BasicData
        pm.drawKV("Явка", bd.timeStartWork?.let { timeManager.formatDateTime(it) })
        pm.drawKV("Сдача", bd.timeEndWork?.let { timeManager.formatDateTime(it) })
        val workTime = calcDiff(bd.timeStartWork, bd.timeEndWork)
        pm.drawKV("Время в работе", workTime)
        pm.drawKV("Начало перерыва", bd.timeStartBreak?.let { timeManager.formatDateTime(it) })
        pm.drawKV("Конец перерыва", bd.timeEndBreak?.let { timeManager.formatDateTime(it) })
        if (bd.restPointOfTurnover) pm.drawKV("Отдых в пункте оборота", "Да")
        if (bd.isOnePersonOperation) pm.drawKV("Работа в одно лицо", "Да")
        pm.drawKV("Примечание", bd.notes)

        // ── Locomotives
        route.locomotives.forEachIndexed { idx, loco ->
            pm.checkNewPage(30f)
            pm.y += 4f
            val locoType = if (loco.type == LocoType.ELECTRIC) "Электровоз" else "Тепловоз"
            val locoNum = loco.number?.takeIf { it.isNotBlank() } ?: "—"
            val locoSeries = loco.series?.takeIf { it.isNotBlank() } ?: ""
            pm.drawText("  Локомотив ${idx + 1}: $locoSeries $locoNum ($locoType)", paintBodyBold)
            pm.drawKV("Начало приёмки", loco.timeStartOfAcceptance?.let { timeManager.formatDateTime(it) }, 16f)
            pm.drawKV("Конец приёмки", loco.timeEndOfAcceptance?.let { timeManager.formatDateTime(it) }, 16f)
            pm.drawKV("Начало сдачи", loco.timeStartOfDelivery?.let { timeManager.formatDateTime(it) }, 16f)
            pm.drawKV("Конец сдачи", loco.timeEndOfDelivery?.let { timeManager.formatDateTime(it) }, 16f)

            if (loco.type == LocoType.ELECTRIC) {
                loco.electricSectionList.forEachIndexed { si, sec ->
                    pm.checkNewPage(18f)
                    pm.y += 2f
                    pm.drawText("    Секция ${si + 1}", paintSmall)
                    val accepted = sec.acceptedEnergy?.let { "принято: ${"%.1f".format(it)} кВт⋅ч" } ?: ""
                    val delivered = sec.deliveryEnergy?.let { "сдано: ${"%.1f".format(it)} кВт⋅ч" } ?: ""
                    if (accepted.isNotBlank() || delivered.isNotBlank()) {
                        pm.drawKV("  Тяга", listOf(accepted, delivered).filter { it.isNotBlank() }.joinToString(" / "), 20f)
                    }
                    val accRec = sec.acceptedRecovery?.let { "принято: ${"%.1f".format(it)} кВт⋅ч" } ?: ""
                    val delRec = sec.deliveryRecovery?.let { "сдано: ${"%.1f".format(it)} кВт⋅ч" } ?: ""
                    if (accRec.isNotBlank() || delRec.isNotBlank()) {
                        pm.drawKV("  Рекуперация", listOf(accRec, delRec).filter { it.isNotBlank() }.joinToString(" / "), 20f)
                    }
                }
            } else {
                loco.dieselSectionList.forEachIndexed { si, sec ->
                    pm.checkNewPage(14f)
                    pm.y += 2f
                    pm.drawText("    Секция ${si + 1}", paintSmall)
                    val accF = sec.acceptedFuel?.let { "принято: ${"%.2f".format(it)} л" } ?: ""
                    val delF = sec.deliveryFuel?.let { "сдано: ${"%.2f".format(it)} л" } ?: ""
                    if (accF.isNotBlank() || delF.isNotBlank()) {
                        pm.drawKV("  Топливо", listOf(accF, delF).filter { it.isNotBlank() }.joinToString(" / "), 20f)
                    }
                }
            }
        }

        // ── Trains
        route.trains.forEachIndexed { idx, train ->
            pm.checkNewPage(30f)
            pm.y += 4f
            val trainNum = train.number?.takeIf { it.isNotBlank() } ?: "—"
            pm.drawText("  Поезд ${idx + 1}: №$trainNum", paintBodyBold)
            pm.drawKV("Масса", train.weight?.takeIf { it.isNotBlank() }, 16f)
            pm.drawKV("Осей", train.axle?.takeIf { it.isNotBlank() }, 16f)
            pm.drawKV("Расстояние", train.distance?.takeIf { it.isNotBlank() }?.let { "$it км" }, 16f)

            train.stations.forEachIndexed { si, station ->
                pm.checkNewPage(14f)
                pm.y += 2f
                val stName = station.stationName ?: "Станция ${si + 1}"
                val arr = station.timeArrival?.let { timeManager.formatTime(it) } ?: "—"
                val dep = station.timeDeparture?.let { timeManager.formatTime(it) } ?: "—"
                pm.canvas.drawText("    $stName:  приб. $arr  отпр. $dep", ml + 20f, pm.y, paintSmall)
                pm.y += 10f
            }
        }

        // ── Passengers
        route.passengers.forEachIndexed { idx, passenger ->
            pm.checkNewPage(24f)
            pm.y += 4f
            pm.drawText("  Пассажир ${idx + 1}", paintBodyBold)
            pm.drawKV("Поезд", passenger.trainNumber, 16f)
            pm.drawKV("Откуда", passenger.stationDeparture, 16f)
            pm.drawKV("Куда", passenger.stationArrival, 16f)
            pm.drawKV("Отправление", passenger.timeDeparture?.let { timeManager.formatDateTime(it) }, 16f)
            pm.drawKV("Прибытие", passenger.timeArrival?.let { timeManager.formatDateTime(it) }, 16f)
            pm.drawKV("Примечание", passenger.notes, 16f)
        }

        pm.y += 6f
        pm.drawLine()
    }

    // ─── Schedule / Calendar section ─────────────────────────────────────────────

    private fun drawScheduleSection(pm: PageManager, routes: List<Route>, monthLabel: String) {
        // Always start schedule on new content block with page check
        pm.checkNewPage(200f)
        pm.drawText("График за $monthLabel", paintTitle)
        pm.y += 4f
        pm.drawLine()

        // Determine month/year from monthLabel or from routes
        val cal = parseMonthYear(monthLabel, routes)
        if (cal == null) {
            pm.drawText("Нет данных для формирования графика.", paintBody)
            return
        }

        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH)

        // Build day map: dayOfMonth -> (hasRoute, hasPassenger)
        val dayRoutes = mutableMapOf<Int, Boolean>()
        val dayPassengers = mutableMapOf<Int, Boolean>()
        val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("GMT+3")
        }
        routes.forEach { route ->
            route.basicData.timeStartWork?.let { ts ->
                val routeCal = Calendar.getInstance(TimeZone.getTimeZone("GMT+3")).apply { timeInMillis = ts }
                if (routeCal.get(Calendar.YEAR) == year && routeCal.get(Calendar.MONTH) == month) {
                    val day = routeCal.get(Calendar.DAY_OF_MONTH)
                    dayRoutes[day] = true
                }
            }
            route.passengers.forEach { passenger ->
                passenger.timeDeparture?.let { ts ->
                    val pCal = Calendar.getInstance(TimeZone.getTimeZone("GMT+3")).apply { timeInMillis = ts }
                    if (pCal.get(Calendar.YEAR) == year && pCal.get(Calendar.MONTH) == month) {
                        val day = pCal.get(Calendar.DAY_OF_MONTH)
                        dayPassengers[day] = true
                    }
                }
            }
        }

        // Calendar grid
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        // firstDow: 1=Sun,2=Mon...7=Sat; convert to 0=Mon…6=Sun
        val firstDow = ((cal.get(Calendar.DAY_OF_WEEK) - 2 + 7) % 7)

        val cellW = contentWidth / 7f
        val cellH = 24f
        val gridX = ml
        val gridY = pm.y

        // Need enough space: header row + up to 6 rows
        val gridHeight = cellH + 6 * cellH + 8f
        pm.checkNewPage(gridHeight + 60f)
        val startY = pm.y

        // Day-of-week headers
        val dowLabels = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
        dowLabels.forEachIndexed { col, label ->
            val x = gridX + col * cellW
            pm.canvas.drawRect(x, startY, x + cellW, startY + cellH, paintTableBorder)
            pm.canvas.drawText(label, x + cellW / 2f - paintSmall.measureText(label) / 2f, startY + cellH - 7f, paintSmall)
        }

        var currentY = startY + cellH
        var col = firstDow
        var row = 0

        for (day in 1..daysInMonth) {
            val x = gridX + col * cellW
            val y = currentY + row * cellH

            // Cell border
            pm.canvas.drawRect(x, y, x + cellW, y + cellH, paintTableBorder)

            // Background fill for route/passenger
            val hasRoute = dayRoutes[day] == true
            val hasPassenger = dayPassengers[day] == true
            if (hasRoute) {
                paintFill.color = Color.argb(60, 33, 150, 243)
                pm.canvas.drawRect(x + 1, y + 1, x + cellW - 1, y + cellH - 1, paintFill)
            } else if (hasPassenger) {
                paintFill.color = Color.argb(60, 255, 152, 0)
                pm.canvas.drawRect(x + 1, y + 1, x + cellW - 1, y + cellH - 1, paintFill)
            }

            // Day number
            val dayStr = day.toString()
            val dayPaint = if (hasRoute || hasPassenger) paintBodyBold else paintBody
            pm.canvas.drawText(dayStr, x + cellW / 2f - dayPaint.measureText(dayStr) / 2f, y + cellH - 7f, dayPaint)

            col++
            if (col == 7) {
                col = 0
                row++
            }
        }

        pm.y = currentY + (row + (if (col > 0) 1 else 0)) * cellH + 8f

        // Legend
        pm.y += 4f
        paintFill.color = Color.argb(80, 33, 150, 243)
        pm.canvas.drawRect(ml, pm.y - 8f, ml + 12f, pm.y + 2f, paintFill)
        pm.canvas.drawText("— Явка (маршрут)", ml + 16f, pm.y, paintSmall)
        pm.y += 12f
        paintFill.color = Color.argb(80, 255, 152, 0)
        pm.canvas.drawRect(ml, pm.y - 8f, ml + 12f, pm.y + 2f, paintFill)
        pm.canvas.drawText("— Отвлечение (пассажиром)", ml + 16f, pm.y, paintSmall)
        pm.y += 16f

        // Summary
        val totalWorkMs = routes.sumOf { r ->
            val s = r.basicData.timeStartWork ?: return@sumOf 0L
            val e = r.basicData.timeEndWork ?: return@sumOf 0L
            val breakMs = if (r.basicData.timeStartBreak != null && r.basicData.timeEndBreak != null)
                (r.basicData.timeEndBreak!! - r.basicData.timeStartBreak!!) else 0L
            maxOf(0L, e - s - breakMs)
        }
        val totalPassengerMs = routes.sumOf { r ->
            r.passengers.sumOf { p ->
                val s = p.timeDeparture ?: return@sumOf 0L
                val e = p.timeArrival ?: return@sumOf 0L
                maxOf(0L, e - s)
            }
        }
        pm.drawKV("Всего маршрутов", "${routes.size}")
        pm.drawKV("Отработано времени", ConverterLongToTime.getTimeInStringFormat(totalWorkMs))
        if (totalPassengerMs > 0) {
            pm.drawKV("Время в отвлечениях", ConverterLongToTime.getTimeInStringFormat(totalPassengerMs))
        }
        pm.y += 6f
        pm.drawLine()
    }

    // ─── Salary section ───────────────────────────────────────────────────────────

    private fun drawSalarySection(pm: PageManager, state: SalaryCalculationUIState) {
        pm.checkNewPage(60f)
        pm.drawText("Расчётный листок${if (state.month.isNotBlank()) " — ${state.month}" else ""}", paintTitle)
        pm.y += 4f
        pm.drawLine()

        // Table columns: description | hours | % | amount
        val colW = floatArrayOf(contentWidth * 0.50f, contentWidth * 0.16f, contentWidth * 0.14f, contentWidth * 0.20f)

        fun tableHeader() {
            pm.checkNewPage(16f)
            drawTableRow(pm, listOf("Вид выплаты", "Часы", "%", "Сумма"), colW, paintBodyBold, isHeader = true)
        }

        tableHeader()

        val fmt = { v: Double? -> v?.let { "${"%.2f".format(it)} ₽" } ?: "" }
        val fmtH = { v: Long? -> ConverterLongToTime.getTimeInStringFormat(v).takeIf { v != null } ?: "" }
        val fmtP = { v: Double? -> v?.let { "${it}%" } ?: "" }

        // Начисления
        salaryRow(pm, colW, "Оплата по тарифу", fmtH(state.paymentAtTariffHours), "", fmt(state.paymentAtTariffMoney))
        salaryRow(pm, colW, "Ночные часы", fmtH(state.paymentNightTimeHours), fmtP(state.paymentNightTimePercent), fmt(state.paymentNightTimeMoney))
        salaryRow(pm, colW, "Пассажиром", fmtH(state.paymentAtPassengerHours), "", fmt(state.paymentAtPassengerMoney))
        salaryRow(pm, colW, "Резервом", fmtH(state.paymentAtSingleLocomotiveHours), "", fmt(state.paymentAtSingleLocomotiveMoney))
        salaryRow(pm, colW, "Праздничные", fmtH(state.paymentHolidayHours), "", fmt(state.paymentHolidayMoney))
        salaryRow(pm, colW, "Оплата по среднему", fmtH(state.averagePaymentHours), "", fmt(state.averagePaymentMoney))
        salaryRow(pm, colW, "По уходу за ребёнком-инвалидом", fmtH(state.caringForDisableChildrenHours), "", fmt(state.caringForDisableChildrenMoney))
        salaryRow(pm, colW, "Зональная надбавка", "", fmtP(state.zonalSurchargePercent), fmt(state.zonalSurchargeMoney))
        salaryRow(pm, colW, "Надбавка за класс квалификации", "", fmtP(state.surchargeQualificationClassPercent), fmt(state.surchargeQualificationClassMoney))
        salaryRow(pm, colW, "В одно лицо (груз.)", "", fmtP(state.onePersonOperationPercent), fmt(state.onePersonOperationMoney))
        salaryRow(pm, colW, "В одно лицо (пас.)", "", fmtP(state.onePersonOperationPassengerTrainPercent), fmt(state.onePersonOperationPassengerTrainMoney))
        salaryRow(pm, colW, "Вредность", "", fmtP(state.harmfulnessSurchargePercent), fmt(state.harmfulnessSurchargeMoney))
        salaryRow(pm, colW, "Районный коэффициент", "", fmtP(state.districtSurchargeCoefficient), fmt(state.districtSurchargeMoney))
        salaryRow(pm, colW, "Северная надбавка", "", fmtP(state.nordicSurchargePercent), fmt(state.nordicSurchargeMoney))

        state.surchargeExtendedServicePhaseHour.forEachIndexed { i, h ->
            val pct = state.surchargeExtendedServicePhasePercent.getOrNull(i)
            val money = state.surchargeExtendedServicePhaseMoney.getOrNull(i)
            salaryRow(pm, colW, "Удлинённое плечо ${i + 1}", fmtH(h), pct ?: "", fmt(money))
        }
        state.surchargeHeavyTransHour.forEachIndexed { i, h ->
            val pct = state.surchargeHeavyTransPercent.getOrNull(i)
            val money = state.surchargeHeavyTransMoney.getOrNull(i)
            salaryRow(pm, colW, "Тяжёлые поезда ${i + 1}", fmtH(h), pct ?: "", fmt(money))
        }
        state.surchargeLongTrainHour.forEachIndexed { i, h ->
            val pct = state.surchargeLongTrainPercent.getOrNull(i)
            val money = state.surchargeLongTrainMoney.getOrNull(i)
            salaryRow(pm, colW, "Длинносост. поезда ${i + 1}", fmtH(h), pct ?: "", fmt(money))
        }
        salaryRow(pm, colW, "Сдвоенные (30%)", fmtH(state.surchargeDoubledTrainFirstHours), "30%", fmt(state.surchargeDoubledTrainFirstMoney))
        salaryRow(pm, colW, "Сдвоенные (15%)", fmtH(state.surchargeDoubledTrainSecondHours), "15%", fmt(state.surchargeDoubledTrainSecondMoney))
        salaryRow(pm, colW, "Сверхурочные", fmtH(state.paymentAtOvertimeHours), "", fmt(state.paymentAtOvertimeMoney))
        salaryRow(pm, colW, "Доплата сверхурочные (50%)", fmtH(state.surchargeAtOvertime05Hours), "50%", fmt(state.surchargeAtOvertime05Money))
        salaryRow(pm, colW, "Доплата сверхурочные (100%)", fmtH(state.surchargeAtOvertimeHours), "100%", fmt(state.surchargeAtOvertimeMoney))
        salaryRow(pm, colW, "Переотдых", fmtH(state.restInExcessOfTheNormTime), "", fmt(state.restInExcessOfTheNormMoney))

        // Total charged
        pm.checkNewPage(14f)
        drawTableRow(pm, listOf("Итого начислено", "", "", fmt(state.totalChargedMoney)), colW, paintBodyBold, isFooter = true)
        pm.y += 6f

        // Uderzhaniya
        pm.checkNewPage(20f)
        pm.drawText("Удержания", paintSection)
        pm.y += 2f
        tableHeader()
        salaryRow(pm, colW, "НДФЛ (13%)", "", "13%", fmt(state.retentionNdfl))
        salaryRow(pm, colW, "Профсоюз", "", "", fmt(state.unionistsRetention))
        salaryRow(pm, colW, "Прочие удержания", "", "", fmt(state.otherRetention))
        drawTableRow(pm, listOf("Всего удержано", "", "", fmt(state.totalRetention)), colW, paintBodyBold, isFooter = true)

        pm.y += 8f
        pm.checkNewPage(20f)
        pm.drawText("К выдаче: ${fmt(state.toBeCredited)}", paintTitle)
        pm.y += 8f
        pm.drawLine()
    }

    private fun salaryRow(pm: PageManager, colW: FloatArray, desc: String, hours: String, pct: String, amount: String) {
        if (hours.isBlank() && pct.isBlank() && amount.isBlank()) return
        drawTableRow(pm, listOf(desc, hours, pct, amount), colW, paintBody)
    }

    private fun drawTableRow(
        pm: PageManager,
        cells: List<String>,
        colW: FloatArray,
        paint: Paint,
        isHeader: Boolean = false,
        isFooter: Boolean = false
    ) {
        val rowH = 14f
        pm.checkNewPage(rowH + 2f)
        var x = ml
        if (isHeader || isFooter) {
            paintFill.color = if (isHeader) Color.argb(30, 0, 0, 0) else Color.argb(20, 0, 120, 200)
            pm.canvas.drawRect(ml, pm.y - 10f, ml + contentWidth, pm.y + 4f, paintFill)
        }
        cells.forEachIndexed { i, text ->
            pm.canvas.drawRect(x, pm.y - 10f, x + colW[i], pm.y + 4f, paintTableBorder)
            val textX = if (i == 0) x + 3f else x + colW[i] - paint.measureText(text) - 3f
            pm.canvas.drawText(text, textX, pm.y, paint)
            x += colW[i]
        }
        pm.y += rowH
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────────

    private fun calcDiff(start: Long?, end: Long?): String? {
        if (start == null || end == null) return null
        val ms = end - start
        if (ms <= 0) return null
        return ConverterLongToTime.getTimeInStringFormat(ms)
    }

    private fun parseMonthYear(monthLabel: String, routes: List<Route>): Calendar? {
        // Try to derive from routes
        val ts = routes.mapNotNull { it.basicData.timeStartWork }.minOrNull()
        if (ts != null) {
            return Calendar.getInstance(TimeZone.getTimeZone("GMT+3")).apply { timeInMillis = ts }
        }
        // Fallback: current month
        return Calendar.getInstance(TimeZone.getTimeZone("GMT+3"))
    }
}

/** Extension to format date+time via TimeManager */
private fun TimeManager.formatDateTime(millis: Long): String =
    "${formatDate(millis)} ${formatTime(millis)}"
