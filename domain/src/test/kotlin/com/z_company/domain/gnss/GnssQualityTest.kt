package com.z_company.domain.gnss

import org.junit.Test
import kotlin.test.assertEquals

class GnssQualityTest {
    private fun raw(age: Long?, acc: Float? = 5f, sats: Int? = 8, enabled: Boolean = true) =
        GnssQualityRules.rawLevel(age, acc, sats, enabled)

    @Test
    fun `raw levels follow the rules`() {
        assertEquals(GnssLevel.GOOD, raw(1_000))
        assertEquals(GnssLevel.GOOD, raw(5_000, 20f, 4))
        assertEquals(GnssLevel.WEAK, raw(1_000, 35f))
        assertEquals(GnssLevel.WEAK, raw(1_000, 5f, 3))
        assertEquals(GnssLevel.WEAK, raw(10_000))
        assertEquals(GnssLevel.WEAK, raw(1_000, 80f))
        assertEquals(GnssLevel.LOST, raw(15_001))
        assertEquals(GnssLevel.UNKNOWN, raw(null))
        assertEquals(GnssLevel.LOST, raw(1_000, enabled = false))
        assertEquals(GnssLevel.GOOD, raw(1_000, 5f, null)) // статус спутников недоступен
    }

    private fun GnssQualityEvaluator.at(nowSec: Int, fixSec: Int?, acc: Float? = 5f, sats: Int? = 8) =
        update(nowSec * 1000L, 0L, fixSec?.let { it * 1000L }, acc, sats, sats).level

    @Test
    fun `unknown until first fix then lost`() {
        val e = GnssQualityEvaluator()
        assertEquals(GnssLevel.UNKNOWN, e.at(5, null))
        assertEquals(GnssLevel.LOST, e.at(16, null))
    }

    @Test
    fun `downgrade to weak needs hold and does not flicker`() {
        val e = GnssQualityEvaluator()
        assertEquals(GnssLevel.GOOD, e.at(1, 1))
        assertEquals(GnssLevel.GOOD, e.at(2, 2, acc = 30f)) // одна плохая секунда - значок не меняется
        assertEquals(GnssLevel.GOOD, e.at(3, 3, acc = 5f)) // снова хорошо - кандидат сброшен
        assertEquals(GnssLevel.GOOD, e.at(4, 4, acc = 30f))
        assertEquals(GnssLevel.GOOD, e.at(6, 6, acc = 30f))
        assertEquals(GnssLevel.WEAK, e.at(7, 7, acc = 30f)) // 3 с подряд
    }

    @Test
    fun `lost is immediate and upgrade is delayed`() {
        val e = GnssQualityEvaluator()
        assertEquals(GnssLevel.GOOD, e.at(1, 1))
        assertEquals(GnssLevel.LOST, e.at(17, 1))
        assertEquals(GnssLevel.LOST, e.at(100, 100)) // fix вернулся
        assertEquals(GnssLevel.LOST, e.at(104, 104))
        assertEquals(GnssLevel.GOOD, e.at(105, 105)) // 5 с стабильно
    }

    @Test
    fun `usable fix`() {
        assertEquals(true, GnssQualityRules.isUsableFix(1_000, 10f))
        assertEquals(false, GnssQualityRules.isUsableFix(20_000, 10f))
        assertEquals(false, GnssQualityRules.isUsableFix(1_000, 60f))
    }

    @Test
    fun `tooltip texts`() {
        assertEquals(
            "GPS: хороший сигнал, 9 спутников, точность ±4 м",
            GnssQualityText.tooltip(GnssQuality(GnssLevel.GOOD, 9, 12, 3.2f, 500)),
        )
        assertEquals(
            "GPS: слабый сигнал, 3 спутника, точность ±35 м",
            GnssQualityText.tooltip(GnssQuality(GnssLevel.WEAK, 3, 5, 35f, 500)),
        )
        assertEquals(
            "Нет сигнала GPS — записи стоянок и отправлений приостановлены",
            GnssQualityText.tooltip(GnssQuality(GnssLevel.LOST)),
        )
        assertEquals("GPS: хороший сигнал, 21 спутник", GnssQualityText.tooltip(GnssQuality(GnssLevel.GOOD, 21)))
    }
}
