package com.z_company.domain.brake

/**
 * Сверка суммы осей в таблице нажатий с «Всего осей» вверху справки. «Всего осей» в бланке - число осей состава (поезда);
 * оси локомотива в сравнении не участвуют (локомотив считается отдельной строкой подсчёта, п. 41 прил. 2 Правил ТО тормозного оборудования).
 * Расчёт всегда идёт по осям таблицы - сверка только предупреждает, что расчёт неполный или расходится.
 */
sealed interface AxleSumCheck {
    /** Таблица не заполнена (нечего сверять). */
    data object NoTable : AxleSumCheck

    /** «Всего осей» вверху пусто, в таблице [tableSum] осей - можно взять сумму из таблицы. */
    data class DeclaredEmpty(val tableSum: Int) : AxleSumCheck

    data class Equal(val total: Int) : AxleSumCheck

    /** В таблице меньше, чем указано вверху: не хватает [diff]. */
    data class TableLess(val tableSum: Int, val declared: Int, val diff: Int) : AxleSumCheck

    /** В таблице больше, чем указано вверху: больше на [diff]. */
    data class TableMore(val tableSum: Int, val declared: Int, val diff: Int) : AxleSumCheck
}

object AxleCheck {
    /**
     * Сверка с учётом локомотива: обе стороны - с осями локомотива ([locoAxles] > 0, если он учитывается) или обе без; так разница не включает оси локомотива
     * ни с одной стороны (раньше таблица считалась без локомотива, а «текущие данные» с ним).
     */
    fun compareWithLoco(declared: Int?, tableSum: Int, locoAxles: Int): AxleSumCheck {
        val l = locoAxles.coerceAtLeast(0)
        return compare(declared?.takeIf { it > 0 }?.let { it + l }, if (tableSum > 0) tableSum + l else tableSum)
    }

    fun compare(declared: Int?, tableSum: Int): AxleSumCheck = when {
        tableSum <= 0 -> AxleSumCheck.NoTable
        declared == null || declared <= 0 -> AxleSumCheck.DeclaredEmpty(tableSum)
        tableSum == declared -> AxleSumCheck.Equal(tableSum)
        tableSum < declared -> AxleSumCheck.TableLess(tableSum, declared, declared - tableSum)
        else -> AxleSumCheck.TableMore(tableSum, declared, tableSum - declared)
    }

    /** Короткий текст предупреждения; null - сходится или сверять нечего. */
    fun message(check: AxleSumCheck): String? = when (check) {
        is AxleSumCheck.TableLess -> "В справке указано осей: ${check.tableSum}, текущие данные: ${check.declared} - не хватает ${check.diff}. Расчёт неполный."
        is AxleSumCheck.TableMore -> "В справке указано осей: ${check.tableSum}, текущие данные: ${check.declared} - больше на ${check.diff}. Проверьте данные на лицевой стороне справки."
        is AxleSumCheck.DeclaredEmpty -> "«Всего осей» не указано, в справке указано осей: ${check.tableSum}."
        else -> null
    }
}
