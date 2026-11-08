package com.z_company.iosapp.viewmodel

import com.z_company.domain.entities.salary.PayrollPaymentCatalog
import com.z_company.domain.entities.salary.SalaryPaymentId
import com.z_company.domain.util.str
import com.z_company.domain.util.str2decimalSign

/**
 * Модели экрана «Расчёт зарплаты» (§11) для Swift.
 * Все строки приходят уже отформатированными: Swift только рисует.
 */

/** Строка таблицы начислений/удержаний (§11.6). `null` в часах/проценте → «—» бледным. */
data class SalaryStatementRow(
    val code: String,
    val title: String,
    val hoursText: String?,
    val percentText: String?,
    val moneyText: String,
)

/** Итоговый снимок расчётного листа месяца (§11.6). */
data class SalaryStatement(
    val monthLabel: String = "",
    val monthIndex: Int = 0,
    val year: Int = 0,
    val currency: String = "₽",
    val toBeCreditedText: String = "0,00",
    val totalWorkText: String = "00:00",
    val isDecimalTime: Boolean = false,
    /** Строка тарифа для карточки-предупреждения: «{ставка} ₽» либо «{старая} / {новая} ₽». */
    val tariffRateText: String = "",
    /** BUG-38 (паритет с Android): при смене тарифа проверяется первое число строки. */
    val tariffMissing: Boolean = false,
    val accrualRows: List<SalaryStatementRow> = emptyList(),
    val deductionRows: List<SalaryStatementRow> = emptyList(),
    val totalChargedText: String = "0,00",
    val totalRetentionText: String = "0,00",
    val showUnderworkInfo: Boolean = false,
)

/** Пара «год, месяц» производственного календаря для стрелок и шторки месяца (§11.7). */
data class MonthYearItem(
    val year: Int,
    val month: Int,
    val label: String,
)

// ── Форматирование (§11.6 «Форматы») ──────────────────────────────────────

private val MONTH_NAMES = listOf(
    "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
    "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь",
)

fun salaryMonthLabel(month: Int, year: Int): String =
    "${MONTH_NAMES.getOrElse(month) { "?" }} $year"

/** Часы: `ЧЧ:ММ` (минуты отбрасываются вниз) либо десятичный `Ч,ДД`. */
fun formatSalaryHours(ms: Long, isDecimal: Boolean): String {
    if (isDecimal) {
        val hours = ms / 3_600_000.0
        val rounded = kotlin.math.floor(hours * 100 + 0.5) / 100
        val intPart = rounded.toLong()
        val frac = kotlin.math.round((rounded - intPart) * 100).toInt()
        return "$intPart,${frac.toString().padStart(2, '0')}"
    }
    val totalMinutes = ms / 60_000L
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return "${hours}:${minutes.toString().padStart(2, '0')}"
}

/** Процент: `40,0` («%.1f» с запятой). */
fun formatSalaryPercent(value: Double): String {
    val rounded = kotlin.math.floor(value * 10 + 0.5) / 10
    val intPart = rounded.toLong()
    val frac = kotlin.math.round((rounded - intPart) * 10).toInt()
    return "$intPart,$frac"
}

// ── Строки расчётного листа (buildAccrualRows / buildDeductionRows) ───────
// Порядок и состав повторяют Android `SalaryCalculationScreen.kt`: экран и PDF
// используют один упорядоченный набор; собственные перечни не допускаются.

internal data class RawSalaryRow(
    val paymentId: SalaryPaymentId,
    val title: String,
    val hours: Long?,
    val percent: Double?,
    val money: Double?,
)

/** Сортировка как в бумажном листе: по первому коду, строки без кода — в конце. */
private fun <T> List<T>.sortedByPayrollCode(paymentId: (T) -> SalaryPaymentId): List<T> =
    sortedWith(
        compareBy<T> { PayrollPaymentCatalog[paymentId(it)].codes.firstOrNull() == null }
            .thenBy { PayrollPaymentCatalog[paymentId(it)].codes.firstOrNull().orEmpty() }
    )

private fun RawSalaryRow.toStatementRow(isDecimalTime: Boolean): SalaryStatementRow {
    val payment = PayrollPaymentCatalog[paymentId]
    return SalaryStatementRow(
        code = payment.codeLabel,
        title = title,
        hoursText = hours?.let { formatSalaryHours(it, isDecimalTime) },
        percentText = percent?.let { formatSalaryPercent(it) },
        moneyText = (money ?: 0.0).str2decimalSign(),
    )
}

/**
 * Строки начислений: только суммы > 0 (сравнение до округления), затем
 * сортировка по коду. [raw] собирается в исходном порядке §11.6.
 */
internal fun buildSalaryAccrualRows(raw: List<RawSalaryRow>, isDecimalTime: Boolean): List<SalaryStatementRow> =
    raw.filter { it.money != null && it.money > 0 }
        .sortedByPayrollCode { it.paymentId }
        .map { it.toStatementRow(isDecimalTime) }

/** Строки удержаний: НДФЛ, Профсоюз, Благосостояние, Алименты, Прочие — сортировка по коду. */
fun buildSalaryDeductionRows(
    ndfl: Double?,
    union: Double?,
    welfare: Double?,
    alimony: Double?,
    other: Double?,
): List<SalaryStatementRow> = listOfNotNull(
    ndfl?.takeIf { it > 0 }?.let {
        // BUG-39 (паритет с Android): в столбце «%» у НДФЛ всегда 13,0,
        // даже если в настройках задан другой процент.
        RawSalaryRow(SalaryPaymentId.NDFL, "НДФЛ", null, 13.0, it)
    },
    union?.takeIf { it > 0 }?.let { RawSalaryRow(SalaryPaymentId.UNION, "Профсоюз", null, null, it) },
    welfare?.takeIf { it > 0 }?.let { RawSalaryRow(SalaryPaymentId.WELFARE, "Благосостояние", null, null, it) },
    alimony?.takeIf { it > 0 }?.let { RawSalaryRow(SalaryPaymentId.ALIMONY, "Алименты", null, null, it) },
    other?.takeIf { it > 0 }?.let { RawSalaryRow(SalaryPaymentId.OTHER_DEDUCTION, "Прочие удержания", null, null, it) },
).sortedByPayrollCode { it.paymentId }
    .map { it.toStatementRow(isDecimalTime = false) }

/** Процент для строки: значение модели без хвостовых нулей (для ввода настроек). */
fun displaySettingValue(value: Double?): String = value.str()

// ── Фабрики пустых моделей (ObjC-мост не видит значения по умолчанию) ─────

fun emptySalaryStatement(): SalaryStatement = SalaryStatement()
fun emptySettingSalaryState(): SettingSalaryState = SettingSalaryState()
fun emptySalaryUi(): SalaryUi = SalaryUi()
