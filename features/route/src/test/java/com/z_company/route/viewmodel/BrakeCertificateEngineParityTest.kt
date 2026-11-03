package com.z_company.route.viewmodel

import com.z_company.domain.brake.BrakeCertificateEngine
import com.z_company.domain.brake.BrakeNorms
import com.z_company.domain.brake.CompositeShare
import com.z_company.domain.brake.DensityLocoGroup
import com.z_company.domain.brake.PressureFieldState
import com.z_company.domain.brake.SlopeStep
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Вычисления состояния экрана Android (`BrakeCertificateUiState`) и общий движок (`BrakeCertificateEngine`, его же
 * использует PWA через Kotlin/JS-мост) обязаны давать одно и то же. Случайные, в том числе «грязные» вводы с фиксированным зерном.
 */
class BrakeCertificateEngineParityTest {
    private val numbers = listOf("", " ", "0", "-1", "7", "7,5", "7.5", "14", "3,5", "1e3", "abc", "12 5", "0,0", "4000", "4000,5", "300", "28", "∞", "NaN")
    private val series = listOf("", "ВЛ80С", "ВЛ10", "ВЛ10У", "2ЭС6", "ЭП1", "неизвестная")

    private fun rnd(r: Random) = numbers[r.nextInt(numbers.size)]

    private fun randomState(r: Random): BrakeCertificateUiState {
        val cats = BrakeNorms.categories
        val rows = List(r.nextInt(0, 5)) { BrakeRowUi(id = it, pressureText = rnd(r), axlesText = rnd(r)) }
        return BrakeCertificateUiState(
            categoryId = if (r.nextInt(10) == 0) "UNKNOWN" else cats[r.nextInt(cats.size)].id,
            slope = SlopeStep.values()[r.nextInt(SlopeStep.values().size)],
            autoblock3 = r.nextBoolean(),
            manualOverride = if (r.nextBoolean()) null else BrakeNorms.ManualBrakeNorm.values()[r.nextInt(BrakeNorms.ManualBrakeNorm.values().size)],
            weightText = rnd(r), totalAxlesText = rnd(r), avgPressureText = rnd(r), rowsMode = r.nextBoolean(), rows = rows,
            locoEnabled = r.nextBoolean(), locoWeightText = rnd(r), locoAxlesText = rnd(r), locoPressureText = rnd(r),
            compositeShare = CompositeShare.values()[r.nextInt(CompositeShare.values().size)],
            manualHaveText = rnd(r),
            densityGroup = if (r.nextBoolean()) null else DensityLocoGroup.values()[r.nextInt(DensityLocoGroup.values().size)],
            customVolumeText = listOf("", "1450", "1500,5", "999", "abc", "3500")[r.nextInt(6)],
            locoSeriesText = series[r.nextInt(series.size)], locoNumberText = listOf("", "5", "19", "1234А", "ВЛ10-18")[r.nextInt(5)],
        )
    }

    @Test
    fun engineMatchesUiStateOnRandomStates() {
        val r = Random(20261021)
        repeat(3000) {
            val ui = randomState(r)
            val d = ui.toData()
            assertEquals("input #$it $ui", ui.toInput(), BrakeCertificateEngine.input(d))
            assertEquals("group #$it", ui.effectiveDensityGroup(), BrakeCertificateEngine.effectiveDensityGroup(d))
            assertEquals(
                "rows #$it",
                ui.effectiveRows().map { it.pressureText to it.axlesText },
                BrakeCertificateEngine.effectiveRows(d).map { it.pressure to it.axles },
            )
            val field = when (val p = ui.pressureField()) {
                PressureField.Empty -> PressureFieldState.Empty
                PressureField.Multiple -> PressureFieldState.Multiple
                is PressureField.Single -> PressureFieldState.Single(p.text)
            }
            assertEquals("pressureField #$it", field, BrakeCertificateEngine.pressureField(d))
            assertEquals("grade #$it", ui.grade, d.slope.grade)
        }
    }
}
