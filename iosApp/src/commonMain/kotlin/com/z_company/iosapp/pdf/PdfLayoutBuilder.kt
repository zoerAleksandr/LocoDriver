package com.z_company.iosapp.pdf

import com.z_company.domain.entities.Day
import com.z_company.domain.entities.ReleaseType
import com.z_company.domain.entities.WorkScheduleProfile
import com.z_company.domain.entities.route.LocoType
import com.z_company.domain.entities.route.Route
import com.z_company.domain.util.validFor
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.Instant
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

/**
 * Сборка модели вёрстки PDF (SCREEN_SPECS §21) из доменных данных.
 *
 * Порт `features/route/.../util/PdfGenerator.kt` (содержание и правила §21):
 * все тексты, числа, порядок блоков и оценки высот — здесь; пиксельный рендер —
 * Swift (`PdfRenderer.swift`, UIGraphicsPDFRenderer).
 *
 * Времена печатаются в фиксированном GMT+3 — как Android (⚠️ BUG-59, паритет).
 * «Расход» отопления/СН = принял − сдал — как Android (⚠️ BUG-60, паритет).
 */
@OptIn(kotlin.time.ExperimentalTime::class)
object PdfLayoutBuilder {

    /** Имена месяцев в именительном падеже (как в шапке Главного, §21). */
    val MONTH_NAMES = listOf(
        "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
        "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь",
    )

    /** «Апрель 2026» → файл «Машинист_Апрель_2026.pdf» (§21 «Файл»). */
    fun fileNameFor(monthLabel: String): String {
        val parts = monthLabel.trim().split(" ")
        val monthPart = parts.getOrNull(0)?.replaceFirstChar { it.uppercase() } ?: "Месяц"
        val yearPart = parts.getOrNull(1) ?: "Год"
        return "Машинист_${monthPart}_${yearPart}.pdf"
    }

    fun monthLabelFor(monthIndex: Int, year: Int): String =
        "${MONTH_NAMES.getOrElse(monthIndex) { "?" }} $year"

    /**
     * Полный документ: Поездки → График → Расчётный листок, каждый раздел
     * с новой страницы (§21 «Страница»).
     */
    internal fun build(
        sections: PdfSectionsModel,
        routes: List<Route>,
        monthLabel: String,
        calendarDays: List<Day>,
        workScheduleProfile: WorkScheduleProfile,
        salary: PdfSalaryStatementData?,
    ): PdfLayoutDocument {
        val metrics = PdfMetrics()
        val blocks = mutableListOf<PdfBlock>()
        if (sections.includeRouteDetails) {
            blocks += PdfNewPageBlock(drawAppHeader = true)
            blocks += buildRouteDetails(routes, monthLabel, metrics)
        }
        if (sections.includeSchedule) {
            blocks += PdfNewPageBlock(drawAppHeader = true)
            blocks += buildSchedule(routes, monthLabel, calendarDays, workScheduleProfile, metrics)
        }
        if (sections.includeSalary) {
            blocks += PdfNewPageBlock(drawAppHeader = false)
            if (salary != null) {
                blocks += salary.toStatementBlock()
            } else {
                blocks += PdfTextBlock("Расчётный листок", PdfTextStyle.TITLE)
                blocks += PdfTextBlock(
                    "Данные расчёта недоступны. Откройте «Расчёт заработной платы» и повторите формирование.",
                    PdfTextStyle.BODY,
                )
            }
        }
        return PdfLayoutDocument(
            metrics = metrics,
            blocks = blocks,
            fileName = fileNameFor(monthLabel),
        )
    }

    // ─── Поездки детально (§21 «Раздел «Поездки детально»») ─────────────────

    private fun buildRouteDetails(
        routes: List<Route>,
        monthLabel: String,
        m: PdfMetrics,
    ): List<PdfBlock> {
        val out = mutableListOf<PdfBlock>()
        out += PdfTextBlock("Поездки за $monthLabel", PdfTextStyle.TITLE, gapBefore = 2.0)
        out += PdfSeparatorBlock()
        if (routes.isEmpty()) {
            out += PdfTextBlock("Нет маршрутов за период.", PdfTextStyle.BODY)
            return out
        }
        // По возрастанию явки; маршрут без явки — в конце.
        val sorted = routes.sortedWith(compareBy(nullsLast()) { it.basicData.timeStartWork })
        for (route in sorted) {
            val children = buildRoute(route, m)
            val estimate = minOf(estimateRouteHeight(route), (m.pageHeight - m.marginTop - m.marginBottom) * 0.85)
            out += PdfBlockGroup(estimatedHeight = estimate, children = children)
        }
        return out
    }

    /** Оценка высоты маршрута — порт `estimateRouteHeight` (§21: маршрут не разрывать, если помещается). */
    internal fun estimateRouteHeight(route: Route): Double {
        var h = 10.0 + 16.0
        h += 14.0 * 2
        val bd = route.basicData
        if ((bd.timeStartBreak ?: 0L) > 0L || (bd.timeEndBreak ?: 0L) > 0L) h += 14.0 * 2
        if (bd.restPointOfTurnover || bd.isOnePersonOperation || !bd.notes.isNullOrBlank()) h += 15.0
        route.locomotives.forEach { loco ->
            h += 4.0 + 14.0
            h += 14.0 * 2
            if (loco.heatingCounterAccepted != null || loco.heatingCounterDelivery != null) h += 28.0
            if (loco.auxiliaryCounterAccepted != null || loco.auxiliaryCounterDelivery != null) h += 28.0
            val secCount = if (loco.type == LocoType.ELECTRIC) loco.electricSectionList.size else loco.dieselSectionList.size
            h += secCount * 56.0
            h += 42.0
        }
        route.trains.forEach { train ->
            h += 4.0 + 14.0
            h += (train.stations.size + 1) * 14.0
            h += train.stations.count {
                !it.segmentTrackNumber.isNullOrBlank() || !it.segmentNotes.isNullOrBlank()
            } * 14.0
        }
        route.passengers.forEach { _ -> h += 4.0 + 14.0 * 3 }
        h += 8.0
        return h
    }

    private fun buildRoute(route: Route, m: PdfMetrics): List<PdfBlock> {
        val out = mutableListOf<PdfBlock>()
        val bd = route.basicData
        val num = bd.number?.takeIf { it.isNotBlank() } ?: "б/н"
        val dateStr = fmtDate(bd.timeStartWork) ?: "—"
        out += PdfRowBlock(
            cells = listOf(PdfCell("Маршрут №$num   $dateStr", PdfTextStyle.BODY_BOLD)),
            weights = listOf(1.0),
            fillRow = true,
            rowHeight = 16.0,
            gapBefore = 10.0,
        )

        val w3 = listOf(1.0 / 3, 1.0 / 3, 1.0 / 3)
        val breakMs = if (bd.timeStartBreak != null && bd.timeEndBreak != null &&
            bd.timeEndBreak!! > bd.timeStartBreak!!
        ) (bd.timeEndBreak!! - bd.timeStartBreak!!) else 0L
        val workDur = if (bd.timeStartWork != null && bd.timeEndWork != null)
            fmtDur(maxOf(0L, bd.timeEndWork!! - bd.timeStartWork!! - breakMs)) else "—"

        out += smallHeaderRow(listOf("Явка", "Сдача", "Рабочее время"), w3)
        out += bodyRow(listOf(fmtDT(bd.timeStartWork) ?: "—", fmtDT(bd.timeEndWork) ?: "—", workDur ?: "—"), w3)

        if ((bd.timeStartBreak ?: 0L) > 0L || (bd.timeEndBreak ?: 0L) > 0L) {
            val w2 = listOf(0.5, 0.5)
            out += smallHeaderRow(listOf("Начало перерыва", "Окончание перерыва"), w2)
            out += bodyRow(listOf(fmtDT(bd.timeStartBreak) ?: "—", fmtDT(bd.timeEndBreak) ?: "—"), w2)
        }

        if (bd.restPointOfTurnover) {
            out += PdfTextBlock("• Отдых в пункте оборота", PdfTextStyle.SMALL, gapBefore = 2.0)
        }
        if (bd.isOnePersonOperation) out += PdfTextBlock("• Работа в одно лицо", PdfTextStyle.SMALL)
        if (!bd.notes.isNullOrBlank()) out += PdfTextBlock("Примечание: ${bd.notes}", PdfTextStyle.SMALL)

        route.locomotives.forEachIndexed { idx, loco ->
            out += PdfRowBlock(
                cells = listOf(
                    PdfCell("Локомотив ${idx + 1} (${if (loco.type == LocoType.ELECTRIC) "Электровоз" else "Тепловоз"})", PdfTextStyle.BODY_BOLD),
                    PdfCell(loco.series?.takeIf { it.isNotBlank() } ?: "—", PdfTextStyle.BODY_BOLD),
                    PdfCell(loco.number?.takeIf { it.isNotBlank() } ?: "—", PdfTextStyle.BODY_BOLD),
                ),
                weights = listOf(0.35, 0.40, 0.25),
                fillRow = true,
                gapBefore = 4.0,
            )
            val w4 = listOf(0.25, 0.25, 0.25, 0.25)
            out += smallHeaderRow(listOf("Нач. приёмки", "Оконч. приёмки", "Нач. сдачи", "Оконч. сдачи"), w4)
            out += bodyRow(
                listOf(
                    fmtDT(loco.timeStartOfAcceptance) ?: "—",
                    fmtDT(loco.timeEndOfAcceptance) ?: "—",
                    fmtDT(loco.timeStartOfDelivery) ?: "—",
                    fmtDT(loco.timeEndOfDelivery) ?: "—",
                ), w4,
            )

            // BUG-60 (паритет с Android): «Расход» = принял − сдал.
            val hcA = loco.heatingCounterAccepted
            val hcD = loco.heatingCounterDelivery
            if (hcA != null || hcD != null) {
                out += smallHeaderRow(listOf("Отопление (счётчик)", "Принял", "Сдал", "Расход"), w4)
                val hcRes = if (hcA != null && hcD != null) fmt1(hcA - hcD) else "—"
                out += bodyRow(listOf("", hcA?.let { fmt1(it) } ?: "—", hcD?.let { fmt1(it) } ?: "—", hcRes), w4)
            }
            val acA = loco.auxiliaryCounterAccepted
            val acD = loco.auxiliaryCounterDelivery
            if (acA != null || acD != null) {
                out += smallHeaderRow(listOf("Собственные нужды", "Принял", "Сдал", "Расход"), w4)
                val acRes = if (acA != null && acD != null) fmt1(acA - acD) else "—"
                out += bodyRow(listOf("", acA?.let { fmt1(it) } ?: "—", acD?.let { fmt1(it) } ?: "—", acRes), w4)
            }

            out += buildLocoSections(loco, w4, m)
        }

        route.trains.forEachIndexed { idx, train ->
            out += PdfRowBlock(
                cells = listOf(
                    PdfCell("Поезд ${idx + 1}", PdfTextStyle.BODY_BOLD),
                    PdfCell(train.number?.let { "№$it" } ?: "б/н", PdfTextStyle.BODY_BOLD),
                    PdfCell(train.weight?.let { "Вес $it т" } ?: "", PdfTextStyle.BODY_BOLD),
                    PdfCell(train.axle?.let { "Оси $it" } ?: "", PdfTextStyle.BODY_BOLD),
                    PdfCell(
                        train.conditionalLength?.let { raw ->
                            val fmt = raw.toDoubleOrNull()
                                ?.let { d -> if (d == kotlin.math.floor(d)) d.toLong().toString() else raw }
                                ?: raw
                            "у.д. $fmt"
                        } ?: "",
                        PdfTextStyle.BODY_BOLD,
                    ),
                ),
                weights = listOf(0.22, 0.20, 0.20, 0.18, 0.20),
                fillRow = true,
                gapBefore = 4.0,
            )
            val wSt = listOf(0.36, 0.32, 0.32)
            if (train.stations.isNotEmpty()) {
                out += smallHeaderRow(listOf("Станция", "Прибытие", "Отправление"), wSt)
                train.stations.forEach { station ->
                    val nameCell = buildString {
                        append(station.stationName ?: "—")
                        station.trackNumber?.takeIf { it.isNotBlank() }?.let { append(" ($it п.)") }
                        if (station.isPassingStation) append(" · проходная")
                    }
                    out += bodyRow(
                        listOf(
                            nameCell,
                            fmtDT(station.timeArrival) ?: "—",
                            if (station.isPassingStation) "—" else fmtDT(station.timeDeparture) ?: "—",
                        ), wSt,
                    )
                    val segmentCell = buildString {
                        station.segmentTrackNumber?.takeIf { it.isNotBlank() }?.let { append("путь $it") }
                        station.segmentNotes?.takeIf { it.isNotBlank() }?.let {
                            if (isNotEmpty()) append(" · ")
                            append(it)
                        }
                    }
                    if (segmentCell.isNotEmpty()) {
                        out += bodyRow(listOf("    перегон · $segmentCell", "", ""), wSt)
                    }
                }
            }
        }

        route.passengers.forEachIndexed { idx, passenger ->
            val w2 = listOf(0.5, 0.5)
            out += PdfRowBlock(
                cells = listOf(
                    PdfCell("Пассажиром ${idx + 1}", PdfTextStyle.BODY_BOLD),
                    PdfCell(passenger.trainNumber?.let { "Поезд №$it" } ?: "", PdfTextStyle.BODY_BOLD),
                ),
                weights = w2,
                fillRow = true,
                gapBefore = 4.0,
            )
            val w3p = listOf(1.0 / 3, 1.0 / 3, 1.0 / 3)
            out += smallHeaderRow(listOf("Отправление", "Прибытие", "В пути"), w3p)
            val passTime = if (passenger.timeDeparture != null && passenger.timeArrival != null)
                fmtDur(maxOf(0L, passenger.timeArrival!! - passenger.timeDeparture!!)) else "—"
            out += bodyRow(
                listOf(
                    fmtDT(passenger.timeDeparture) ?: "—",
                    fmtDT(passenger.timeArrival) ?: "—",
                    passTime ?: "—",
                ), w3p,
            )
            val stFrom = passenger.stationDeparture?.takeIf { it.isNotBlank() }
            val stTo = passenger.stationArrival?.takeIf { it.isNotBlank() }
            if (stFrom != null || stTo != null) {
                out += smallHeaderRow(listOf("Откуда", "Куда", ""), w3p)
                out += bodyRow(listOf(stFrom ?: "—", stTo ?: "—", ""), w3p)
            }
            if (!passenger.notes.isNullOrBlank()) {
                out += PdfTextBlock("Примечание: ${passenger.notes}", PdfTextStyle.SMALL)
            }
        }

        out += PdfTextBlock("", PdfTextStyle.BODY, gapBefore = 8.0)
        return out
    }

    private fun buildLocoSections(
        loco: com.z_company.domain.entities.route.Locomotive,
        w4: List<Double>,
        m: PdfMetrics,
    ): List<PdfBlock> {
        val out = mutableListOf<PdfBlock>()
        if (loco.type == LocoType.ELECTRIC) {
            val hasNorma1 = loco.normaElectricCurrent1 != null
            val hasNorma2 = loco.normaElectricCurrent2 != null
            val hasAnyNorma = hasNorma1 || hasNorma2
            val wSec: List<Double> = if (hasAnyNorma) listOf(0.28, 0.13, 0.13, 0.13, 0.16, 0.17) else w4

            val sectionsWithElecData = loco.electricSectionList.filter { sec ->
                listOf(sec.acceptedEnergy, sec.deliveryEnergy, sec.acceptedRecovery, sec.deliveryRecovery)
                    .any { (it ?: 0.0) != 0.0 }
            }
            val singleElecSection = sectionsWithElecData.size == 1

            loco.electricSectionList.forEachIndexed { si, sec ->
                val hasData = listOf(sec.acceptedEnergy, sec.deliveryEnergy, sec.acceptedRecovery, sec.deliveryRecovery)
                    .any { (it ?: 0.0) != 0.0 }
                if (!hasData) return@forEachIndexed
                val secHeader = if (hasAnyNorma)
                    listOf("Секция ${si + 1}", "Принял", "Сдал", "Итого", "Норма", "Результат")
                else
                    listOf("Секция ${si + 1}", "Принял", "Сдал", "Итого")
                out += smallHeaderRow(secHeader, wSec, gapBefore = 2.0)
                if ((sec.acceptedEnergy ?: 0.0) != 0.0 || (sec.deliveryEnergy ?: 0.0) != 0.0) {
                    val accept = sec.acceptedEnergy?.let { fmt1(it) } ?: "—"
                    val deliv = sec.deliveryEnergy?.let { fmt1(it) } ?: "—"
                    val total = sec.deliveryEnergy?.let { d -> sec.acceptedEnergy?.let { a -> fmt1(d - a) } } ?: "—"
                    val cells = mutableListOf("Расход тяги", accept, deliv, total)
                    if (hasAnyNorma && singleElecSection) {
                        cells.add(loco.normaElectricCurrent1?.let { fmt1(it) } ?: "")
                        cells.add(
                            loco.normaElectricCurrent1?.let { n ->
                                fmt1((sec.deliveryEnergy ?: 0.0) - (sec.acceptedEnergy ?: 0.0) - n)
                            } ?: "",
                        )
                    } else if (hasAnyNorma) {
                        cells.add(""); cells.add("")
                    }
                    out += bodyRow(cells, wSec)
                }
                if ((sec.acceptedRecovery ?: 0.0) != 0.0 || (sec.deliveryRecovery ?: 0.0) != 0.0) {
                    val accept = sec.acceptedRecovery?.let { fmt1(it) } ?: "—"
                    val deliv = sec.deliveryRecovery?.let { fmt1(it) } ?: "—"
                    val total = sec.deliveryRecovery?.let { d -> sec.acceptedRecovery?.let { a -> fmt1(d - a) } } ?: "—"
                    val cells = mutableListOf("Рекуперация", accept, deliv, total)
                    if (hasAnyNorma && singleElecSection) {
                        cells.add(loco.normaElectricCurrent2?.let { fmt1(it) } ?: "")
                        cells.add(
                            loco.normaElectricCurrent2?.let { n ->
                                fmt1((sec.deliveryRecovery ?: 0.0) - (sec.acceptedRecovery ?: 0.0) - n)
                            } ?: "",
                        )
                    } else if (hasAnyNorma) {
                        cells.add(""); cells.add("")
                    }
                    out += bodyRow(cells, wSec)
                }
            }
            val showElecTotals = sectionsWithElecData.size > 1 || (sectionsWithElecData.size == 1 && !hasAnyNorma)
            if (showElecTotals) {
                val totalExpend = loco.electricSectionList.sumOf { sec ->
                    (sec.deliveryEnergy ?: 0.0) - (sec.acceptedEnergy ?: 0.0)
                }
                val totalRecov = loco.electricSectionList.sumOf { sec ->
                    (sec.deliveryRecovery ?: 0.0) - (sec.acceptedRecovery ?: 0.0)
                }
                if (totalExpend != 0.0 || totalRecov != 0.0) {
                    val itHeader = if (hasAnyNorma && sectionsWithElecData.size > 1)
                        listOf("Итого", "", "", "", "Норма", "Результат")
                    else
                        listOf("Итого", "", "", "")
                    out += smallHeaderRow(itHeader, wSec, gapBefore = 2.0)
                    if (totalExpend != 0.0) {
                        val cells = mutableListOf("Расход тяги", "", "", fmt1(totalExpend))
                        if (hasAnyNorma && sectionsWithElecData.size > 1) {
                            cells.add(loco.normaElectricCurrent1?.let { fmt1(it) } ?: "")
                            cells.add(loco.normaElectricCurrent1?.let { n -> fmt1(totalExpend - n) } ?: "")
                        }
                        out += bodyRow(cells, wSec)
                    }
                    if (totalRecov != 0.0) {
                        val cells = mutableListOf("Рекуперация", "", "", fmt1(totalRecov))
                        if (hasAnyNorma && sectionsWithElecData.size > 1) {
                            cells.add(loco.normaElectricCurrent2?.let { fmt1(it) } ?: "")
                            cells.add(loco.normaElectricCurrent2?.let { n -> fmt1(totalRecov - n) } ?: "")
                        }
                        out += bodyRow(cells, wSec)
                    }
                }
            }
        } else {
            val normaDieselVal = loco.normaDiesel?.trim()?.toDoubleOrNull()
            val hasNormaDiesel = normaDieselVal != null
            val wSec: List<Double> = if (hasNormaDiesel) listOf(0.28, 0.13, 0.13, 0.13, 0.16, 0.17) else w4

            val dieselSectionsWithData = loco.dieselSectionList.filter {
                (it.acceptedFuel ?: 0.0) != 0.0 || (it.deliveryFuel ?: 0.0) != 0.0
            }
            val singleDieselSection = dieselSectionsWithData.size == 1

            loco.dieselSectionList.forEachIndexed { si, sec ->
                val hasData = (sec.acceptedFuel ?: 0.0) != 0.0 || (sec.deliveryFuel ?: 0.0) != 0.0
                if (!hasData) return@forEachIndexed
                val secHeader = if (hasNormaDiesel)
                    listOf("Секция ${si + 1}", "Принял", "Сдал", "Итого", "Норма", "Результат")
                else
                    listOf("Секция ${si + 1}", "Принял", "Сдал", "Итого")
                out += smallHeaderRow(secHeader, wSec, gapBefore = 2.0)
                val accept = sec.acceptedFuel?.let { "${fmt2(it)} л" } ?: "—"
                val deliv = sec.deliveryFuel?.let { "${fmt2(it)} л" } ?: "—"
                val expendLitres = sec.deliveryFuel?.let { d -> sec.acceptedFuel?.let { a -> a - d } }
                val total = expendLitres?.let { "${fmt2(it)} л" } ?: "—"
                val cells = mutableListOf("Топливо", accept, deliv, total)
                if (hasNormaDiesel && singleDieselSection) {
                    cells.add(normaDieselVal?.let { "${fmt2(it)} л" } ?: "")
                    cells.add(normaDieselVal?.let { n -> "${fmt2((expendLitres ?: 0.0) - n)} л" } ?: "")
                } else if (hasNormaDiesel) {
                    cells.add(""); cells.add("")
                }
                out += bodyRow(cells, wSec)
                sec.coefficient?.let { coeff ->
                    val expendKg = expendLitres?.let { it * coeff }
                    if (expendKg != null && expendKg > 0.0) {
                        val acceptKg = sec.acceptedFuel?.let { "${fmt1(it * coeff)} кг" } ?: "—"
                        val delivKg = sec.deliveryFuel?.let { "${fmt1(it * coeff)} кг" } ?: "—"
                        val resKg = "${fmt1(expendKg)} кг"
                        val kgCells = mutableListOf("", acceptKg, delivKg, resKg)
                        if (hasNormaDiesel) {
                            kgCells.add(""); kgCells.add("")
                        }
                        out += bodyRow(kgCells, wSec)
                    }
                }
            }
            val showDieselTotals = dieselSectionsWithData.size > 1 || (dieselSectionsWithData.size == 1 && !hasNormaDiesel)
            if (showDieselTotals) {
                val totalExpend = loco.dieselSectionList.sumOf { sec ->
                    (sec.acceptedFuel ?: 0.0) - (sec.deliveryFuel ?: 0.0)
                }
                if (totalExpend > 0.0) {
                    val itHeader = if (hasNormaDiesel && dieselSectionsWithData.size > 1)
                        listOf("Итого", "", "", "", "Норма", "Результат")
                    else
                        listOf("Итого", "", "", "")
                    out += smallHeaderRow(itHeader, wSec, gapBefore = 2.0)
                    val cells = mutableListOf("Топливо", "", "", "${fmt2(totalExpend)} л")
                    if (hasNormaDiesel && dieselSectionsWithData.size > 1) {
                        cells.add(normaDieselVal?.let { "${fmt2(it)} л" } ?: "")
                        cells.add(normaDieselVal?.let { n -> "${fmt2(totalExpend - n)} л" } ?: "")
                    }
                    out += bodyRow(cells, wSec)
                }
            }
        }
        return out
    }

    // ─── График (§21 «Раздел «График»») ─────────────────────────────────────

    private fun buildSchedule(
        routes: List<Route>,
        monthLabel: String,
        calendarDays: List<Day>,
        workScheduleProfile: WorkScheduleProfile,
        m: PdfMetrics,
    ): List<PdfBlock> {
        val out = mutableListOf<PdfBlock>()
        out += PdfTextBlock("График за $monthLabel", PdfTextStyle.TITLE, gapBefore = 2.0)
        out += PdfSeparatorBlock()

        val parsed = parseMonthYear(monthLabel) ?: run {
            out += PdfTextBlock("Не удалось определить период из «$monthLabel».", PdfTextStyle.BODY)
            return out
        }
        val (month, year) = parsed

        val tz = TimeZone.of("GMT+3") // BUG-59 (паритет с Android): фиксированный GMT+3
        val firstDow = firstDow(year, month)
        val daysInMonth = daysInMonth(year, month)
        val monthStartMs = dayStartMs(year, month, 1, tz)
        val nextMonthStartMs = dayStartMs(
            if (month == 11) year + 1 else year,
            if (month == 11) 0 else month + 1,
            1, tz,
        )

        val dayPassenger = BooleanArray(daysInMonth + 1)
        val dayEntries = Array(daysInMonth + 1) { mutableListOf<String>() }
        val dayPassDurations = Array(daysInMonth + 1) { mutableListOf<String>() }
        val dayRelease = Array<ReleaseType?>(daysInMonth + 1) { null }
        calendarDays.forEach { day ->
            if (day.isReleaseDay && day.dayOfMonth in 1..daysInMonth) {
                dayRelease[day.dayOfMonth] = day.releaseType
            }
        }

        data class WorkInterval(val startFrac: Double, val endFrac: Double)
        val dayWorkIntervals = Array(daysInMonth + 1) { mutableListOf<WorkInterval>() }

        routes.forEach { route ->
            val start = route.basicData.timeStartWork ?: return@forEach
            val endRaw = route.basicData.timeEndWork

            if (endRaw != null) {
                for (day in 1..daysInMonth) {
                    val dayStart = dayStartMs(year, month, day, tz)
                    val dayEnd = dayStart + 86_400_000L
                    val clippedStart = maxOf(start, dayStart)
                    val clippedEnd = minOf(endRaw, dayEnd)
                    if (clippedEnd > clippedStart) {
                        val startFrac = (clippedStart - dayStart).toDouble() / 86_400_000.0
                        val endFrac = (clippedEnd - dayStart).toDouble() / 86_400_000.0
                        dayWorkIntervals[day].add(
                            WorkInterval(startFrac.coerceIn(0.0, 1.0), endFrac.coerceIn(0.0, 1.0)),
                        )
                    }
                }
            }

            val startCal = local(start, tz)
            val startMonth = startCal.monthNumber - 1
            val startYear = startCal.year
            if (startMonth == month && startYear == year) {
                val d = startCal.dayOfMonth
                if (d in 1..daysInMonth) {
                    dayEntries[d].add(fmtTime(start).orEmpty())
                }
            }

            if (endRaw != null) {
                val endCal = local(endRaw, tz)
                if ((endCal.monthNumber - 1) == month && endCal.year == year &&
                    (startMonth != month || startYear != year)
                ) {
                    val endDay = endCal.dayOfMonth.coerceIn(1, daysInMonth)
                    dayEntries[endDay].add("← ${fmtTime(endRaw)}")
                }
            }

            route.passengers.forEach { p ->
                val ps = p.timeDeparture?.takeIf { it > 0L } ?: return@forEach
                val pe = p.timeArrival?.takeIf { it > 0L } ?: (ps + 3_600_000L)
                for (day in 1..daysInMonth) {
                    val dayStart = dayStartMs(year, month, day, tz)
                    val dayEnd = dayStart + 86_400_000L
                    if (ps < dayEnd && pe > dayStart) dayPassenger[day] = true
                }
                val psCal = local(ps, tz)
                if ((psCal.monthNumber - 1) == month && psCal.year == year) {
                    val d = psCal.dayOfMonth
                    if (d in 1..daysInMonth) {
                        val dur = fmtDur(pe - ps) ?: ""
                        if (dur.isNotBlank()) dayPassDurations[d].add(dur)
                    }
                }
            }
        }

        val cells = mutableListOf<PdfScheduleDayCell>()
        for (day in 1..daysInMonth) {
            val fills = mutableListOf<PdfCellFill>()
            dayWorkIntervals[day].forEach {
                fills += PdfCellFill(it.startFrac, it.endFrac, ARGb_WORK_FILL)
            }
            if (dayRelease[day] != null) fills += PdfCellFill(0.0, 1.0, ARGb_RELEASE_FILL)
            if (dayPassenger[day] && dayEntries[day].isEmpty()) {
                fills += PdfCellFill(0.0, 1.0, ARGb_PASSENGER_FILL)
            }
            val entries = dayEntries[day].toList()
            val releaseLabel = if (entries.isEmpty()) {
                dayRelease[day]?.let { rt ->
                    when (rt) {
                        is ReleaseType.Vacation -> "Отпуск"
                        is ReleaseType.SickLeave -> "Больн."
                        is ReleaseType.Courses -> "Курсы"
                        is ReleaseType.Donor -> "Донор"
                        is ReleaseType.ChildCare -> "Уход"
                        is ReleaseType.BusinessTrip -> "Команд."
                        is ReleaseType.TechnicalStudy -> "Техзан."
                        else -> "Отвл."
                    }
                }
            } else null
            val passengerDuration = if (entries.isEmpty() && releaseLabel == null) {
                dayPassDurations[day].firstOrNull()
            } else null
            cells += PdfScheduleDayCell(
                dayOfMonth = day,
                fills = fills,
                entries = entries,
                releaseLabel = releaseLabel,
                passengerDuration = passengerDuration,
            )
        }
        out += PdfScheduleGridBlock(firstColumnIndex = firstDow, cells = cells)

        // Сводка под сеткой (§21).
        val totalWorkMs = routes.sumOf { r ->
            val s = r.basicData.timeStartWork ?: return@sumOf 0L
            val e = r.basicData.timeEndWork ?: return@sumOf 0L
            val sCal = local(s, tz)
            val eCal = local(e, tz)
            val isTransition = (sCal.monthNumber - 1) != month || sCal.year != year ||
                (eCal.monthNumber - 1) != month || eCal.year != year
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

        val calendarDaysValid = calendarDays.validFor(year, month)
        val standardNormaHours = calendarDaysValid.sumOf { day ->
            workScheduleProfile.effectiveHours(
                LocalDate(year, month + 1, day.dayOfMonth),
                day.tag,
            )
        }
        val detachmentHours = calendarDaysValid.filter {
            it.isReleaseDay &&
                it.releaseType != ReleaseType.DayOff &&
                it.releaseType != ReleaseType.TechnicalStudy
        }.sumOf { day ->
            workScheduleProfile.effectiveHours(
                LocalDate(year, month + 1, day.dayOfMonth),
                day.tag,
            )
        }
        val personalNormaHours = standardNormaHours - detachmentHours

        if (standardNormaHours > 0) out += summaryRow("Норма на месяц", "$standardNormaHours ч.")
        if (personalNormaHours > 0 && personalNormaHours != standardNormaHours) {
            out += summaryRow("Личная норма", "$personalNormaHours ч.")
        }
        out += summaryRow("Маршрутов в месяце", "${routes.size}")
        out += summaryRow("Отработано в месяце", fmtDur(totalWorkMs) ?: "—")
        if (totalPassMs > 0) out += summaryRow("Следование пассажиром", fmtDur(totalPassMs) ?: "—")
        if (detachmentHours > 0) out += summaryRow("Отвлечения", "$detachmentHours ч.")
        val detachmentMs = detachmentHours * 3_600_000L
        if (totalWorkMs > 0 || detachmentMs > 0) {
            out += summaryRow("Всего", fmtDur(totalWorkMs + detachmentMs) ?: "—")
        }
        out += PdfTextBlock("", PdfTextStyle.BODY, gapBefore = 4.0)
        out += PdfSeparatorBlock()
        return out
    }

    private fun calcBreakOverlap(route: Route, from: Long, to: Long): Long {
        val bs = route.basicData.timeStartBreak?.takeIf { it > 0L } ?: return 0L
        val be = route.basicData.timeEndBreak?.takeIf { it > 0L } ?: return 0L
        if (be <= bs) return 0L
        val bFrom = maxOf(bs, from)
        val bTo = minOf(be, to)
        return if (bTo > bFrom) bTo - bFrom else 0L
    }

    private fun summaryRow(label: String, value: String): PdfRowBlock = PdfRowBlock(
        cells = listOf(
            PdfCell(label, PdfTextStyle.SMALL),
            PdfCell(value, PdfTextStyle.BODY),
        ),
        weights = listOf(0.38, 0.62),
        rowHeight = 14.0,
    )

    // ─── Вспомогательные строки ─────────────────────────────────────────────

    private fun smallHeaderRow(texts: List<String>, weights: List<Double>, gapBefore: Double = 0.0): PdfRowBlock =
        PdfRowBlock(
            cells = texts.map { PdfCell(it, PdfTextStyle.SMALL) },
            weights = weights,
            fillRow = true,
            gapBefore = gapBefore,
        )

    private fun bodyRow(texts: List<String>, weights: List<Double>): PdfRowBlock =
        PdfRowBlock(
            cells = texts.map { PdfCell(it, PdfTextStyle.BODY_BOLD) },
            weights = weights,
        )

    // ─── Форматирование (порт TimeManager/ConverterLongToTime из core_android) ──

    private fun local(ms: Long, tz: TimeZone) =
        Instant.fromEpochMilliseconds(ms).toLocalDateTime(tz)

    private fun two(v: Int) = v.toString().padStart(2, '0')

    /** BUG-59 (паритет с Android): время в фиксированном GMT+3. */
    internal fun fmtTime(ms: Long?): String? = ms?.takeIf { it > 0L }?.let {
        local(it, TimeZone.of("GMT+3")).let { d -> "${two(d.hour)}:${two(d.minute)}" }
    }

    /** Спецификация §21: `dd.MM.yyyy`. */
    internal fun fmtDate(ms: Long?): String? = ms?.takeIf { it > 0L }?.let {
        local(it, TimeZone.of("GMT+3")).let { d -> "${two(d.dayOfMonth)}.${two(d.monthNumber)}.${d.year}" }
    }

    internal fun fmtDT(ms: Long?): String? = ms?.takeIf { it > 0L }?.let {
        "${fmtDate(it)} ${fmtTime(it)}"
    }

    /** `ЧЧ:ММ` (`ConverterLongToTime.getTimeInStringFormat`), часы без ограничения 24. */
    internal fun fmtDur(ms: Long?): String? = ms?.let {
        if (it < 0) "00:00" else {
            val totalMinutes = it / 60_000
            "${two((totalMinutes / 60).toInt())}:${two((totalMinutes % 60).toInt())}"
        }
    }

    private fun fmt1(v: Double): String {
        // Ручной формат вместо "%.1f".format — String.format недоступен в common.
        val tenths = kotlin.math.round(v * 10).toLong()
        return "${tenths / 10},${kotlin.math.abs(tenths % 10)}"
    }

    private fun fmt2(v: Double): String {
        val hundredths = kotlin.math.round(v * 100).toLong()
        val frac = kotlin.math.abs(hundredths % 100)
        return "${hundredths / 100},${if (frac < 10) "0$frac" else "$frac"}"
    }

    // ─── Разбор метки месяца ────────────────────────────────────────────────

    private val russianMonthsLower = mapOf(
        "январь" to 0, "февраль" to 1, "март" to 2, "апрель" to 3,
        "май" to 4, "июнь" to 5, "июль" to 6, "август" to 7,
        "сентябрь" to 8, "октябрь" to 9, "ноябрь" to 10, "декабрь" to 11,
    )

    /** «Апрель 2026» → (3, 2026); иначе null (§21: «Не удалось определить период из «…»»). */
    internal fun parseMonthYear(monthLabel: String): Pair<Int, Int>? {
        val parts = monthLabel.trim().split(" ").filter { it.isNotBlank() }
        if (parts.size < 2) return null
        val month = russianMonthsLower[parts[0].lowercase()] ?: return null
        val year = parts[1].toIntOrNull() ?: return null
        return Pair(month, year)
    }

    /** 0 = Пн … 6 = Вс. */
    private fun firstDow(year: Int, month: Int): Int {
        val d = LocalDate(year, month + 1, 1).dayOfWeek.ordinal // 0 = Monday
        return d
    }

    private fun daysInMonth(year: Int, month: Int): Int {
        val start = LocalDate(year, month + 1, 1).toEpochDays()
        val (nextYear, nextMonth) = if (month == 11) year + 1 to 1 else year to (month + 2)
        val end = LocalDate(nextYear, nextMonth, 1).toEpochDays()
        return (end - start).toInt()
    }

    private fun dayStartMs(year: Int, month: Int, day: Int, tz: TimeZone): Long =
        LocalDate(year, month + 1, day).atStartOfDayIn(tz).toEpochMilliseconds()

    // Цвета заливок сетки (§21; те же ARGB, что в Android PdfGenerator).
    private const val ARGb_WORK_FILL = 0x462196F3L    // argb(70, 33, 150, 243)
    private const val ARGb_RELEASE_FILL = 0x3C9C27B0L // argb(60, 156, 39, 176)
    private const val ARGb_PASSENGER_FILL = 0x46FF9800L // argb(70, 255, 152, 0)
}
