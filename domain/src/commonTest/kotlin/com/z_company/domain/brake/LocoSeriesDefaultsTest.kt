package com.z_company.domain.brake

import com.z_company.domain.brake.LocoSeriesDefaults.withDefaults
import com.z_company.domain.entities.norma_time.LocomotiveSeries
import com.z_company.domain.entities.route.LocoType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LocoSeriesDefaultsTest {
    @Test
    fun sectionsByLeadingDigit() {
        assertEquals(2, LocoSeriesDefaults.sectionCountFor("2ЭС6"))
        assertEquals(3, LocoSeriesDefaults.sectionCountFor("3ЭС4К"))
        assertEquals(4, LocoSeriesDefaults.sectionCountFor("4ТЭ10С"))
        assertEquals(2, LocoSeriesDefaults.sectionCountFor("2ТЭ10У"))
        assertEquals(2, LocoSeriesDefaults.sectionCountFor(" 2М62У "))
    }

    @Test
    fun sectionsWithoutLeadingDigitIsOne() {
        listOf("ЭП1М", "ВЛ80С", "ВЛ10", "ЧС7", "ТЭМ2", "ЧМЭ3", "ЭП2К", "ЧС200", "ТГ102", "", "  ").forEach {
            assertEquals(1, LocoSeriesDefaults.sectionCountFor(it), it)
        }
        assertEquals(1, LocoSeriesDefaults.sectionCountFor(null))
        assertEquals(1, LocoSeriesDefaults.sectionCountFor("0ЭС"))
    }

    @Test
    fun catalogSeriesNamesStartingWithDigitAreAllRealSectionCounts() {
        // Все серии каталога с первой цифрой - число секций (2ЭС5К, 2ТЭП60, 2ТЭ116 ...), исключений нет.
        val withDigit = BrakeLocoCatalog.items.map { it.series }.filter { it.first().isDigit() }
        assertTrue(withDigit.isNotEmpty())
        withDigit.forEach { assertEquals(it.first() - '0', LocoSeriesDefaults.sectionCountFor(it), it) }
    }

    @Test
    fun confirmedSeriesArePrefilled() {
        val d = LocoSeriesDefaults.forSeries("ВЛ80С")
        assertEquals(192.0, d.weightT)
        assertEquals(8, d.axleCount)
        assertEquals(14.0, d.axleLoadTf)
        assertEquals(1800, d.brakeVolumeL)
        assertEquals(1, d.sectionCount)
        val es5 = LocoSeriesDefaults.forSeries("2ЭС5К")
        assertEquals(2000, es5.brakeVolumeL)
        assertEquals(192.0, es5.weightT)
        assertEquals(8, es5.axleCount)
        assertEquals(2, es5.sectionCount)
    }

    @Test
    fun esSeriesGetPressureVolumeAxlesButNotReferenceWeight() {
        listOf(Triple("2ЭС4К", 2000, 8), Triple("2ЭС6", 2000, 8), Triple("3ЭС4К", 3000, 12), Triple("3ЭС5К", 3000, 12)).forEach { (name, liters, axles) ->
            val d = LocoSeriesDefaults.forSeries(name)
            assertEquals(14.0, d.axleLoadTf, name)
            assertEquals(liters, d.brakeVolumeL, name)
            assertEquals(axles, d.axleCount, name)
            assertNull(d.weightT, name)
        }
    }

    @Test
    fun doubtfulVolumesAreNotPrefilled() {
        // выведено по допущениям / диапазон / зависит от номера / нет данных
        listOf("2ТЭП60", "2М62У", "3ТЭ10М", "4ТЭ10С", "ЧС2", "ЧС3", "ЧМЭ3Т", "ТЭМ2А", "ТЭМ2УМ", "ВЛ10", "ВЛ11К", "ТЭМ16", "2ТЭ25К", "ВЛ60", "2ТЭ10В", "неизвестная")
            .forEach { assertNull(LocoSeriesDefaults.forSeries(it).brakeVolumeL, it) }
        // но вес, оси и нажатие из таблиц 4-5 есть
        assertEquals(123.0, LocoSeriesDefaults.forSeries("ТЭМ16").weightT)
        assertEquals(4, LocoSeriesDefaults.forSeries("4ТЭ10С").sectionCount)
        assertNotNull(LocoSeriesDefaults.forSeries("4ТЭ10С").axleCount)
    }

    @Test
    fun unknownSeriesGetsOnlySections() {
        val d = LocoSeriesDefaults.forSeries("5Икс")
        assertEquals(SeriesBrakeDefaults(sectionCount = 5), d)
    }

    @Test
    fun withDefaultsFillsOnlyEmptyFields() {
        val s = LocomotiveSeries(name = "2ЭС6", type = LocoType.ELECTRIC, brakeVolumeL = 2100, axleLoadTf = null)
        val r = s.withDefaults()
        assertEquals(2100, r.brakeVolumeL) // пользовательское не перезаписывается
        assertEquals(14.0, r.axleLoadTf)
        assertEquals(2, r.sectionCount)
        assertNull(r.weightT)
        assertTrue(LocoSeriesDefaults.needsDefaults(s))
        assertTrue(!LocoSeriesDefaults.needsDefaults(r))
    }
}

class LocoPassportTest {
    private val now = 1_000L

    @Test
    fun userRecordBeatsCatalogPerField() {
        val user = LocomotiveSeries(name = "ВЛ80С", type = LocoType.ELECTRIC, weightT = 200.0, axleLoadTf = null, brakeVolumeL = 1900, sectionCount = 1)
        val p = LocoSeriesDefaults.passportFor("ВЛ80С", user)
        assertEquals(200.0, p.weightT) // пользовательское
        assertEquals(8, p.axleCount) // пусто у пользователя -> каталог
        assertEquals(14.0, p.axleLoadTf)
        assertEquals(1900, p.brakeVolumeL)
    }

    @Test
    fun catalogFillsWhenNoRecordAndEmptyWhenUnknown() {
        val p = LocoSeriesDefaults.passportFor("ЭП1", null)
        assertEquals(135.0, p.weightT)
        assertEquals(1020, p.brakeVolumeL)
        assertEquals(LocoPassport(), LocoSeriesDefaults.passportFor("Неизвестная", null))
    }

    @Test
    fun referenceEsWeightStaysInVu45HintWithNote() {
        val p = LocoSeriesDefaults.passportFor("3ЭС4К", null)
        assertEquals(288.0, p.weightT)
        assertEquals(3000, p.brakeVolumeL)
        assertNotNull(p.note)
    }

    @Test
    fun editCreatesRecordWithDefaultsForOthers() {
        val r = LocoSeriesDefaults.recordWithEdits(
            "2ЭС6", null, LocoPassport(weightT = 200.0), setOf(SeriesPassportField.WEIGHT), now,
        )!!
        assertEquals(200.0, r.weightT)
        assertEquals(14.0, r.axleLoadTf) // по умолчанию
        assertEquals(2, r.sectionCount)
        assertEquals(LocoType.ELECTRIC, r.type)
        assertEquals(now, r.updatedAt)
    }

    @Test
    fun editOverwritesOnlyEditedFieldsOfExistingRecord() {
        val ex = LocomotiveSeries(name = "ВЛ80С", type = LocoType.ELECTRIC, weightT = 192.0, axleCount = 8, axleLoadTf = 14.0, brakeVolumeL = 1800, sectionCount = 1, updatedAt = 5)
        val r = LocoSeriesDefaults.recordWithEdits(
            "ВЛ80С", ex, LocoPassport(axleLoadTf = 15.5, weightT = 999.0), setOf(SeriesPassportField.AXLE_LOAD), now,
        )!!
        assertEquals(15.5, r.axleLoadTf)
        assertEquals(192.0, r.weightT) // не редактировалось
        assertEquals(now, r.updatedAt)
    }

    @Test
    fun clearingFieldWritesNull() {
        val ex = LocomotiveSeries(name = "ВЛ80С", type = LocoType.ELECTRIC, weightT = 192.0, sectionCount = 1)
        val r = LocoSeriesDefaults.recordWithEdits("ВЛ80С", ex, LocoPassport(), setOf(SeriesPassportField.WEIGHT), now)!!
        assertNull(r.weightT)
    }

    @Test
    fun noWriteWhenNothingChanged() {
        // без записи: значение совпало с каталогом - запись не создаётся
        assertNull(LocoSeriesDefaults.recordWithEdits("ЭП1", null, LocoPassport(weightT = 135.0), setOf(SeriesPassportField.WEIGHT), now))
        // с записью: то же значение - updatedAt не растёт
        val ex = LocomotiveSeries(name = "ЭП1", type = LocoType.ELECTRIC, weightT = 135.0, sectionCount = 1)
        assertNull(LocoSeriesDefaults.recordWithEdits("ЭП1", ex, LocoPassport(weightT = 135.0), setOf(SeriesPassportField.WEIGHT), now))
        assertNull(LocoSeriesDefaults.recordWithEdits("  ", null, LocoPassport(weightT = 1.0), setOf(SeriesPassportField.WEIGHT), now))
    }

    @Test
    fun guessType() {
        listOf("2ТЭ10У", "ТЭМ2", "ЧМЭ3", "2М62У", "ТЭП70", "ТГ102").forEach { assertEquals(LocoType.DIESEL, LocoSeriesDefaults.guessType(it), it) }
        listOf("ВЛ80С", "ЭП1М", "2ЭС6", "ЧС7", "ВЛ10", "ЭП2К").forEach { assertEquals(LocoType.ELECTRIC, LocoSeriesDefaults.guessType(it), it) }
    }
}
