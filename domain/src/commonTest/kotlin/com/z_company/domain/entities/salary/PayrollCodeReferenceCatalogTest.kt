package com.z_company.domain.entities.salary

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PayrollCodeReferenceCatalogTest {

    @Test
    fun catalog_contains_every_verified_row() {
        val entries = PayrollCodeReferenceCatalog.entries

        assertEquals(285, entries.size)
        assertEquals(218, entries.count { it.type == PayrollPaymentType.ACCRUAL })
        assertEquals(67, entries.count { it.type == PayrollPaymentType.DEDUCTION })
        assertTrue(entries.all { it.source.isNotBlank() })
        assertTrue(entries.all { it.code.matches(Regex("[0-9A-Z]+")) })
        assertTrue(entries.all { it.shortName.isNotBlank() && it.description.isNotBlank() })
    }

    @Test
    fun blank_query_returns_whole_catalog() {
        assertEquals(PayrollCodeReferenceCatalog.entries, PayrollCodeReferenceCatalog.search("  "))
    }

    @Test
    fun search_matches_each_supported_parameter_and_is_case_insensitive() {
        assertEquals(listOf("501Z"), PayrollCodeReferenceCatalog.search("501z").map { it.code })
        assertEquals(listOf("152P"), PayrollCodeReferenceCatalog.search("152Р").map { it.code })
        assertEquals(listOf("365P"), PayrollCodeReferenceCatalog.search("премзасодейстизобррацион").map { it.code })
        assertTrue(PayrollCodeReferenceCatalog.search("стоимости бытового топлива").any { it.code == "501Z" })
        assertEquals(67, PayrollCodeReferenceCatalog.search("удержание").size)
        assertTrue(PayrollCodeReferenceCatalog.search("0342").isEmpty())
    }

    @Test
    fun search_requires_all_terms_even_when_they_come_from_different_parameters() {
        val crossFieldResults = PayrollCodeReferenceCatalog.search("501z бытового топлива")
        assertEquals(listOf("501Z"), crossFieldResults.map { it.code })
        assertTrue(PayrollCodeReferenceCatalog.search("несуществующий код").isEmpty())
    }

    @Test
    fun ambiguous_code_keeps_both_verified_meanings() {
        val results = PayrollCodeReferenceCatalog.search("035L")

        assertEquals(2, results.size)
        assertEquals(setOf("0339", "0340"), results.map { it.source }.toSet())
    }

    @Test
    fun entries_are_sorted_by_type_then_numeric_code() {
        val entries = PayrollCodeReferenceCatalog.entries
        val firstDeduction = entries.indexOfFirst { it.type == PayrollPaymentType.DEDUCTION }

        assertEquals(218, firstDeduction)
        assertTrue(entries.take(firstDeduction).all { it.type == PayrollPaymentType.ACCRUAL })
        assertTrue(entries.drop(firstDeduction).all { it.type == PayrollPaymentType.DEDUCTION })
        listOf(entries.take(firstDeduction), entries.drop(firstDeduction)).forEach { group ->
            val numericParts = group.map {
                it.code.takeWhile(Char::isDigit).toIntOrNull() ?: Int.MAX_VALUE
            }
            assertEquals(numericParts.sorted(), numericParts)
        }
    }
}
