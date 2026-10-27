package com.z_company.domain.util_test

import com.z_company.domain.util.StationListCleanup
import kotlin.test.Test
import kotlin.test.assertEquals

class StationListCleanupTest {
    @Test
    fun removesFragmentsOfLongerNames() {
        val list = listOf("Zqx", "Zq", "Z", "Луга", "лу", "М", "Мга")
        assertEquals(listOf("Zqx", "Луга", "Мга"), StationListCleanup.removeFragments(list, emptyList()))
    }

    @Test
    fun keepsProtectedNamesEvenIfPrefixOfAnother() {
        val list = listOf("Луга", "Луга-2", "лу")
        assertEquals(listOf("Луга", "Луга-2"), StationListCleanup.removeFragments(list, listOf("луга")))
    }

    @Test
    fun keepsUnrelatedShortNamesAndDuplicatesOrder() {
        val list = listOf("50 км", "Мга", "Кириши")
        assertEquals(list, StationListCleanup.removeFragments(list, emptyList()))
    }
}
