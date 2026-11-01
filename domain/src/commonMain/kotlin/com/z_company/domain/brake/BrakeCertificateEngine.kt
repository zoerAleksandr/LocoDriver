package com.z_company.domain.brake

import com.z_company.domain.util.toFiniteDoubleOrNull

/** Состояние поля «Нажатие на ось» на «Расчёте» по таблице справки. */
sealed interface PressureFieldState {
    /** Таблица пуста (или нет полностью заполненных строк): обычное поле ввода. */
    data object Empty : PressureFieldState

    /** Одно значение нажатия на всех заполненных строках: показывается в поле, правка меняет все эти строки. */
    data class Single(val text: String) : PressureFieldState

    /** Две и более разных значения: «см. в справке», поле недоступно для ввода. */
    data object Multiple : PressureFieldState
}

/**
 * Производные величины справки из [BrakeCertificateData] - общие для Android и PWA (через Kotlin/JS-мост).
 * Android-экран держит те же вычисления в `BrakeCertificateUiState` (features/route); их совпадение с этим объектом
 * проверяет `BrakeCertificateEngineParityTest`.
 */
object BrakeCertificateEngine {
    private fun AxleRowData.filled(): Boolean = (pressure.toFiniteDoubleOrNull() ?: 0.0) > 0 && (axles.trim().toIntOrNull() ?: 0) > 0

    /** Вход калькулятора; null - данных недостаточно (нет веса или категории). */
    fun input(d: BrakeCertificateData, requireWeight: Boolean = true): BrakeCertificateInput? {
        val cat = BrakeNorms.category(d.categoryId) ?: return null
        // Для оборота (изменение состава) вес лицевой не обязателен: число осей считается и без него.
        val weight = d.weight.toFiniteDoubleOrNull()?.takeIf { it > 0 } ?: if (requireWeight) return null else 0.0
        val axleRows = if (d.rowsMode) d.rows.mapNotNull { r ->
            val p = r.pressure.toFiniteDoubleOrNull() ?: return@mapNotNull null
            val n = r.axles.trim().toIntOrNull() ?: return@mapNotNull null
            if (p <= 0 || n <= 0) null else AxleRow(p, n)
        } else {
            val p = d.avgPressure.toFiniteDoubleOrNull()
            val n = d.totalAxles.trim().toIntOrNull()
            if (p != null && n != null && p > 0 && n > 0) listOf(AxleRow(p, n)) else emptyList()
        }
        val loco = if (d.locoEnabled) {
            val w = d.locoWeight.toFiniteDoubleOrNull()
            val a = d.locoAxles.trim().toIntOrNull()
            val p = d.locoPressure.toFiniteDoubleOrNull()
            if (w != null && a != null && p != null && w > 0 && a > 0 && p > 0) LocoBrakeInput(w, a, p) else null
        } else null
        return BrakeCertificateInput(
            category = cat,
            compositionWeight = weight,
            rows = axleRows,
            loco = loco,
            grade = d.slope.grade,
            slope = d.slope,
            autoblock3 = d.autoblock3,
            compositeShare = d.compositeShare,
            manualNorm = d.manualNorm,
            actualManualAxles = d.manualHave.trim().toIntOrNull(),
            declaredAxles = if (d.rowsMode) d.totalAxles.trim().toIntOrNull() else null,
            enteredAxles = d.totalAxles.trim().toIntOrNull(),
        )
    }

    fun calculate(d: BrakeCertificateData): BrakeCertificateResult? = input(d)?.let { BrakeCertificateCalculator.calculate(it) }

    /** Строки таблицы нажатий «как в бланке»: в режиме таблицы - введённые полностью строки, иначе одна строка из среднего нажатия. */
    fun effectiveRows(d: BrakeCertificateData): List<AxleRowData> =
        if (d.rowsMode) d.rows.filter { it.filled() }
        else {
            val p = d.avgPressure.toFiniteDoubleOrNull()
            val n = d.totalAxles.trim().toIntOrNull()
            if (p != null && p > 0 && n != null && n > 0) listOf(AxleRowData(d.avgPressure, d.totalAxles)) else emptyList()
        }

    fun pressureField(d: BrakeCertificateData): PressureFieldState {
        if (!d.rowsMode) return PressureFieldState.Empty
        val filled = d.rows.filter { it.filled() }
        val distinct = filled.map { it.pressure.toFiniteDoubleOrNull() }.distinct()
        return when {
            filled.isEmpty() -> PressureFieldState.Empty
            distinct.size == 1 -> PressureFieldState.Single(filled.first().pressure)
            else -> PressureFieldState.Multiple
        }
    }

    /** Итоговая группа плотности: свой объём ГР (прим. 4 к табл. 4 п. 9.3.3) > выбранная вручную > автоподбор по серии и номеру локомотива. */
    fun effectiveDensityGroup(d: BrakeCertificateData): DensityLocoGroup? =
        if (d.customVolume.isNotBlank()) (BrakeDensity.groupByVolume(d.customVolume) as? BrakeDensity.VolumeGroup.Found)?.group
        else d.densityGroup ?: BrakeDensity.groupForLoco(d.locoSeries, d.locoNumber)
}
