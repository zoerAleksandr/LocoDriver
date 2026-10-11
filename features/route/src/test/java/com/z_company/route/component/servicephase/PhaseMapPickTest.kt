package com.z_company.route.component.servicephase

import com.z_company.domain.util.PointStation
import com.z_company.domain.util.PointStationResult
import com.z_company.domain.util.PointStationSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Кнопки и диалоги карты редактора плеча (SCREEN_SPECS §6.6). */
class PhaseMapPickTest {
    private fun st(name: String, d: Int = 120) = PointStation(name, 60.0, 30.0, d, PointStationSource.OSM)

    @Test
    fun `both stations empty - first and last`() {
        val b = PhaseMapPick.buttons("", "  ")
        assertEquals(listOf("Указать станцию отправления", "Указать станцию прибытия"), b.map { it.label })
        assertEquals(listOf(true, false), b.map { it.isStart })
    }

    @Test
    fun `one station empty - departure or arrival wording`() {
        assertEquals(
            listOf("Указать станцию отправления", "Уточнить на карте прибытие"),
            PhaseMapPick.buttons("", "Ультрамар").map { it.label },
        )
        assertEquals(
            listOf("Уточнить на карте отправление", "Указать станцию прибытия"),
            PhaseMapPick.buttons("Луга", "").map { it.label },
        )
    }

    @Test
    fun `both stations named - refine`() {
        assertEquals(
            listOf("Уточнить на карте отправление", "Уточнить на карте прибытие"),
            PhaseMapPick.buttons("Луга", "Ультрамар").map { it.label },
        )
    }

    @Test
    fun `buttons are always enabled even without coordinates`() {
        for (dep in listOf("", "Луга")) for (arr in listOf("", "Ультрамар")) {
            assertTrue(PhaseMapPick.buttons(dep, arr).all { it.enabled })
        }
    }

    @Test
    fun `measure offered only when both stations are named and located`() {
        assertTrue(PhaseMapPick.canMeasure("Луга", "Ультрамар", true, true))
        assertFalse(PhaseMapPick.canMeasure("Луга", "Ультрамар", true, false))
        assertFalse(PhaseMapPick.canMeasure("Луга", "Ультрамар", false, true))
        assertFalse(PhaseMapPick.canMeasure("", "Ультрамар", true, true))
        assertFalse(PhaseMapPick.canMeasure("Луга", " ", true, true))
    }

    @Test
    fun `prompt names the station being picked`() {
        assertEquals("Нажмите на карте место станции отправления", PhaseMapPick.prompt(true))
        assertEquals("Нажмите на карте место станции прибытия", PhaseMapPick.prompt(false))
    }

    @Test
    fun `target - first tap departure, second arrival, whatever button`() {
        assertEquals(true, PhaseMapPick.target(null, "", ""))
        assertEquals(true, PhaseMapPick.target(false, "", " "))
        assertEquals(false, PhaseMapPick.target(null, "Луга", ""))
        assertEquals(true, PhaseMapPick.target(null, "", "Луга"))
        assertEquals(false, PhaseMapPick.target(false, "Луга", "Ультрамар"))
        assertEquals(true, PhaseMapPick.target(true, "Луга", "Ультрамар"))
        assertEquals(null, PhaseMapPick.target(null, "Луга", "Ультрамар"))
    }

    @Test
    fun `found station asks to apply with distance`() {
        val c = PhaseMapPick.confirm(PointStationResult(st("Ультрамар", 350), offline = false), "")
        c as PickConfirm.Found
        assertEquals("Станция «Ультрамар»?", c.title)
        assertEquals("", c.text)
        assertEquals("Применить", c.confirmText)
        assertEquals("Отмена", c.dismissText)
    }

    @Test
    fun `found station warns when it replaces another name`() {
        val c = PhaseMapPick.confirm(PointStationResult(st("Ультрамар"), offline = false), "Ультра")
        assertEquals("Название в поле «Ультра» будет заменено.", c.text)
        // то же название (без учёта регистра) - без предупреждения
        val same = PhaseMapPick.confirm(PointStationResult(st("Ультрамар"), offline = false), "ультрамар")
        assertEquals("", same.text)
    }

    @Test
    fun `offline note is not shown`() {
        val c = PhaseMapPick.confirm(PointStationResult(st("Луга"), offline = true), "")
        assertEquals("", c.text)
    }

    @Test
    fun `nothing nearby with typed name - offer to use this point`() {
        val c = PhaseMapPick.confirm(PointStationResult(null, offline = false), "Ультрамар")
        c as PickConfirm.UseTypedName
        assertEquals("Ультрамар", c.name)
        assertEquals("Применить", c.confirmText)
        assertTrue(c.text.contains("«Ультрамар»"))
    }

    @Test
    fun `nothing nearby and no name - only pick another place`() {
        val c = PhaseMapPick.confirm(PointStationResult(null, offline = true), " ")
        assertTrue(c is PickConfirm.NothingNearby)
        assertEquals("Выбрать другую", c.confirmText)
    }

    @Test
    fun `camera without stops - my location, then last station, then Russia`() {
        assertEquals(MapStart(59.0, 30.0, 12.0), PhaseMapPick.startWithoutStops(59.0 to 30.0, 55.0 to 37.0))
        assertEquals(MapStart(55.0, 37.0, 9.0), PhaseMapPick.startWithoutStops(null, 55.0 to 37.0))
        val russia = PhaseMapPick.startWithoutStops(null, null)
        assertTrue(russia.zoom < 4.0)
        assertTrue(russia.latitude in 50.0..70.0 && russia.longitude in 60.0..130.0)
    }

    @Test
    fun `geo pin description tells whether coordinates exist`() {
        assertEquals("Координаты станции есть. Открыть карточку станции", stationGeoPinDescription(true))
        assertEquals("Нет координат. Открыть карточку станции", stationGeoPinDescription(false))
    }
}
