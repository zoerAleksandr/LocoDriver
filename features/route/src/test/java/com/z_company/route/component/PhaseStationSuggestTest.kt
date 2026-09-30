package com.z_company.route.component

import com.z_company.route.component.servicephase.matchStart
import com.z_company.route.component.servicephase.suggest
import org.junit.Assert.assertEquals
import org.junit.Test

/** Подсказки названия в шторке станции плеча — на кириллице. */
class PhaseStationSuggestTest {

    private val list = listOf("Лужская", "Луга I", "Гатчина-Варшавская", "Толмачёво", "Санкт-Петербург-Товарный-Витебский", "Мга")

    @Test
    fun `prefix of any word, case insensitive`() {
        assertEquals(listOf("Лужская", "Луга I"), suggest(list, "лу"))
        assertEquals(listOf("Гатчина-Варшавская"), suggest(list, "варш"))
        assertEquals(listOf("Санкт-Петербург-Товарный-Витебский"), suggest(list, "Витеб"))
    }

    @Test
    fun `middle of a word does not match`() {
        assertEquals(emptyList<String>(), suggest(list, "ская"))
    }

    @Test
    fun `yo and e are the same`() {
        assertEquals(listOf("Толмачёво"), suggest(list, "толмачев"))
        assertEquals(0, matchStart("Толмачево", "ТОЛМАЧЁ"))
    }

    @Test
    fun `exact name is not suggested again`() {
        assertEquals(emptyList<String>(), suggest(list, "мга"))
        assertEquals(emptyList<String>(), suggest(list, "Толмачево"))
    }

    @Test
    fun `match position is kept for highlighting`() {
        assertEquals(8, matchStart("Гатчина-Варшавская", "Вар"))
    }
}
