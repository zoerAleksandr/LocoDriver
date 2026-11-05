package com.z_company.iosapp.pdf

/*
 * Модель вёрстки PDF (SCREEN_SPECS §21) для iOS.
 *
 * Android рисует PDF через Canvas/PdfDocument (features/route/.../util/PdfGenerator.kt) —
 * эти API недоступны на iOS. Поэтому всё СОДЕРЖАНИЕ (блоки, тексты, числа, шрифты,
 * высоты строк, оценки высоты групп, разрывы разделов) собирается здесь, в чистом
 * Kotlin, а пиксельный рендер выполняет Swift (UIGraphicsPDFRenderer) по этой модели
 * («рендер нативным UIKit, вёрстка по модели §21»).
 *
 * Именование: избегаем экспортируемых `new`/`init`/`copy` и C-ключевых слов
 * (Kotlin/Native экспортирует `copy` как `doCopy`, init — конструктором).
 */

/** Геометрия страницы и стили шрифтов (§21 «Страница»). */
class PdfMetrics(
    /** A4 в пунктах. */
    val pageWidth: Double = 595.0,
    val pageHeight: Double = 842.0,
    val marginSide: Double = 36.0,
    val marginTop: Double = 52.0,
    val marginBottom: Double = 40.0,
    val titleFontSize: Double = 13.0,
    val sectionFontSize: Double = 10.0,
    val bodyFontSize: Double = 9.0,
    val smallFontSize: Double = 7.0,
    val tinyFontSize: Double = 7.5,
    val appHeaderFontSize: Double = 7.0,
    /** Высота строки таблицы (§21: «прямоугольные ячейки с рамкой»). */
    val tableRowHeight: Double = 14.0,
    val scheduleCellHeight: Double = 48.0,
    val scheduleHeaderHeight: Double = 18.0,
    /** Ячейки расчётного листка (§21 «Расчётный листок»). */
    val statementRowMinHeight: Double = 18.0,
    val statementLineHeight: Double = 9.0,
) {
    val contentWidth: Double get() = pageWidth - marginSide * 2
}

/** Режим текста (§21: заголовок 13 жирный, подзаголовок 10 жирный, текст 9, мелкий 7–7.5). */
enum class PdfTextStyle {
    TITLE,
    SECTION,
    BODY,
    BODY_BOLD,
    SMALL,
    TINY,
}

/** Ячейка строки таблицы. [alignRight] — текст у правого края (суммы/проценты). */
class PdfCell(
    val text: String,
    val style: PdfTextStyle = PdfTextStyle.BODY,
    val alignRight: Boolean = false,
)

/**
 * Строка таблицы с N ячейками (все табличные строки «Поездков», шапки секций,
 * сводка «Графика»). Доли ширин — от contentWidth. Текст, не влезающий в ячейку,
 * обрезается посимвольно на рендере (§21 «Страница»).
 */
class PdfRowBlock(
    val cells: List<PdfCell>,
    val weights: List<Double>,
    val fillRow: Boolean = false,
    val rowHeight: Double = 14.0,
    /** >0 — оценка высоты группы (маршрут целиком): не разрывать между страницами, если помещается. */
    val keepGroupHeight: Double = 0.0,
    /** Отступ сверху перед строкой (как `pm.y += n` в Android). */
    val gapBefore: Double = 0.0,
) : PdfBlock()

/** Абзац с переносом слов (примечания, флаги, пустые состояния). */
class PdfTextBlock(
    val text: String,
    val style: PdfTextStyle = PdfTextStyle.BODY,
    val indent: Double = 0.0,
    val keepGroupHeight: Double = 0.0,
    val gapBefore: Double = 0.0,
) : PdfBlock()

/** Заголовок раздела с заливкой фона («Маршрут №…», «Секция N», …). */
class PdfSectionTitleBlock(
    val text: String,
    val gapBefore: Double = 14.0,
) : PdfBlock()

/** Тонкий разделитель (§21: «с линией»). */
class PdfSeparatorBlock : PdfBlock()

/** Разрыв страницы перед следующим блоком; [drawAppHeader] — колонтитул §21. */
class PdfNewPageBlock(
    val drawAppHeader: Boolean = true,
) : PdfBlock()

/**
 * Группа блоков «маршрут целиком»: рендерер начинает группу с новой страницы,
 * если оценка высоты не помещается (§21: «маршрут не разрывается между страницами,
 * если помещается (оценка высоты)»).
 */
class PdfBlockGroup(
    val estimatedHeight: Double,
    val children: List<PdfBlock>,
) : PdfBlock()

/** Базовый тип блока вёрстки. */
open class PdfBlock

// ─── График (§21 «Раздел «График»») ─────────────────────────────────────────

/** Прямоугольная заливка внутри ячейки сетки (доля ширины ячейки 0..1). */
class PdfCellFill(
    val startFraction: Double,
    val endFraction: Double,
    /** ARGB как Long: рендерер разбирает каналы (совместимо и с Android BUG-59 — цвета те же). */
    val argb: Long,
)

/** Ячейка календарной сетки (высота 48, §21). */
class PdfScheduleDayCell(
    val dayOfMonth: Int,
    /**
     * Заливки ячейки в порядке отрисовки: пропорциональные доли времени работы
     * (голубая), затем отвлечение (фиолетовая, вся ячейка) и «только пассажиром»
     * (оранжевая). Цвета (ARGB) задаёт сборщик — рендерер их не знает.
     */
    val fills: List<PdfCellFill> = emptyList(),
    /** Строки времени явок (крупно по центру; несколько — столбиком). */
    val entries: List<String> = emptyList(),
    /** Подпись отвлечения («Отпуск», «Больн.», …) — только когда нет явок. */
    val releaseLabel: String? = null,
    /** Длительность следования пассажиром — только когда нет явок. */
    val passengerDuration: String? = null,
)

/** Календарная сетка 7 колонок (Пн…Вс). */
class PdfScheduleGridBlock(
    /** 0 = Пн … 6 = Вс — колонка первого дня месяца. */
    val firstColumnIndex: Int,
    val cells: List<PdfScheduleDayCell>,
) : PdfBlock()

// ─── Расчётный листок (§21 «Раздел «Расчётный листок»») ────────────────────

/** Строка начисления/удержания листка (все значения уже отформатированы). */
class PdfStatementRowData(
    val code: String,
    val name: String,
    /** Часы десятичные (ч); пусто — не печатается (§21: «нулевые часы и проценты не печатаются»). */
    val hours: String,
    val percent: String,
    val amount: String,
)

/**
 * Чёрно-белая форма ФТУ-69 целиком: рендерер сам разбивает строки по страницам
 * и повторяет шапку с «(продолжение)» (§21) — границы страниц зависят от метрик
 * шрифта рендерера, поэтому механика разбиения остаётся на рендере, содержимое — здесь.
 */
class PdfSalaryStatementBlock(
    /** «Расчётный листок за апрель месяц 2026 года». */
    val title: String,
    val tariffText: String,
    val normaText: String,
    val totalWorkText: String,
    val currencyText: String,
    /** Значение колонки «Месяц» в строках (имя месяца в нижнем регистре). */
    val monthCell: String,
    val accruals: List<PdfStatementRowData>,
    val deductions: List<PdfStatementRowData>,
    val totalChargedText: String,
    val totalRetentionText: String,
    val totalHoursText: String,
    val toBeCreditedText: String,
    val footerText: String = "Сформировано в приложении «Машинист»",
) : PdfBlock()

// ─── Документ ────────────────────────────────────────────────────────────────

/** Выбор разделов шторки «Содержание PDF» (§21 сценарий п.1). */
class PdfSectionsModel(
    val includeRouteDetails: Boolean = true,
    val includeSchedule: Boolean = true,
    val includeSalary: Boolean = true,
)

/** Полный документ: упорядоченный поток блоков; страницы рендерер нарезает сам. */
class PdfLayoutDocument(
    val metrics: PdfMetrics,
    val blocks: List<PdfBlock>,
    /** Имя файла вида `Машинист_Апрель_2026.pdf` (§21 «Файл»). */
    val fileName: String,
)
